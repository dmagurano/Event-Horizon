package fi.aalto.mcc.mcc.fragment;

import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v4.app.DialogFragment;
import android.support.v4.content.FileProvider;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.BitmapRequestBuilder;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.animation.GlideAnimation;
import com.bumptech.glide.request.target.SimpleTarget;
import com.firebase.ui.storage.images.FirebaseImageLoader;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.storage.FileDownloadTask;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.OnProgressListener;
import com.google.firebase.storage.StorageReference;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.concurrent.ExecutionException;

import fi.aalto.mcc.mcc.BuildConfig;
import fi.aalto.mcc.mcc.R;
import fi.aalto.mcc.mcc.activity.MainActivity;
import fi.aalto.mcc.mcc.activity.SettingsActivity;
import fi.aalto.mcc.mcc.helper.Connectivity;
import fi.aalto.mcc.mcc.helper.TouchImageView;
import fi.aalto.mcc.mcc.model.GalleryObject;

/**
 * Created by user on 14/11/2017.
 */


public class GalleryObjectDetails extends DialogFragment {

    public static final String DIR_SHARED       = "/Shared";
    public static final String DIR_DATA         = "/Android/data/";
    public static final String DIR_PRIVATE      = "/PrivateFiles/";
    public static final String DIR_SAVE         = "/Event Horizon";


    private String TAG = GalleryObjectDetails.class.getSimpleName();
    private ArrayList<GalleryObject> listObjects;
    private ViewPager viewPager;
    private CustomViewPagerAdapter viewPagerAdapter;
    private TextView labelCount;
    private ImageView buttonShare, buttonBack, buttonSave;
    private TextView buttonShareTouch, buttonBackTouch, buttonSaveTouch;
    private int selectedPosition = 0;
    private Connectivity cm;
    int syncLAN = 1, syncWAN = 1;
    private File sharedFile;
    private Uri uriSharedFile;

    FirebaseStorage storage = FirebaseStorage.getInstance();
    StorageReference storageReference = storage.getReference();

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

        buttonBack  = (ImageView) v.findViewById(R.id.backButton);
        buttonShare = (ImageView) v.findViewById(R.id.shareButton);
        buttonSave = (ImageView) v.findViewById(R.id.saveButton);

        buttonBackTouch  = (TextView) v.findViewById(R.id.backButtonTouchArea);
        buttonShareTouch = (TextView) v.findViewById(R.id.shareButtonTouchArea);
        buttonSaveTouch = (TextView) v.findViewById(R.id.saveButtonTouchArea);

        buttonBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                OnBackButton(view);
            }
        });

        buttonBackTouch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                OnBackButton(view);
            }
        });

        buttonShare.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                OnShareButton(view);
            }
        });

        buttonShareTouch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                OnShareButton(view);
            }
        });

        buttonSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                OnSaveButton(view);
            }
        });

        buttonSaveTouch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                OnSaveButton(view);
            }
        });



        listObjects = (ArrayList<GalleryObject>) getArguments().getSerializable("images");
        selectedPosition = getArguments().getInt("position");

        viewPagerAdapter = new CustomViewPagerAdapter();
        viewPager.setAdapter(viewPagerAdapter);
        viewPager.addOnPageChangeListener(viewPagerPageChangeListener);


        cm = new Connectivity();
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(getContext());
        syncLAN = Integer.parseInt(sharedPref.getString("sync_quality_lan","1"));
        syncWAN = Integer.parseInt(sharedPref.getString("sync_quality_wan","1"));



        setCurrentItem(selectedPosition);


        return v;
    }


    public void OnBackButton(View v)
    {
        dismiss();
    }


    public void OnShareButton(View v) {
        String path = null;

        final ProgressDialog busy = new ProgressDialog(getActivity());

        if (selectedPosition < listObjects.size())
            path = listObjects.get(selectedPosition).getXL();

        if (path == null){
            return;
        }


        try {
            if (path.startsWith("gs:")) {
                sharedFile = getOutputMediaFile();
                uriSharedFile = FileProvider.getUriForFile(getActivity().getApplicationContext(),
                        BuildConfig.APPLICATION_ID + ".provider",
                        sharedFile);

                final Intent intent = new Intent(Intent.ACTION_SEND);
                intent.setType("image/jpg");
                intent.putExtra(Intent.EXTRA_STREAM, uriSharedFile);
                busy.setMessage("Downloading...");
                busy.show();

                storage.getReferenceFromUrl(path).getFile(sharedFile).addOnSuccessListener(new OnSuccessListener<FileDownloadTask.TaskSnapshot>() {
                    @Override
                    public void onSuccess(FileDownloadTask.TaskSnapshot taskSnapshot) {
                        startActivity(Intent.createChooser(intent, "Share"));
                        busy.hide();
                        busy.dismiss();
                        getActivity().getContentResolver().delete(uriSharedFile, null, null);
                    }
                }).addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception exception) {
                        Log.e("Share Image","local temp file not created " +exception.toString());
                    }
                }).addOnProgressListener(new OnProgressListener<FileDownloadTask.TaskSnapshot>() {
                    @Override
                    public void onProgress(FileDownloadTask.TaskSnapshot taskSnapshot) {
                        // progress percentage
                        double progress = (100.0 * taskSnapshot.getBytesTransferred()) / taskSnapshot.getTotalByteCount();

                        // percentage in progress dialog
                        busy.setMessage("Downloaded " + ((int) progress) + "%...");
                    }
                });;

            }
            else {
                String filename = path.split("/")[10];
                sharedFile = new File(Environment.getExternalStorageDirectory()
                        + DIR_DATA
                        + getActivity().getApplicationContext().getPackageName()
                        + DIR_PRIVATE
                        + filename);
                uriSharedFile = FileProvider.getUriForFile(getActivity().getApplicationContext(),
                        BuildConfig.APPLICATION_ID + ".provider",
                        sharedFile);

                Intent intent = new Intent(Intent.ACTION_SEND);
                intent.setType("image/jpg");
                intent.putExtra(Intent.EXTRA_STREAM, uriSharedFile);
                startActivity(Intent.createChooser(intent, "Share"));
            }
        } catch (Exception e) {
            Log.e(TAG, e.getMessage());
        }


        return;

    }

    public void OnSaveButton(View v) {
        String path = null;

        final ProgressDialog busy = new ProgressDialog(getActivity());

        if (selectedPosition < listObjects.size())
            path = listObjects.get(selectedPosition).getXL();

        if (path == null){
            return;
        }

        try {
            if (path.startsWith("gs:")) {
                sharedFile = getSavedFile();
                uriSharedFile = FileProvider.getUriForFile(getActivity().getApplicationContext(),
                        BuildConfig.APPLICATION_ID + ".provider",
                        sharedFile);

                busy.setMessage("Downloading...");
                busy.show();

                storage.getReferenceFromUrl(path).getFile(sharedFile).addOnSuccessListener(new OnSuccessListener<FileDownloadTask.TaskSnapshot>() {
                    @Override
                    public void onSuccess(FileDownloadTask.TaskSnapshot taskSnapshot) {
                        busy.hide();
                        busy.dismiss();
                        Toast.makeText(getActivity().getApplicationContext(), "Image downloaded in Event Horizon folder",
                                Toast.LENGTH_SHORT).show();
                        //Snackbar.make(getActivity().getCurrentFocus(), "Image downloaded in Event Horizon folder", Snackbar.LENGTH_LONG).setAction("Notification", null).show();

                    }
                }).addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception exception) {
                        Log.e("Share Image","local temp file not created " +exception.toString());
                    }
                }).addOnProgressListener(new OnProgressListener<FileDownloadTask.TaskSnapshot>() {
                    @Override
                    public void onProgress(FileDownloadTask.TaskSnapshot taskSnapshot) {
                        // progress percentage
                        double progress = (100.0 * taskSnapshot.getBytesTransferred()) / taskSnapshot.getTotalByteCount();

                        // percentage in progress dialog
                        busy.setMessage("Downloaded " + ((int) progress) + "%...");
                    }
                });;

            }
            else {
                String filename = path.split("/")[10];
                sharedFile = new File(Environment.getExternalStorageDirectory()
                        + DIR_DATA
                        + getActivity().getApplicationContext().getPackageName()
                        + DIR_PRIVATE
                        + filename);
                uriSharedFile = FileProvider.getUriForFile(getActivity().getApplicationContext(),
                        BuildConfig.APPLICATION_ID + ".provider",
                        sharedFile);
                File destination = getSavedFile();
                try {
                    copyFile(sharedFile, destination);
                }
                catch (IOException e) {
                    Log.e(TAG, e.getMessage());
                }

                // XXX should send gallery app invoke to reload images
                //getActivity().sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.fromFile(destination)));


                Toast.makeText(getActivity().getApplicationContext(), "Image downloaded in Event Horizon folder",                        Toast.LENGTH_SHORT).show();
                //Snackbar.make(getActivity().getCurrentFocus(), "Image downloaded in Event Horizon folder", Snackbar.LENGTH_LONG).setAction("Notification", null).show();

            }
        } catch (Exception e) {
            Log.e(TAG, e.getMessage());
        }


        return;

    }

    void copyFile(File src, File dst) throws IOException {
        FileChannel inChannel = new FileInputStream(src).getChannel();
        FileChannel outChannel = new FileOutputStream(dst).getChannel();
        try {
            inChannel.transferTo(0, inChannel.size(), outChannel);
        } finally {
            if (inChannel != null)
                inChannel.close();
            if (outChannel != null)
                outChannel.close();
        }
    }

    public File saveToFile(Bitmap bitmap) {

        File pictureFile = getOutputMediaFile();

        if (pictureFile == null) {
            return null;
        }
        try {
            FileOutputStream streamOutput = new FileOutputStream(pictureFile);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, streamOutput);
            streamOutput.close();
        } catch (FileNotFoundException e) {
            Log.d(TAG, "File not found: " + e.getMessage());
            return null;
        } catch (IOException e) {
            Log.d(TAG, "Error accessing file: " + e.getMessage());
            return null;
        }
        return pictureFile;
    }



    private File getOutputMediaFile() {
        File mediaStorageDir = null;

        mediaStorageDir = new File(Environment.getExternalStorageDirectory()
                        + DIR_DATA
                        + getActivity().getApplicationContext().getPackageName()
                        + DIR_SHARED);


        if (!mediaStorageDir.exists()) {
            if (!mediaStorageDir.mkdirs()) {
                return null;
            }
        }

        String mPhotoName = "SHARE_" + new SimpleDateFormat("ddMMyyyy_HHmmss").format(new Date()) + ".jpg";
        File mPhotoFile = new File(mediaStorageDir.getPath() + File.separator + mPhotoName);
        return mPhotoFile;
    }

    private File getSavedFile() {
        File saveDir = new File(Environment.getExternalStorageDirectory()
                                + DIR_SAVE);
        if (!saveDir.exists()) {
            if (!saveDir.mkdirs()) {
                return null;
            }
        }

        String mPhotoName = "SAVED_" + new SimpleDateFormat("ddMMyyyy_HHmmss").format(new Date()) + ".jpg";
        File mPhotoFile = new File(saveDir.getPath() + File.separator + mPhotoName);
        return mPhotoFile;
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

            TouchImageView imageViewDetails = (TouchImageView) _view.findViewById(R.id.image_preview);
            imageViewDetails.setImageResource(R.drawable.ic_menu_gallery);


            // XXX might move the bandwidth detection code inside helper later (SM)
            String path = "";
            boolean hasWifi = cm.isConnectedWifi(getContext());

            // in LAN or Wifi
            if (hasWifi && syncLAN == 0)        path = listObjects.get(position).getSmall();
            else if ( hasWifi && syncLAN == 1)   path = listObjects.get(position).getLarge();
            else if ( hasWifi && syncLAN == 2)   path = listObjects.get(position).getXL();
            else
            {
                // in WAN
                if(syncWAN == 0) path = listObjects.get(position).getSmall();
                else if (syncWAN == 1) path = listObjects.get(position).getLarge();
                else path = listObjects.get(position).getXL();
            }

            if (path != null) {
                if (path.startsWith("gs:"))
                    Glide.with(getActivity())
                            .using(new FirebaseImageLoader())
                            .load(storage.getReferenceFromUrl(path))
                            .thumbnail(0.5f)
                            .placeholder(R.drawable.ic_menu_gallery)
                            .crossFade()
                            .diskCacheStrategy(DiskCacheStrategy.ALL)
                            .into(imageViewDetails);
                else
                    Glide.with(getActivity()).load(path)
                            .thumbnail(0.5f)
                            .placeholder(R.drawable.ic_menu_gallery)
                            .crossFade()
                            .diskCacheStrategy(DiskCacheStrategy.ALL)
                            .into(imageViewDetails);
            }

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
