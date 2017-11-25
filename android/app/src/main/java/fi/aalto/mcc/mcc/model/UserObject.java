package fi.aalto.mcc.mcc.model;

/**
 * Created by Matias on 25/11/2017.
 */



public class UserObject{

    public String name;
    public String email;
    public String group;

    private UserObject() {
        // Default constructor required for calls to DataSnapshot.getValue(UserObject.class)
    }

    public UserObject(String name, String email) {
        this.name = name;
        this.email = email;
        this.group = null;
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

}

