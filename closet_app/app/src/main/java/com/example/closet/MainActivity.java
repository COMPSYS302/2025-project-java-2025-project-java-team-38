package com.example.closet;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

/**
 * Main activity showing category buttons and recent items
 * Users can browse categories which opens ListActivity
 */
public class MainActivity extends AppCompatActivity implements ItemAdapter.OnItemClickListener, ItemAdapter.OnItemLikeListener {

    private static final String TAG = "MainActivity";

    // UI Components
    private Button btnShirts, btnPants, btnAccessories, btnDresses, btnShoes;
    private RecyclerView recyclerViewRecentItems;
    private ItemAdapter recentItemsAdapter;
    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private ImageView hamburgerIcon;

    // Data
    private List<ClothingItem> recentItems = new ArrayList<>();
    private FirebaseFirestore firestore;
    private FirebaseAuth firebaseAuth;
    private String currentUserId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        supportRequestWindowFeature(Window.FEATURE_NO_TITLE);

        // 2) Hide any existing ActionBar
        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        setContentView(R.layout.activity_main);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize Firebase
        firestore = FirebaseFirestore.getInstance();
        firebaseAuth = FirebaseAuth.getInstance();

        // Get current user ID
        if (firebaseAuth.getCurrentUser() != null) {
            currentUserId = firebaseAuth.getCurrentUser().getUid();
        }

        // Initialize views
        initializeViews();

        // Set up navigation drawer
        setupNavigationDrawer();

        // Set up category buttons
        setupCategoryButtons();

        // Set up recent items RecyclerView
        setupRecentItemsRecyclerView();

