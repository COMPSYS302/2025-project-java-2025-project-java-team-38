package com.example.closet;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.DocumentSnapshot;

import java.util.List;

public class DetailsActivity extends AppCompatActivity {

    private DrawerLayout drawerLayout;
    private ActionBarDrawerToggle toggle;
    private FirebaseFirestore db;

    private TextView itemName, fabricText, fitText, careText;
    private ViewPager2 viewPager;
    private LinearLayout dotsContainer;

    private static final String TAG = "DetailsActivity";

    private ImageView likeButton;
    private SharedPreferences prefs;
    private String itemId;

    @Override

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_details);

        db = FirebaseFirestore.getInstance();

        // UI hooks
        itemName = findViewById(R.id.product_name);
        fabricText = findViewById(R.id.fabric_text);
        fitText = findViewById(R.id.fit_text);
        careText = findViewById(R.id.care_text);
        viewPager = findViewById(R.id.view_pager);
        dotsContainer = findViewById(R.id.dots_container);

        likeButton = findViewById(R.id.like_button);
        prefs = getSharedPreferences("LikedPrefs", MODE_PRIVATE);

        // Spinner setup
        Spinner sizeSpinner = findViewById(R.id.size_spinner);
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                this, R.array.sizes_array, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        sizeSpinner.setAdapter(adapter);

        // Toggle section buttons
        findViewById(R.id.btn_fabric).setOnClickListener(v -> toggleSection(fabricText));
        findViewById(R.id.btn_fit).setOnClickListener(v -> toggleSection(fitText));
        findViewById(R.id.btn_care).setOnClickListener(v -> toggleSection(careText));

        // Drawer setup
        drawerLayout = findViewById(R.id.drawer_layout);
        toggle = new ActionBarDrawerToggle(this, drawerLayout,
                R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();

        findViewById(R.id.hamburger_icon).setOnClickListener(v -> drawerLayout.openDrawer(GravityCompat.START));

        NavigationView navigationView = findViewById(R.id.navigation_view);
        navigationView.setNavigationItemSelectedListener(item -> {
            int itemId = item.getItemId();
            Intent intent = null;

            if (itemId == R.id.nav_home) {
                intent = new Intent(this, MainActivity.class);
            } else if (itemId == R.id.nav_top_picks) {
                intent = new Intent(this, TopPicksActivity.class);
            } else if (itemId == R.id.nav_most_viewed) {
                intent = new Intent(this, MostViewedActivity.class);
            } else if (itemId == R.id.nav_favourites) {
                intent = new Intent(this, FavouritesActivity.class);
            } else if (itemId == R.id.nav_new_in) {
                Toast.makeText(this, "New In clicked", Toast.LENGTH_SHORT).show();
            } else if (itemId == R.id.nav_categories) {
                Toast.makeText(this, "Categories clicked", Toast.LENGTH_SHORT).show();
            } else if (itemId == R.id.nav_virtual_avatar) {
                Toast.makeText(this, "Virtual Avatar clicked", Toast.LENGTH_SHORT).show();
            }

            if (intent != null) {
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);
                finish();
            }

            drawerLayout.closeDrawer(GravityCompat.START);
            return true;
        });

        // Logo click
        findViewById(R.id.logo_title).setOnClickListener(v -> goHome());
        findViewById(R.id.logo_icon).setOnClickListener(v -> goHome());

        // Load Firestore data
        itemId = getIntent().getStringExtra("ITEM_ID");
        if (itemId != null) {
            setupLikeLogic();
            loadFirestore(itemId);
            incrementViewCount(itemId);
        } else {
            Log.e(TAG, "No ITEM_ID passed to DetailsActivity");
        }
    }


    private void goHome() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
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

                    try {
                        List<String> images = (List<String>) document.get("images");
                        if (images != null && !images.isEmpty()) {
                            setupImageSlider(images);
                        } else {
                            Log.w(TAG, "No images found for item");
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error retrieving images list", e);
                    }
                })
                .addOnFailureListener(e -> Log.e(TAG, "Failed to load Firestore doc", e));
    }
    private void incrementViewCount(String itemId) {
        db.collection("Clothes")
                .document(itemId)
                .update("Views", com.google.firebase.firestore.FieldValue.increment(1))
                .addOnSuccessListener(aVoid ->
                        Log.d(TAG, "View count incremented for item: " + itemId))
                .addOnFailureListener(e ->
                        Log.e(TAG, "Failed to increment view count", e));
    }


    private void setupImageSlider(List<String> urls) {
        ImageAdapter adapter = new ImageAdapter(this, urls);
        viewPager.setAdapter(adapter);

        // Dots
        dotsContainer.removeAllViews();
        ImageView[] dots = new ImageView[urls.size()];

        for (int i = 0; i < urls.size(); i++) {
            dots[i] = new ImageView(this);
            dots[i].setImageDrawable(ContextCompat.getDrawable(this, R.drawable.dot_unseen));

            LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );
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
                            i == pos ? R.drawable.dot_open : R.drawable.dot_unseen
                    ));
                }
            }
        });

        // Arrow buttons
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
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        FirebaseAuth auth = FirebaseAuth.getInstance();
        String userId = auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : null;

        if (userId == null || itemId == null) return;

        db.collection("Clothes").document(itemId).get()
                .addOnSuccessListener(doc -> {
                    List<String> likedUsers = (List<String>) doc.get("likedUsers");
                    boolean[] isLiked = {likedUsers != null && likedUsers.contains(userId)};

                    // Set initial heart icon
                    likeButton.setImageResource(isLiked[0] ? R.drawable.ic_heart_filled : R.drawable.ic_favorite);

                    likeButton.setOnClickListener(v -> {
                        isLiked[0] = !isLiked[0];  // Flip like status
                        likeButton.setImageResource(isLiked[0] ? R.drawable.ic_heart_filled : R.drawable.ic_favorite);

                        db.collection("Clothes").document(itemId)
                                .update("likedUsers", isLiked[0]
                                        ? com.google.firebase.firestore.FieldValue.arrayUnion(userId)
                                        : com.google.firebase.firestore.FieldValue.arrayRemove(userId))
                                .addOnSuccessListener(aVoid -> Log.d(TAG, "Like updated"))
                                .addOnFailureListener(e -> Log.e(TAG, "Failed to update like", e));
                    });
                })
                .addOnFailureListener(e -> Log.e(TAG, "Failed to get item for like setup", e));
    }

}