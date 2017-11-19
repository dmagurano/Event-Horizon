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
import fi.aalto.mcc.mcc.model.GalleryObject;

/**
 * Created by user on 14/11/2017.
 */


public class ByCategory extends Fragment {

    private String TAG = ByCategory.class.getSimpleName();
    private ArrayList<GalleryObject> listObjects;
    private GalleryViewAdapter viewAdapter;
    private RecyclerView recyclerView;
    private Context context;

    public ByCategory(ArrayList<GalleryObject> galleryObjects)
    {
        this.listObjects = galleryObjects;
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View _view = inflater.inflate(R.layout.by_category, container, false);

        recyclerView = (RecyclerView) _view.findViewById(R.id.my_recycler_view);

        viewAdapter = new GalleryViewAdapter(getActivity().getApplicationContext(), listObjects);

        RecyclerView.LayoutManager mLayoutManager = new GridLayoutManager(getActivity().getApplicationContext(), 3);
        recyclerView.setLayoutManager(mLayoutManager);
        recyclerView.setItemAnimator(new DefaultItemAnimator());
        recyclerView.setAdapter(viewAdapter);

        recyclerView.addOnItemTouchListener(new GalleryViewAdapter
                                                .GalleryTouchListener(getActivity()
                                                .getApplicationContext(), recyclerView, new GalleryViewAdapter.ClickListener() {
            @Override
            public void onClick(View view, int position) {
                Bundle bundle = new Bundle();
                bundle.putSerializable("images", listObjects);
                bundle.putInt("position", position);


                FragmentTransaction ft = getFragmentManager().beginTransaction();
                GalleryObjectDetails newFragment = GalleryObjectDetails.newInstance();
                newFragment.setArguments(bundle);
                newFragment.show(ft, "slideshow");
            }

            @Override
            public void onLongClick(View view, int position) {

            }
        }));


        viewAdapter.notifyDataSetChanged();

        return _view;
    }

}