package fi.aalto.mcc.mcc.model;

/**
 * Created by Matias on 25/11/2017.
 */



public class UserObject{

    public String name;
    public String email;
    public String photo;
    public String group;

    public String mAuthToken;
    public String mUid;


    private UserObject() {
        // Default constructor required for calls to DataSnapshot.getValue(UserObject.class)
        this.name = "Anonymous";
        this.email = "dev@null.com";
        this.group = "na";
        this.mAuthToken = "na";
    }

    public UserObject(String name, String email) {
        this.name = name;
        this.email = email;
        this.group = "na";
        this.mAuthToken = "na";
    }

    public String getName(){
        return name;
    }

    public void setName(String name) { this.name = name; }

    public String getEmail(){
        return email;
    }

    public String getGroup(){
        return group;
    }

    public void setGroup(String group) { this.group = group; }

    public String getAuthToken() {return mAuthToken;}

    public void setAuthToken(String token) { this.mAuthToken = token; }

    public String getUid() {return mUid;}

    public void setUid(String uid) { this.mUid = uid; }

    public String getAvatarImage() {return photo;}

    public void setAvatarImage(String photoUrl) {this.photo = photoUrl;}

}

