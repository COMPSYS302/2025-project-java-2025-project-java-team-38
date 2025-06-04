package com.example.closet;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.List;

/**
 * Activity for displaying a list of clothing items filtered by category.
 * Now also checks whether the current user has "liked" each item,
 * so the adapter can show a heart overlay when appropriate.
 */
public class ListActivity extends AppCompatActivity
        implements RowListItemAdapter.OnItemClickListener,
        RowListItemAdapter.OnItemLikeListener {

    private static final String TAG = "ListActivity";
    public static final String EXTRA_CATEGORY = "category";

    // UI Components
    private RecyclerView recyclerViewItems;
    private ProgressBar progressBar;
    private TextView textEmptyState;
    private DrawerLayout drawerLayout;
    private ImageView hamburgerIcon;
    private ImageView logoIcon;
    private TextView logoTitle;
    private EditText searchBar;

    // Data components
    private FirebaseFirestore firestore;
    private RowListItemAdapter itemAdapter;
    private List<ClothingItem> clothingItems;
    private List<ClothingItem> filteredItems; // For search functionality
    private String selectedCategory;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_list);

        // 1) Initialize all view references
        initializeViews();

        // 2) Read category from intent (default to "Shirts" if missing)
        selectedCategory = getIntent().getStringExtra(EXTRA_CATEGORY);
        if (selectedCategory == null) {
            selectedCategory = "Shirts";
        }

        // 3) Setup header (logo/title click actions)
        setupHeader();

        // 4) Initialize Firestore
        firestore = FirebaseFirestore.getInstance();

        // 5) Initialize data lists
        clothingItems = new ArrayList<>();
        filteredItems = new ArrayList<>();

        // 6) Set up RecyclerView + adapter
        setupRecyclerView();

        // 7) Set up search‐bar listener
        setupSearch();

        // 8) Set up navigation drawer toggle
        setupNavigation();

        // 9) Finally, load data from Firestore
        loadClothingItems();
    }

    /** Initialize all view components (findViewById). */
    private void initializeViews() {
        recyclerViewItems = findViewById(R.id.recycler_view_items);
        progressBar       = findViewById(R.id.progress_bar);
        textEmptyState    = findViewById(R.id.text_empty_state);
        drawerLayout      = findViewById(R.id.drawer_layout);
        hamburgerIcon     = findViewById(R.id.hamburger_icon);
        logoIcon          = findViewById(R.id.logo_icon);
        logoTitle         = findViewById(R.id.logo_title);
        searchBar         = findViewById(R.id.search_bar);
    }

    /** Set up the header section (logo + title). */
    private void setupHeader() {
        // Show “Closet – <Category>”
        logoTitle.setText("Closet - " + selectedCategory);

        // Optional: clicking the logo or title shows a toast (you can replace with real navigation if needed)
        logoIcon.setOnClickListener(v -> Toast.makeText(this, "Logo clicked", Toast.LENGTH_SHORT).show());
        logoTitle.setOnClickListener(v -> Toast.makeText(this, "Title clicked", Toast.LENGTH_SHORT).show());
    }

    /** Set up navigation drawer toggle (hamburger icon). */
    private void setupNavigation() {
        hamburgerIcon.setOnClickListener(v -> {
            if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
                drawerLayout.closeDrawer(GravityCompat.START);
            } else {
                drawerLayout.openDrawer(GravityCompat.START);
            }
        });
    }

    /** Set up real‐time search filtering on EditText. */
    private void setupSearch() {
        searchBar.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { /* no-op */ }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterItems(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) { /* no-op */ }
        });
    }

    /**
     * Filter items in memory based on the query string,
     * then call updateUI() to refresh the list or show the empty state.
     */
    private void filterItems(String query) {
        filteredItems.clear();

        if (query.isEmpty()) {
            // If no query, show all loaded items
            filteredItems.addAll(clothingItems);
        } else {
            String lowerCaseQuery = query.toLowerCase().trim();
            for (ClothingItem item : clothingItems) {
                if (item.getName().toLowerCase().contains(lowerCaseQuery) ||
                        (item.getFabric() != null && item.getFabric().toLowerCase().contains(lowerCaseQuery)) ||
                        (item.getFit() != null && item.getFit().toLowerCase().contains(lowerCaseQuery))) {
                    filteredItems.add(item);
                }
            }
        }

        // Tell adapter to update its data
        itemAdapter.updateItems(filteredItems);

        // Show either list or empty‐state text
        updateUI();
    }

    /** Set up RecyclerView + attach our RowListItemAdapter. */
    private void setupRecyclerView() {
        itemAdapter = new RowListItemAdapter(this, filteredItems);
        itemAdapter.setOnItemClickListener(this);
        itemAdapter.setOnItemLikeListener(this);

        recyclerViewItems.setLayoutManager(new LinearLayoutManager(this));
        recyclerViewItems.setAdapter(itemAdapter);
        recyclerViewItems.setHasFixedSize(true);
    }

    /**
     * Query Firestore’s “Clothes” collection for all items where “Category” == selectedCategory,
     * then sort by “Name,” convert each document to ClothingItem, check “likedUsers” to see if
     * current user has liked it, and populate clothingItems + filteredItems.
     */
    private void loadClothingItems() {
        // 1) Show the progress bar while we fetch
        showLoading(true);
        Log.d(TAG, "Loading items for category: " + selectedCategory);

        firestore.collection("Clothes")
                .whereEqualTo("Category", selectedCategory)
                .orderBy("Name")
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        // Hide loading spinner
                        showLoading(false);

                        if (task.isSuccessful() && task.getResult() != null) {
                            clothingItems.clear();

                            // Determine current user’s UID (if logged in)
                            String currentUid = null;
                            if (FirebaseAuth.getInstance().getCurrentUser() != null) {
                                currentUid = FirebaseAuth.getInstance().getCurrentUser().getUid();
                            }

                            for (DocumentSnapshot document : task.getResult()) {
                                try {
                                    ClothingItem item = document.toObject(ClothingItem.class);
                                    if (item != null) {
                                        item.setId(document.getId());

                                        // Check if user has liked this item
                                        if (currentUid != null) {
                                            List<String> likedUsers = document.get("likedUsers", List.class);
                                            boolean likedByMe = false;
                                            if (likedUsers != null && likedUsers.contains(currentUid)) {
                                                likedByMe = true;
                                            }
                                            item.setLikedByCurrentUser(likedByMe);
                                        } else {
                                            // Not logged in → treat as not liked
                                            item.setLikedByCurrentUser(false);
                                        }

                                        clothingItems.add(item);
                                        Log.d(TAG, "Added item: " + item.getName());
                                    }
                                } catch (Exception e) {
                                    Log.e(TAG, "Error deserializing document: " + document.getId(), e);
                                }
                            }

                            // Copy all to filteredItems for initial display
                            filteredItems.clear();
                            filteredItems.addAll(clothingItems);

                            // Refresh UI (list vs. empty state)
                            updateUI();

                            Log.d(TAG, "Loaded " + clothingItems.size() + " items");
                        } else {
                            // Query failed
                            String errorMessage = "Failed to load items";
                            if (task.getException() != null) {
                                errorMessage += ": " + task.getException().getMessage();
                            }
                            Log.e(TAG, errorMessage);
                            Toast.makeText(ListActivity.this, errorMessage, Toast.LENGTH_LONG).show();
                            showEmptyState(true);
                        }
                    }
                });
    }

    /** Show or hide the RecyclerView / empty‐state text depending on filteredItems. */
    private void updateUI() {
        if (filteredItems.isEmpty()) {
            showEmptyState(true);
        } else {
            showEmptyState(false);
            // Make sure adapter has the latest filteredItems
            itemAdapter.updateItems(filteredItems);
        }
    }

    /**
     * Show or hide the loading spinner. When loading = true, we show progressBar and hide RecyclerView.
     * When loading = false, we hide progressBar and show RecyclerView.
     */
    private void showLoading(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        recyclerViewItems.setVisibility(show ? View.GONE : View.VISIBLE);
    }

    /**
     * Show or hide the “empty state” TextView. When show = true, we show textEmptyState and hide RecyclerView.
     * When show = false, we hide textEmptyState and show RecyclerView.
     */
    private void showEmptyState(boolean show) {
        textEmptyState.setVisibility(show ? View.VISIBLE : View.GONE);
        recyclerViewItems.setVisibility(show ? View.GONE : View.VISIBLE);
    }

    /** Handle single‐tap on any row. For now, just show a Toast. */
    @Override
    public void onItemClick(ClothingItem item, int position) {
        String message = "Clicked: " + item.getName();
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
        Log.d(TAG, "Item clicked: " + item.getName() + " at position " + position);
    }

    /**
     * Handle double‐tap on image (like/unlike).
     * We update Firestore’s “likedUsers” array for the given item ID.
     */
    @Override
    public void onItemLike(ClothingItem item, int position, boolean isLiked) {
        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            Toast.makeText(this, "Please log in to like items", Toast.LENGTH_SHORT).show();
            return;
        }

        String currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        if (item == null || item.getId() == null) {
            Log.e(TAG, "Invalid item for like operation");
            return;
        }

        String itemId = item.getId();
        java.util.Map<String, Object> updates = new java.util.HashMap<>();
        if (isLiked) {
            updates.put("likedUsers", com.google.firebase.firestore.FieldValue.arrayUnion(currentUserId));
        } else {
            updates.put("likedUsers", com.google.firebase.firestore.FieldValue.arrayRemove(currentUserId));
        }

        firestore.collection("Clothes")
                .document(itemId)
                .update(updates)
                .addOnSuccessListener(aVoid -> {
                    item.setLikedByCurrentUser(isLiked);
                    itemAdapter.notifyItemChanged(position);
                    String msg = isLiked ? "Added to favorites" : "Removed from favorites";
                    Toast.makeText(ListActivity.this, msg, Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to update like status", e);
                    Toast.makeText(ListActivity.this, "Failed to update favorite status", Toast.LENGTH_SHORT).show();
                    // Revert the change locally:
                    item.setLikedByCurrentUser(!isLiked);
                    itemAdapter.notifyItemChanged(position);
                });
    }

    /** If the drawer is open, close it on back‐pressed; otherwise just behave normally. */
    @Override
    public void onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }
}
