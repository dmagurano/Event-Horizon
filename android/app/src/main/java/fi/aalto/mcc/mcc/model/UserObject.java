package fi.aalto.mcc.mcc.model;

/**
 * Created by Matias on 25/11/2017.
 */



public class UserObject{

    public String name;
    public String email;
    public String group;
    public String authToken;

    private UserObject() {
        // Default constructor required for calls to DataSnapshot.getValue(UserObject.class)
        this.name = "Anonymous";
        this.email = "dev@null.com";
        this.group = "0";
        this.authToken = "0";
    }

    public UserObject(String name, String email) {
        this.name = name;
        this.email = email;
        this.group = "0";
        this.authToken = "0";
    }

    public String getName(){
        return name;
    }

    public String getEmail(){
        return email;
    }

    public String getGroup(){
        return group;
    }

    public String getAuthToken() {return authToken;}

}

