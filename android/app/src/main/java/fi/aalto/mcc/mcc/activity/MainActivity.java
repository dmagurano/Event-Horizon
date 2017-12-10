package fi.aalto.mcc.mcc.activity;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.SystemClock;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.CollapsingToolbarLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.FileProvider;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.util.SparseArray;
import android.util.TypedValue;
import android.view.View;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.widget.ArrayAdapter;
import android.widget.ImageView;

import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Base64;
import android.util.Log;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.ByteArrayBody;
import org.apache.http.entity.mime.content.ContentBody;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;

import com.bumptech.glide.Glide;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.TaskCompletionSource;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GetTokenResult;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import android.net.Uri;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.vision.Frame;
import com.google.android.gms.vision.barcode.Barcode;
import com.google.android.gms.vision.barcode.BarcodeDetector;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.remoteconfig.FirebaseRemoteConfig;

import fi.aalto.mcc.mcc.BuildConfig;
import fi.aalto.mcc.mcc.R;
import fi.aalto.mcc.mcc.adapter.AlbumViewAdapter;
import fi.aalto.mcc.mcc.model.AlbumObject;
import fi.aalto.mcc.mcc.helper.GridSpacingItemDecoration;
import fi.aalto.mcc.mcc.model.GalleryObject;
import fi.aalto.mcc.mcc.model.UserObject;


