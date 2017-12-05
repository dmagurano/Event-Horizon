package fi.aalto.mcc.mcc.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created by user on 17/11/2017.
 * this is a bit quick and dirty (SM)
 */

public class AlbumObject implements Serializable {

    private static final int VIEW_HEADER = 0;
    private static final int VIEW_NORMAL = 1;

    private static final int BY_AUTHOR = 0;
    private static final int BY_CATEGORY = 1;

    private ArrayList<GalleryObject> listObjects;
    private String name;
    private String Id;
    private boolean isPublic;

    public AlbumObject(String id, HashMap<String, Object> map) {
        listObjects = new ArrayList<GalleryObject>();
        this.Id = id;
        this.isPublic = true;

        if (map.get("name") != null) this.name =  map.get("name").toString();
        else this.name = "broken data";
    }

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

    public String getId() {return Id;}

    public String thumbnail() {
        String result = "";

        if(listObjects.size() >0)
            result =  listObjects.get(0).getSmall();

        return result;
    }

    public String backdrop() {
        String result ="";

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
        if (obj == null) return;

        // check if already exists and update data
        if(obj.getId() != null && obj.getId() != "") {
            for (int i = 0; i < listObjects.size(); i++) {

                if (listObjects.get(i).getId().equals(obj.getId())) {
                    listObjects.get(i).update(obj);
                    return;
                }
            }
        }
        // add new object
        listObjects.add(obj);
    }



    public ArrayList<String> enumCategories()
    {
        ArrayList<String> result = new ArrayList<String>();

        for (GalleryObject obj : listObjects) {
            boolean exists = false;
            for (String s : result)
                if (s.equals(obj.getCategory()) )
                    exists = true;
            if (!exists) result.add(obj.getCategory());
        }
        return result;
    }

    public ArrayList<GalleryObject> queryByCategory(String category)
    {
        ArrayList<GalleryObject> result = new ArrayList<GalleryObject>();

        for (GalleryObject obj : listObjects) {
            if(category.equals(obj.getCategory()))
                result.add(obj);
        }
        return result;
    }



    public int getFlatViewTypeByCategory(int position)
    {
        ArrayList<String> categories = this.enumCategories();
        int[] size = new int[categories.size()];
        int[] pos = new int[categories.size()];
        int increment = 0;

        for (int i = 0; i!= categories.size(); i++ )
            size[i] = queryByCategory(categories.get(i)).size();

        for (int t = 0; t != categories.size(); t++ )
        {
            pos[t] = increment + t;
            increment += size[t];
        }
        for (int q = 0; q != categories.size(); q++ ) {
            if (pos[q] == position) return VIEW_HEADER;
        }
        return VIEW_NORMAL;
    }


    public ArrayList<GalleryObject> flattenByCategory()
    {
        ArrayList<GalleryObject> result = new ArrayList<GalleryObject>();
        ArrayList<String> categories = this.enumCategories();
        int totalCount = listObjects.size() + categories.size();
        int category_enum_index = 0;


        for (int a = 0; a < totalCount && category_enum_index < categories.size();)
        {
            if (getFlatViewTypeByCategory(a) == VIEW_HEADER)
                result.add(a++, new GalleryObject(VIEW_HEADER, categories.get(category_enum_index)) );
            else{
                ArrayList<GalleryObject> fragment  = queryByCategory(categories.get(category_enum_index++));
                for (GalleryObject obj : fragment) result.add(a++, obj);
            }

        }
        return result;
    }


    public ArrayList<String> enumAuthors()
    {
        ArrayList<String> result = new ArrayList<String>();

        for (GalleryObject obj : listObjects) {
            boolean exists = false;
            for (String s : result)
                if (s.equals(obj.getAuthor()) )
                    exists = true;
            if (!exists) result.add(obj.getAuthor());
        }
        return result;

    }

    public int getFlatViewTypeByAuthor(int position)
    {
        ArrayList<String> authors = this.enumAuthors();
        int[] size = new int[authors.size()];
        int[] pos = new int[authors.size()];
        int increment = 0;

        for (int i = 0; i!= authors.size(); i++ )
            size[i] = queryByAuthor(authors.get(i)).size();

        for (int t = 0; t != authors.size(); t++ )
        {
            pos[t] = increment + t;
            increment += size[t];
        }
        for (int q = 0; q != authors.size(); q++ ) {
            if (pos[q] == position) return VIEW_HEADER;
        }
        return VIEW_NORMAL;
    }


    public ArrayList<GalleryObject> flattenByAuthor()
    {
        ArrayList<GalleryObject> result = new ArrayList<GalleryObject>();
        ArrayList<String> authors = this.enumAuthors();
        int totalCount = listObjects.size() + authors.size();
        int author_enum_index = 0;


        for (int a = 0; a < totalCount && author_enum_index < authors.size();)
        {
            if (getFlatViewTypeByAuthor(a) == VIEW_HEADER)
                result.add(a++, new GalleryObject(VIEW_HEADER, authors.get(author_enum_index)) );
            else{
                ArrayList<GalleryObject> fragment  = queryByAuthor(authors.get(author_enum_index++));
                for (GalleryObject obj : fragment) result.add(a++, obj);
            }

        }
        return result;
    }


    public ArrayList<GalleryObject> queryByAuthor(String author)
    {
        ArrayList<GalleryObject> result = new ArrayList<GalleryObject>();

        for (GalleryObject obj : listObjects) {
            if(author.equals(obj.getAuthor()))
                result.add(obj);
        }
        return result;
    }

    public int getFlatViewType(int position, int type)
    {
        if (type == BY_CATEGORY) return getFlatViewTypeByCategory(position);
        if (type == BY_AUTHOR) return getFlatViewTypeByAuthor(position);

        return 0;
    }

    public ArrayList<GalleryObject> flatten(int type)
    {
        if (type == BY_CATEGORY) return flattenByCategory();
        if (type == BY_AUTHOR) return flattenByAuthor();

        return null;
    }


}
