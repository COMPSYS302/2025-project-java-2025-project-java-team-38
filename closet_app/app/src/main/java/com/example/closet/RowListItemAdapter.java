package com.example.closet;

import android.content.Context;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.util.Log;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.RequestOptions;

import java.util.ArrayList;
import java.util.List;

/**
 * Adapter responsible for displaying each ClothingItem in a RecyclerView row.
 * - Loads image via Glide (first URL from item.getImages()).
 * - Shows a heart overlay and “Liked” text if item.isLikedByCurrentUser() == true.
 * - Single‐tap on row → notifies OnItemClickListener.
 * - Double‐tap on image → toggles “like” via OnItemLikeListener.
 */
public class RowListItemAdapter extends RecyclerView.Adapter<RowListItemAdapter.ViewHolder> {

    private static final String TAG = "RowListItemAdapter";

    private final Context context;
    private final List<ClothingItem> items;
    private OnItemClickListener clickListener;
    private OnItemLikeListener likeListener;

    /** Callback interface for row clicks. */
    public interface OnItemClickListener {
        void onItemClick(ClothingItem item, int position);
    }

    /** Callback interface for like/unlike (double‐tap). */
    public interface OnItemLikeListener {
        void onItemLike(ClothingItem item, int position, boolean isLiked);
    }

    public RowListItemAdapter(Context context, List<ClothingItem> items) {
        this.context = context;
        // Make a defensive copy so updateItems() won’t clear the original list
        this.items = new ArrayList<>(items);
    }

    /** Register a row‐click listener. */
    public void setOnItemClickListener(OnItemClickListener listener) {
        this.clickListener = listener;
    }

    /** Register a double‐tap “like” listener. */
    public void setOnItemLikeListener(OnItemLikeListener listener) {
        this.likeListener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Inflate row_list_item.xml
        View view = LayoutInflater.from(context)
                .inflate(R.layout.row_list_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ClothingItem item = items.get(position);
        holder.bind(item, position);
    }

    @Override
    public int getItemCount() {
        return (items != null) ? items.size() : 0;
    }

    /** Update full list and refresh. */
    public void updateItems(List<ClothingItem> newItems) {
        if (newItems != null) {
            items.clear();
            items.addAll(newItems);
            notifyDataSetChanged();
        }
    }

    /** Clean up resources if needed. */
    public void cleanup() {
        // If you want to null‐out context reference (rarely needed in adapters), do it here.
    }

    /**
     * ViewHolder for a single row.
     */
    public class ViewHolder extends RecyclerView.ViewHolder {
        private final ImageView imageItem;
        private final ImageView imageHeart;
        private final TextView textItemName;
        private final TextView textItemSubtitle;

        private final GestureDetector gestureDetector;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            imageItem        = itemView.findViewById(R.id.image_item);
            imageHeart       = itemView.findViewById(R.id.image_heart);
            textItemName     = itemView.findViewById(R.id.text_item_name);
            textItemSubtitle = itemView.findViewById(R.id.text_item_subtitle);


            // Set up gesture detector for single‐tap and double‐tap on the image
            gestureDetector = new GestureDetector(context, new GestureDetector.SimpleOnGestureListener() {
                @Override
                public boolean onDoubleTap(MotionEvent e) {
                    int pos = getBindingAdapterPosition();
                    if (pos != RecyclerView.NO_POSITION && likeListener != null) {
                        ClothingItem clickedItem = items.get(pos);
                        boolean newLikeStatus = !clickedItem.isLikedByCurrentUser();
                        likeListener.onItemLike(clickedItem, pos, newLikeStatus);
                        return true;
                    }
                    return false;
                }

                @Override
                public boolean onSingleTapConfirmed(MotionEvent e) {
                    int pos = getBindingAdapterPosition();
                    if (pos != RecyclerView.NO_POSITION && clickListener != null) {
                        clickListener.onItemClick(items.get(pos), pos);
                        return true;
                    }
                    return false;
                }
            });

            // Attach touch listener to imageItem so double‐taps are detected on the image
            imageItem.setOnTouchListener((v, event) -> gestureDetector.onTouchEvent(event));

            // Entire row single‐tap (alternative entry point)
            itemView.setOnClickListener(v -> {
                int pos = getBindingAdapterPosition();
                if (pos != RecyclerView.NO_POSITION && clickListener != null) {
                    clickListener.onItemClick(items.get(pos), pos);
                }
            });
        }

        /** Bind data to views. */
        public void bind(ClothingItem item, int position) {
            if (item == null) {
                Log.w(TAG, "Attempting to bind null item at position " + position);
                return;
            }

            // 1) Name
            String name = item.getName();
            textItemName.setText((name != null) ? name : "Unknown Item");

            // 2) Subtitle = "fabric · fit"
            String material = item.getFabric();
            String fit      = item.getFit();
            StringBuilder subtitle = new StringBuilder();
            if (material != null && !material.isEmpty()) {
                subtitle.append(material);
            }
            if (fit != null && !fit.isEmpty()) {
                if (subtitle.length() > 0) subtitle.append(" · ");
                subtitle.append(fit);
            }
            if (subtitle.length() == 0) {
                subtitle.append("No details available");
            }
            textItemSubtitle.setText(subtitle.toString());


            // 4) Load image (first URL in item.getImages())
            List<String> imageUrls = item.getImages();
            String imageUrl = (imageUrls != null && !imageUrls.isEmpty()) ? imageUrls.get(0) : null;

            RequestOptions requestOptions = new RequestOptions()
                    .placeholder(R.drawable.ic_placeholder_image)
                    .error(R.drawable.ic_placeholder_image)
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .centerCrop();

            if (imageUrl != null && !imageUrl.isEmpty()) {
                Glide.with(context)
                        .load(imageUrl)
                        .apply(requestOptions)
                        .into(imageItem);
            } else {
                // Fallback to placeholder
                Glide.with(context)
                        .load(R.drawable.ic_placeholder_image)
                        .apply(requestOptions)
                        .into(imageItem);
            }
        }
    }
}
