const express = require('express')
, bodyParser = require('body-parser');
const app = express();
const Multer = require('multer');
const format = require('util').format;
//const router = express.Router();

process.title = "mcc";

const googleStorage = require('@google-cloud/storage');
var admin = require("firebase-admin");
var crypto = require('crypto');

/* FIREBASE REALTIME DB CONFIG */

var serviceAccount = require("./mcc-fall-2017-g04-35ff1cc15d6d.json");

admin.initializeApp({
	credential: admin.credential.cert(serviceAccount),
	databaseURL: "https://mcc-fall-2017-g04.firebaseio.com/"
});

// As an admin, the app has access to read and write all data, regardless of Security Rules
var db = admin.database();
var ref = db.ref("/");
ref.once("value", function(snapshot) {
  //console.log(snapshot.val());
});

/* IMAGE UPLOAD CONFIG */

// Multer is required to process file uploads and make them available via
// req.files.
const multer = Multer({
  storage: Multer.memoryStorage(),
  limits: {
    fileSize: 50 * 1024 * 1024 // no larger than 50mb, you can change as needed.
  }
});

const storage = googleStorage({
  projectId: "mcc-fall-2017-g04",
  keyFilename: "./mcc-fall-2017-g04-35ff1cc15d6d.json"
});

// Google Cloud Storage Bucket
const bucket = storage.bucket("mcc-fall-2017-g04.appspot.com");

app.use(bodyParser.json());

app.post('/create', (req, res) => {
  if(req.body.idToken != undefined && req.body.groupName != undefined && req.body.expiration !=undefined){
    // admin.auth().verifyIdToken(req.body.idToken)
    // .then(function(decodedToken) {
      // var uid = decodedToken.uid;
      var uid = "dshajhdsuia";
      // console.log("Token OK. UID: " + uid)
      var timestamp = new Date(req.body.expiration).getTime()
      //console.log(timestamp)
      //Reference to groups 
      var groupsRef = ref.child("Groups");
      //ref for the created group
      var newGroupRef = groupsRef.push();
      newGroupRef.set({
        admin: uid,
        name: req.body.groupName,
        expiration_date: timestamp,
        single_use_token: null,
        members: {
          [uid]: true
      },
      images: {
          "image1_ID": true
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
      // send response to user
      res.json({
        groupId: newGroupId,
        oneTimeToken: token
    })
  // }).catch(function(error) {
  //     res.send(error.message)
  // });
} else {
    res.send(req.body)
}


});

app.post('/join', (req, res) => {
  if(req.body.idToken != undefined && req.body.groupId != undefined &&  req.body.groupToken != undefined){
    // admin.auth().verifyIdToken(req.body.idToken)
    // .then(function(decodedToken) {
      // var uid = decodedToken.uid;
      // console.log("Token OK. UID: " + uid)
      //Reference to the group
      var groupRef = ref.child("Groups/" + req.body.groupId);
      // retrieve initial data
      groupRef.once("value")
      .then(function (data) {
            var trueToken = data.val().single_use_token;
            // console.log(trueToken)
            // console.log(req.body.groupToken)
            if(trueToken === req.body.groupToken){
                res.send("OK")
            } else {
                res.status("400").send("The Token is not correct")
            }
      })
      .catch(function (errorObject) {
            console.log("The read failed: " + errorObject.code);
            res.status(500).send("Error");
      }) 
    
        
  // }).catch(function(error) {
  //     res.send(error.message)
  // });
    } else {
        res.send(req.body)
    }
    });

app.post('/leave', (req, res) => {
  if(req.body.idToken != undefined && req.body.groupId != undefined){
    // admin.auth().verifyIdToken(req.body.idToken)
    // .then(function(decodedToken) {
      // var uid = decodedToken.uid;
      // console.log("Token OK. UID: " + uid)
      //Reference to the group
        var groupRef = ref.child("Groups/" + req.body.groupId);
        uid = "fjdskljflskj"
        // retrieve initial data
        groupRef.once("value")
        .then(function(data) {
            var groupAdmin = data.val().admin;
            if(groupAdmin === uid){
                groupRef.update({
                    expiration_date: new Date().getTime()
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


  // }).catch(function(error) {
  //     res.send(error.message)
  // });
} else {
    res.send(req.body)
}
});

/* IMAGE UPLOAD */ 

/**
 * Adding new file to the storage
 */
app.post('/upload', multer.single('file'), (req, res) => {
        console.log('Upload Image');
        if(req.body.idToken != undefined && req.body.groupId != undefined){
        // admin.auth().verifyIdToken(req.body.idToken)
        // .then(function(decodedToken) {

            let file = req.file;
            if (file) {
            uploadImageToStorage(file, req.body.groupId).then((success) => {
              res.status(200).send({
                status: 'success'
              });
            }).catch((error) => {
              console.error(error);
              res.status(500).send("Failure in uploading the image")
            });
            } else {
            res.status(400).send("No image was uploaded")
            }
        // }).catch(function(error) {
        //     res.send(error.message)
        // });
        } else {
            res.send(req.body)
        }
});

/**
 * Upload the image file to Google Storage
 * @param {File} file object that will be uploaded to Google Storage
 */
const uploadImageToStorage = (file, groupId) => {
  let prom = new Promise((resolve, reject) => {
    if (!file) {
      reject('No image file');
    }

    if (file.mimetype != "image/jpeg") {
        reject("Only JPG images allowed")
    }
    let newFileName = `/${groupId}/fullres/${file.originalname}_${Date.now()}`;

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
      const url = format(`https://storage.googleapis.com/${bucket.name}/${groupId}/fullres/${fileUpload.name}`);
      resolve(url);
      console.log(url);
    });

    blobStream.end(file.buffer);
  });
  return prom;
}

// Start the server
const PORT = process.env.PORT || 8080;
app.listen(PORT, () => {
  console.log(`App listening on port ${PORT}`);
  console.log('Press Ctrl+C to quit.');
});
// [END app]
