package com.example.closet;

import android.util.Log;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.List;

public class TopPicksManager {

    public interface TopPicksCallback {
        void onTopPicksLoaded(List<ClothingItem> items);
        void onError(Exception e);
    }

    public static void loadTopPicks(String currentUserId, TopPicksCallback callback) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        db.collection("Clothes")
                .orderBy("Likes", Query.Direction.DESCENDING) // ✅ Sort by Likes count
                .limit(3) // or 10 for Most Viewed button later
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    Log.d("TopPicksManager", "Fetched item count: " + querySnapshot.size());

                    List<ClothingItem> topPicks = new ArrayList<>();

                    for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        Long likesCount = doc.getLong("Likes"); // Optional: read likes count for debug
                        Log.d("TopPicksManager", "Doc ID: " + doc.getId() + ", Likes: " + likesCount);

                        ClothingItem item = doc.toObject(ClothingItem.class);
                        if (item != null) {
                            item.setId(doc.getId());

                            // Optional: keep likedUsers field in sync if it's still used
                            List<String> likedUsers = (List<String>) doc.get("likedUsers");
                            boolean liked = likedUsers != null && currentUserId != null && likedUsers.contains(currentUserId);
                            item.setLikedByCurrentUser(liked);

                            topPicks.add(item);
                            Log.d("TopPicksManager", "Added item: " + item.getName());
                        } else {
                            Log.e("TopPicksManager", "Item was null after parsing doc: " + doc.getId());
                        }
                    }

                    callback.onTopPicksLoaded(topPicks);
                })
                .addOnFailureListener(e -> {
                    Log.e("TopPicksManager", "Failed to load top picks", e);
                    callback.onError(e);
                });
    }


}
