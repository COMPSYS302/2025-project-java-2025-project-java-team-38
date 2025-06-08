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

import com.google.android.material.navigation.NavigationView;
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

public class ListActivity extends AppCompatActivity implements
        RowListItemAdapter.OnItemClickListener,
        RowListItemAdapter.OnItemLikeListener {

    public static final String EXTRA_CATEGORY = "category";
    private static final String TAG = "ListActivity";

    private RecyclerView recyclerViewItems;
    private ProgressBar progressBar;
    private TextView textEmptyState;
    private DrawerLayout drawerLayout;
    private ImageView hamburgerIcon, logoIcon;
    private TextView logoTitle;
    private EditText searchBar;

    private FirebaseFirestore firestore;
    private RowListItemAdapter itemAdapter;
    private List<ClothingItem> clothingItems = new ArrayList<>();
    private List<ClothingItem> filteredItems = new ArrayList<>();
    private String selectedCategory = null;
    private boolean isSearchMode = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_list);

        initializeViews();
        firestore = FirebaseFirestore.getInstance();
        setupRecyclerView();
        setupSearch();
        setupNavigation();

        String searchQuery = getIntent().getStringExtra("SEARCH_QUERY");
        selectedCategory = getIntent().getStringExtra(EXTRA_CATEGORY);

        if (searchQuery != null && !searchQuery.isEmpty()) {
            isSearchMode = true;
            logoTitle.setText("Closet - Search");
            searchBar.setText(searchQuery);
            searchBar.setSelection(searchQuery.length());
            performSearchOnly(searchQuery);
        } else {
            isSearchMode = false;
            if (selectedCategory == null || selectedCategory.isEmpty()) {
                selectedCategory = "Shirts";
            }
            logoTitle.setText("Closet - " + selectedCategory);
            loadClothingItems();
        }

        logoIcon.setOnClickListener(v -> goHome());
        logoTitle.setOnClickListener(v -> goHome());
    }

    private void initializeViews() {
        recyclerViewItems = findViewById(R.id.recycler_view_items);
        progressBar = findViewById(R.id.progress_bar);
        textEmptyState = findViewById(R.id.text_empty_state);
        drawerLayout = findViewById(R.id.drawer_layout);
        hamburgerIcon = findViewById(R.id.hamburger_icon);
        logoIcon = findViewById(R.id.logo_icon);
        logoTitle = findViewById(R.id.logo_title);
        searchBar = findViewById(R.id.search_bar);
    }

    private void setupNavigation() {
        hamburgerIcon.setOnClickListener(v -> drawerLayout.openDrawer(GravityCompat.START));
        NavigationView navigationView = findViewById(R.id.navigation_view);
        navigationView.setNavigationItemSelectedListener(item -> {
            int itemId = item.getItemId();
            Intent intent = null;
            if (itemId == R.id.nav_home) intent = new Intent(this, MainActivity.class);
            else if (itemId == R.id.nav_top_picks) intent = new Intent(this, TopPicksActivity.class);
            else if (itemId == R.id.nav_most_viewed) intent = new Intent(this, MostViewedActivity.class);
            else if (itemId == R.id.nav_favourites) intent = new Intent(this, FavouritesActivity.class);

            if (intent != null) {
                startActivity(intent);
                finish();
            }

            drawerLayout.closeDrawer(GravityCompat.START);
            return true;
        });
    }

    private void setupSearch() {
        searchBar.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void afterTextChanged(Editable s) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (isSearchMode) {
                    performSearchOnly(s.toString());
                } else {
                    filterItemsWithinCategory(s.toString());
                }
            }
        });
    }

    private void setupRecyclerView() {
        itemAdapter = new RowListItemAdapter(this, filteredItems);
        itemAdapter.setOnItemClickListener(this);
        itemAdapter.setOnItemLikeListener(this);
        recyclerViewItems.setLayoutManager(new LinearLayoutManager(this));
        recyclerViewItems.setAdapter(itemAdapter);
        recyclerViewItems.setHasFixedSize(true);
    }

    private void performSearchOnly(String query) {
        showLoading(true);
        firestore.collection("Clothes").get().addOnCompleteListener(task -> {
            showLoading(false);
            if (task.isSuccessful() && task.getResult() != null) {
                filteredItems.clear();
                String q = query.toLowerCase().trim();
                for (DocumentSnapshot doc : task.getResult()) {
                    ClothingItem item = doc.toObject(ClothingItem.class);
                    if (item != null) {
                        boolean matches = (item.getName() != null && item.getName().toLowerCase().contains(q)) ||
                                (item.getFabric() != null && item.getFabric().toLowerCase().contains(q)) ||
                                (item.getFit() != null && item.getFit().toLowerCase().contains(q)) ||
                                (item.getCategory() != null && item.getCategory().toLowerCase().contains(q));
                        if (matches) {
                            item.setId(doc.getId());
                            filteredItems.add(item);
                        }
                    }
                }
                updateUI();
            } else {
                Toast.makeText(this, "Search failed.", Toast.LENGTH_SHORT).show();
                showEmptyState(true);
            }
        });
    }

    private void filterItemsWithinCategory(String query) {
        filteredItems.clear();
        String q = query.toLowerCase().trim();

        if (q.isEmpty()) {
            filteredItems.addAll(clothingItems);
        } else {
            for (ClothingItem item : clothingItems) {
                if (item != null) {
                    boolean matches =
                            (item.getName() != null && item.getName().toLowerCase().contains(q)) ||
                                    (item.getFabric() != null && item.getFabric().toLowerCase().contains(q)) ||
                                    (item.getFit() != null && item.getFit().toLowerCase().contains(q)) ||
                                    (item.getCare() != null && item.getCare().toLowerCase().contains(q)) ||
                                    (item.getCategory() != null && item.getCategory().toLowerCase().contains(q));

                    if (matches) {
                        filteredItems.add(item);
                    }
                }
            }
        }

        updateUI();
    }


    private void loadClothingItems() {
        showLoading(true);
        firestore.collection("Clothes")
                .whereEqualTo("Category", selectedCategory)
                .get()
                .addOnSuccessListener(snapshot -> {
                    showLoading(false);
                    clothingItems.clear();
                    for (DocumentSnapshot doc : snapshot) {
                        ClothingItem item = doc.toObject(ClothingItem.class);
                        if (item != null) {
                            item.setId(doc.getId());
                            clothingItems.add(item);
                        }
                    }
                    filteredItems.clear();
                    filteredItems.addAll(clothingItems);
                    updateUI();
                })
                .addOnFailureListener(e -> {
                    showLoading(false);
                    Toast.makeText(this, "Failed to load category items.", Toast.LENGTH_SHORT).show();
                    showEmptyState(true);
                });
    }

    private void updateUI() {
        if (filteredItems.isEmpty()) {
            showEmptyState(true);
        } else {
            showEmptyState(false);
            itemAdapter.updateItems(filteredItems);
        }
    }

    private void showLoading(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        recyclerViewItems.setVisibility(show ? View.GONE : View.VISIBLE);
    }

    private void showEmptyState(boolean show) {
        textEmptyState.setVisibility(show ? View.VISIBLE : View.GONE);
        recyclerViewItems.setVisibility(show ? View.GONE : View.VISIBLE);
    }

    private void goHome() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }

    @Override
    public void onItemClick(ClothingItem item, int position) {
        if (item == null || item.getId() == null) return;
        Intent intent = new Intent(this, DetailsActivity.class);
        intent.putExtra("ITEM_ID", item.getId());
        startActivity(intent);
    }

    @Override
    public void onItemLike(ClothingItem item, int position, boolean isLiked) {
        String currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        Map<String, Object> updates = new HashMap<>();
        updates.put("likedUsers", isLiked ?
                FieldValue.arrayUnion(currentUserId) :
                FieldValue.arrayRemove(currentUserId));

        firestore.collection("Clothes").document(item.getId())
                .update(updates)
                .addOnSuccessListener(aVoid -> {
                    item.setLikedByCurrentUser(isLiked);
                    itemAdapter.notifyItemChanged(position);
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to update like", Toast.LENGTH_SHORT).show();
                    item.setLikedByCurrentUser(!isLiked);
                    itemAdapter.notifyItemChanged(position);
                });
    }
}
