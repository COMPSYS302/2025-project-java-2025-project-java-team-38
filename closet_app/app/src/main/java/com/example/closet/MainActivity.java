package com.example.closet;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;
import android.util.Log;
import android.widget.EditText;
import android.text.TextWatcher;
import android.text.Editable;




import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.List;
import java.util.Arrays;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import android.content.Intent;
import com.google.android.material.bottomnavigation.BottomNavigationView;



/**
 * Main activity showing category buttons and recent items
 * Users can browse categories which opens ListActivity
 */
public class MainActivity extends AppCompatActivity implements ItemAdapter.OnItemClickListener, ItemAdapter.OnItemLikeListener {

    private static final String TAG = "MainActivity";

    // UI Components
    private Button btnShirts, btnPants, btnAccessories, btnDresses, btnShoes;

    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private ImageView hamburgerIcon;

    // Data

    private FirebaseFirestore firestore;
    private FirebaseAuth firebaseAuth;
    private String currentUserId;
    private RecyclerView recyclerViewTopPicks;
    private ItemAdapter topPicksAdapter;

    private GoogleSignInClient googleSignInClient;
    private List<ClothingItem> topPicks = new ArrayList<>();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Hide ActionBar (before setting content view)
        supportRequestWindowFeature(Window.FEATURE_NO_TITLE);
        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        // Set layout
        setContentView(R.layout.activity_main);

        // Initialize Firebase
        firebaseAuth = FirebaseAuth.getInstance();

        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(
                GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
        googleSignInClient = GoogleSignIn.getClient(this, gso);

        // wire up logout button
        findViewById(R.id.btn_logout).setOnClickListener(v -> signOut());

        // Get current user ID
        if (firebaseAuth.getCurrentUser() != null) {
            currentUserId = firebaseAuth.getCurrentUser().getUid();
        }

        // Initialize views
        initializeViews();

        // Wire up the Logout button in your header
        ImageView btnLogout = findViewById(R.id.btn_logout);
        btnLogout.setOnClickListener(v -> signOut());

        // Set up components
        setupNavigationDrawer();
        setupCategoryButtons();
        setupTopPicksRecyclerView();

        // Get current user ID (again)
        if (firebaseAuth.getCurrentUser() != null) {
            currentUserId = firebaseAuth.getCurrentUser().getUid();
        }

        // Initialize views (again)
        initializeViews();

        // Set up components (again)
        setupNavigationDrawer();
        setupCategoryButtons();
        setupTopPicksRecyclerView();

        // 🔍 Search bar
        EditText searchBar = findViewById(R.id.search_bar);
        searchBar.setOnEditorActionListener((v, actionId, event) -> {
            String query = searchBar.getText().toString().trim();
            if (!query.isEmpty()) {
                Intent intent = new Intent(MainActivity.this, ListActivity.class);
                intent.putExtra("SEARCH_QUERY", query);
                startActivity(intent);
            }
            return true;
        });

        // 🧭 Clickable "Swipe to explore →"
        findViewById(R.id.text_swipe_to_explore).setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, TopPicksActivity.class);
            startActivity(intent);
        });

        // Load top picks
        loadTopPicks();

        // ─── Bottom Navigation Bar Setup ───
        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation);
        bottomNav.setSelectedItemId(R.id.nav_home);
        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_home) {
                // we’re already here
                return true;
            } else if (id == R.id.nav_favorites) {
                startActivity(new Intent(this, FavouritesActivity.class));
                finish();
                return true;
            } else if (id == R.id.nav_account) {
                startActivity(new Intent(this, AccountActivity.class));
                finish();
                return true;
            }
            return false;
        });

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
            recyclerViewTopPicks = findViewById(R.id.recycler_view_top_picks);


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
                int itemId = item.getItemId();

                if (itemId == R.id.nav_most_viewed) {
                    startActivity(new Intent(this, MostViewedActivity.class));
                } else if (itemId == R.id.nav_top_picks) {
                    startActivity(new Intent(this, TopPicksActivity.class));
                } else if (itemId == R.id.nav_favourites) {
                    startActivity(new Intent(this, FavouritesActivity.class));
                    drawerLayout.closeDrawers();
                } else {
                    Toast.makeText(this, "Clicked: " + item.getTitle(), Toast.LENGTH_SHORT).show();
                }

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
    private void setupTopPicksRecyclerView() {
        recyclerViewTopPicks = findViewById(R.id.recycler_view_top_picks);
        Log.d("MainActivity", "setupTopPicksRecyclerView called");

        if (recyclerViewTopPicks != null) {
            topPicks = new ArrayList<>();  // ensure it's initialized here

            topPicksAdapter = new ItemAdapter(this, topPicks, R.layout.row_list_item);
            topPicksAdapter.setOnItemClickListener(this);
            topPicksAdapter.setOnItemLikeListener(this);

            LinearLayoutManager layoutManager = new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false);
            recyclerViewTopPicks.setLayoutManager(layoutManager);
            recyclerViewTopPicks.setAdapter(topPicksAdapter);
            recyclerViewTopPicks.setHasFixedSize(true);
            recyclerViewTopPicks.setNestedScrollingEnabled(false);

            // ✅ Add fake item here, after adapter is ready

            topPicksAdapter.updateItems(topPicks);
            Log.d("MainActivity", "Manually added test item to topPicks");

            Log.d(TAG, "Top Picks RecyclerView setup complete");
        } else {
            Log.e(TAG, "recyclerViewTopPicks is null");
        }
    }




    /**
     * Load recent items from Firestore
     */
    private void loadTopPicks() {
        TopPicksManager.loadTopPicks(currentUserId, new TopPicksManager.TopPicksCallback() {
            @Override
            public void onTopPicksLoaded(List<ClothingItem> items) {
                topPicks.clear();
                topPicks.addAll(items);
                if (topPicksAdapter != null) {
                    topPicksAdapter.updateItems(topPicks);

                }
                Log.d(TAG, "Top picks loaded successfully.");
            }

            @Override
            public void onError(Exception e) {
                Log.e(TAG, "Failed to load top picks", e);
                Toast.makeText(MainActivity.this, "Failed to load Top Picks", Toast.LENGTH_SHORT).show();
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
                    if (topPicksAdapter != null) {
                        topPicksAdapter.notifyItemChanged(position);
                    }

                    String message = isLiked ? "Added to favorites" : "Removed from favorites";
                    Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to update like status", e);
                    Toast.makeText(this, "Failed to update favorite status", Toast.LENGTH_SHORT).show();

                    // Revert like state and update UI
                    item.setLikedByCurrentUser(!isLiked);
                    if (topPicksAdapter != null) {
                        topPicksAdapter.notifyItemChanged(position);
                    }
                });
    }


    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "onResume called - refreshing data");
        // Refresh data when returning to main activity
        loadTopPicks();  // New method we'll add below

    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy called");
        if (topPicksAdapter != null) {
            topPicksAdapter.cleanup();
        }
    }

    private void signOut() {
        // 1) Firebase sign-out
        firebaseAuth.signOut();

        // 2) Google sign-out
        googleSignInClient.signOut()
                .addOnCompleteListener(this, task -> {
                    // 3) Back to Login screen
                    startActivity(new Intent(MainActivity.this, LoginActivity.class));
                    finish();
                });
    }


}

