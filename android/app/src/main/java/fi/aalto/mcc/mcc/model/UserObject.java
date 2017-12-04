package fi.aalto.mcc.mcc.model;

/**
 * Created by Matias on 25/11/2017.
 */



public class UserObject{

    public String mScreenName;
    public String mEmail;
    public String mGroup;
    public String mAuthToken;
    public String mUid;
    public String mPhotoUrl;

    private UserObject() {
        // Default constructor required for calls to DataSnapshot.getValue(UserObject.class)
        this.mScreenName = "Anonymous";
        this.mEmail = "dev@null.com";
        this.mGroup = "na";
        this.mAuthToken = "na";
    }

    public UserObject(String name, String email) {
        this.mScreenName = name;
        this.mEmail = email;
        this.mGroup = "na";
        this.mAuthToken = "na";
    }

    public String getName(){
        return mScreenName;
    }

    public void setName(String name) { this.mScreenName = name; }

    public String getEmail(){
        return mEmail;
    }

    public String getGroup(){
        return mGroup;
    }

    public void setGroup(String group) { this.mGroup = group; }

    public String getAuthToken() {return mAuthToken;}

    public void setAuthToken(String token) { this.mAuthToken = token; }

    public String getUid() {return mUid;}

    public void setUid(String uid) { this.mUid = uid; }

    public String getAvatarImage() {return mPhotoUrl;}

    public void setAvatarImage(String photoUrl) {this.mPhotoUrl = photoUrl;}

}

