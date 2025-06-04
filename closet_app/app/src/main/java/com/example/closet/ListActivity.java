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
public class ListActivity extends AppCompatActivity implements ItemAdapter.OnItemClickListener {

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
    private ItemAdapter itemAdapter;
    private List<ClothingItem> clothingItems;
    private List<ClothingItem> filteredItems; // For search functionality
    private String selectedCategory;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_list);

        // Initialize views
        initializeViews();

        // Get selected category from intent
        selectedCategory = getIntent().getStringExtra(EXTRA_CATEGORY);
        if (selectedCategory == null) {
            selectedCategory = "Shirts"; // Default fallback
        }

        // Set up header
        setupHeader();

        // Initialize Firebase Firestore
        firestore = FirebaseFirestore.getInstance();

        // Initialize data lists
        clothingItems = new ArrayList<>();
        filteredItems = new ArrayList<>();

        // Set up RecyclerView & adapter
        setupRecyclerView();

        // Set up search functionality
        setupSearch();

        // Set up navigation
        setupNavigation();

        // Load data from Firestore
        loadClothingItems();
    }

    /**
     * Initialize all view components
     */
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

    /**
     * Set up the header section
     */
    private void setupHeader() {
        // Set the title to show current category
        logoTitle.setText("Closet - " + selectedCategory);

        // Make logo clickable (optional - could navigate to home)
        logoIcon.setOnClickListener(v -> {
            // Navigate to home or perform action
            Toast.makeText(this, "Logo clicked", Toast.LENGTH_SHORT).show();
        });

        logoTitle.setOnClickListener(v -> {
            // Navigate to home or perform action
            Toast.makeText(this, "Title clicked", Toast.LENGTH_SHORT).show();
        });
    }

    /**
     * Set up navigation drawer
     */
    private void setupNavigation() {
        hamburgerIcon.setOnClickListener(v -> {
            if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
                drawerLayout.closeDrawer(GravityCompat.START);
            } else {
                drawerLayout.openDrawer(GravityCompat.START);
            }
        });
    }

    /**
     * Set up search functionality
     */
    private void setupSearch() {
        searchBar.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterItems(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    /**
     * Filter items based on search query
     */
    private void filterItems(String query) {
        filteredItems.clear();

        if (query.isEmpty()) {
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

        // Update adapter with filtered results
        itemAdapter.updateItems(filteredItems);

        // Update empty state
        updateUI();
    }

    /**
     * Set up RecyclerView with adapter and layout manager
     */
    private void setupRecyclerView() {
        itemAdapter = new ItemAdapter(this, filteredItems);
        itemAdapter.setOnItemClickListener(this);

        recyclerViewItems.setLayoutManager(new LinearLayoutManager(this));
        recyclerViewItems.setAdapter(itemAdapter);
        recyclerViewItems.setHasFixedSize(true);
    }

    /**
     * Load clothing items from Firestore based on selected category
     */
    private void loadClothingItems() {
        // Show progress bar & hide list initially
        showLoading(true);

        Log.d(TAG, "Loading items for category: " + selectedCategory);

        firestore.collection("Clothes")
                .whereEqualTo("Category", selectedCategory)
                .orderBy("Name")
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        // Hide progress bar
                        showLoading(false);

                        if (task.isSuccessful() && task.getResult() != null) {
                            clothingItems.clear();

                            // Get current user ID (for like-checking)
                            String currentUid = null;
                            if (FirebaseAuth.getInstance().getCurrentUser() != null) {
                                currentUid = FirebaseAuth.getInstance().getCurrentUser().getUid();
                            }

                            // Process each document
                            for (DocumentSnapshot document : task.getResult()) {
                                try {
                                    ClothingItem item = document.toObject(ClothingItem.class);
                                    if (item != null) {
                                        item.setId(document.getId());

                                        // Check if this user has liked the item
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

                            // Initialize filtered items with all items
                            filteredItems.clear();
                            filteredItems.addAll(clothingItems);

                            // Update UI (show list or empty state)
                            updateUI();

                            Log.d(TAG, "Loaded " + clothingItems.size() + " items");
                        } else {
                            // Handle query failure
                            String errorMessage = "Failed to load items";
                            if (task.getException() != null) {
                                errorMessage += ": " + task.getException().getMessage();
                            }

                            Log.e(TAG, errorMessage);
                            Toast.makeText(ListActivity.this, errorMessage, Toast.LENGTH_LONG).show();

                            // Show empty state
                            showEmptyState(true);
                        }
                    }
                });
    }

    /**
     * Update UI after data loading
     */
    private void updateUI() {
        if (filteredItems.isEmpty()) {
            showEmptyState(true);
        } else {
            showEmptyState(false);
            // Notify adapter of data changes
            itemAdapter.updateItems(filteredItems);
        }
    }

    /**
     * Show/hide loading progress bar
     *
     * @param show true to show, false to hide
     */
    private void showLoading(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        recyclerViewItems.setVisibility(show ? View.GONE : View.VISIBLE);
    }

    /**
     * Show/hide empty state message
     *
     * @param show true to show, false to hide
     */
    private void showEmptyState(boolean show) {
        textEmptyState.setVisibility(show ? View.VISIBLE : View.GONE);
        recyclerViewItems.setVisibility(show ? View.GONE : View.VISIBLE);
    }

    /**
     * Handle item click from RecyclerView adapter
     *
     * @param item     Clicked ClothingItem
     * @param position Position in the list
     */
    @Override
    public void onItemClick(ClothingItem item, int position) {
        // For now, show a Toast with the item name
        String message = "Clicked: " + item.getName();
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
        Log.d(TAG, "Item clicked: " + item.getName() + " at position " + position);
    }

    /**
     * Handle back button press
     */
    @Override
    public void onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }
}