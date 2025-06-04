package com.example.closet;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
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
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FieldValue;

import java.util.List;

/**
 * Shows details for a single ClothingItem. Expects an Intent extra "ITEM_ID".
 * Layout: res/layout/activity_details.xml must define exactly these IDs:
 *   - @+id/product_name
 *   - @+id/fabric_text
 *   - @+id/fit_text
 *   - @+id/care_text
 *   - @+id/view_pager
 *   - @+id/dots_container
 *   - @+id/arrow_left
 *   - @+id/arrow_right
 *   - @+id/drawer_layout
 *   - @+id/hamburger_icon
 *   - @+id/navigation_view
 *   - @+id/logo_title
 *   - @+id/logo_icon
 *   - @+id/size_spinner
 *   - @+id/btn_fabric
 *   - @+id/btn_fit
 *   - @+id/btn_care
 */
public class DetailsActivity extends AppCompatActivity {
    private static final String TAG = "DetailsActivity";

    // Drawer + Navigation
    private DrawerLayout drawerLayout;
    private ActionBarDrawerToggle toggle;

    // Firestore instance and currently loaded item
    private FirebaseFirestore db;
    private ClothingItem currentItem;

    // UI components (matching your XML IDs exactly)
    private TextView productName;
    private TextView fabricText;
    private TextView fitText;
    private TextView careText;
    private ViewPager2 viewPager;
    private LinearLayout dotsContainer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_details);

        // Initialize Firestore
        db = FirebaseFirestore.getInstance();

        // 1) Pull the ITEM_ID from the Intent
        String itemId = getIntent().getStringExtra("ITEM_ID");
        if (itemId == null || itemId.isEmpty()) {
            Toast.makeText(this, "No item ID provided", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // 2) Hook up all UI references
        productName   = findViewById(R.id.product_name);
        fabricText    = findViewById(R.id.fabric_text);
        fitText       = findViewById(R.id.fit_text);
        careText      = findViewById(R.id.care_text);
        viewPager     = findViewById(R.id.view_pager);
        dotsContainer = findViewById(R.id.dots_container);

        // 3) Drawer + hamburger + navigation
        drawerLayout = findViewById(R.id.drawer_layout);
        toggle = new ActionBarDrawerToggle(
                this,
                drawerLayout,
                R.string.navigation_drawer_open,
                R.string.navigation_drawer_close
        );
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();

        ImageView hamburger = findViewById(R.id.hamburger_icon);
        hamburger.setOnClickListener(v -> drawerLayout.openDrawer(GravityCompat.START));

        NavigationView navigationView = findViewById(R.id.navigation_view);
        navigationView.setNavigationItemSelectedListener(menuItem -> {
            int id = menuItem.getItemId();
            if (id == R.id.nav_top_picks || id == R.id.nav_most_viewed || id == R.id.nav_new_in) {
                Intent intent = new Intent(DetailsActivity.this, MainActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);
            }
            drawerLayout.closeDrawers();
            return true;
        });

        // 4) Logo/title also navigates home
        findViewById(R.id.logo_title).setOnClickListener(v -> {
            Intent intent = new Intent(DetailsActivity.this, MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
        });
        findViewById(R.id.logo_icon).setOnClickListener(v -> {
            Intent intent = new Intent(DetailsActivity.this, MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
        });

        // 5) Size spinner (matching your XML: @+id/size_spinner)
        Spinner sizeSpinner = findViewById(R.id.size_spinner);
        if (sizeSpinner != null) {
            ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                    this,
                    R.array.sizes_array,
                    android.R.layout.simple_spinner_item
            );
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            sizeSpinner.setAdapter(adapter);
        }

        // 6) Toggle buttons for fabric/fit/care (matching your XML IDs)
        findViewById(R.id.btn_fabric).setOnClickListener(v -> toggleVisibility(fabricText));
        findViewById(R.id.btn_fit).setOnClickListener(v -> toggleVisibility(fitText));
        findViewById(R.id.btn_care).setOnClickListener(v -> toggleVisibility(careText));

        // 7) Load from Firestore and increment view‐count
        loadItemDetails(itemId);
        incrementViewCount(itemId);

        // (Optional) Change status bar color
        getWindow().setStatusBarColor(ContextCompat.getColor(this, R.color.categoryButtonLightGrey));
    }

    /**
     * Fetches the item document from Firestore, deserializes into ClothingItem,
     * and calls updateUI() once loaded.
     */
    private void loadItemDetails(String itemId) {
        // Use the same collection name you used in ListActivity ("Clothes")
        db.collection("Clothes").document(itemId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        currentItem = documentSnapshot.toObject(ClothingItem.class);
                        if (currentItem != null) {
                            updateUI();
                        }
                    } else {
                        Toast.makeText(DetailsActivity.this,
                                "Item not found", Toast.LENGTH_SHORT).show();
                        finish();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(DetailsActivity.this,
                            "Error loading item: " + e.getMessage(),
                            Toast.LENGTH_LONG).show();
                    Log.e(TAG, "Firestore get failed", e);
                    finish();
                });
    }

    /**
     * Increments a "viewCount" field in Firestore. Make sure your document
     * actually has a numeric field "viewCount" (or adapt this to your schema).
     */
    private void incrementViewCount(String itemId) {
        DocumentReference docRef = db.collection("Clothes").document(itemId);
        docRef.update("viewCount", FieldValue.increment(1))
                .addOnFailureListener(e -> Log.e(TAG, "Failed to increment viewCount", e));
    }

    /**
     * Once currentItem is loaded, populate text fields and set up the image slider.
     */
    private void updateUI() {
        // (1) Update text fields
        productName.setText(currentItem.getName());
        fabricText .setText(currentItem.getFabric());
        fitText    .setText(currentItem.getFit());
        careText   .setText(currentItem.getCare());

        // (2) Set up image slider if there are image URLs
        List<String> imageUrls = currentItem.getImages();
        if (imageUrls != null && !imageUrls.isEmpty()) {
            setupImageSlider(imageUrls);
        }
    }

    /**
     * Builds a ViewPager2 slider with dots underneath. Uses Glide to load each URL.
     */
    private void setupImageSlider(List<String> imageUrls) {
        // Adapter for ViewPager2 (use your existing ImageAdapter class)
        ImageAdapter adapter = new ImageAdapter(this, imageUrls);
        viewPager.setAdapter(adapter);

        // Clear old dots (if any)
        dotsContainer.removeAllViews();

        // Create one dot per image
        ImageView[] dots = new ImageView[imageUrls.size()];
        for (int i = 0; i < imageUrls.size(); i++) {
            dots[i] = new ImageView(this);
            dots[i].setImageDrawable(
                    ContextCompat.getDrawable(this, R.drawable.dot_unseen)
            );
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );
            params.setMargins(8, 0, 8, 0);
            dotsContainer.addView(dots[i], params);
        }

        // Highlight first dot
        if (dots.length > 0) {
            dots[0].setImageDrawable(
                    ContextCompat.getDrawable(this, R.drawable.dot_open)
            );
        }

        // Change dots on page swipe
        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                for (int j = 0; j < dots.length; j++) {
                    int drawableId = (j == position ? R.drawable.dot_open : R.drawable.dot_unseen);
                    dots[j].setImageDrawable(
                            ContextCompat.getDrawable(DetailsActivity.this, drawableId)
                    );
                }
            }
        });

        // Arrows to manually swipe
        ImageView arrowLeft  = findViewById(R.id.arrow_left);
        ImageView arrowRight = findViewById(R.id.arrow_right);

        arrowLeft.setOnClickListener(v -> {
            int curr = viewPager.getCurrentItem();
            if (curr > 0) {
                viewPager.setCurrentItem(curr - 1, true);
            }
        });
        arrowRight.setOnClickListener(v -> {
            int curr = viewPager.getCurrentItem();
            if (curr < imageUrls.size() - 1) {
                viewPager.setCurrentItem(curr + 1, true);
            }
        });
    }

    /**
     * Simple toggle helper: if the TextView is visible, hide it, otherwise show it.
     * Also hides the other two details so only one is expanded at a time.
     */
    private void toggleVisibility(TextView textView) {
        boolean isVisible = (textView.getVisibility() == View.VISIBLE);
        textView.setVisibility(isVisible ? View.GONE : View.VISIBLE);

        if (!isVisible) {
            // Hide the other two
            if (textView == fabricText) {
                fitText.setVisibility(View.GONE);
                careText.setVisibility(View.GONE);
            } else if (textView == fitText) {
                fabricText.setVisibility(View.GONE);
                careText.setVisibility(View.GONE);
            } else { // careText
                fabricText.setVisibility(View.GONE);
                fitText.setVisibility(View.GONE);
            }
        }
    }

    /**
     * Handles ActionBarDrawerToggle click (hamburger) if needed.
     */
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (toggle.onOptionsItemSelected(item)) return true;
        return super.onOptionsItemSelected(item);
    }
}
