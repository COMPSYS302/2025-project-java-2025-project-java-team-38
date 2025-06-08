package com.example.closet;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.List;

public class DetailsActivity extends AppCompatActivity {

    private static final String TAG = "DetailsActivity";

    private DrawerLayout drawerLayout;
    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private String itemId;
    private String userId;

    private TextView itemName, fabricText, fitText, careText;
    private ViewPager2 viewPager;
    private LinearLayout dotsContainer;
    private ImageView likeButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // displaying details activity
        setContentView(R.layout.activity_details);

        // setting up bottom navigation panel and back button
        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation);
        ImageButton back = findViewById(R.id.btn_back);
        back.setOnClickListener(v -> finish());
        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_home) {
                startActivity(new Intent(this, MainActivity.class));
                finish();
                return true;
            } else if (id == R.id.nav_favorites) {
                startActivity(new Intent(this, FavouritesActivity.class));
                finish();
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


        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
        userId = auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : null;

        // setting up info for the animations
        itemName = findViewById(R.id.product_name);
        fabricText = findViewById(R.id.fabric_text);
        fitText = findViewById(R.id.fit_text);
        careText = findViewById(R.id.care_text);
        viewPager = findViewById(R.id.view_pager);
        dotsContainer = findViewById(R.id.dots_container);
        likeButton = findViewById(R.id.like_button);

        // sizes drop down logic
        Spinner sizeSpinner = findViewById(R.id.size_spinner);
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                this, R.array.sizes_array, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        sizeSpinner.setAdapter(adapter);

        findViewById(R.id.btn_fabric).setOnClickListener(v -> toggleSection(fabricText));
        findViewById(R.id.btn_fit).setOnClickListener(v -> toggleSection(fitText));
        findViewById(R.id.btn_care).setOnClickListener(v -> toggleSection(careText));

        setupDrawer();
        setupSearchBar();

        // since its being viewed, increment count in firestore
        itemId = getIntent().getStringExtra("ITEM_ID");
        if (itemId != null) {
            setupLikeLogic();
            loadFirestore(itemId);
            incrementViewCount(itemId);
        } else {
            Log.e(TAG, "No ITEM_ID passed to DetailsActivity");
        }

        // transitions for home page
        findViewById(R.id.logo_title).setOnClickListener(v -> goHome());
        findViewById(R.id.logo_icon).setOnClickListener(v -> goHome());
    }
    
    private void setupSearchBar() {
        EditText searchBar = findViewById(R.id.search_bar);
        searchBar.setOnEditorActionListener((v, actionId, event) -> {
            String query = searchBar.getText().toString().trim();
            if (!query.isEmpty()) {
                Intent intent = new Intent(DetailsActivity.this, ListActivity.class);
                intent.putExtra("SEARCH_QUERY", query);
                intent.putExtra("SEARCH_SCOPE", "global");
                startActivity(intent);
            }
            return true;
        });
    }

    private void setupDrawer() {
        drawerLayout = findViewById(R.id.drawer_layout);
        findViewById(R.id.hamburger_icon).setOnClickListener(v -> drawerLayout.openDrawer(GravityCompat.START));

        NavigationView navigationView = findViewById(R.id.navigation_view);
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
                intent = new Intent(this, FavouritesActivity.class);
            } else {
                Toast.makeText(this, item.getTitle() + " clicked", Toast.LENGTH_SHORT).show();
            }

            if (intent != null) {
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);
                finish();
            }

            drawerLayout.closeDrawer(GravityCompat.START);
            return true;
        });
    }

    private void toggleSection(TextView view) {
        boolean visible = view.getVisibility() == View.VISIBLE;
        fabricText.setVisibility(View.GONE);
        fitText.setVisibility(View.GONE);
        careText.setVisibility(View.GONE);
        if (!visible) view.setVisibility(View.VISIBLE);
    }

    private void loadFirestore(String itemId) {
        db.collection("Clothes").document(itemId).get()
                .addOnSuccessListener(document -> {
                    if (!document.exists()) {
                        Log.e(TAG, "Document doesn't exist");
                        return;
                    }

                    itemName.setText(document.getString("Name"));
                    fabricText.setText(document.getString("Fabric"));
                    fitText.setText(document.getString("Fit"));
                    careText.setText(document.getString("Care"));

                    List<String> images = (List<String>) document.get("images");
                    if (images != null && !images.isEmpty()) {
                        setupImageSlider(images);
                    }
                })
                .addOnFailureListener(e -> Log.e(TAG, "Failed to load Firestore doc", e));
    }

    private void incrementViewCount(String itemId) {
        db.collection("Clothes").document(itemId)
                .update("Views", com.google.firebase.firestore.FieldValue.increment(1))
                .addOnSuccessListener(aVoid ->
                        Log.d(TAG, "View count incremented for item: " + itemId))
                .addOnFailureListener(e ->
                        Log.e(TAG, "Failed to increment view count", e));
    }

    private void setupImageSlider(List<String> urls) {
        ImageAdapter adapter = new ImageAdapter(this, urls);
        viewPager.setAdapter(adapter);

        dotsContainer.removeAllViews();
        ImageView[] dots = new ImageView[urls.size()];

        for (int i = 0; i < urls.size(); i++) {
            dots[i] = new ImageView(this);
            dots[i].setImageDrawable(ContextCompat.getDrawable(this, R.drawable.dot_unseen));

            LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT);
            p.setMargins(8, 0, 8, 0);
            dotsContainer.addView(dots[i], p);
        }

        if (dots.length > 0)
            dots[0].setImageDrawable(ContextCompat.getDrawable(this, R.drawable.dot_open));

        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int pos) {
                for (int i = 0; i < dots.length; i++) {
                    dots[i].setImageDrawable(ContextCompat.getDrawable(
                            DetailsActivity.this,
                            i == pos ? R.drawable.dot_open : R.drawable.dot_unseen));
                }
            }
        });

        findViewById(R.id.arrow_left).setOnClickListener(v -> {
            int current = viewPager.getCurrentItem();
            if (current > 0) viewPager.setCurrentItem(current - 1, true);
        });

        findViewById(R.id.arrow_right).setOnClickListener(v -> {
            int current = viewPager.getCurrentItem();
            if (current < urls.size() - 1) viewPager.setCurrentItem(current + 1, true);
        });
    }

    private void setupLikeLogic() {
        if (userId == null || itemId == null) return;

        db.collection("Clothes").document(itemId).get()
                .addOnSuccessListener(doc -> {
                    List<String> likedUsers = (List<String>) doc.get("likedUsers");
                    boolean[] isLiked = {likedUsers != null && likedUsers.contains(userId)};

                    likeButton.setImageResource(isLiked[0] ? R.drawable.ic_heart_filled : R.drawable.ic_favorite);

                    likeButton.setOnClickListener(v -> {
                        isLiked[0] = !isLiked[0];
                        likeButton.setImageResource(isLiked[0] ? R.drawable.ic_heart_filled : R.drawable.ic_favorite);

                        db.collection("Clothes").document(itemId)
                                .update("likedUsers", isLiked[0]
                                        ? com.google.firebase.firestore.FieldValue.arrayUnion(userId)
                                        : com.google.firebase.firestore.FieldValue.arrayRemove(userId))
                                .addOnSuccessListener(aVoid -> Log.d(TAG, "Like status updated"))
                                .addOnFailureListener(e -> Log.e(TAG, "Failed to update like", e));
                    });
                })
                .addOnFailureListener(e -> Log.e(TAG, "Failed to get item for like setup", e));
    }

    private void goHome() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
    }
}
