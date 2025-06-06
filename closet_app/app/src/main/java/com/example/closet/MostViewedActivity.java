package com.example.closet;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Window;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.List;

public class MostViewedActivity extends AppCompatActivity implements ItemAdapter.OnItemClickListener, ItemAdapter.OnItemLikeListener {

    private static final String TAG = "MostViewedActivity";

    private RecyclerView recyclerView;
    private ItemAdapter adapter;
    private final List<ClothingItem> mostViewedList = new ArrayList<>();

    private FirebaseFirestore firestore;
    private FirebaseAuth firebaseAuth;
    private String currentUserId;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Hide action bar
        supportRequestWindowFeature(Window.FEATURE_NO_TITLE);
        if (getSupportActionBar() != null) getSupportActionBar().hide();

        setContentView(R.layout.activity_most_viewed);

        recyclerView = findViewById(R.id.recycler_view_items);  // Match the ID in your layout

        firestore = FirebaseFirestore.getInstance();
        firebaseAuth = FirebaseAuth.getInstance();

        if (firebaseAuth.getCurrentUser() != null) {
            currentUserId = firebaseAuth.getCurrentUser().getUid();
        }

        adapter = new ItemAdapter(this, mostViewedList, R.layout.row_list_item);// You can change this to item_most_viewed if needed
        adapter.setOnItemClickListener(this);
        adapter.setOnItemLikeListener(this);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        loadMostViewedItems();
    }

    private void loadMostViewedItems() {
        firestore.collection("Clothes")
                .orderBy("Views", Query.Direction.DESCENDING)
                .limit(10)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    mostViewedList.clear();
                    for (DocumentSnapshot doc : querySnapshot) {
                        ClothingItem item = doc.toObject(ClothingItem.class);
                        if (item != null) {
                            item.setId(doc.getId());

                            List<String> likedUsers = (List<String>) doc.get("likedUsers");
                            boolean liked = likedUsers != null && currentUserId != null && likedUsers.contains(currentUserId);
                            item.setLikedByCurrentUser(liked);

                            mostViewedList.add(item);
                        }
                    }
                    adapter.updateItems(mostViewedList);
                    Log.d(TAG, "Loaded top 10 most viewed items");
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to load most viewed items", e);
                    Toast.makeText(this, "Error loading most viewed items", Toast.LENGTH_SHORT).show();
                });
    }

    @Override
    public void onItemClick(ClothingItem item, int position) {
        Intent intent = new Intent(this, DetailsActivity.class);
        intent.putExtra("ITEM_ID", item.getId());
        startActivity(intent);
    }

    @Override
    public void onItemLike(ClothingItem item, int position, boolean isLiked) {
        if (currentUserId == null || item.getId() == null) return;

        firestore.collection("Clothes")
                .document(item.getId())
                .update("likedUsers", isLiked
                        ? com.google.firebase.firestore.FieldValue.arrayUnion(currentUserId)
                        : com.google.firebase.firestore.FieldValue.arrayRemove(currentUserId))
                .addOnSuccessListener(aVoid -> {
                    item.setLikedByCurrentUser(isLiked);
                    adapter.notifyItemChanged(position);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to update like status", e);
                    item.setLikedByCurrentUser(!isLiked);
                    adapter.notifyItemChanged(position);
                    Toast.makeText(this, "Failed to update like", Toast.LENGTH_SHORT).show();
                });
    }
}
