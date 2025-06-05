package com.example.closet;

import android.content.Intent;
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
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.SetOptions;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Activity for displaying a list of clothing items filtered by category.
 * Also checks whether the current user has "liked" each item.
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
    private List<ClothingItem> filteredItems;
    private String selectedCategory;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_list);

        Log.d(TAG, "onCreate started");

        initializeViews();
        firestore = FirebaseFirestore.getInstance();
        clothingItems = new ArrayList<>();
        filteredItems = new ArrayList<>();

        setupRecyclerView();
        setupSearch();
        setupNavigation();

        String searchQuery = getIntent().getStringExtra("SEARCH_QUERY");

        if (searchQuery != null && !searchQuery.isEmpty()) {
            searchBar.setText(searchQuery); // ✅ This line makes the EditText show the query
            searchBar.setSelection(searchQuery.length()); // Optional: move cursor to end

            selectedCategory = null;
            if (logoTitle != null) logoTitle.setText("Closet - Search");
            setupHeader();
            performSearchOnly(searchQuery);
            return;
        }

        // Category path
        selectedCategory = getIntent().getStringExtra(EXTRA_CATEGORY);
        if (selectedCategory == null || selectedCategory.isEmpty()) {
            selectedCategory = "Shirts";
        }
        findViewById(R.id.logo_icon).setOnClickListener(v -> goHome());
        findViewById(R.id.logo_title).setOnClickListener(v -> goHome());

        if (logoTitle != null) logoTitle.setText("Closet - " + selectedCategory);
        setupHeader();
        loadClothingItems();
    }


    private void performSearchOnly(String query) {
        showLoading(true);

        FirebaseFirestore.getInstance().collection("Clothes")
                .get()
                .addOnCompleteListener(task -> {
                    showLoading(false);
                    if (task.isSuccessful() && task.getResult() != null) {
                        clothingItems.clear();
                        filteredItems.clear();

                        String lowerQuery = query.toLowerCase();

                        for (DocumentSnapshot doc : task.getResult()) {
                            ClothingItem item = doc.toObject(ClothingItem.class);
                            if (item != null) {
                                boolean matches = false;

                                if (item.getName() != null && item.getName().toLowerCase().contains(lowerQuery)) {
                                    matches = true;
                                } else if (item.getFabric() != null && item.getFabric().toLowerCase().contains(lowerQuery)) {
                                    matches = true;
                                } else if (item.getFit() != null && item.getFit().toLowerCase().contains(lowerQuery)) {
                                    matches = true;
                                } else if (item.getCategory() != null && item.getCategory().toLowerCase().contains(lowerQuery)) {
                                    matches = true;
                                }

                                if (matches) {
                                    item.setId(doc.getId());
                                    filteredItems.add(item);
                                }
                            }
                        }

                        if (filteredItems.isEmpty()) {
                            Toast.makeText(this, "No matching items found.", Toast.LENGTH_SHORT).show();
                        }


                        updateUI();
                    } else {
                        Toast.makeText(this, "Failed to load items.", Toast.LENGTH_SHORT).show();
                        showEmptyState(true);
                    }
                });
    }

    private void goHome() {
        Log.d("ListActivity", "goHome() called");
        Intent intent = new Intent(ListActivity.this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
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

        if (recyclerViewItems == null) Log.e(TAG, "RecyclerView not found!");
        if (progressBar == null) Log.e(TAG, "ProgressBar not found!");
        if (textEmptyState == null) Log.e(TAG, "EmptyState TextView not found!");
    }


    /** Set up the header section (logo + title). */
    private void setupHeader() {
        if (logoTitle != null) {
            if (selectedCategory != null) {
                logoTitle.setText("Closet - " + selectedCategory);
            } else {
                logoTitle.setText("Closet - Search");
            }

            logoTitle.setOnClickListener(v -> goHome());
        }

        if (logoIcon != null) {
            logoIcon.setOnClickListener(v -> goHome());
        }
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

    /** Set up real-time search filtering on EditText. */
    private void setupSearch() {
        searchBar.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterItems(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) { }
        });
    }

    /**
     * Filter items in memory based on the query string,
     * then update the UI to refresh the list or show empty state.
     */
    private void filterItems(String query) {
        Log.d(TAG, "Filtering items with query: '" + query + "'");

        filteredItems.clear();

        if (query == null || query.trim().isEmpty()) {
            // If no query, show all loaded items
            filteredItems.addAll(clothingItems);
        } else {
            String lowerCaseQuery = query.toLowerCase().trim();
            for (ClothingItem item : clothingItems) {
                if (item != null && (
                        (item.getName() != null && item.getName().toLowerCase().contains(lowerCaseQuery)) ||
                                (item.getFabric() != null && item.getFabric().toLowerCase().contains(lowerCaseQuery)) ||
                                (item.getFit() != null && item.getFit().toLowerCase().contains(lowerCaseQuery))
                )) {
                    filteredItems.add(item);
                }
            }
        }

        Log.d(TAG, "Filter result: " + filteredItems.size() + " items");

        // Update the UI
        updateUI();
    }

    /** Set up RecyclerView + attach our RowListItemAdapter. */
    private void setupRecyclerView() {
        Log.d(TAG, "Setting up RecyclerView");

        // Initialize adapter with empty list
        itemAdapter = new RowListItemAdapter(this, filteredItems);
        itemAdapter.setOnItemClickListener(this);
        itemAdapter.setOnItemLikeListener(this);

        // Set up RecyclerView
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        recyclerViewItems.setLayoutManager(layoutManager);
        recyclerViewItems.setAdapter(itemAdapter);
        recyclerViewItems.setHasFixedSize(true);

        Log.d(TAG, "RecyclerView setup complete");

        // Debug RecyclerView dimensions after layout
        recyclerViewItems.post(() -> {
            Log.d(TAG, "RecyclerView dimensions: " +
                    recyclerViewItems.getWidth() + "x" + recyclerViewItems.getHeight());
        });
    }

    /** Load clothing items from Firestore for the selected category. */
    private void loadClothingItems() {
        Log.d(TAG, "Starting to load items for category: " + selectedCategory);

        // Show loading spinner
        showLoading(true);

        firestore.collection("Clothes")
                .whereEqualTo("Category", selectedCategory)
                .orderBy("Name")
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        Log.d(TAG, "Firestore query completed");

                        // Always hide loading spinner first
                        showLoading(false);

                        if (task.isSuccessful() && task.getResult() != null) {
                            clothingItems.clear();

                            QuerySnapshot querySnapshot = task.getResult();
                            Log.d(TAG, "Query returned " + querySnapshot.size() + " documents");

                            // Get current user ID for like status
                            String currentUid = null;
                            if (FirebaseAuth.getInstance().getCurrentUser() != null) {
                                currentUid = FirebaseAuth.getInstance().getCurrentUser().getUid();
                            }

                            // Process each document
                            for (DocumentSnapshot document : querySnapshot) {
                                try {
                                    ClothingItem item = document.toObject(ClothingItem.class);
                                    if (item != null) {
                                        item.setId(document.getId());

                                        // Check if user has liked this item
                                        if (currentUid != null) {
                                            @SuppressWarnings("unchecked")
                                            List<String> likedUsers = (List<String>) document.get("likedUsers");
                                            boolean likedByMe = (likedUsers != null && likedUsers.contains(currentUid));
                                            item.setLikedByCurrentUser(likedByMe);
                                        } else {
                                            item.setLikedByCurrentUser(false);
                                        }

                                        clothingItems.add(item);
                                        Log.d(TAG, "Added item: " + item.getName());
                                    } else {
                                        Log.w(TAG, "Failed to deserialize document: " + document.getId());
                                    }
                                } catch (Exception e) {
                                    Log.e(TAG, "Error deserializing document: " + document.getId(), e);
                                    Log.e(TAG, "Raw document data: " + document.getData());
                                }
                            }

                            // Copy all items to filtered list for initial display
                            filteredItems.clear();
                            filteredItems.addAll(clothingItems);

                            Log.d(TAG, "Final clothingItems size: " + clothingItems.size());
                            Log.d(TAG, "Final filteredItems size: " + filteredItems.size());

                            // Update UI to show the loaded items
                            updateUI();

                        } else {
                            // Query failed
                            String errorMessage = "Failed to load items";
                            if (task.getException() != null) {
                                errorMessage += ": " + task.getException().getMessage();
                                Log.e(TAG, "Firestore query failed", task.getException());
                            }
                            Toast.makeText(ListActivity.this, errorMessage, Toast.LENGTH_LONG).show();
                            showEmptyState(true);
                        }
                    }
                });
    }

    /** Update UI based on current filteredItems list. */
    private void updateUI() {
        Log.d(TAG, "updateUI called - filteredItems size: " + filteredItems.size());
        Log.d(TAG, "RecyclerView visibility: " + recyclerViewItems.getVisibility());
        Log.d(TAG, "EmptyState visibility: " + textEmptyState.getVisibility());

        if (filteredItems.isEmpty()) {
            Log.d(TAG, "Showing empty state");
            showEmptyState(true);
        } else {
            Log.d(TAG, "Showing items list");
            showEmptyState(false);

            // Update adapter with new data
            if (itemAdapter != null) {
                itemAdapter.updateItems(filteredItems);
                Log.d(TAG, "Adapter updated with " + filteredItems.size() + " items");
                Log.d(TAG, "Adapter getItemCount: " + itemAdapter.getItemCount());
            } else {
                Log.e(TAG, "itemAdapter is null!");
            }
        }
    }

    /**
     * Show or hide the loading spinner.
     * When loading = true: show progressBar, hide RecyclerView
     * When loading = false: hide progressBar, show RecyclerView
     */
    private void showLoading(boolean show) {
        Log.d(TAG, "showLoading: " + show);

        if (progressBar != null && recyclerViewItems != null) {
            progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
            recyclerViewItems.setVisibility(show ? View.GONE : View.VISIBLE);
            // Also hide empty state when loading
            if (show && textEmptyState != null) {
                textEmptyState.setVisibility(View.GONE);
            }
        }
    }

    /**
     * Show or hide the "empty state" message.
     * When show = true: show textEmptyState, hide RecyclerView
     * When show = false: hide textEmptyState, show RecyclerView
     */
    private void showEmptyState(boolean show) {
        Log.d(TAG, "showEmptyState: " + show);

        if (textEmptyState != null && recyclerViewItems != null) {
            textEmptyState.setVisibility(show ? View.VISIBLE : View.GONE);
            recyclerViewItems.setVisibility(show ? View.GONE : View.VISIBLE);
        }
    }

    /** Handle single-tap on any row item. */
    @Override
    public void onItemClick(ClothingItem item, int position) {
        if (item == null || item.getId() == null) return;

        // 1) Write the clicked item into Firestore under "RecentViews/<currentUserId>/<itemId>"
        String currentUid = null;
        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            currentUid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        }

        if (currentUid != null) {
            // Use a “RecentViews” collection for this user, storing a timestamp
            Map<String, Object> data = new HashMap<>();
            data.put("itemId", item.getId());
            data.put("viewedAt", FieldValue.serverTimestamp());

            FirebaseFirestore
                    .getInstance()
                    .collection("Users")
                    .document(currentUid)
                    .collection("RecentViews")
                    .document(item.getId())
                    .set(data, SetOptions.merge())
                    .addOnSuccessListener(aVoid -> Log.d(TAG, "Saved to recent views: " + item.getId()))
                    .addOnFailureListener(e -> Log.e(TAG, "Failed to save recent view", e));
        } else {
            Log.w(TAG, "User not signed in; skipping recent‐view save");
        }

        // 2) Still launch DetailsActivity as before
        Intent intent = new Intent(this, DetailsActivity.class);
        intent.putExtra("ITEM_ID", item.getId());
        startActivity(intent);
        Log.d(TAG, "Item clicked: " + item.getName() +
                "; launching DetailsActivity with ITEM_ID=" + item.getId());
    }

    /** Handle like/unlike action on an item. */
    @Override
    public void onItemLike(ClothingItem item, int position, boolean isLiked) {
        Log.d(TAG, "onItemLike called - item: " + (item != null ? item.getName() : "null") +
                ", position: " + position + ", isLiked: " + isLiked);

        // Check if user is logged in
        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            Toast.makeText(this, "Please log in to like items", Toast.LENGTH_SHORT).show();
            return;
        }

        // Validate item
        if (item == null || item.getId() == null || item.getId().isEmpty()) {
            Log.e(TAG, "Invalid item for like operation");
            Toast.makeText(this, "Unable to update item", Toast.LENGTH_SHORT).show();
            return;
        }

        String currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        String itemId = item.getId();

        // Prepare Firestore update
        java.util.Map<String, Object> updates = new java.util.HashMap<>();
        if (isLiked) {
            updates.put("likedUsers", com.google.firebase.firestore.FieldValue.arrayUnion(currentUserId));
        } else {
            updates.put("likedUsers", com.google.firebase.firestore.FieldValue.arrayRemove(currentUserId));
        }

        // Update Firestore
        firestore.collection("Clothes")
                .document(itemId)
                .update(updates)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Like status updated successfully");

                    // Update local item state
                    item.setLikedByCurrentUser(isLiked);

                    // Notify adapter of change
                    if (itemAdapter != null) {
                        itemAdapter.notifyItemChanged(position);
                    }

                    // Show user feedback
                    String msg = isLiked ? "Added to favorites" : "Removed from favorites";
                    Toast.makeText(ListActivity.this, msg, Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to update like status", e);
                    Toast.makeText(ListActivity.this, "Failed to update favorite status", Toast.LENGTH_SHORT).show();

                    // Revert the change locally since Firestore update failed
                    item.setLikedByCurrentUser(!isLiked);
                    if (itemAdapter != null) {
                        itemAdapter.notifyItemChanged(position);
                    }
                });
    }

    /**
     * Debug method to log RecyclerView state - call this when troubleshooting
     */
    private void debugRecyclerViewState() {
        Log.d(TAG, "=== RecyclerView Debug Info ===");
        Log.d(TAG, "clothingItems size: " + (clothingItems != null ? clothingItems.size() : "NULL"));
        Log.d(TAG, "filteredItems size: " + (filteredItems != null ? filteredItems.size() : "NULL"));
        Log.d(TAG, "RecyclerView adapter: " + (recyclerViewItems.getAdapter() != null ? "SET" : "NULL"));
        Log.d(TAG, "RecyclerView layoutManager: " + (recyclerViewItems.getLayoutManager() != null ? "SET" : "NULL"));
        Log.d(TAG, "RecyclerView visibility: " + recyclerViewItems.getVisibility());
        Log.d(TAG, "ProgressBar visibility: " + progressBar.getVisibility());
        Log.d(TAG, "EmptyState visibility: " + textEmptyState.getVisibility());

        if (itemAdapter != null) {
            Log.d(TAG, "Adapter item count: " + itemAdapter.getItemCount());
        }

        recyclerViewItems.post(() -> {
            Log.d(TAG, "RecyclerView actual size: " +
                    recyclerViewItems.getWidth() + "x" + recyclerViewItems.getHeight());
        });
    }
}