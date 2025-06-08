package com.example.closet;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

public class TopPicksActivity extends AppCompatActivity
        implements ItemAdapter.OnItemClickListener, ItemAdapter.OnItemLikeListener {

    private static final String TAG = "TopPicksActivity";

    private RecyclerView recyclerView;
    private EditText searchBar;
    private ItemAdapter adapter;
    private final List<ClothingItem> topPicksList = new ArrayList<>();
    private final List<ClothingItem> filteredList = new ArrayList<>();

    private FirebaseFirestore firestore;
    private FirebaseAuth firebaseAuth;
    private String currentUserId;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        supportRequestWindowFeature(Window.FEATURE_NO_TITLE);
        if (getSupportActionBar() != null) getSupportActionBar().hide();

        setContentView(R.layout.activity_top_picks);

        firestore = FirebaseFirestore.getInstance();
        firebaseAuth = FirebaseAuth.getInstance();
        if (firebaseAuth.getCurrentUser() != null) {
            currentUserId = firebaseAuth.getCurrentUser().getUid();
        }

        // Logo click = return to main
        findViewById(R.id.logo_title).setOnClickListener(v -> goHome());
        findViewById(R.id.logo_icon).setOnClickListener(v -> goHome());

        // Hamburger menu setup
        DrawerLayout drawerLayout = findViewById(R.id.drawer_layout);
        ImageView hamburgerIcon = findViewById(R.id.hamburger_icon);
        hamburgerIcon.setOnClickListener(v -> drawerLayout.open());

        NavigationView navigationView = findViewById(R.id.navigation_view);
        navigationView.setNavigationItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.nav_home) {
                startActivity(new Intent(this, MainActivity.class));
            } else if (itemId == R.id.nav_top_picks) {
                // Already here
            } else if (itemId == R.id.nav_most_viewed) {
                startActivity(new Intent(this, MostViewedActivity.class));
            } else if (itemId == R.id.nav_favourites) {
                startActivity(new Intent(this, FavouritesActivity.class));
            }
            drawerLayout.closeDrawers();
            return true;
        });

        // RecyclerView + Adapter
        recyclerView = findViewById(R.id.recycler_view_items);
        adapter = new ItemAdapter(this, filteredList, R.layout.row_list_item);
        adapter.setOnItemClickListener(this);
        adapter.setOnItemLikeListener(this);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        // Search Bar setup
        searchBar = findViewById(R.id.search_bar);
        searchBar.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            @Override public void afterTextChanged(Editable s) { }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterTopPicks(s.toString());
            }
        });

        // Load data
        loadTopPicks();
    }

    private void goHome() {
        Intent intent = new Intent(TopPicksActivity.this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }

    private void loadTopPicks() {
        TopPicksManager.loadTopPicks(currentUserId, new TopPicksManager.TopPicksCallback() {
            @Override
            public void onTopPicksLoaded(List<ClothingItem> items) {
                topPicksList.clear();
                topPicksList.addAll(items);

                filteredList.clear();
                filteredList.addAll(items);

                adapter.updateItems(filteredList);
                Log.d(TAG, "Loaded top picks successfully");
            }

            @Override
            public void onError(Exception e) {
                Log.e(TAG, "Failed to load top picks", e);
                Toast.makeText(TopPicksActivity.this, "Error loading top picks", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void filterTopPicks(String query) {
        filteredList.clear();
        String lower = query.toLowerCase().trim();

        for (ClothingItem item : topPicksList) {
            boolean matches = false;

            if (item.getName() != null && item.getName().toLowerCase().contains(lower)) {
                matches = true;
            } else if (item.getCategory() != null && item.getCategory().toLowerCase().contains(lower)) {
                matches = true;
            } else if (item.getFabric() != null && item.getFabric().toLowerCase().contains(lower)) {
                matches = true;
            } else if (item.getFit() != null && item.getFit().toLowerCase().contains(lower)) {
                matches = true;
            } else if (item.getCare() != null && item.getCare().toLowerCase().contains(lower)) {
                matches = true;
            }

            if (matches) {
                filteredList.add(item);
            }
        }

        adapter.updateItems(filteredList);

        findViewById(R.id.text_empty_state).setVisibility(
                filteredList.isEmpty() ? View.VISIBLE : View.GONE
        );
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
                    Log.e(TAG, "Failed to update like", e);
                    item.setLikedByCurrentUser(!isLiked);
                    adapter.notifyItemChanged(position);
                    Toast.makeText(this, "Failed to update like", Toast.LENGTH_SHORT).show();
                });
    }
}
