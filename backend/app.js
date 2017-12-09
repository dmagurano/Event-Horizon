const express = require('express')
, bodyParser = require('body-parser');
const app = express();
const Multer = require('multer');
const format = require('util').format;
const router = express.Router();

process.title = "mcc";
process.env.GOOGLE_APPLICATION_CREDENTIALS = "./mcc-fall-2017-g04-35ff1cc15d6d.json"
process.env.GCLOUD_PROJECT = "mcc-fall-2017-g04"

/* Image processing */
const sharp = require('sharp')
var vision = require('@google-cloud/vision');
// Instantiate a vision client
// Creates a client
var client = new vision.ImageAnnotatorClient();

/* Gcloud Storage */

const googleStorage = require('@google-cloud/storage');
var admin = require("firebase-admin");
var crypto = require('crypto');

const storage = googleStorage();
//   projectId: "mcc-fall-2017-g04",
//   keyFilename: "./mcc-fall-2017-g04-35ff1cc15d6d.json"
// });

// Google Cloud Storage Bucket
const bucket = storage.bucket("mcc-fall-2017-g04.appspot.com");

/* FIREBASE REALTIME DB CONFIG */

var serviceAccount = require("./mcc-fall-2017-g04-35ff1cc15d6d.json");

admin.initializeApp({
	credential: admin.credential.cert(serviceAccount),
	databaseURL: "https://mcc-fall-2017-g04.firebaseio.com/",
  storageBucket: "mcc-fall-2017-g04.appspot.com"
});

var db = admin.database();
var ref = db.ref("/");

/* IMAGE UPLOAD CONFIG */

// Multer is required to process file uploads and make them available via
// req.files.
const multer = Multer({
  storage: Multer.memoryStorage(),
  limits: {
    fileSize: 50 * 1024 * 1024 // no larger than 50mb, you can change as needed.
  }
});

/* FOR THE CLEANUP CRON JOB */

var tasksRoutes = require('./tasks');
app.use('/tasks', tasksRoutes);

//////////////////////////////

app.use(bodyParser.json());

app.post('/create', (req, res) => {
  if(req.body.idToken != undefined && req.body.groupName != undefined && req.body.expiration !=undefined){
    admin.auth().verifyIdToken(req.body.idToken)
    .then(function(decodedToken) {
      var uid = decodedToken.uid;
      //var uid = "ERUG65W6rHdWlwk5gTEABlU70vv2";
      // console.log("Token OK. UID: " + uid)
      console.log("Group creation: "+ req.body.groupName + " User: "+ uid)
      var timestamp = new Date(req.body.expiration).getTime()
      //console.log(timestamp)
      //Reference to groups 
      var groupsRef = ref.child("Groups");
      //Reference to user
      var userRef = ref.child("Users/" + uid)
      //ref for the created group
      var newGroupRef = groupsRef.push();
      newGroupRef.set({
        admin: uid,
        name: req.body.groupName,
        expiration_date: timestamp,
        single_use_token: null,
        members: {
          [uid]: true
        }
      });
      // Get unique id of the new group
      var newGroupId = newGroupRef.key;
      // generate one time token
      var token = crypto.randomBytes(64).toString('base64');
      //update value on db
      newGroupRef.update({
        single_use_token: token
      })
      //Update user's group field
      userRef.update({
        group: newGroupId
      })
      // send response to user
      res.json({
        groupId: newGroupId,
        oneTimeToken: token
      })
  }).catch(function(error) {
      res.send(error.message)
    });
} else {
    res.send("Missing Parameters")
}


});

