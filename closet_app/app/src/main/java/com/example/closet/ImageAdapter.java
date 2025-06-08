package com.example.closet;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;
import java.util.List;

public class ImageAdapter extends RecyclerView.Adapter<ImageAdapter.ViewHolder> {

    private final Context context;
    private final List<String> imageUrls;

    // Sets context and image urls
    public ImageAdapter(Context context, List<String> imageUrls) {
        this.context = context;
        this.imageUrls = imageUrls;
    }


    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.images, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        String url = imageUrls.get(position);

        if (url != null && url.startsWith("http")) {
            // try catch to ensure app doesn't crash
            try {
                Picasso.get()
                        .load(url)
                        .placeholder(R.drawable.clothes) // pending image
                        .error(R.drawable.hanger)        // broken link image
                        .into(holder.imageView, new Callback() {
                            @Override
                            public void onSuccess() {
                                Log.d("ImageAdapter", "Loaded: " + url);
                            }

                            @Override
                            public void onError(Exception e) {
                                Log.e("ImageAdapter", "Failed to load: " + url, e);
                            }
                        });
            } catch (Exception e) {
                Log.e("ImageAdapter", "Exception during image load at position " + position + ": " + url, e);
                holder.imageView.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.dot_unseen));
            }
        } else {
            Log.w("ImageAdapter", "Invalid URL at position " + position + ": " + url);
            holder.imageView.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.dot_unseen));
        }
    }

    @Override
    public int getItemCount() {
        return imageUrls.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView imageView;

        // holds images to be viewed
        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.image_view);
        }
    }
}