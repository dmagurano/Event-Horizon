var express = require('express');
var router = express.Router();

const googleStorage = require('@google-cloud/storage');
var admin = require("firebase-admin");
var crypto = require('crypto');

const storage = googleStorage();
// Google Cloud Storage Bucket
const bucket = storage.bucket("mcc-fall-2017-g04.appspot.com");

var db = admin.database();
var ref = db.ref("/");


router.route('/cleanup').get( (req, res) => {
	var groupsRef = ref.child("Groups");
	groupsRef.once("value", function(snapshot) {
	  snapshot.forEach(function(grp) {
	    //console.log("Key: " + data.key + " ,value: " + data.val().expiration_date);
	    if(grp.val().expiration_date < new Date().getTime()){
        groupsRef.child(grp.key).remove()
        .then(function() {
          var groupImagesRef = ref.child("Images/"+grp.key)
          groupImagesRef.once("value", function(snapshot){
            snapshot.forEach(function(img) {
              var fImgName = "/"+grp.key+"/fullres/"+(img.val().full_res_url.split("/")[5])
              var fImgRef = bucket.file(fImgName)
              // Delete the file
              fImgRef.delete().then(function() {
                // File deleted successfully
                var hImgName = "/"+grp.key+"/highres/"+(img.val().high_res_url.split("/")[5])
                var hImgRef = bucket.file(hImgName)
                return hImgRef.delete()
              })
              .then(function() {
                var lImgName = "/"+grp.key+"/lowres/"+(img.val().low_res_url.split("/")[5])
                var lImgRef = bucket.file(lImgName)
                return lImgRef.delete()
              })
              .then(function() {
                //Images deleted, remove firebase record
                return groupImagesRef.remove()
              })
              .then(function() {
                console.log("Group "+grp.key+" was expired and removed correctly")
                //res.send("Group "+grp.key+" was expired and removed correctly")
              })
              .catch(function(error) {
                console.log("Error deleting "+grp.key+": "+error)
              })
              .then(function() {
                res.send("Cleanup finished")
              })
            })
          })
          
        })
      }	
	  });
	});
});


module.exports = router;