app.post('/join', (req, res) => {
  if(req.body.idToken != undefined && req.body.groupId != undefined &&  req.body.groupToken != undefined){
    admin.auth().verifyIdToken(req.body.idToken)
    .then(function(decodedToken) {
      var uid = decodedToken.uid;
      // console.log("Token OK. UID: " + uid)
      //Reference to the group
      var groupRef = ref.child("Groups/" + req.body.groupId);
      var userRef = ref.child("Users/" + uid)
      console.log("Group joining: "+ req.body.groupId + " User: "+ uid)
      // retrieve initial data
      groupRef.once("value")
      .then(function (data) {
            var trueToken = data.val().single_use_token;
            // console.log(trueToken)
            // console.log(req.body.groupToken)
            if(trueToken === req.body.groupToken){
              //Update user 
              userRef.update({
                group: req.body.groupId
              })
              // Update group member list
              var membersRef = groupRef.child("members/");
              membersRef.update({
                [uid]: true
              })
              //Update join token
              var newToken = crypto.randomBytes(64).toString('base64');
              groupRef.update({
                single_use_token: newToken
              })
              res.send("Group succesfully joined")
            } else {
                res.status("400").send("The Token is not correct")
            }
      })
      .catch(function (errorObject) {
            console.log("The read failed: " + errorObject.code);
            res.status(500).send("Error");
      }) 
    }).catch(function(error) {
        res.status(500).send(error.message)
      });
  } else {
      res.status("400").send("Missing Parameters")
  }
});

app.post('/leave', (req, res) => {
  if(req.body.idToken != undefined && req.body.groupId != undefined){
    admin.auth().verifyIdToken(req.body.idToken)
    .then(function(decodedToken) {
      var uid = decodedToken.uid;
      // console.log("Token OK. UID: " + uid)
      //Reference to the group
      console.log("Group leaving/del: "+ req.body.groupId + " User: "+ uid)
      var groupRef = ref.child("Groups/" + req.body.groupId);
      var userRef = ref.child("Users/" + uid);
      //uid = "fjdskljflskj"
      // retrieve initial data
      groupRef.once("value")
      .then(function(data) {
        var groupAdmin = data.val().admin;
        if(groupAdmin === uid){
            groupRef.update({
                expiration_date: new Date().getTime()
            })
            userRef.update({
              group: null
            })
            res.send("Group Deleted")
        } else {
            var memberRef = groupRef.child("members/"+uid);
            memberRef.once("value")
            .then(function (member){
                //checking if user is member of the group
                if(member.val() != null){
                    memberRef.remove()
                    .then(function(){
                      userRef.update({
                          group: null
                      })
                      ref.child("Users/" + uid + "/name").once("value")
                      .then(function(username) {
                        /* sending push notification to device */
                        var payload = {
                          notification: {
                            title: "An user left the group",
                            body: "User "+username.val()+" left the group"
                          }
                        };
                        // Send a message to devices subscribed to the provided topic.
                        admin.messaging().sendToTopic(req.body.groupId, payload)
                          .then(function(response) {
                            // See the MessagingTopicResponse reference documentation for the
                            // contents of response.
                            console.log("Successfully sent message:", response);
                          })
                          .catch(function(error) {
                            console.log("Error sending message:", error);
                          });
                      })
                      res.send("User "+uid+" remove succeded")
                    })
                    .catch(function(error) {
                        res.status(500).send("Remove failed: " + error.message)
                    });
                } else {
                    //user not member
                    res.status(400).send("The user is not member of the group")
                }
            });
          }
        }).catch(function (errorObject) {
            console.log("The read failed: " + errorObject.code);
            res.status(500).send("Error")
        })
    }).catch(function(error) {
        res.status(500).send(error.message)
      });
  } else {
    res.status("400").send("Missing Parameters")
  }
});

/* IMAGE UPLOAD */ 

//TODO: check if the user belongs to the group

/**
 * Adding new file to the storage
 */
