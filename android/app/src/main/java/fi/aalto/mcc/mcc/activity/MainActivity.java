package fi.aalto.mcc.mcc.activity;

import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.CollapsingToolbarLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
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
import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GetTokenResult;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.ArrayList;
import java.util.List;

import fi.aalto.mcc.mcc.R;
import fi.aalto.mcc.mcc.adapter.AlbumViewAdapter;
import fi.aalto.mcc.mcc.model.AlbumObject;
import fi.aalto.mcc.mcc.helper.GridSpacingItemDecoration;
import fi.aalto.mcc.mcc.model.GalleryObject;


public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    private String TAG = "Main";
    private FirebaseAuth.AuthStateListener mAuthListener;
    private FirebaseAuth mAuth;
    private DatabaseReference mDatabase;

    private RecyclerView recyclerView;
    private AlbumViewAdapter adapter;
    private ArrayList<AlbumObject> albumList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance().getReference();

        mAuthListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                FirebaseUser user = firebaseAuth.getCurrentUser();
                if (user != null) {
                    // User is signed in
                    Log.d(TAG, "onAuthStateChanged:signed_in:" + user.getUid());


                    //Get id token and send to backend via HTTPS
                    user.getIdToken(true)
                            .addOnCompleteListener(new OnCompleteListener<GetTokenResult>() {
                                public void onComplete(@NonNull Task<GetTokenResult> task) {
                                    if (task.isSuccessful()) {
                                        String idToken = task.getResult().getToken();

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


        prepareAlbums();

        try {
            Glide.with(this).load(R.drawable.backdrop).into((ImageView) findViewById(R.id.backdrop));
        } catch (Exception e) {
            e.printStackTrace();
        }

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Add camera functionality here", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

    }

    /**
     * Converting dp to pixel
     */
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

    private void prepareAlbums() {

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
            else if(i == 11) obj.setAuthor("Seppo");
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


        a = new AlbumObject("Tyhjä", true);
        albumList.add(a);

        adapter.notifyDataSetChanged();
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
            Snackbar.make(findViewById(android.R.id.content), "Add camera functionality here", Snackbar.LENGTH_LONG)
                    .setAction("Action", null).show();

        } else if (id == R.id.nav_gallery) {

        } else if (id == R.id.nav_group) {
            Intent i = new Intent(MainActivity.this, GroupManagementActivity.class);
            FirebaseUser user = mAuth.getCurrentUser();
            if(user != null){
                String uid = mAuth.getCurrentUser().getUid();
                String group_id = mDatabase.child("Users").child(uid).child("group").toString();

                i.putExtra("USER_IN_GROUP", group_id != null);

                startActivity(i);
            }
        } else if (id == R.id.nav_account) {

        } else if (id == R.id.nav_settings) {

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
}
