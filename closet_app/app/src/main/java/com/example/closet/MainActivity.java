package com.example.closet;

import android.content.Intent;
import android.os.Bundle;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

import com.google.android.gms.auth.api.signin.GoogleSignIn;

public class MainActivity extends AppCompatActivity implements ItemAdapter.OnItemClickListener, ItemAdapter.OnItemLikeListener {

    private static final String TAG = "MainActivity";

    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private ImageView hamburgerIcon;

    private Button btnShirts, btnPants, btnAccessories, btnDresses, btnShoes;
    private RecyclerView recyclerViewTopPicks;
    private ItemAdapter topPicksAdapter;

    private FirebaseFirestore firestore;
    private FirebaseAuth firebaseAuth;
    private String currentUserId;

    private GoogleSignInClient googleSignInClient;
    private List<ClothingItem> topPicks = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        supportRequestWindowFeature(Window.FEATURE_NO_TITLE);
        if (getSupportActionBar() != null) getSupportActionBar().hide();

        setContentView(R.layout.activity_main);

        // Firebase and Google auth
        firebaseAuth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();

        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
        googleSignInClient = GoogleSignIn.getClient(this, gso);

        if (firebaseAuth.getCurrentUser() != null) {
            currentUserId = firebaseAuth.getCurrentUser().getUid();
        }

        initializeViews();
        setupNavigationDrawer();
        setupCategoryButtons();
        setupTopPicksRecyclerView();
        loadTopPicks();

        // 🔍 Search bar functionality (full-field search)
        EditText searchBar = findViewById(R.id.search_bar);
        searchBar.setOnEditorActionListener((v, actionId, event) -> {
            String query = searchBar.getText().toString().trim();
            if (!query.isEmpty()) {
                Intent intent = new Intent(MainActivity.this, ListActivity.class);
                intent.putExtra("SEARCH_QUERY", query);  // Passed for full-field search
                startActivity(intent);
            }
            return true;
        });

        // 🧭 Explore → Top Picks
        findViewById(R.id.text_swipe_to_explore).setOnClickListener(v ->
                startActivity(new Intent(MainActivity.this, TopPicksActivity.class)));

        // 🔓 Logout
        ImageView btnLogout = findViewById(R.id.btn_logout);
        btnLogout.setOnClickListener(v -> signOut());
    }

    private void initializeViews() {
        drawerLayout = findViewById(R.id.drawer_layout);
        navigationView = findViewById(R.id.navigation_view);
        hamburgerIcon = findViewById(R.id.hamburger_icon);

        btnShirts = findViewById(R.id.btn_shirts);
        btnPants = findViewById(R.id.btn_pants);
        btnAccessories = findViewById(R.id.btn_accessories);
        btnDresses = findViewById(R.id.btn_dresses);
        btnShoes = findViewById(R.id.btn_shoes);

        recyclerViewTopPicks = findViewById(R.id.recycler_view_top_picks);
    }

    private void setupNavigationDrawer() {
        hamburgerIcon.setOnClickListener(v -> drawerLayout.openDrawer(GravityCompat.START));

        navigationView.setNavigationItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.nav_most_viewed) {
                startActivity(new Intent(this, MostViewedActivity.class));
            } else if (itemId == R.id.nav_top_picks) {
                startActivity(new Intent(this, TopPicksActivity.class));
            } else if (itemId == R.id.nav_favourites) {
                startActivity(new Intent(this, FavouritesActivity.class));
            }
            drawerLayout.closeDrawers();
            return true;
        });
    }

    private void setupCategoryButtons() {
        btnShirts.setOnClickListener(v -> openCategoryList("Shirts"));
        btnPants.setOnClickListener(v -> openCategoryList("Pants"));
        btnAccessories.setOnClickListener(v -> openCategoryList("Accessories"));
        btnDresses.setOnClickListener(v -> openCategoryList("Dresses"));
        btnShoes.setOnClickListener(v -> openCategoryList("Shoes"));
    }

    private void openCategoryList(String category) {
        Intent intent = new Intent(this, ListActivity.class);
        intent.putExtra(ListActivity.EXTRA_CATEGORY, category);
        startActivity(intent);
    }

    private void setupTopPicksRecyclerView() {
        topPicksAdapter = new ItemAdapter(this, topPicks, R.layout.row_list_item);
        topPicksAdapter.setOnItemClickListener(this);
        topPicksAdapter.setOnItemLikeListener(this);

        recyclerViewTopPicks.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        recyclerViewTopPicks.setAdapter(topPicksAdapter);
        recyclerViewTopPicks.setHasFixedSize(true);
        recyclerViewTopPicks.setNestedScrollingEnabled(false);
    }

    private void loadTopPicks() {
        TopPicksManager.loadTopPicks(currentUserId, new TopPicksManager.TopPicksCallback() {
            @Override
            public void onTopPicksLoaded(List<ClothingItem> items) {
                topPicks.clear();
                topPicks.addAll(items);
                topPicksAdapter.updateItems(topPicks);
            }

            @Override
            public void onError(Exception e) {
                Log.e(TAG, "Failed to load top picks", e);
                Toast.makeText(MainActivity.this, "Failed to load Top Picks", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onItemClick(ClothingItem item, int position) {
        if (item != null && item.getId() != null) {
            Intent intent = new Intent(this, DetailsActivity.class);
            intent.putExtra("ITEM_ID", item.getId());
            startActivity(intent);
        }
    }

    @Override
    public void onItemLike(ClothingItem item, int position, boolean isLiked) {
        if (currentUserId == null || item.getId() == null) return;

        firestore.collection("Clothes").document(item.getId())
                .update("likedUsers", isLiked
                        ? com.google.firebase.firestore.FieldValue.arrayUnion(currentUserId)
                        : com.google.firebase.firestore.FieldValue.arrayRemove(currentUserId))
                .addOnSuccessListener(aVoid -> {
                    item.setLikedByCurrentUser(isLiked);
                    topPicksAdapter.notifyItemChanged(position);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to update like", e);
                    item.setLikedByCurrentUser(!isLiked);
                    topPicksAdapter.notifyItemChanged(position);
                    Toast.makeText(this, "Failed to update like", Toast.LENGTH_SHORT).show();
                });
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadTopPicks();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (topPicksAdapter != null) topPicksAdapter.cleanup();
    }

    private void signOut() {
        firebaseAuth.signOut();
        googleSignInClient.signOut().addOnCompleteListener(this, task -> {
            startActivity(new Intent(MainActivity.this, LoginActivity.class));
            finish();
        });
    }
}
