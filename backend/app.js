const express = require('express')
, bodyParser = require('body-parser');
const app = express();
//const router = express.Router();

process.title = "mcc";

var admin = require("firebase-admin");
var notp = require('notp');

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
    admin.auth().verifyIdToken(req.body.idToken)
    .then(function(decodedToken) {
      var uid = decodedToken.uid;
      console.log("Token OK. UID: " + uid)
      var timestamp = new Date(req.body.expiration).getTime()
      console.log(timestamp)
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
      token = notp.totp.gen(newGroupId, {})
      //update value on db
      newGroupRef.update({
        single_use_token: token
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
