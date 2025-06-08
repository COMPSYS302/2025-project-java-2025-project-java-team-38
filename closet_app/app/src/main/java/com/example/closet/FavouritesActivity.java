package com.example.closet;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Window;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.List;

public class FavouritesActivity extends AppCompatActivity implements ItemAdapter.OnItemClickListener, ItemAdapter.OnItemLikeListener {

    private static final String TAG = "FavouritesActivity";

    private RecyclerView recyclerView;
    private EditText searchBar;
    private ItemAdapter adapter;

    private final List<ClothingItem> favouritesList = new ArrayList<>();
    private final List<ClothingItem> filteredList = new ArrayList<>();

    private FirebaseFirestore firestore;
    private FirebaseAuth firebaseAuth;
    private String currentUserId;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        supportRequestWindowFeature(Window.FEATURE_NO_TITLE);
        if (getSupportActionBar() != null) getSupportActionBar().hide();
        setContentView(R.layout.activity_favourites);
        ImageButton back = findViewById(R.id.btn_back);
        back.setOnClickListener(v -> finish());
        // inside onCreate(), after setContentView(...)
        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation);
// highlight the “Favorites” tab
        bottomNav.setSelectedItemId(R.id.nav_favorites);

        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_home) {
                startActivity(new Intent(this, MainActivity.class));
                finish();
                return true;
            } else if (id == R.id.nav_favorites) {
                // already here
                return true;
            } else if (id == R.id.nav_virtual_avatar) {
                startActivity(new Intent(this, AvatarActivity.class));
                finish();
                return true;
            } else if (id == R.id.nav_account) {
                startActivity(new Intent(this, AccountActivity.class));
                finish();
                return true;
            }
            return false;
        });



        firestore = FirebaseFirestore.getInstance();
        firebaseAuth = FirebaseAuth.getInstance();

        if (firebaseAuth.getCurrentUser() != null) {
            currentUserId = firebaseAuth.getCurrentUser().getUid();
        } else {
            Toast.makeText(this, "Not logged in", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        setupDrawer();
        setupRecyclerView();
        setupSearch();
        loadFavourites();
    }

    private void setupDrawer() {
        DrawerLayout drawerLayout = findViewById(R.id.drawer_layout);
        NavigationView navigationView = findViewById(R.id.navigation_view);
        ImageView hamburgerIcon = findViewById(R.id.hamburger_icon);
        hamburgerIcon.setOnClickListener(v -> drawerLayout.openDrawer(GravityCompat.START));

        findViewById(R.id.logo_title).setOnClickListener(v -> {
            startActivity(new Intent(this, MainActivity.class));
            finish();
        });

        navigationView.setNavigationItemSelectedListener(item -> {
            Intent intent = null;
            int itemId = item.getItemId();

            if (itemId == R.id.nav_home) {
                intent = new Intent(this, MainActivity.class);
            } else if (itemId == R.id.nav_top_picks) {
                intent = new Intent(this, TopPicksActivity.class);
            } else if (itemId == R.id.nav_most_viewed) {
                intent = new Intent(this, MostViewedActivity.class);
            } else if (itemId == R.id.nav_favourites) {
                drawerLayout.closeDrawer(GravityCompat.START);
                return true;
            }

            if (intent != null) {
                startActivity(intent);
                finish();
            }

            drawerLayout.closeDrawer(GravityCompat.START);
            return true;
        });
    }

    private void setupRecyclerView() {
        recyclerView = findViewById(R.id.recycler_view_items);
        searchBar = findViewById(R.id.search_bar);
        adapter = new ItemAdapter(this, filteredList, R.layout.row_list_item);
        adapter.setOnItemClickListener(this);
        adapter.setOnItemLikeListener(this);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);
    }

    private void setupSearch() {
        searchBar.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void afterTextChanged(Editable s) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterItems(s.toString());
            }
        });
    }

    private void loadFavourites() {
        firestore.collection("Clothes")
                .whereArrayContains("likedUsers", currentUserId)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    favouritesList.clear();
                    filteredList.clear();
                    for (DocumentSnapshot doc : querySnapshot) {
                        ClothingItem item = doc.toObject(ClothingItem.class);
                        if (item != null) {
                            item.setId(doc.getId());
                            item.setLikedByCurrentUser(true);
                            favouritesList.add(item);
                            filteredList.add(item);
                        }
                    }
                    adapter.updateItems(filteredList);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to load favourites", e);
                    Toast.makeText(this, "Error loading favourites", Toast.LENGTH_SHORT).show();
                });
    }

    private void filterItems(String query) {
        String lower = query.toLowerCase().trim();
        filteredList.clear();

        for (ClothingItem item : favouritesList) {
            if ((item.getName() != null && item.getName().toLowerCase().contains(lower)) ||
                    (item.getCategory() != null && item.getCategory().toLowerCase().contains(lower)) ||
                    (item.getFabric() != null && item.getFabric().toLowerCase().contains(lower)) ||
                    (item.getFit() != null && item.getFit().toLowerCase().contains(lower))) {
                filteredList.add(item);
            }
        }

        adapter.updateItems(filteredList);
    }

    @Override
    public void onItemClick(ClothingItem item, int position) {
        Intent intent = new Intent(this, DetailsActivity.class);
        intent.putExtra("ITEM_ID", item.getId());
        startActivity(intent);
    }

    @Override
    public void onItemLike(ClothingItem item, int position, boolean isLiked) {
        firestore.collection("Clothes").document(item.getId())
                .update("likedUsers", isLiked
                        ? com.google.firebase.firestore.FieldValue.arrayUnion(currentUserId)
                        : com.google.firebase.firestore.FieldValue.arrayRemove(currentUserId))
                .addOnSuccessListener(unused -> {
                    if (isLiked) {
                        item.setLikedByCurrentUser(true);
                    } else {
                        item.setLikedByCurrentUser(false);
                        favouritesList.remove(item);
                        filteredList.remove(item);
                        adapter.notifyItemRemoved(position);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to update like status", e);
                    Toast.makeText(this, "Failed to update like", Toast.LENGTH_SHORT).show();
                });
    }
}
