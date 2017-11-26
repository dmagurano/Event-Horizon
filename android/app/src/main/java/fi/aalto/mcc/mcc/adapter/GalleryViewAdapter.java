package fi.aalto.mcc.mcc.adapter;


/**
 * Created by user on 14/11/2017.
 */

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;

import java.util.ArrayList;
import java.util.List;

import fi.aalto.mcc.mcc.model.AlbumObject;
import fi.aalto.mcc.mcc.model.GalleryObject;
import fi.aalto.mcc.mcc.R;


public class GalleryViewAdapter extends  RecyclerView.Adapter<GalleryViewAdapter.CustomViewHolder> {

    private static final int VIEW_HEADER = 0;
    private static final int VIEW_NORMAL = 1;

    private String TAG = GalleryViewAdapter.class.getSimpleName();
    private Context c;
    private View headerView;
    ArrayList<GalleryObject> gridArray;


    public class CustomViewHolder extends RecyclerView.ViewHolder {
        public ImageView thumbnail;
        public TextView header;

        public CustomViewHolder(View view) {
            super(view);
            thumbnail = (ImageView) view.findViewById(R.id.thumbnail);
            header = (TextView) view.findViewById(R.id.headertext);
        }
    }


    public GalleryViewAdapter(Context context, ArrayList<GalleryObject> gridArray) {
        this.c = context;
        this.gridArray = gridArray;
    }

    public void setHeader(View v) {
        this.headerView = v;
    }


    @Override
    public int getItemViewType(int position)
    {
        return gridArray.get(position).getType();

    }


    @Override
    public int getItemCount() {

        return gridArray.size();
    }


    @Override
    public CustomViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {

        if (viewType == VIEW_HEADER) {
            View itemView = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.list_header, parent, false);

            return new CustomViewHolder(itemView);

        } else {
            View itemView = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.image_thumbnail, parent, false);

            return new CustomViewHolder(itemView);

        }

    }

    @Override
    public void onBindViewHolder(CustomViewHolder holder, int position)
    {

        if ( gridArray.get(position).getType() == VIEW_HEADER )
        {
            holder.header.setText(gridArray.get(position).getHeader());
            return;

        }
        else Glide.with(c).load(gridArray.get(position).getSmall())
                    .thumbnail(0.5f)
                    .crossFade()
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .into(holder.thumbnail);

    }

    public interface ClickListener {
        void onClick(View view, int position);

        void onLongClick(View view, int position);
    }

    public static class GalleryTouchListener implements RecyclerView.OnItemTouchListener {

        private GestureDetector gestureDetector;
        private GalleryViewAdapter.ClickListener clickListener;

        public GalleryTouchListener(Context context, final RecyclerView recyclerView, final GalleryViewAdapter.ClickListener clickListener) {
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