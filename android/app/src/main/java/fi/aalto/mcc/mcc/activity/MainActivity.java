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
import java.nio.charset.Charset;
import java.util.ArrayList;

import com.bumptech.glide.Glide;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
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
    public static final int MEDIA_REQUEST_CODE = 2;
    public static final int RECORD_REQUEST_CODE = 3;
    private String TAG = "Main";
    public static String uploadURL = "https://mcc-fall-2017-g04.appspot.com/upload";
    public static final String IMAGES_CHILD = "Images";

    private FirebaseAuth.AuthStateListener mAuthListener;
    private FirebaseAuth mAuth;
    private DatabaseReference mDatabase;
    private FirebaseUser mUser;
    private FirebaseRemoteConfig mRemoteConfig;


    BarcodeDetector barcodeDetector;

    static Uri fileUri;
    String uploadTarget;
    byte[] byteUploadTarget;
    UserObject userContext;

    private RecyclerView recyclerView;
    private AlbumViewAdapter adapter;
    private ArrayList<AlbumObject> albumList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mAuth = FirebaseAuth.getInstance();
        mUser = mAuth.getCurrentUser();
        mDatabase = FirebaseDatabase.getInstance().getReference();

        userContext = new UserObject("nano", "nano");

        mAuthListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                mUser = firebaseAuth.getCurrentUser();
                if (mUser != null) {
                    // User is signed in
                    Log.d(TAG, "onAuthStateChanged:signed_in:" + mUser.getUid());

                    userContext.setUid(mUser.getUid());
                    userContext.setName(mUser.getDisplayName());
                    if (mUser.getPhotoUrl() != null) {
                        userContext.setAvatarImage(mUser.getPhotoUrl().toString());
                    }

                    //Get id token and send to backend via HTTPS
                    mUser.getIdToken(true)
                            .addOnCompleteListener(new OnCompleteListener<GetTokenResult>() {
                                public void onComplete(@NonNull Task<GetTokenResult> task) {
                                    if (task.isSuccessful()) {
                                        String idToken = task.getResult().getToken();
                                        userContext.setAuthToken(idToken);
                                        String group = mDatabase.child("Users").child(mUser.getUid()).child("group").toString();
                                        userContext.setGroup(group);

                                        Log.d(TAG, "IDtoken: " + idToken);

                                        //TODO: send the token to backend via HTTPS

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

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        initCoolbar();

        recyclerView = (RecyclerView) findViewById(R.id.recycler_view);

        albumList = new ArrayList<>();
        adapter = new AlbumViewAdapter(this, albumList);

        RecyclerView.LayoutManager mLayoutManager = new GridLayoutManager(this, 2);
        recyclerView.setLayoutManager(mLayoutManager);
        recyclerView.addItemDecoration(new GridSpacingItemDecoration(2, dpToPx(3), true));
        recyclerView.setItemAnimator(new DefaultItemAnimator());
        recyclerView.setAdapter(adapter);

        barcodeDetector =
                new BarcodeDetector.Builder(getApplicationContext())
                        .setBarcodeFormats(Barcode.ALL_FORMATS)
                        .build();

        recyclerView.addOnItemTouchListener(new AlbumViewAdapter
                .AlbumTouchListener(this
                .getApplicationContext(), recyclerView, new AlbumViewAdapter.ClickListener() {

            @Override
            public void onClick(View view, int position) {
                AlbumObject obj = albumList.get(position);

                Intent intent = new Intent(MainActivity.this, GalleryActivity.class);

                if (intent != null && obj != null )
                {
                    intent.putExtra("album", obj);
                    startActivity(intent);
                }

            }

            @Override
            public void onLongClick(View view, int position) {

            }
        }));



        try {
            Glide.with(this).load(R.drawable.backdrop).into((ImageView) findViewById(R.id.backdrop));
        } catch (Exception e) {
            e.printStackTrace();
        }

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snap(view);
            }
        });

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);


        mDatabase.child(IMAGES_CHILD).addValueEventListener(new ValueEventListener() {

            @Override
            public void onDataChange(DataSnapshot snapshot) {
                Log.e("Count " ,""+snapshot.getChildrenCount());
                for (DataSnapshot postSnapshot: snapshot.getChildren())
                {
                    HashMap<String, Object> map = new HashMap<>();
                    for (DataSnapshot dataSnapshot: postSnapshot.getChildren())
                    {
                        map.put(dataSnapshot.getKey(), dataSnapshot.getValue());
                    }
                    // XXX incomplete implamentation
                    AlbumObject ao= albumList.get(1);
                    GalleryObject obj = new GalleryObject(postSnapshot.getKey(), map, "unknown user");
                    ao.add(obj);
                    adapter.notifyDataSetChanged();

                }
            }
            @Override
            public void onCancelled(DatabaseError error) {
                // Failed to read value
                Log.w(TAG, "Failed to read value.", error.toException());
            }
        });

        // XXX to be removed (SM)
        makeDummyAlbums();
    }


    public void Snap(View v)
    {
        if(checkPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED)
        {
            makeRequest(Manifest.permission.WRITE_EXTERNAL_STORAGE);
            return;
        } else if(checkPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {

            makeRequest(Manifest.permission.CAMERA);
            return;
        } else
        {
            // XXX passing Uri to intent commmented out for time being (SM)
            // due to unknown crash
            /*
            final Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

            File xfile = getOutputMediaFile();
            if (!xfile.exists()) {
                try {
                    xfile.createNewFile();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else {
                xfile.delete();
                try {
                    xfile.createNewFile();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            fileUri = Uri.fromFile(xfile);

            takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, fileUri);
            startActivityForResult(takePictureIntent, CAMERA_REQUEST_CODE);
            */

            // XXX using temporarily media selector for testing purposes (SM)
            Intent intent = new Intent();
            intent.setType("image/*");
            intent.setAction(Intent.ACTION_GET_CONTENT);
            startActivityForResult(Intent.createChooser(intent, "Select an image"), MEDIA_REQUEST_CODE);
        }


    }


    private int checkPermission(String permission) {
        return ContextCompat.checkSelfPermission(this, permission);
    }

    private void makeRequest(String permission) {
        ActivityCompat.requestPermissions(this, new String[]{permission}, RECORD_REQUEST_CODE);
    }


    public Uri getImageUri(Context inContext, Bitmap inImage) {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        inImage.compress(Bitmap.CompressFormat.JPEG, 100, bytes);
        String path = MediaStore.Images.Media.insertImage(inContext.getContentResolver(), inImage, "Title", null);
        return Uri.parse(path);
    }

    public String getRealPathFromURI(Uri uri) {
        Cursor cursor = getContentResolver().query(uri, null, null, null, null);
        cursor.moveToFirst();
        int idx = cursor.getColumnIndex(MediaStore.Images.ImageColumns.DATA);
        return cursor.getString(idx);
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        Bitmap originalBitmap = null;
        Uri uri = null;

        // decode answer from camera intent with or without image path
        if (requestCode == CAMERA_REQUEST_CODE && resultCode == RESULT_OK) {

            Log.i(TAG, "A photograph was selected");

            if (data != null )fileUri = data.getData();
            try {
                if (fileUri != null) originalBitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), fileUri);
                else originalBitmap = (Bitmap) data.getExtras().get("data");
            } catch (IOException e) {
                e.printStackTrace();
            }
        // in case media gallery was used instead of camera, decode file path to bitmap
        } else if(requestCode == MEDIA_REQUEST_CODE && resultCode==RESULT_OK) {
            try {
                if (data != null )fileUri = data.getData();
                originalBitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), fileUri);
                Log.i(TAG, "An image was selected from Media Gallery");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        if (originalBitmap != null) {

            /*
            // do image resize if needed
            float aspectRatio = originalBitmap.getWidth() / (float) originalBitmap.getHeight();
            int width = 960;
            int height = Math.round(width / aspectRatio);
            originalBitmap = Bitmap.createScaledBitmap(originalBitmap, width, height, false);
            */

            /*
            FileOutputStream out = null;
            try {
                out = new FileOutputStream(fileUri.toString());
                originalBitmap.compress(Bitmap.CompressFormat.JPEG, 100, out);
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                try {
                    if (out != null) {
                        out.close();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }*/

            // detect number of barcodes in image
            int value = doBarcodeClasssification(originalBitmap);

            if (value < 0) {
                // classification failed
                return;
            } else {

                AlbumObject ao;

                // XXX need to build album selector based on group later
                if( value == 0 ) ao = albumList.get(1); // put in public folder
                else ao = albumList.get(0);             // put in private folder


                GalleryObject obj = new GalleryObject();
                obj.setCategory("Not Human");
                obj.setAuthor("Teppo");
                obj.setSmall(fileUri.toString());
                obj.setLarge(fileUri.toString());
                ao.add(obj);
                adapter.notifyDataSetChanged();

                // send new public image to server (testing image upload here)
                if (value == 0) uploadToServer(originalBitmap);
            }

        }

    }

    private void uploadToServer(Bitmap bm) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bm.compress(Bitmap.CompressFormat.JPEG, 90, baos);
        byteUploadTarget = baos.toByteArray();
        //uploadTarget = Base64.encodeToString(byteUploadTarget, Base64.DEFAULT);
        //Log.i("base64", "uploading data: " + uploadTarget);

        new asyncUpload().execute();
    }


    public class asyncUpload extends AsyncTask<Void, Void, String> {

        private ProgressDialog busy = new ProgressDialog(MainActivity.this);
        protected void onPreExecute() {
            super.onPreExecute();
            busy.setMessage("Uploading to Server...");
            busy.show();
        }

        @Override
        protected String doInBackground(Void... params) {

            ArrayList<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>();
            nameValuePairs.add(new BasicNameValuePair("file", uploadTarget));


            //nameValuePairs.add(new BasicNameValuePair("idToken", "potato"));
            //nameValuePairs.add(new BasicNameValuePair("groupId", "sausage"));

            //nameValuePairs.add(new BasicNameValuePair("idToken", userContext.getAuthToken()));
            //nameValuePairs.add(new BasicNameValuePair("groupId", userContext.getGroup()));

            try {
                HttpClient httpclient = new DefaultHttpClient();
                HttpPost httppost = new HttpPost(uploadURL);


                MultipartEntityBuilder builder = MultipartEntityBuilder.create();
                builder.setMode(HttpMultipartMode.BROWSER_COMPATIBLE);

                ContentBody cd = new ByteArrayBody(byteUploadTarget, "image/jpeg", "file.jpg");
                builder.addPart("file",cd);


                //builder.addPart("file", new StringBody(uploadTarget, ContentType.TEXT_PLAIN));
                builder.addPart("idToken", new StringBody(userContext.getAuthToken(), ContentType.TEXT_PLAIN));
                builder.addPart("groupId", new StringBody(userContext.getGroup(), ContentType.TEXT_PLAIN));
                httppost.setEntity(builder.build());

                //httppost.setEntity(new UrlEncodedFormEntity(nameValuePairs));

                HttpResponse response = httpclient.execute(httppost);
                String s = EntityUtils.toString(response.getEntity());
                Log.i(TAG, "Connection response: " + s);

            } catch (Exception e) {
                Log.i(TAG, "Error in http connection " + e.toString());
            }
            Log.i(TAG, "Upload done" );
            return "Success";
        }

        protected void onPostExecute(String result) {
            super.onPostExecute(result);
            busy.hide();
            busy.dismiss();
        }
    }



    private File getOutputMediaFile(){
        File mediaStorageDir = new File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES), "EventPictures");

        if (!mediaStorageDir.exists()){
            if (!mediaStorageDir.mkdirs()){
                Log.d(this.TAG, "failed to create directory");
                return null;
            }
        }

        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        return new File(mediaStorageDir.getPath() + File.separator +
                "IMG_"+ timeStamp + ".jpg");
    }


    public int doBarcodeClasssification(Bitmap bitmap )
    {
        if (!barcodeDetector.isOperational() ) {
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

    private void initCoolbar() {
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
            if(user != null){
                String uid = mAuth.getCurrentUser().getUid();
                String group_id = mDatabase.child("Users").child(uid).child("group").toString();
                userContext.setGroup(group_id);

                i.putExtra("USER_IN_GROUP", group_id != null);

                startActivity(i);
            }
        } else if (id == R.id.nav_account) {

        } else if (id == R.id.nav_settings) {

            Intent intent = new Intent(MainActivity.this, SettingsActivity.class);
            if (intent != null)
            {
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




    //  a garbage generator for testing purposes
    private void makeDummyAlbums() {

        String [] samples = getResources().getStringArray(R.array.test_images);;

        AlbumObject a = new AlbumObject("Private", false);
        for (int i = 0; i < 8; i++) {
            GalleryObject obj = new GalleryObject();
            if(i%2 == 0) obj.setCategory("Human");
            else obj.setCategory("Not Human");
            obj.setAuthor("Pätkä");
            if(i%3 == 0) obj.setAuthor("Pekka");
            obj.setSmall(samples[i]);
            obj.setLarge(samples[i]);
            a.add(obj);
        }
        albumList.add(a);

        a = new AlbumObject("IXDA", true);
        for (int i = 8; i < 20; i++) {
            GalleryObject obj = new GalleryObject();
            if(i == 9) obj.setCategory("Human");
            else if(i == 10) obj.setCategory("Not human");
            else if(i == 11) obj.setCategory("Something else");
            else obj.setCategory("Test");


            if(i == 9) obj.setAuthor("Matti");
            else if(i == 10) obj.setAuthor("Teppo");
            else if(i == 11) obj.setAuthor("Teppo");
            else if(i == 12) obj.setAuthor("Seppo");
            else if(i == 13) obj.setAuthor("Seppo");
            else if(i == 14) obj.setAuthor("Seppo");
            else if(i == 15) obj.setAuthor("Seppo");
            else obj.setAuthor("Jaakko");


            obj.setSmall(samples[i]);
            obj.setLarge(samples[i]);
            a.add(obj);
        }
        albumList.add(a);

        a = new AlbumObject("Wappu", true);
        for (int i = 20; i < 32; i++) {
            GalleryObject obj = new GalleryObject();
            if(i%2 == 0) obj.setCategory("Human");
            else obj.setCategory("Not Human");
            obj.setAuthor("Pätkä");
            if(i%3 == 0) obj.setAuthor("Pekka");

            obj.setSmall(samples[i]);
            obj.setLarge(samples[i]);
            a.add(obj);
        }
        albumList.add(a);
        a = new AlbumObject("Miscellaneous", true);
        for (int i = 32; i < 40; i++) {
            GalleryObject obj = new GalleryObject();
            if(i%2 == 0) obj.setCategory("Human");
            else obj.setCategory("Not Human");
            obj.setAuthor("Pätkä");
            if(i%3 == 0) obj.setAuthor("Pekka");

            obj.setSmall(samples[i]);
            obj.setLarge(samples[i]);
            a.add(obj);
        }
        albumList.add(a);


        a = new AlbumObject("Empty folder", true);
        albumList.add(a);

        adapter.notifyDataSetChanged();
    }


}
