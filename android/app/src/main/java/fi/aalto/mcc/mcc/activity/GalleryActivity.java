package fi.aalto.mcc.mcc.activity;

/**
 * Created by user on 18/11/2017.
 */

import android.os.Bundle;
import android.support.design.widget.CollapsingToolbarLayout;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.firebase.ui.storage.images.FirebaseImageLoader;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.ArrayList;

import fi.aalto.mcc.mcc.R;
import fi.aalto.mcc.mcc.fragment.ByCategory;
import fi.aalto.mcc.mcc.model.AlbumObject;


public class GalleryActivity extends AppCompatActivity {

    private static final int BY_AUTHOR = 0;
    private static final int BY_CATEGORY = 1;

    private static final String TAG = GalleryActivity.class.getSimpleName();
    private AlbumObject album;

    FirebaseStorage storage = FirebaseStorage.getInstance();
    StorageReference storageReference = storage.getReference();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gallery);

        album = (AlbumObject) getIntent().getSerializableExtra("album");

        final Toolbar toolbar = (Toolbar) findViewById(R.id.gallery_tab_toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) getSupportActionBar().setTitle(album.getName());
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        final ViewPager viewPager = (ViewPager) findViewById(R.id.gallery_tab_viewpager);
        TabPagerAdapter adapter = new TabPagerAdapter(getSupportFragmentManager());

        adapter.add(new ByCategory(album,BY_CATEGORY), "Category");
        adapter.add(new ByCategory(album,BY_AUTHOR), "Author");

        viewPager.setAdapter(adapter);

        TabLayout tabLayout = (TabLayout) findViewById(R.id.gallery_tab_tabs);
        tabLayout.setupWithViewPager(viewPager);

        final CollapsingToolbarLayout collapsingToolbarLayout = (CollapsingToolbarLayout) findViewById(R.id.gallery_tab_collapse_toolbar);

        try {
            String header_backdrop = album.backdrop();
            if (header_backdrop != null) {
                if(header_backdrop.startsWith("gs:"))
                     Glide.with(this).using(new FirebaseImageLoader()).load(storage.getReferenceFromUrl(header_backdrop)).into((ImageView) findViewById(R.id.gallery_tab_header));
                else Glide.with(this).load(album.backdrop()).into((ImageView) findViewById(R.id.gallery_tab_header));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                viewPager.setCurrentItem(tab.getPosition());
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {

            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {

            }
        });
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private static class TabPagerAdapter extends FragmentPagerAdapter {
        private final ArrayList<Fragment> fragmentList = new ArrayList<>();
        private final ArrayList<String> fragmentTitleList = new ArrayList<>();

        public TabPagerAdapter(FragmentManager manager) {
            super(manager);
        }

        @Override
        public Fragment getItem(int position) {
            return fragmentList.get(position);
        }

        @Override
        public int getCount() {
            return fragmentList.size();
        }

        public void add(Fragment fragment, String title) {
            fragmentList.add(fragment);
            fragmentTitleList.add(title);
        }

        @Override
        public CharSequence getPageTitle(int position) {
            return fragmentTitleList.get(position);
        }
    }


}
