package fi.aalto.mcc.mcc.adapter;

/**
 * Created by user on 17/11/2017.
 */



import android.content.Context;

import android.support.v7.widget.PopupMenu;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;

import java.util.ArrayList;
import java.util.List;

import fi.aalto.mcc.mcc.model.AlbumObject;
import fi.aalto.mcc.mcc.R;

public class AlbumViewAdapter extends RecyclerView.Adapter<AlbumViewAdapter.CustomViewHolder> {

    private String TAG = AlbumViewAdapter.class.getSimpleName();
    private ArrayList<AlbumObject> objectList;
    private Context c;

    public class CustomViewHolder extends RecyclerView.ViewHolder {
        public TextView  title, count;
        public ImageView thumbnail, indicator;

        public CustomViewHolder(View view) {
            super(view);
            title = (TextView) view.findViewById(R.id.title);
            count = (TextView) view.findViewById(R.id.count);
            thumbnail = (ImageView) view.findViewById(R.id.thumbnail);
            indicator = (ImageView) view.findViewById(R.id.indicator);
        }
    }


    public AlbumViewAdapter(Context context, ArrayList<AlbumObject> obj) {
        this.c = context;
        this.objectList = obj;
    }

    @Override
    public CustomViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.album, parent, false);

        return new CustomViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(final CustomViewHolder holder, int position) {
        AlbumObject album = objectList.get(position);

        if(album != null) {
            holder.title.setText(album.getName());
            holder.count.setText(album.size() + " images");
            String thumbnail = album.thumbnail();

            Log.d(TAG, album.getName() + " " + thumbnail);

            if (!album.isPublic())
                holder.indicator.setImageResource(R.drawable.ic_not_shared);

            if (thumbnail.isEmpty()) {
                Glide.with(c).load(R.drawable.no_image_thumb).into(holder.thumbnail);
            }  else {
                Glide.with(c).load(thumbnail).into(holder.thumbnail);
            }
        }
    }

    public interface ClickListener {
        void onClick(View view, int position);

        void onLongClick(View view, int position);
    }


    @Override
    public int getItemCount() {
        return objectList.size();
    }

    public static class AlbumTouchListener implements RecyclerView.OnItemTouchListener {

        private GestureDetector gestureDetector;
        private AlbumViewAdapter.ClickListener clickListener;

        public AlbumTouchListener(Context context, final RecyclerView recyclerView, final AlbumViewAdapter.ClickListener clickListener) {
            this.clickListener = clickListener;
            gestureDetector = new GestureDetector(context, new GestureDetector.SimpleOnGestureListener() {
                @Override
                public boolean onSingleTapUp(MotionEvent e) {
                    return true;
                }

                @Override
                public void onLongPress(MotionEvent e) {
                    View child = recyclerView.findChildViewUnder(e.getX(), e.getY());
                    if (child != null && clickListener != null) {
                        clickListener.onLongClick(child, recyclerView.getChildAdapterPosition(child));
                    }
                }
            });
        }

        @Override
        public boolean onInterceptTouchEvent(RecyclerView rv, MotionEvent e) {

            View child = rv.findChildViewUnder(e.getX(), e.getY());
            if (child != null && clickListener != null && gestureDetector.onTouchEvent(e)) {
                clickListener.onClick(child, rv.getChildAdapterPosition(child));
            }
            return false;
        }

        @Override
        public void onTouchEvent(RecyclerView rv, MotionEvent e) {
        }

        @Override
        public void onRequestDisallowInterceptTouchEvent(boolean disallowIntercept) {

        }
    }


}

