package fi.aalto.mcc.mcc.model;

import java.io.Serializable;
import java.util.ArrayList;

/**
 * Created by user on 17/11/2017.
 */

public class AlbumObject implements Serializable {

    private ArrayList<GalleryObject> listObjects;
    private String name;
    boolean isPublic;

    public AlbumObject() {
        listObjects = new ArrayList<GalleryObject>();
    }

    public AlbumObject(String name, boolean ispublic) {
        listObjects = new ArrayList<GalleryObject>();
        this.name = name;
        this.isPublic = ispublic;
    }

    public boolean isPublic() {
        return isPublic;
    }
    public void setPublic(Boolean value)   {  this.isPublic = value; }

    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }

    public int size() {
        return listObjects.size();
    }

    public String thumbnail() {
        String result = new String();

        if(listObjects.size() >0)
            result =  listObjects.get(0).getSmall();

        return result;
    }

    public String backdrop() {
        String result = new String();

        if(listObjects.size() >0)
            result =  listObjects.get(0).getLarge();

        return result;
    }

    public ArrayList<GalleryObject> getGallery()
    {
        return listObjects;
    }


    public void add(GalleryObject obj)
    {
        listObjects.add(obj);
    }


    // this is a bit quick and dirty (SM)
    public ArrayList<String> enumCategories()
    {
        ArrayList<String> result = new ArrayList<String>();

        for (GalleryObject obj : listObjects) {
            for (String s : result)
                if (s == obj.getCategory() )
                    break;
            result.add(obj.getCategory());
        }
        return result;
    }

    public ArrayList<String> enumAuthors()
    {
        ArrayList<String> result = new ArrayList<String>();
        Boolean exists = false;

        for (GalleryObject obj : listObjects) {
            for (String s : result)
                if (s == obj.getAuthor() )
                    break;
            result.add(obj.getAuthor());
        }

        return result;
    }


    public ArrayList<GalleryObject> queryByCategory(String category)
    {
        ArrayList<GalleryObject> result = new ArrayList<GalleryObject>();

        for (GalleryObject obj : listObjects) {
           if(obj.getCategory() == category)
               result.add(obj);
        }
        return result;
    }

    public ArrayList<GalleryObject> queryByAuthor(String author)
    {
        ArrayList<GalleryObject> result = new ArrayList<GalleryObject>();

        for (GalleryObject obj : listObjects) {
            if(obj.getAuthor() == author)
                result.add(obj);
        }
        return result;
    }



}