app.post('/upload', multer.single('file'), (req, res) => {
  //console.log('Upload Image');
  if(req.body.idToken != undefined && req.body.groupId != undefined){
  admin.auth().verifyIdToken(req.body.idToken)
    .then(function(decodedToken) {
      var uid = decodedToken.uid;
      console.log("Image upload to group: "+ req.body.groupId + " User: "+ uid)
      let file = req.file;
      if (file) {
        processAndUploadImage(file, req.body.groupId, uid).then((success) => {
          var payload = {
            notification: {
              title: "New image added",
              body: "A new image was added to your group"
            }
          };
          // Send a message to devices subscribed to the provided topic.
          admin.messaging().sendToTopic(req.body.groupId, payload)
            .then(function(response) {
              // See the MessagingTopicResponse reference documentation for the
              // contents of response.
              console.log("Successfully sent message:", response);
            })
            .catch(function(error) {
              console.log("Error sending message:", error);
            });
          res.status(200).send({
            status: 'success'
          });
        }).catch((error) => {
            console.error(error);
            res.status(500).send(error)
        });
      } else {
        res.status(400).send("No image was uploaded")
      }
    }).catch(function(error) {
        res.send(error.message)
    });
  } else {
      res.send("Missing Parameters")
  }
});


const processAndUploadImage = (file, groupId, uid) => {
  let prom = new Promise((resolve, reject) => {
    if (!file) {
      return reject('No image file');
    }

    if (file.mimetype != "image/jpeg") {
      return reject("Only JPG images allowed")
    }
    var fullres_url = null,
     highres_url = null,
     lowres_url = null,
     author_name = null;

    var userRef = ref.child("Users/" + uid);
    userRef.once('value')
    .then((data) => {
      author_name = data.val().name
    })
    .then(() => {
      return uploadToStorage(file, groupId, "fullres")
    })
    .then((url) => {
      fullres_url = url;
      return sharp(file.buffer).resize(1280).toBuffer()
    })
    .then((data) => {
      file.buffer = data
      return uploadToStorage(file, groupId, "highres")
    })
    .then((url) => {
      highres_url = url;
      return sharp(file.buffer).resize(640).toBuffer()
    })
    .then((data) => {
      file.buffer = data
      return uploadToStorage(file, groupId, "lowres")
    })
    .then((url) => {
      lowres_url = url;
      return detectFaces(file)
    })
    .then((faces) => {
      var people = false;
      if(faces > 0)
        people = true;
      var imgsRef = ref.child("Images/"+groupId);
      var newImgRef = imgsRef.push();
      newImgRef.set({
        author: uid,
        author_name: author_name,
        has_people: people,
        full_res_url: fullres_url,
        high_res_url: highres_url,
        low_res_url: lowres_url
      });
      resolve()
    })
    .catch((error) => {
      return reject(error)
    })
  });
  return prom;
}

const uploadToStorage = (file, groupId, resolution) => {
  let prom = new Promise((resolve, reject) => {
    let newFileName = `/${groupId}/${resolution}/${file.originalname}_${Date.now()}`;

    let fileUpload = bucket.file(newFileName);

    const blobStream = fileUpload.createWriteStream({
      metadata: {
        contentType: file.mimetype
      }
    });

    blobStream.on('error', (error) => {

      reject('Something is wrong! Unable to upload at the moment.');
    });

    blobStream.on('finish', () => {
      // The public URL can be used to directly access the file via HTTP.
      const url = format(`gs://${bucket.name}${newFileName}`);
      console.log(url);
      
      resolve(url);
    });
    blobStream.end(file.buffer);
  });
 return prom
}

/**
 * Uses the Vision API to detect faces in the given file.
 */
function detectFaces(file) {
  let prom = new Promise((resolve, reject) => {
    // Make a call to the Vision API to detect the faces
    //const request = {image: {source: {filename: inputFile}}};
    client
      .faceDetection(file.buffer)
      .then(results => {
        const faces = results[0].faceAnnotations;
        var numFaces = faces.length;
        console.log('Found ' + numFaces + (numFaces === 1 ? ' face' : ' faces'));
        resolve(numFaces);
      })
      .catch(err => {
        console.error('ERROR:', err);
        reject(err);
      });
  });
  return prom
}

// Start the server
const PORT = process.env.PORT || 8080;
app.listen(PORT, () => {
  console.log(`App listening on port ${PORT}`);
  console.log('Press Ctrl+C to quit.');
});
// [END app]
