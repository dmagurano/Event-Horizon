package fi.aalto.mcc.mcc.model;

import java.io.Serializable;

/**
 * Created by user on 14/11/2017.
 */

public class GalleryObject implements Serializable
{

    private static final int VIEW_HEADER = 0;
    private static final int VIEW_NORMAL = 1;

    // image location as filepath or URL
    private String pathThumbnail, pathImage;

    // metadata
    private String authorName;
    private String authorImage;
    private String imageTimestamp;
    private String imageDescription;
    private float lat, lon;

    // object classification data
    private boolean isPublic;
    private int type;
    private String header;

    private String category;

    public GalleryObject() {
        type = VIEW_NORMAL;
    }

    public GalleryObject(int type, String header) {
        this.type = type;
        this.header = header;
    }

    public GalleryObject( String small, String large, String timestamp, boolean ispublic, String category, String author) {
        this.pathThumbnail = small;
        this.pathImage = large;
        this.imageTimestamp = timestamp;
        this.isPublic = ispublic;
        this.category = category;
        this.authorName = author;
    }

    public String getSmall() {
        return pathThumbnail;
    }

    public void setSmall(String small) {
        this.pathThumbnail = small;
    }

    public String getLarge() {
        return pathImage;
    }

    public void setLarge(String large) {
        this.pathImage = large;
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

    public boolean isPublic() {
        return isPublic;
    }

    public void setPublic(Boolean value)   {  this.isPublic = value; }

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
