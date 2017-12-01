const express = require('express')
, bodyParser = require('body-parser');
const app = express();
//const router = express.Router();

process.title = "mcc";

var admin = require("firebase-admin");
var crypto = require('crypto');

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

// Start the server
const PORT = process.env.PORT || 8080;
app.listen(PORT, () => {
  console.log(`App listening on port ${PORT}`);
  console.log('Press Ctrl+C to quit.');
});
// [END app]
