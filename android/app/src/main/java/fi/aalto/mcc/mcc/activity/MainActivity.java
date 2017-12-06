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
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.CollapsingToolbarLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
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
import android.widget.Toast;

import com.google.android.gms.vision.Frame;
import com.google.android.gms.vision.barcode.Barcode;
import com.google.android.gms.vision.barcode.BarcodeDetector;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.remoteconfig.FirebaseRemoteConfig;

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

    private String TAG = "Main";
    public static String uploadURL = "https://mcc-fall-2017-g04.appspot.com/upload";

    public static final String IMAGES_CHILD = "Images";
    public static final String GROUP_CHILD  = "group";
    public static final String NAME_CHILD   = "name";
    public static final String GROUPS_CHILD = "Groups";
    public static final String USERS_CHILD  = "Users";

    private FirebaseAuth.AuthStateListener mAuthListener;
    private FirebaseAuth mAuth;
    private DatabaseReference mDatabase;
    private FirebaseUser mUser;
    private FirebaseRemoteConfig mRemoteConfig;


    BarcodeDetector barcodeDetector;

    static Uri fileUri;
    String uploadTarget;
    byte[] byteUploadTarget;
    String idToken;
    String myGroup;
    String myName;

    private RecyclerView recyclerView;
    private AlbumViewAdapter adapter;
    private ArrayList<AlbumObject> albumList;
    AlbumObject privateAlbum;

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
                AlbumObject obj = albumList.get(position);

                Intent intent = new Intent(MainActivity.this, GalleryActivity.class);

                if (intent != null && obj != null) {
                    intent.putExtra("album", obj);
                    startActivity(intent);
                }

            }

            @Override
            public void onLongClick(View view, int position) {

            }
        }));


        // add camera button to main view
        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snap(view);
            }
        });

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

        // initialize user name and load images from private folder
        addUserNameValueListener();

        // initialize primary data listeners in order
        addUserGroupValueListener();

    }


    public void Snap(View v) {
        if (checkPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            makeRequest(Manifest.permission.WRITE_EXTERNAL_STORAGE);
            return;
        } else if (checkPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {

            makeRequest(Manifest.permission.CAMERA);
            return;
        } else {
            final Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

            // XXX passing Uri to intent commmented out for time being (SM)
            // due to unknown crash
            // takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, fileUri);

            startActivityForResult(takePictureIntent, CAMERA_REQUEST_CODE);


            // XXX used for temporarily media selector for testing purposes (SM)
            //Intent intent = new Intent();
            //intent.setType("image/*");
            //intent.setAction(Intent.ACTION_GET_CONTENT);
            //startActivityForResult(Intent.createChooser(intent, "Select an image"), MEDIA_REQUEST_CODE);
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
        Uri uri = null;

        // decode answer from camera intent with or without image path
        if (requestCode == CAMERA_REQUEST_CODE && resultCode == RESULT_OK) {

            Log.i(TAG, "A photograph was selected");

            if (data != null) fileUri = data.getData();
            try {
                if (fileUri != null)
                    originalBitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), fileUri);
                else {
                    originalBitmap = (Bitmap) data.getExtras().get("data");
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            // in case media gallery was used instead of camera, decode file path to bitmap
        } else if (requestCode == MEDIA_REQUEST_CODE && resultCode == RESULT_OK) {
            try {
                if (data != null) fileUri = data.getData();
                originalBitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), fileUri);
                Log.i(TAG, "An image was selected from Media Gallery");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        if (originalBitmap != null) {

            // XXX image resize done server side now
            /*
            float aspectRatio = originalBitmap.getWidth() / (float) originalBitmap.getHeight();
            int width = 960;
            int height = Math.round(width / aspectRatio);
            originalBitmap = Bitmap.createScaledBitmap(originalBitmap, width, height, false);
            */
            int w = originalBitmap.getWidth();
            int h = originalBitmap.getHeight();

            // detect number of barcodes in image
            int value = doBarcodeClasssification(originalBitmap);

            if (value < 0) return;                                  // <-- classification failed
            else if (value > 0) savePrivateImage(originalBitmap);   // <-- save to private folder
            else uploadToServer(originalBitmap);                    // <-- publish to server
        }

    }

    private void uploadToServer(Bitmap bm) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bm.compress(Bitmap.CompressFormat.JPEG, 90, baos);
        byteUploadTarget = baos.toByteArray();
        //uploadTarget = Base64.encodeToString(byteUploadTarget, Base64.DEFAULT);

        new asyncUpload().execute();
    }


    public class asyncUpload extends AsyncTask<Void, Void, String> {

        private ProgressDialog busy = new ProgressDialog(MainActivity.this);

        protected void onPreExecute() {
            super.onPreExecute();
            // XXX crashes on emulator, but not on device (SM)
            busy.setMessage("Uploading to Server...");
            busy.show();
        }

        @Override
        protected String doInBackground(Void... params) {

            ArrayList<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>();
            nameValuePairs.add(new BasicNameValuePair("file", uploadTarget));

            try {
                HttpClient httpclient = new DefaultHttpClient();
                HttpPost httppost = new HttpPost(uploadURL);

                MultipartEntityBuilder builder = MultipartEntityBuilder.create();
                builder.setMode(HttpMultipartMode.BROWSER_COMPATIBLE);

                ContentBody cd = new ByteArrayBody(byteUploadTarget, "image/jpeg", "file.jpg");
                builder.addPart("file", cd);

                builder.addPart("idToken", new StringBody(idToken, ContentType.TEXT_PLAIN));
                builder.addPart("groupId", new StringBody(myGroup, ContentType.TEXT_PLAIN));
                builder.addPart("author_name", new StringBody(myName, ContentType.TEXT_PLAIN));
                httppost.setEntity(builder.build());

                HttpResponse response = httpclient.execute(httppost);
                String s = EntityUtils.toString(response.getEntity());
                //Toast.makeText(MainActivity.this, "Server: " + s, Toast.LENGTH_SHORT).show();
                Log.i(TAG, "Connection response: " + s);

            } catch (Exception e) {
                //Toast.makeText(MainActivity.this, "Connection error: " + e.toString(), Toast.LENGTH_SHORT).show();
                Log.i(TAG, "Connection error: " + e.toString());
            }
            Log.i(TAG, "Upload done");
            return "Success";
        }

        protected void onPostExecute(String result) {
            super.onPostExecute(result);
            busy.hide();
            busy.dismiss();
        }
    }


    public int doBarcodeClasssification(Bitmap bitmap) {
        if (!barcodeDetector.isOperational()) {
            /*
            new android.os.Handler().postDelayed(
                    new Runnable() {
                        public void run() {
                            doBarcodeClasssification(bitmap);
                            Log.e(TAG, "Barcode detector is not yet operational. Retrying after 10 seconds");
                        }
                    }, 10000);
                    */
            return -1;
        }

        Frame frame = new Frame.Builder().setBitmap(bitmap).build();
        // detecting barcodes
        SparseArray<Barcode> barcodes = barcodeDetector.detect(frame);
        return barcodes.size();
    }


    // Converting dp to pixel
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
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.nav_camera) {
            Snap(this.getCurrentFocus());
        } else if (id == R.id.nav_gallery) {

        } else if (id == R.id.nav_group) {
            Intent i = new Intent(MainActivity.this, GroupManagementActivity.class);
            FirebaseUser user = mAuth.getCurrentUser();
            if (user != null) {
                String uid = mAuth.getCurrentUser().getUid();
                String group_id = mDatabase.child(USERS_CHILD).child(uid).child(GROUP_CHILD).toString();

                i.putExtra("USER_IN_GROUP", group_id != null);

                startActivity(i);
            }
        } else if (id == R.id.nav_account) {

        } else if (id == R.id.nav_settings) {

            Intent intent = new Intent(MainActivity.this, SettingsActivity.class);
            if (intent != null) {
                startActivity(intent);
            }

        } else if (id == R.id.nav_logout) {
            mAuth.signOut();
        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
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


    public AlbumObject makePrivateAlbum() {

        AlbumObject a = new AlbumObject("Private", false);
        loadPrivateImages(a);

        return a;
    }

    private void loadPrivateImages(AlbumObject to) {
        GalleryObject obj = null;
        String path = Environment.getExternalStorageDirectory().toString() + "/Android/data/"
                + getApplicationContext().getPackageName()
                + "/MyFiles";
        File directory = new File(path);
        File[] files = directory.listFiles();
        if (files != null) {
            for (int i = 0; i < files.length; i++) {
                obj = new GalleryObject(Uri.fromFile(files[i]), myName);
                to.add(obj);
            }
        }
    }


    public void savePrivateImage(Bitmap bmp) {
        fileUri = storeImage(bmp);

        if (fileUri != null) {
            GalleryObject obj = new GalleryObject(fileUri, myName);
            privateAlbum.add(obj);

            adapter.notifyDataSetChanged();
            Toast.makeText(MainActivity.this, "Image added to Private folder", Toast.LENGTH_SHORT).show();
        }
    }

    private Uri storeImage(Bitmap image) {
        File pictureFile = getOutputMediaFile();
        if (pictureFile == null) {
            return null;
        }
        try {
            FileOutputStream fos = new FileOutputStream(pictureFile);
            image.compress(Bitmap.CompressFormat.JPEG, 90, fos);
            fos.close();
            return Uri.fromFile(pictureFile);
        } catch (FileNotFoundException e) {
            Log.d(TAG, "File not found: " + e.getMessage());
        } catch (IOException e) {
            Log.d(TAG, "Error accessing file: " + e.getMessage());
        }
        return null;
    }

    private File getOutputMediaFile() {
        File mediaStorageDir = new File(Environment.getExternalStorageDirectory()
                + "/Android/data/"
                + getApplicationContext().getPackageName()
                + "/MyFiles");

        // Create the storage directory if it does not exist
        if (!mediaStorageDir.exists()) {
            if (!mediaStorageDir.mkdirs()) {
                return null;
            }
        }
        // Create a media file name
        String timeStamp = new SimpleDateFormat("ddMMyyyy_HHmmss").format(new Date());
        File mediaFile;
        String mImageName = "MI_" + timeStamp + ".jpg";
        mediaFile = new File(mediaStorageDir.getPath() + File.separator + mImageName);
        return mediaFile;
    }



    public void addUserNameValueListener() {
        mDatabase.child(USERS_CHILD).child(mUser.getUid()).child(NAME_CHILD).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                if( snapshot.getValue() != null) {
                    myName = snapshot.getValue().toString();
                }
                privateAlbum = makePrivateAlbum();
            }

            @Override
            public void onCancelled(DatabaseError error) {
                // Failed to read value
                Log.w(TAG, "Failed to read value.", error.toException());
            }
        });
    }


    public void addUserGroupValueListener() {
        mDatabase.child(USERS_CHILD).child(mUser.getUid()).child(GROUP_CHILD).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                if( snapshot.getValue() != null) {
                    myGroup = snapshot.getValue().toString();
                    addGroupListener(myGroup);
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {
                // Failed to read value
                Log.w(TAG, "Failed to read value.", error.toException());
            }
        });
    }

    public void addGroupListener(String myGroupValue) {
        mDatabase.child(GROUPS_CHILD).child(myGroupValue).addValueEventListener(new ValueEventListener() {

            @Override
            public void onDataChange(DataSnapshot snapshot) {
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
                Log.w(TAG, "Failed to read value.", error.toException());
            }
        });
    }

    public void addImageListener(String myGroupValue) {

        if(myGroupValue != null && !myGroupValue.equals("")) {
            mDatabase.child(IMAGES_CHILD).child(myGroupValue).addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot snapshot) {

                    for (DataSnapshot data : snapshot.getChildren())
                    {
                        HashMap<String, Object> map = new HashMap<>();
                        for (DataSnapshot snap : data.getChildren()) {
                            map.put(snap.getKey(), snap.getValue());
                        }
                        GalleryObject obj = new GalleryObject(data.getKey(), map);
                        for (int i = 0; i < albumList.size(); i++) {
                            if (albumList.get(i).getId() != null && albumList.get(i).getId().equals(myGroup)) {
                                albumList.get(i).add(obj);
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
    }

    public void doLogin()
    {
        mAuthListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                mUser = firebaseAuth.getCurrentUser();
                if (mUser != null) {
                    // User is signed in
                    Log.d(TAG, "onAuthStateChanged:signed_in:" + mUser.getUid());

                    if (mUser.getPhotoUrl() != null) {
                        //userContext.setAvatarImage(mUser.getPhotoUrl().toString());
                    }

                    //Get id token
                    mUser.getIdToken(true)
                            .addOnCompleteListener(new OnCompleteListener<GetTokenResult>() {
                                public void onComplete(@NonNull Task<GetTokenResult> task) {
                                    if (task.isSuccessful()) {
                                        idToken = task.getResult().getToken();
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



