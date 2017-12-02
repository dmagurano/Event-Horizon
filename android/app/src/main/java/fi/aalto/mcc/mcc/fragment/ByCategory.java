package fi.aalto.mcc.mcc.fragment;

import android.app.ProgressDialog;
import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.GridLayoutManager;


import java.util.ArrayList;

import fi.aalto.mcc.mcc.R;
import fi.aalto.mcc.mcc.adapter.GalleryViewAdapter;
import fi.aalto.mcc.mcc.model.AlbumObject;
import fi.aalto.mcc.mcc.model.GalleryObject;

/**
 * Created by user on 14/11/2017.
 */


public class ByCategory extends Fragment {

    private static final int VIEW_HEADER = 0;
    private static final int VIEW_NORMAL = 1;

    private String TAG = ByCategory.class.getSimpleName();
    ArrayList<GalleryObject> gridArray;
    AlbumObject album;
    int type;

    private GalleryViewAdapter viewAdapter;
    private RecyclerView recyclerView;
    private Context context;

    public ByCategory(AlbumObject obj, int type)
    {
        this.album = obj;
        this.type = type;
        this.gridArray = obj.flatten(type);
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

    }

    public void updateData(AlbumObject obj, int type)
    {
        this.album = obj;
        this.type = type;
        this.gridArray = obj.flatten(type);

        viewAdapter.updateData(album, type);
    }


    @Override
    public View onCreateView(final LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View _view = inflater.inflate(R.layout.by_category, container, false);

        recyclerView = (RecyclerView) _view.findViewById(R.id.my_recycler_view);

        viewAdapter = new GalleryViewAdapter(getActivity().getApplicationContext(), album, type);

        GridLayoutManager mLayoutManager = new GridLayoutManager(getActivity().getApplicationContext(), 3);

        mLayoutManager.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
            @Override
            public int getSpanSize(int position) {

                if ( gridArray.get(position).getType() == VIEW_HEADER) return 3; // header takes 3 spans
                return 1;// image takes 1 span
            }
        });
        recyclerView.setLayoutManager(mLayoutManager);
        recyclerView.setItemAnimator(new DefaultItemAnimator());
        recyclerView.setAdapter(viewAdapter);

        recyclerView.addOnItemTouchListener(new GalleryViewAdapter
                                                .GalleryTouchListener(getActivity()
                                                .getApplicationContext(), recyclerView, new GalleryViewAdapter.ClickListener() {
            @Override
            public void onClick(View view, int position) {

                if ( gridArray.get(position).getType() == VIEW_HEADER) return;

                //remove grid headers before passing array to details view
                ArrayList<GalleryObject> array = new ArrayList<GalleryObject>();
                for (GalleryObject obj : gridArray) {
                    if (obj.getType() == VIEW_NORMAL) array.add(obj);
                }
                // recalculate relative position in new array
                int offset = 0;
                for (int i = 0; i <= position; i++) {
                    if (gridArray.get(i).getType() == VIEW_HEADER) offset++;
                }

                Bundle bundle = new Bundle();
                bundle.putSerializable("images", array);
                bundle.putInt("position", (position-offset));


                FragmentTransaction ft = getFragmentManager().beginTransaction();
                GalleryObjectDetails newFragment = GalleryObjectDetails.newInstance();
                newFragment.setArguments(bundle);
                newFragment.show(ft, "details");
            }

            @Override
            public void onLongClick(View view, int position) {

            }
        }));


        return _view;
    }

}