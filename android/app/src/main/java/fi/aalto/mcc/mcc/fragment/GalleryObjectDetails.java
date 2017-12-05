package fi.aalto.mcc.mcc.fragment;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.DialogFragment;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;

import java.util.ArrayList;

import fi.aalto.mcc.mcc.R;
import fi.aalto.mcc.mcc.activity.SettingsActivity;
import fi.aalto.mcc.mcc.helper.Connectivity;
import fi.aalto.mcc.mcc.helper.TouchImageView;
import fi.aalto.mcc.mcc.model.GalleryObject;

/**
 * Created by user on 14/11/2017.
 */


public class GalleryObjectDetails extends DialogFragment {

    private String TAG = GalleryObjectDetails.class.getSimpleName();
    private ArrayList<GalleryObject> listObjects;
    private ViewPager viewPager;
    private CustomViewPagerAdapter viewPagerAdapter;
    private TextView labelCount;
    private int selectedPosition = 0;
    private Connectivity cm;
    int syncLAN = 1;
    int syncWAN = 1;

    public static GalleryObjectDetails newInstance() {
        GalleryObjectDetails instance = new GalleryObjectDetails();
        return instance;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.image_slider, container, false);
        viewPager = (ViewPager) v.findViewById(R.id.viewpager);
        labelCount = (TextView) v.findViewById(R.id.labelcount);

        listObjects = (ArrayList<GalleryObject>) getArguments().getSerializable("images");
        selectedPosition = getArguments().getInt("position");

        viewPagerAdapter = new CustomViewPagerAdapter();
        viewPager.setAdapter(viewPagerAdapter);
        viewPager.addOnPageChangeListener(viewPagerPageChangeListener);


        cm = new Connectivity();
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(getContext());
        int syncLAN = Integer.parseInt(sharedPref.getString("sync_quality_lan","1"));
        int syncWAN = Integer.parseInt(sharedPref.getString("sync_quality_wan","1"));

        setCurrentItem(selectedPosition);


        return v;
    }

    private void setCurrentItem(int position) {

        viewPager.setCurrentItem(position, false);
        displayMetaInfo(selectedPosition);
    }

    //	page change listener
    ViewPager.OnPageChangeListener viewPagerPageChangeListener = new ViewPager.OnPageChangeListener() {

        @Override
        public void onPageSelected(int position) {
            displayMetaInfo(position);
        }

        @Override
        public void onPageScrolled(int position, float positionOffset, int arg2)
        {

        }

        @Override
        public void onPageScrollStateChanged(int arg0) {

        }
    };

    private void displayMetaInfo(int position) {

        labelCount.setText((position + 1) + " of " + listObjects.size());
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStyle(DialogFragment.STYLE_NORMAL, android.R.style.Theme_Black_NoTitleBar_Fullscreen);
    }

    //	adapter
    public class CustomViewPagerAdapter extends PagerAdapter {

        private LayoutInflater layoutInflater;

        public CustomViewPagerAdapter() {
        }

        @Override
        public Object instantiateItem(ViewGroup container, int position) {

            layoutInflater = (LayoutInflater) getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            View _view = layoutInflater.inflate(R.layout.image_fullscreen, null);


            TouchImageView imageViewPreview = (TouchImageView) _view.findViewById(R.id.image_preview);
            imageViewPreview.setImageResource(R.drawable.ic_menu_gallery);

            // XXX might move the bandwidth detection code inside helper later (SM)
            String imagePath = "";
            boolean hasWifi = cm.isConnectedWifi(getContext());

            // in LAN or Wifi
            if (hasWifi && syncLAN == 0)   imagePath = listObjects.get(position).getSmall();
            else if ( hasWifi && syncLAN == 1)   imagePath = listObjects.get(position).getLarge();
            else if ( hasWifi && syncLAN == 2)   imagePath = listObjects.get(position).getXL();
            else
            {
                // in WAN
                if(syncWAN == 0) imagePath = listObjects.get(position).getSmall();
                else if (syncWAN == 1) imagePath = listObjects.get(position).getLarge();
                else imagePath = listObjects.get(position).getXL();
            }

            Glide.with(getActivity()).load(imagePath)
                    .thumbnail(0.5f)
                    .placeholder(R.drawable.ic_menu_gallery)
                    .crossFade()
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .into(imageViewPreview );

            container.addView(_view);

            return _view;
        }

        @Override
        public int getCount() {
            return listObjects.size();
        }

        @Override
        public boolean isViewFromObject(View view, Object obj) {
            return view == ((View) obj);
        }


        @Override
        public void destroyItem(ViewGroup container, int position, Object object) {
            container.removeView((View) object);
        }
    }
}
