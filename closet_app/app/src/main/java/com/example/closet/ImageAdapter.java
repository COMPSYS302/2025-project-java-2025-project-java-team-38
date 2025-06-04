package com.example.closet;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.util.List;

public class ImageAdapter extends RecyclerView.Adapter<ImageAdapter.ViewHolder> {

    private final Context context;
    private final List<String> imageUrls;

    public ImageAdapter(Context context, List<String> imageUrls) {
        this.context = context;
        this.imageUrls = imageUrls;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Inflate the layout file: res/layout/images.xml (must define an ImageView with @+id/image_view)
        View view = LayoutInflater.from(context).inflate(R.layout.images, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        String url = imageUrls.get(position);

        // Use Glide instead of Picasso
        Glide.with(context)
                .load(url)
                .placeholder(R.drawable.ic_placeholder_image)  // grey placeholder we added earlier
                .error(R.drawable.ic_error_image)              // add a "error" drawable in /res/drawable/
                .into(holder.imageView);
    }

    @Override
    public int getItemCount() {
        return imageUrls.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView imageView;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            // In images.xml, there must be an ImageView with id="@+id/image_view"
            imageView = itemView.findViewById(R.id.image_view);
        }
    }
}
