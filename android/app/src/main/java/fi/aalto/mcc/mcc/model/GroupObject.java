package fi.aalto.mcc.mcc.model;

import java.util.Map;

/**
 * Created by Matias on 25/11/2017.
 */

public class GroupObject{

    public String name;
    public Map<String, Object> members;
    public Map<String, Object> images;
    public String admin;
    public Long expiration_date;
    public String single_use_token;

    private GroupObject() {
        // Default constructor required for calls to DataSnapshot.getValue(UserObject.class)
    }

    public GroupObject(String name, String admin, Long expiration_date, String single_use_token, Map <String, Object> images, Map <String, Object> members) {
        this.name = name;
        this.admin = admin;
        this.expiration_date = expiration_date;
        this.single_use_token = single_use_token;
        this.images = images;
        this.members = members;
    }

    public String getName(){
        return name;
    }

    public String getAdmin(){
        return admin;
    }

    public Long getExpirationDate(){
        return expiration_date;
    }

    public String getSingleUseToken(){
        return single_use_token;
    }

    public Map getImages(){
        return images;
    }

    public Map getMembers(){
        return members;
    }


}
