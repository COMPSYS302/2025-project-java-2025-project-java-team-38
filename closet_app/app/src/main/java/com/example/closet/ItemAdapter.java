package com.example.closet;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.widget.ImageView;
import android.widget.TextView;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.RequestOptions;

import java.util.List;

/**
 * Adapter for displaying clothing items in RecyclerView
 * Features:
 * - Image loading with Glide
 * - Double tap to like functionality
 * - Heart overlay for liked items
 * - Proper view recycling and performance optimization
 */
public class ItemAdapter extends RecyclerView.Adapter<ItemAdapter.ItemViewHolder> {

    private static final String TAG = "ItemAdapter";

    private Context context;
    private List<ClothingItem> items;
    private OnItemClickListener clickListener;
    private OnItemLikeListener likeListener;

    // Interfaces for callbacks
    public interface OnItemClickListener {
        void onItemClick(ClothingItem item, int position);
    }

    public interface OnItemLikeListener {
        void onItemLike(ClothingItem item, int position, boolean isLiked);
    }

    public ItemAdapter(Context context, List<ClothingItem> items) {
        this.context = context;
        this.items = items;
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.clickListener = listener;
    }

    public void setOnItemLikeListener(OnItemLikeListener listener) {
        this.likeListener = listener;
    }

    @NonNull
    @Override
    public ItemViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.row_list_item, parent, false);
        return new ItemViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ItemViewHolder holder, int position) {
        ClothingItem item = items.get(position);
        holder.bind(item, position);
    }

    @Override
    public int getItemCount() {
        return items != null ? items.size() : 0;
    }

    /**
     * Update the items list and notify adapter
     */
    public void updateItems(List<ClothingItem> newItems) {
        if (newItems != null) {
            this.items.clear();
            this.items.addAll(newItems);
            notifyDataSetChanged();
        }
    }

    /**
     * Clean up resources
     */
    public void cleanup() {
        if (context != null) {
            context = null;
        }
    }

    /**
     * ViewHolder class for item views
     */
    public class ItemViewHolder extends RecyclerView.ViewHolder {

        private ImageView imageItem;
        private ImageView imageHeart;
        private TextView textItemName;
        private TextView textItemSubtitle;
        private TextView textLikedStatus;
        private GestureDetector gestureDetector;

        public ItemViewHolder(@NonNull View itemView) {
            super(itemView);

            // Find views
            imageItem = itemView.findViewById(R.id.image_item);
            imageHeart = itemView.findViewById(R.id.image_heart);
            textItemName = itemView.findViewById(R.id.text_item_name);
            textItemSubtitle = itemView.findViewById(R.id.text_item_subtitle);
            textLikedStatus = itemView.findViewById(R.id.text_liked_status);

            // Set up gesture detector for double tap
            setupGestureDetector();
        }

        /**
         * Set up gesture detector for handling double tap on image
         */
        private void setupGestureDetector() {
            gestureDetector = new GestureDetector(context, new GestureDetector.SimpleOnGestureListener() {
                @Override
                public boolean onDoubleTap(MotionEvent e) {
                    int position = getAdapterPosition();
                    if (position != RecyclerView.NO_POSITION && likeListener != null) {
                        ClothingItem item = items.get(position);
                        boolean newLikeStatus = !item.isLikedByCurrentUser();
                        likeListener.onItemLike(item, position, newLikeStatus);
                        return true;
                    }
                    return false;
                }

                @Override
                public boolean onSingleTapConfirmed(MotionEvent e) {
                    int position = getAdapterPosition();
                    if (position != RecyclerView.NO_POSITION && clickListener != null) {
                        clickListener.onItemClick(items.get(position), position);
                        return true;
                    }
                    return false;
                }
            });

            // Set touch listener on the image for double tap
            imageItem.setOnTouchListener((v, event) -> {
                return gestureDetector.onTouchEvent(event);
            });

            // Set click listener on the entire item for normal tap
            itemView.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION && clickListener != null) {
                    clickListener.onItemClick(items.get(position), position);
                }
            });
        }

        /**
         * Bind data to views
         */
        public void bind(ClothingItem item, int position) {
            if (item == null) {
                Log.w(TAG, "Attempting to bind null item at position " + position);
                return;
            }

            try {
                // Set item name
                if (textItemName != null) {
                    String name = item.getName();
                    textItemName.setText(name != null ? name : "Unknown Item");
                }

                // Set subtitle (Material · Fit)
                if (textItemSubtitle != null) {
                    String material = item.getFabric();
                    String fit = item.getFit();

                    StringBuilder subtitle = new StringBuilder();
                    if (material != null && !material.isEmpty()) {
                        subtitle.append(material);
                    }
                    if (fit != null && !fit.isEmpty()) {
                        if (subtitle.length() > 0) {
                            subtitle.append(" · ");
                        }
                        subtitle.append(fit);
                    }

                    if (subtitle.length() == 0) {
                        subtitle.append("No details available");
                    }

                    textItemSubtitle.setText(subtitle.toString());
                }

                // Set liked status
                boolean isLiked = item.isLikedByCurrentUser();
                updateLikeStatus(isLiked);

                // Load image
                loadItemImage(item);

            } catch (Exception e) {
                Log.e(TAG, "Error binding item at position " + position, e);
            }
        }

        /**
         * Update the like status UI elements
         */
        private void updateLikeStatus(boolean isLiked) {
            // Show/hide heart overlay
            if (imageHeart != null) {
                imageHeart.setVisibility(isLiked ? View.VISIBLE : View.GONE);
            }

            // Show/hide liked status text
            if (textLikedStatus != null) {
                textLikedStatus.setVisibility(isLiked ? View.VISIBLE : View.GONE);
            }
        }

        /**
         * Load item image using Glide
         */
        private void loadItemImage(ClothingItem item) {
            if (imageItem == null) return;

            try {
                // 1) Get the List<String> of image URLs
                List<String> imageUrls = item.getImages();
                String imageUrl = null;

                // 2) If the list is non-null & non-empty, grab the first URL
                if (imageUrls != null && !imageUrls.isEmpty()) {
                    imageUrl = imageUrls.get(0);
                }

                RequestOptions requestOptions = new RequestOptions()
                        .placeholder(R.drawable.ic_placeholder_image)
                        .error(R.drawable.ic_placeholder_image)
                        .diskCacheStrategy(DiskCacheStrategy.ALL)
                        .centerCrop();

                // 3) Load the first URL (or fallback to placeholder if null/empty)
                if (imageUrl != null && !imageUrl.isEmpty()) {
                    Glide.with(context)
                            .load(imageUrl)
                            .apply(requestOptions)
                            .into(imageItem);
                } else {
                    Glide.with(context)
                            .load(R.drawable.ic_placeholder_image)
                            .apply(requestOptions)
                            .into(imageItem);
                }
            } catch (Exception e) {
                Log.e(TAG, "Error loading image for item: " + item.getName(), e);
                imageItem.setImageResource(R.drawable.ic_placeholder_image);
            }
        }

    }
}