        // Load recent items
        loadRecentItems();
    }

    /**
     * Initialize all view components
     */
    private void initializeViews() {
        try {
            // Drawer components
            drawerLayout = findViewById(R.id.drawer_layout);
            navigationView = findViewById(R.id.navigation_view);
            hamburgerIcon = findViewById(R.id.hamburger_icon);

            // Category buttons
            btnShirts = findViewById(R.id.btn_shirts);
            btnPants = findViewById(R.id.btn_pants);
            btnAccessories = findViewById(R.id.btn_accessories);
            btnDresses = findViewById(R.id.btn_dresses);
            btnShoes = findViewById(R.id.btn_shoes);

            // Recent items RecyclerView
            recyclerViewRecentItems = findViewById(R.id.recycler_view_recent_items);

            Log.d(TAG, "Views initialized successfully");

        } catch (Exception e) {
            Log.e(TAG, "Error initializing views", e);
            Toast.makeText(this, "Error loading interface", Toast.LENGTH_LONG).show();
        }
    }

    /**
     * Set up navigation drawer
     */
    private void setupNavigationDrawer() {
        if (drawerLayout != null && navigationView != null) {
            // Set up hamburger menu button
            if (hamburgerIcon != null) {
                hamburgerIcon.setOnClickListener(v -> {
                    Log.d(TAG, "Hamburger menu clicked");
                    drawerLayout.openDrawer(GravityCompat.START);
                });
            } else {
                Log.e(TAG, "Hamburger icon is null");
            }

            // Handle navigation menu item clicks
            navigationView.setNavigationItemSelectedListener(item -> {
                Log.d(TAG, "Navigation item clicked: " + item.getTitle());

                // Handle drawer menu clicks here
                int itemId = item.getItemId();

                // Add your navigation logic here based on your menu items
                // Example:
                // if (itemId == R.id.nav_profile) {
                //     startActivity(new Intent(this, ProfileActivity.class));
                // } else if (itemId == R.id.nav_favorites) {
                //     startActivity(new Intent(this, FavoritesActivity.class));
                // } else if (itemId == R.id.nav_settings) {
                //     startActivity(new Intent(this, SettingsActivity.class));
                // }

                // For now, just show a toast
                Toast.makeText(this, "Clicked: " + item.getTitle(), Toast.LENGTH_SHORT).show();

                drawerLayout.closeDrawers();
                return true;
            });
        } else {
            Log.e(TAG, "DrawerLayout or NavigationView is null");
        }
    }

    /**
     * Set up category browse buttons
     */
    private void setupCategoryButtons() {
        if (btnShirts != null) {
            btnShirts.setOnClickListener(v -> openCategoryList("Shirts"));
        } else {
            Log.e(TAG, "btnShirts is null");
        }

        if (btnPants != null) {
            btnPants.setOnClickListener(v -> openCategoryList("Pants"));
        } else {
            Log.e(TAG, "btnPants is null");
        }

        if (btnAccessories != null) {
            btnAccessories.setOnClickListener(v -> openCategoryList("Accessories"));
        } else {
            Log.e(TAG, "btnAccessories is null");
        }

        if (btnDresses != null) {
            btnDresses.setOnClickListener(v -> openCategoryList("Dresses"));
        } else {
            Log.e(TAG, "btnDresses is null");
        }

        if (btnShoes != null) {
            btnShoes.setOnClickListener(v -> openCategoryList("Shoes"));
        } else {
            Log.e(TAG, "btnShoes is null");
        }
    }

    /**
     * Open ListActivity for specific category
     */
    private void openCategoryList(String category) {
        Log.d(TAG, "Opening category: " + category);
        Intent intent = new Intent(this, ListActivity.class);
        intent.putExtra(ListActivity.EXTRA_CATEGORY, category);
        startActivity(intent);
    }

    /**
     * Set up RecyclerView for recent items
     */
    private void setupRecentItemsRecyclerView() {
        if (recyclerViewRecentItems != null) {
            try {
                recentItemsAdapter = new ItemAdapter(this, recentItems);
                recentItemsAdapter.setOnItemClickListener(this);
                recentItemsAdapter.setOnItemLikeListener(this);

                LinearLayoutManager layoutManager = new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false);
                recyclerViewRecentItems.setLayoutManager(layoutManager);
                recyclerViewRecentItems.setAdapter(recentItemsAdapter);
                recyclerViewRecentItems.setHasFixedSize(true);

                Log.d(TAG, "RecyclerView setup completed");

            } catch (Exception e) {
                Log.e(TAG, "Error setting up recent items RecyclerView", e);
            }
        } else {
            Log.e(TAG, "recyclerViewRecentItems is null");
        }
    }

    /**
     * Load recent items from Firestore
     */
    private void loadRecentItems() {
        Log.d(TAG, "Loading recent items...");

        if (firestore == null) {
            Log.e(TAG, "Firestore is null");
            return;
        }

        firestore.collection("Clothes")
                .orderBy("Name")
                .limit(10) // Limit to 10 recent items
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        recentItems.clear();

                        for (DocumentSnapshot doc : task.getResult()) {
                            try {
                                ClothingItem item = doc.toObject(ClothingItem.class);
                                if (item != null) {
                                    item.setId(doc.getId());

                                    // Check if current user has liked this item
                                    if (currentUserId != null) {
                                        List<String> likedUsers = (List<String>) doc.get("likedUsers");
                                        boolean likedByMe = likedUsers != null && likedUsers.contains(currentUserId);
                                        item.setLikedByCurrentUser(likedByMe);
                                    }

                                    recentItems.add(item);
                                }
                            } catch (Exception e) {
                                Log.e(TAG, "Error processing document: " + doc.getId(), e);
                            }
                        }

                        // Update adapter
                        if (recentItemsAdapter != null) {
                            recentItemsAdapter.updateItems(recentItems);
                        }

                        Log.d(TAG, "Loaded " + recentItems.size() + " recent items");

                    } else {
                        Log.e(TAG, "Failed to load recent items", task.getException());
                        Toast.makeText(this, "Failed to load recent items", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    /**
     * Handle item click from adapter
     */
    @Override
    public void onItemClick(ClothingItem item, int position) {
        if (item == null || item.getId() == null) {
            Log.e(TAG, "MainActivity onItemClick: invalid item or missing ID");
            return;
        }
        Intent intent = new Intent(this, DetailsActivity.class);
        intent.putExtra("ITEM_ID", item.getId());
        startActivity(intent);
        Log.d(TAG, "MainActivity clicked: " + item.getName() + " → DetailsActivity");
    }


    /**
     * Handle item like from adapter
     */
    @Override
    public void onItemLike(ClothingItem item, int position, boolean isLiked) {
        if (currentUserId == null) {
            Toast.makeText(this, "Please log in to like items", Toast.LENGTH_SHORT).show();
            return;
        }

        if (item == null || item.getId() == null) {
            Log.e(TAG, "Invalid item for like operation");
            return;
        }

        // Update like status in Firestore
        updateItemLikeStatus(item, position, isLiked);
    }

    /**
     * Update item like status in Firestore
     */
    private void updateItemLikeStatus(ClothingItem item, int position, boolean isLiked) {
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
                    if (recentItemsAdapter != null) {
                        recentItemsAdapter.notifyItemChanged(position);
                    }

                    String message = isLiked ? "Added to favorites" : "Removed from favorites";
                    Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to update like status", e);
                    Toast.makeText(this, "Failed to update favorite status", Toast.LENGTH_SHORT).show();

                    // Revert the change
                    item.setLikedByCurrentUser(!isLiked);
                    if (recentItemsAdapter != null) {
                        recentItemsAdapter.notifyItemChanged(position);
                    }
                });
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "onResume called - refreshing data");
        // Refresh data when returning to main activity
        loadRecentItems();
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy called");
        if (recentItemsAdapter != null) {
            recentItemsAdapter.cleanup();
        }
    }
}