public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    public static final int CAMERA_REQUEST_CODE = 1;
    public static final int MEDIA_REQUEST_CODE  = 2;
    public static final int RECORD_REQUEST_CODE = 3;

    public static final int FILE_UNCLASSIFIED   = 1;
    public static final int FILE_CLASSIFIED     = 2;

    public static final String DIR_CLASSIFIED   = "/PrivateFiles";
    public static final String DIR_UNCLASSIFIED = "/Unprocessed";
    public static final String DIR_DATA         = "/Android/data/";

    public static final String IMAGES_CHILD = "Images";
    public static final String GROUP_CHILD  = "group";
    public static final String NAME_CHILD   = "name";
    public static final String PHOTO_CHILD  = "photo";
    public static final String EMAIL_CHILD  = "e-mail";
    public static final String GROUPS_CHILD = "Groups";
    public static final String USERS_CHILD  = "Users";

    public static final String UPLOAD_FORM_ID         = "idToken";
    public static final String UPLOAD_FORM_GROUP      = "groupId";
    public static final String UPLOAD_FORM_NAME       = "author_name";
    public static final String UPLOAD_FORM_FILE       = "file";
    public static final String UPLOAD_FORM_FILE_NAME  = "file.jpg";
    public static final String UPLOAD_FORM_MIME       = "image/jpeg";

    private String TAG = GalleryActivity.class.getSimpleName();

    public static String uploadURL = "https://mcc-fall-2017-g04.appspot.com/upload";

    BarcodeDetector                 barcodeDetector;
    static Uri                      uriPhotoFileTarget;
    byte[]                          bytePhotoUploadTarget;
    boolean                         initDone = false;

    String                          myID;
    String                          myGroup;
    String                          myName;
    String                          myPhotoUrl;
    String                          myEmail;

    private RecyclerView            recyclerView;
    private AlbumViewAdapter        adapter;
    private ArrayList<AlbumObject>  albumList;
    private AlbumObject             privateAlbum;

    private FirebaseAuth.AuthStateListener mAuthListener;
    private FirebaseAuth mAuth;
    private DatabaseReference mDatabase = null, mGroupRef = null, mImageRef = null;
    private ValueEventListener valueListenerGroup = null, valueListenerImage = null;
    private FirebaseUser mUser;
    private FirebaseRemoteConfig mRemoteConfig;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // initialize firebase stuff
        mAuth = FirebaseAuth.getInstance();
        mUser = mAuth.getCurrentUser();
        mDatabase = FirebaseDatabase.getInstance().getReference();

        // invoke login and load authToken
        doLogin();

        // initialize toolbar
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        initCollapsingToolbar();

        // load banner image to toolbar
        try {
            Glide.with(this).load(R.drawable.backdrop).into((ImageView) findViewById(R.id.backdrop));
        } catch (Exception e) {
            e.printStackTrace();
        }

        // initialize album view as main view
        recyclerView = (RecyclerView) findViewById(R.id.recycler_view);
        albumList = new ArrayList<>();
        adapter = new AlbumViewAdapter(this, albumList);
        RecyclerView.LayoutManager mLayoutManager = new GridLayoutManager(this, 2);
        recyclerView.setLayoutManager(mLayoutManager);
        recyclerView.addItemDecoration(new GridSpacingItemDecoration(2, dpToPx(3), true));
        recyclerView.setItemAnimator(new DefaultItemAnimator());
        recyclerView.setAdapter(adapter);

        // add click listener to album view
        recyclerView.addOnItemTouchListener(new AlbumViewAdapter
                .AlbumTouchListener(this
                .getApplicationContext(), recyclerView, new AlbumViewAdapter.ClickListener() {

            @Override
            public void onClick(View view, int position) {

                if (albumList != null && position >=0 && albumList.size() > position ) {
                    AlbumObject obj = albumList.get(position);
                    if (obj == null) return;

                    Intent intent = new Intent(MainActivity.this, GalleryActivity.class);

                    if (intent != null && obj != null) {
                        intent.putExtra("album", obj);
                        startActivity(intent);
                    }
                }

            }

            @Override
            public void onLongClick(View view, int position) {

            }
        }));

        // add camera button to main view
        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setImageResource(R.drawable.camera);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                OnPhotoButton(view);
            }
        });

        // hook "no group selected" indicator and hide it
        ImageView pleaseNote = (ImageView)  findViewById(R.id.imagePleaseNote);
        pleaseNote.setVisibility(View.INVISIBLE);

        // add main navigation
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        // initialize barcode detector (this may take a minute on a new phone)
        barcodeDetector =
                new BarcodeDetector.Builder(getApplicationContext())
                        .setBarcodeFormats(Barcode.ALL_FORMATS)
                        .build();

    }


    public void OnPhotoButton(View v)
    {
        // get permission one by one
        if (checkPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            makeRequest(Manifest.permission.WRITE_EXTERNAL_STORAGE);
            return;
        } else if (checkPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            makeRequest(Manifest.permission.CAMERA);
            return;
        } else {

            final Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

            File photoFile = getOutputMediaFile(FILE_UNCLASSIFIED);

            uriPhotoFileTarget = FileProvider.getUriForFile(MainActivity.this,
                    BuildConfig.APPLICATION_ID + ".provider",
                     photoFile);

            takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, uriPhotoFileTarget);

            startActivityForResult(takePictureIntent, CAMERA_REQUEST_CODE);

        }


    }


    public void OnGroupButton(View v)
    {
        Intent i = new Intent(MainActivity.this, GroupManagementActivity.class);
        FirebaseUser user = mAuth.getCurrentUser();

        if (user != null)
        {
            String uid = mAuth.getCurrentUser().getUid();
            String group_id = mDatabase.child(USERS_CHILD).child(uid).child(GROUP_CHILD).toString();
            i.putExtra("USER_IN_GROUP", group_id != null);
            startActivity(i);
        }
    }



    private int checkPermission(String permission) {
        return ContextCompat.checkSelfPermission(this, permission);
    }

    private void makeRequest(String permission) {
        ActivityCompat.requestPermissions(this, new String[]{permission}, RECORD_REQUEST_CODE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        Bitmap originalBitmap = null;

        // decode answer from camera intent with or without image path
        if (requestCode == CAMERA_REQUEST_CODE && resultCode == RESULT_OK) {

            Log.i(TAG, "A photograph was selected");

            new startPhotoPostProcessing().execute();
        }
    }




    public class startPhotoPostProcessing extends AsyncTask<Void, Void, String> {

        int value;
        Bitmap bitmap;
        private ProgressDialog busy = new ProgressDialog(MainActivity.this);

        protected void onPreExecute() {
            super.onPreExecute();
            busy.setMessage("Processing new image...");
            busy.show();
        }
        @Override
        protected String doInBackground(Void... params) {
            bitmap = null;

            // load photo to bitmap
            try {
                if (uriPhotoFileTarget != null) {
                    bitmap = MediaStore.Images.Media.getBitmap(getApplicationContext().getContentResolver(), uriPhotoFileTarget);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }

            // classify image
            value = doPhotoClasssification(bitmap);

            if (value < 0) return "Fail";                             // <-- classification failed
            else return "Success";
        }

        protected void onPostExecute(String result) {
            super.onPostExecute(result);
            if (value > 0)
            {
                savePhotoToPrivateFolder(bitmap);                   // <-- save to private folder
                //Snackbar.make(getCurrentFocus(),
                //        "New image was added to Private album.", Snackbar.LENGTH_LONG);
            }
            else uploadPhotoToServer( bitmap );                     // <-- publish to server
            busy.hide();
            busy.dismiss();
            adapter.notifyDataSetChanged();

        }

    }

    public int doPhotoClasssification(Bitmap bitmap) {
        if (!barcodeDetector.isOperational() || bitmap == null)
        {
            return -1;
        }
        Frame frame = new Frame.Builder().setBitmap(bitmap).build();
        SparseArray<Barcode> barcodes = barcodeDetector.detect(frame);
        return barcodes.size();
    }


    private void uploadPhotoToServer(Bitmap bitmap)
    {
        ByteArrayOutputStream streamOutput = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 90, streamOutput);
        bytePhotoUploadTarget = streamOutput.toByteArray();

        new startPhotoUpload().execute();
    }


    public class startPhotoUpload extends AsyncTask<Void, Void, String> {

        private ProgressDialog busy = new ProgressDialog(MainActivity.this);

        protected void onPreExecute() {
            super.onPreExecute();
            busy.setMessage("Uploading to Server...");
            busy.show();
        }

        @Override
        protected String doInBackground(Void... params) {

            try {
                HttpClient httpclient = new DefaultHttpClient();
                HttpPost httppost = new HttpPost(uploadURL);

                MultipartEntityBuilder builder = MultipartEntityBuilder.create();
                builder.setMode(HttpMultipartMode.BROWSER_COMPATIBLE);

                ContentBody contentBody = new ByteArrayBody(bytePhotoUploadTarget, UPLOAD_FORM_MIME, UPLOAD_FORM_FILE_NAME);
                builder.addPart(UPLOAD_FORM_FILE, contentBody);
                builder.addPart(UPLOAD_FORM_ID, new StringBody(myID, ContentType.TEXT_PLAIN));
                builder.addPart(UPLOAD_FORM_GROUP, new StringBody(myGroup, ContentType.TEXT_PLAIN));
                builder.addPart(UPLOAD_FORM_NAME, new StringBody(myName, ContentType.TEXT_PLAIN));

                httppost.setEntity(builder.build());

                HttpResponse response = httpclient.execute(httppost);
                String s = EntityUtils.toString(response.getEntity());
                Log.i(TAG, "Connection response: " + s);

            } catch (Exception e) {
                Log.i(TAG, "Connection error: " + e.toString());
                return "Fail";
            }
            Log.i(TAG, "Upload done.");
            return "Success";
        }

        protected void onPostExecute(String result) {
            super.onPostExecute(result);

            getContentResolver().delete(uriPhotoFileTarget, null, null);
            sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, uriPhotoFileTarget));

            if(result.equals("Fail"))
                Toast.makeText(MainActivity.this, "Connection Error", Toast.LENGTH_SHORT).show();

            busy.hide();
            busy.dismiss();
        }
    }



    private int dpToPx(int dp) {
        Resources r = getResources();
        return Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, r.getDisplayMetrics()));
    }


    private void initCollapsingToolbar() {
        final CollapsingToolbarLayout collapsingToolbar =
                (CollapsingToolbarLayout) findViewById(R.id.main_collapse_toolbar);

        collapsingToolbar.setTitle(" ");
        AppBarLayout appBarLayout = (AppBarLayout) findViewById(R.id.appbar);
        appBarLayout.setExpanded(true);

        // hiding & showing the title when toolbar expanded & collapsed
        appBarLayout.addOnOffsetChangedListener(new AppBarLayout.OnOffsetChangedListener() {
            boolean isShow = false;
            int scrollRange = -1;

            @Override
            public void onOffsetChanged(AppBarLayout appBarLayout, int verticalOffset) {
                if (scrollRange == -1) {
                    scrollRange = appBarLayout.getTotalScrollRange();
                }
                if (scrollRange + verticalOffset == 0) {
                    collapsingToolbar.setTitle(getString(R.string.app_name));
                    isShow = true;
                } else if (isShow) {
                    collapsingToolbar.setTitle(" ");
                    isShow = false;
                }
            }
        });
    }


    // Adapter for the viewpager using FragmentPagerAdapter
    class ViewPagerAdapter extends FragmentPagerAdapter {
        private final List<Fragment> mFragmentList = new ArrayList<>();
        private final List<String> mFragmentTitleList = new ArrayList<>();

        public ViewPagerAdapter(FragmentManager manager) {
            super(manager);
        }

        @Override
        public Fragment getItem(int position) {
            return mFragmentList.get(position);
        }

        @Override
        public int getCount() {
            return mFragmentList.size();
        }

        public void addFragment(Fragment fragment, String title) {
            mFragmentList.add(fragment);
            mFragmentTitleList.add(title);
        }

        @Override
        public CharSequence getPageTitle(int position) {
            return mFragmentTitleList.get(position);
        }
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        int id = item.getItemId();

        if (id == R.id.action_settings) {
            Intent intent = new Intent(MainActivity.this, SettingsActivity.class);
            if (intent != null) {
                startActivity(intent);
            }
        }

        return super.onOptionsItemSelected(item);
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.nav_camera) {
            if (myGroup == null || myGroup.equals("") )
                Snackbar.make(getCurrentFocus(), "Please join or create group event first.", Snackbar.LENGTH_LONG).setAction("Notification", null).show();
            else
                OnPhotoButton(this.getCurrentFocus());
        } else if (id == R.id.nav_gallery) {

        } else if (id == R.id.nav_group)
        {
            OnGroupButton(this.getCurrentFocus());

        } else if (id == R.id.nav_settings) {

            Intent intent = new Intent(MainActivity.this, SettingsActivity.class);
            if (intent != null) {
                startActivity(intent);
            }

        } else if (id == R.id.nav_logout) {
            if(myGroup != null){
                FirebaseMessaging.getInstance().unsubscribeFromTopic(myGroup);
                Log.d(TAG, "Unsubscribed from group: " + myGroup);
            }
            mAuth.signOut();
        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }





    /* ======================================================== PHOTO FILE IO  ======================================================== */


    public AlbumObject createPrivateAlbum() {

        AlbumObject albumObject = new AlbumObject("Private", false);
        loadPrivateImages(albumObject);

        return albumObject;
    }


    private void loadPrivateImages(AlbumObject toAlbum) {
        GalleryObject obj = null;

        String path = Environment.getExternalStorageDirectory().toString()
                + DIR_DATA
                + getApplicationContext().getPackageName()
                + DIR_CLASSIFIED;

        File directory = new File(path);
        File[] files = directory.listFiles();

        if (files != null) {
            for (int i = 0; i < files.length; i++) {
                obj = new GalleryObject(Uri.fromFile(files[i]), myName);
                toAlbum.add(obj);
            }
        }
    }


    public void savePhotoToPrivateFolder(Bitmap bitmap) {

        File pictureFile = getOutputMediaFile(FILE_CLASSIFIED);

        if (pictureFile == null) {
            return;
        }
        try {
            FileOutputStream streamOutput = new FileOutputStream(pictureFile);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, streamOutput);
            streamOutput.close();
        } catch (FileNotFoundException e) {
            Log.d(TAG, "File not found: " + e.getMessage());
            return;
        } catch (IOException e) {
            Log.d(TAG, "Error accessing file: " + e.getMessage());
            return;
        }

        GalleryObject privatePhoto = new GalleryObject(Uri.fromFile(pictureFile), myName);
        privateAlbum.add(privatePhoto);
    }



    private File getOutputMediaFile(int type) {
        File mediaStorageDir = null;

        switch (type){
            case FILE_UNCLASSIFIED:
                 mediaStorageDir = new File(Environment.getExternalStorageDirectory()
                        + DIR_DATA
                        + getApplicationContext().getPackageName()
                        + DIR_UNCLASSIFIED);
                 break
                         ;
            case FILE_CLASSIFIED:
                mediaStorageDir = new File(Environment.getExternalStorageDirectory()
                        + DIR_DATA
                        + getApplicationContext().getPackageName()
                        + DIR_CLASSIFIED);
                break;
        }

        if (!mediaStorageDir.exists()) {
            if (!mediaStorageDir.mkdirs()) {
                return null;
            }
        }

        String mPhotoName = "MCC_" + new SimpleDateFormat("ddMMyyyy_HHmmss").format(new Date()) + ".jpg";
        File mPhotoFile = new File(mediaStorageDir.getPath() + File.separator + mPhotoName);
        return mPhotoFile;
    }

    public void setGroupVisibility()
    {

        ImageView mPhoto =  (ImageView)findViewById(R.id.imageUserPhoto);
        TextView  mName  =  (TextView) findViewById(R.id.textUserName);
        TextView  mEmail =  (TextView) findViewById(R.id.textUserEmail);


        // adjust default action to accordingly (camera when user is in group, otherwise join group)
        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        ImageView pleaseNote = (ImageView)  findViewById(R.id.imagePleaseNote);

        if (myGroup == null || myGroup.equals(""))
        {
            pleaseNote.setVisibility(View.VISIBLE);

            fab.setImageResource(R.drawable.join_group);
            fab.invalidate();

            fab.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    OnGroupButton(view);
                }
            });
        } else {


            pleaseNote.setVisibility(View.INVISIBLE);

            fab.setImageResource(R.drawable.camera);
            fab.invalidate();

            fab.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    OnPhotoButton(view);
                }
            });

        }


        //mPhoto.setImageResource(R.drawable.user);
        mName.setText(myName);
        mEmail.setText(myEmail);



    }












    /* ======================================================== FIREBASE  ======================================================== */



    public void addUserNameValueListenerEx() {

        if (mUser.getUid() == null) {
            Snackbar.make(this.getCurrentFocus(), "User login has failed.", Snackbar.LENGTH_LONG)
                    .setAction("Error", null).show();
            return;
        }


        mDatabase.child(USERS_CHILD).child(mUser.getUid()).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                for (DataSnapshot data : snapshot.getChildren()) {
                    if( data.getKey().equals(NAME_CHILD)  && data.getValue() != null )  myName = data.getValue().toString();
                    if( data.getKey().equals(PHOTO_CHILD) && data.getValue() != null)   myPhotoUrl = data.getValue().toString();
                    if( data.getKey().equals(EMAIL_CHILD) && data.getValue() != null)   myEmail = data.getValue().toString();
                    if( data.getKey().equals(GROUP_CHILD) && data.getValue() != null)   myGroup = data.getValue().toString();
                }

                setGroupVisibility();
                addGroupListener(myGroup);
                privateAlbum = createPrivateAlbum();
            }

            @Override
            public void onCancelled(DatabaseError error) {
                // Failed to read value
                Snackbar.make(getCurrentFocus(), "Failed to read user information: " +  error.toException(), Snackbar.LENGTH_LONG).setAction("Error", null).show();
                Log.w(TAG, "Failed to read value.", error.toException());
            }
        });
    }



    public void addGroupListener(String myGroupValue) {

        // sanity check for valid group key
        if( myGroupValue== null || myGroupValue.equals("")) return;

        // cancel previous listener
        if (mGroupRef != null && valueListenerGroup != null) mGroupRef.removeEventListener(valueListenerGroup);

        FirebaseMessaging.getInstance().subscribeToTopic(myGroupValue);
        Log.d(TAG, "Subscribed to group: " + myGroupValue);

        // add new listener
        mGroupRef= mDatabase.child(GROUPS_CHILD).child(myGroupValue);
        mGroupRef.addValueEventListener(new ValueEventListener() {

            @Override
            public void onDataChange(DataSnapshot snapshot) {
                valueListenerGroup = this;
                albumList.clear();
                albumList.add(privateAlbum);
                HashMap<String, Object> map = new HashMap<>();

                for (DataSnapshot snap : snapshot.getChildren()) {
                        map.put(snap.getKey(), snap.getValue());
                    }
                AlbumObject obj = new AlbumObject(snapshot.getKey(), map);
                albumList.add(obj);
                adapter.notifyDataSetChanged();
                addImageListener(myGroup);
            }

            @Override
            public void onCancelled(DatabaseError error) {
                // Failed to read value
                //Snackbar.make(getCurrentFocus(), "Failed to read user group information: " +  error.toException(), Snackbar.LENGTH_LONG).setAction("Error", null).show();
                Snackbar.make(getCurrentFocus(), "User has been ejected from current group event.", Snackbar.LENGTH_LONG).setAction("Error", null).show();
                myGroup = null;
                setGroupVisibility();
                albumList.clear();
                adapter.notifyDataSetChanged();
                Log.w(TAG, "Failed to read value.", error.toException());
            }
        });
    }

    public void addImageListener(String myGroupValue) {

        if(myGroupValue != null && !myGroupValue.equals("")) {

            // cancel previous listener
            if (mImageRef != null && valueListenerImage != null) mImageRef.removeEventListener(valueListenerImage);

            // add new listener
            mImageRef = mDatabase.child(IMAGES_CHILD).child(myGroupValue);
            mImageRef.addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot snapshot) {
                    valueListenerImage = this;
                    GalleryObject lastAdded = null;

                    for (DataSnapshot data : snapshot.getChildren()) {
                        HashMap<String, Object> map = new HashMap<>();
                        for (DataSnapshot snap : data.getChildren()) {
                            map.put(snap.getKey(), snap.getValue());
                        }
                        GalleryObject obj = new GalleryObject(data.getKey(), map);
                        for (int i = 0; i < albumList.size(); i++) {
                            if (albumList.get(i).getId() != null && albumList.get(i).getId().equals(myGroup)) {
                                int isNew = albumList.get(i).add(obj);
                                if (isNew>0) lastAdded = obj;
                            }
                        }
                    }
                    if (lastAdded != null && initDone) {
                        Snackbar.make(getCurrentFocus(), "User " + lastAdded.getAuthor() + " posted a new image.", Snackbar.LENGTH_LONG).setAction("Notification", null).show();
                    }
                    adapter.notifyDataSetChanged();
                    initDone = true;

                }

                @Override
                public void onCancelled(DatabaseError error) {
                    Snackbar.make(getCurrentFocus(), "Failed to read greoup images: " +  error.toException(), Snackbar.LENGTH_LONG).setAction("Error", null).show();
                    Log.w(TAG, "Failed to read value.", error.toException());
                }
            });
        }
    }

    public void doLogin()
    {
        mAuthListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                mUser = firebaseAuth.getCurrentUser();
                if (mUser != null) {

                    if(!initDone) {
                        // initialize user data and then load images from private folder
                        // then if succesfull initialize primary data listeners in order
                        addUserNameValueListenerEx();
                    }

                    // get id token
                    mUser.getIdToken(true)
                            .addOnCompleteListener(new OnCompleteListener<GetTokenResult>() {
                                public void onComplete(@NonNull Task<GetTokenResult> task) {
                                    if (task.isSuccessful()) {
                                        myID = task.getResult().getToken();
                                        String group = mDatabase.child(USERS_CHILD).child(mUser.getUid()).child(GROUP_CHILD).toString();
                                    } else {
                                        // Handle error -> task.getException();
                                    }
                                }
                            });

                } else {
                    // User is signed out
                    Log.d(TAG, "onAuthStateChanged:signed_out");

                    Intent i = new Intent(MainActivity.this, LoginActivity.class);
                    i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    startActivity(i);
                }

            }
        };

    }

    @Override
    public void onStart() {
        super.onStart();
        mAuth.addAuthStateListener(mAuthListener);
    }

    @Override
    public void onStop() {
        super.onStop();
        if (mAuthListener != null) {
            mAuth.removeAuthStateListener(mAuthListener);
        }
    }

    /* ======================================================== NOT IN USE ======================================================== */

    // XXX multigrouping removed from use due popular demand (SM)
    public void addMultiGroupImageListener() {
        mDatabase.child(IMAGES_CHILD).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                for (DataSnapshot keySnapshot : snapshot.getChildren()) {
                    for (DataSnapshot imageSnapshot : keySnapshot.getChildren()) {
                        HashMap<String, Object> map = new HashMap<>();
                        for (DataSnapshot dataSnapshot : imageSnapshot.getChildren()) {
                            map.put(dataSnapshot.getKey(), dataSnapshot.getValue());
                        }
                        GalleryObject obj = new GalleryObject(imageSnapshot.getKey(), map);

                        for (int i = 0; i < albumList.size(); i++) {
                            if (albumList.get(i).getId() != null && albumList.get(i).getId().equals(keySnapshot.getKey())) {
                                albumList.get(i).add(obj);
                            }
                        }
                    }
                }
                adapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(DatabaseError error) {
                Log.w(TAG, "Failed to read value.", error.toException());
            }
        });
    }


    // XXX multigrouping removed from use due popular demand (SM)
    public void addMultiGroupListener() {
        mDatabase.child(GROUPS_CHILD).addValueEventListener(new ValueEventListener() {

            @Override
            public void onDataChange(DataSnapshot snapshot) {
                albumList.clear();
                albumList.add(privateAlbum);

                for (DataSnapshot postSnapshot : snapshot.getChildren()) {
                    HashMap<String, Object> map = new HashMap<>();
                    for (DataSnapshot dataSnapshot : postSnapshot.getChildren()) {
                        map.put(dataSnapshot.getKey(), dataSnapshot.getValue());
                    }

                    AlbumObject obj = new AlbumObject(postSnapshot.getKey(), map);
                    albumList.add(obj);
                }
                adapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(DatabaseError error) {
                // Failed to read value
                Log.w(TAG, "Failed to read value.", error.toException());
            }
        });
    }


}

/* ======================================================== NOT IN USE ======================================================== */



