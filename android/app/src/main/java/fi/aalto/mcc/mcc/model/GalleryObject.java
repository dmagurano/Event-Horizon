package fi.aalto.mcc.mcc.model;

import android.net.Uri;

import java.io.Serializable;
import java.util.HashMap;

/**
 * Created by user on 14/11/2017.
 */

public class GalleryObject implements Serializable
{

    private static final int VIEW_HEADER = 0;
    private static final int VIEW_NORMAL = 1;

    private String lowRes, highRes, fullRes;

    // metadata
    private String id;
    private String author_id;
    private String group_id;
    private String authorName;
    private String imageTimestamp;
    private String imageDescription;

    // object classification data
    private int type;
    private String header;

    private String category;

    public GalleryObject() {
        type = VIEW_NORMAL;
    }

    public GalleryObject(int type, String header) {
        this.type = type;
        this.header = header;
        this.authorName = "Unresolved author name";
    }

    public GalleryObject(Uri fileUri, String author)
    {
        this.type = VIEW_NORMAL;

        if(author == null || author.equals("")) this.authorName = "Unresolved author name";
        else this.authorName = author;

        this.category = "Not Human";
        if (fileUri != null) {
            this.setSmall(fileUri.toString());
            this.setLarge(fileUri.toString());
            this.setXL(fileUri.toString());
        }

    }

    public GalleryObject(String id, HashMap<String, Object> map) {
        type = VIEW_NORMAL;
        this.id = id;

        if (map.get("low_res_url") != null )    this.lowRes     = map.get("low_res_url").toString();
        if (map.get("high_res_url") != null )   this.highRes    = map.get("high_res_url").toString();
        if (map.get("full_res_url") != null )   this.fullRes    = map.get("full_res_url").toString();
        if (map.get("author") != null )         this.author_id  = map.get("author").toString();
        if (map.get("group") != null )          this.group_id   = map.get("group").toString();

        if (map.get("author_name") != null )
            this.authorName = map.get("author_name").toString();
        else
            this.authorName = "Unresolved author name";


        // try decoding category string if any exists
        if (map.get("category") !=null)
        {
            this.category = map.get("category").toString();
        }
        // else revert to old format
        else if ( map.get("has_people")!= null && map.get("has_people").toString() == "false")
             this.category = "Not Human";
        else this.category = "Human";

    }

    public void update(GalleryObject obj)
    {
        this.lowRes = obj.lowRes;
        this.highRes = obj.highRes;
        this.fullRes = obj.fullRes;

        this.id = obj.id;
        this.author_id = obj.author_id;
        this.group_id = obj.group_id;
        this.authorName = obj.authorName;
        this.imageTimestamp = obj.imageTimestamp;
        this.imageDescription = obj.imageDescription;

        this.type = obj.type;
        this.header = obj.header;
        this.category = obj.category;
    }


    public String getSmall() {
        if(lowRes != null)   return lowRes;
        if(highRes != null)  return highRes;
        if(fullRes != null)  return fullRes;

        return lowRes;
    }

    public void setSmall(String small) {
        this.lowRes = small;
    }

    public String getLarge() {
        if(highRes != null)  return highRes;
        if(lowRes != null)   return lowRes;
        if(fullRes != null)  return fullRes;

        return highRes;
    }

    public void setLarge(String large) {
        this.highRes = large;
    }

    public String getXL() {
        if(fullRes != null)  return fullRes;
        if(highRes != null)  return highRes;
        if(lowRes != null)   return lowRes;

        return fullRes;
    }

    public void setXL(String xl) {
        this.fullRes = xl;
    }

    public String getGroup() {
        return group_id;
    }

    public void setGroup(String group) {
        this.group_id = group;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getAuthor() {
        return authorName;
    }

    public void setAuthor(String author) {
        this.authorName = author;
    }

    public String getTimestamp() {
        return imageTimestamp;
    }

    public void setTimestamp(String timestamp) {
        this.imageTimestamp = timestamp;
    }

    public String getHeader() {
        return header;
    }

    public void setHeader(String header) {
        this.header = header;
        type = VIEW_HEADER;
    }

    public String getDescription() {
        return imageDescription;
    }

    public void setDescription(String description) {
        this.imageDescription = description;
    }


    public int getType() {
        return type;
    }

    public void setType(int type)   {  this.type = type; }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

}
