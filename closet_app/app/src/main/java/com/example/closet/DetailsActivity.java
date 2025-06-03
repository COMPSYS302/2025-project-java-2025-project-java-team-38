package com.example.closet;

import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import androidx.core.content.ContextCompat;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.navigation.NavigationView;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FieldValue;
import com.squareup.picasso.Picasso;

import java.util.List;

public class DetailsActivity extends AppCompatActivity {

    DrawerLayout drawerLayout;
    ActionBarDrawerToggle toggle;

    // Firestore instance
    private FirebaseFirestore db;
    private ClothingItem currentItem;

    // UI components
    private TextView itemName, fabricText, fitText, careText;
    private ViewPager2 viewPager;
    private LinearLayout dotsContainer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(R.style.Theme_Closet);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_details);

        // Initialize Firestore
        db = FirebaseFirestore.getInstance();

        // Get item ID from intent
        String itemId = getIntent().getStringExtra("ITEM_ID");
        if (itemId == null || itemId.isEmpty()) {
            finish();
            return;
        }

        // Initialize UI components
        itemName = findViewById(R.id.item_name);
        fabricText = findViewById(R.id.fabric_text);
        fitText = findViewById(R.id.fit_text);
        careText = findViewById(R.id.care_text);
        viewPager = findViewById(R.id.view_pager);
        dotsContainer = findViewById(R.id.dots_container);

        // Side window set up
        drawerLayout = findViewById(R.id.drawer_layout);
        toggle = new ActionBarDrawerToggle(
                this,
                drawerLayout,
                R.string.navigation_drawer_open,
                R.string.navigation_drawer_close
        );
        drawerLayout.addDrawerListener(toggle);

        // Hamburger click set up
        ImageView hamburger = findViewById(R.id.hamburger_icon);
        hamburger.setOnClickListener(v -> drawerLayout.openDrawer(GravityCompat.START));

        // Navigating menu via clicks
        NavigationView navigationView = findViewById(R.id.navigation_view);
        navigationView.setNavigationItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_top_picks || id == R.id.nav_most_viewed || id == R.id.nav_new_in) {
                startActivity(new Intent(DetailsActivity.this, MainActivity.class));
            }
            drawerLayout.closeDrawers();
            return true;
        });

        // Click logo or title sends to home page
        TextView title = findViewById(R.id.logo_title);
        ImageView logo = findViewById(R.id.logo_icon);
        View.OnClickListener goHome = v -> {
            Intent intent = new Intent(DetailsActivity.this, MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
        };
        title.setOnClickListener(goHome);
        logo.setOnClickListener(goHome);

        // Dropdown for sizes
        Spinner sizeSpinner = findViewById(R.id.size_spinner);
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                this,
                R.array.sizes_array,
                android.R.layout.simple_spinner_item
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        sizeSpinner.setAdapter(adapter);

        // Button listeners
        findViewById(R.id.btn_fabric).setOnClickListener(v -> toggleVisibility(fabricText));
        findViewById(R.id.btn_fit).setOnClickListener(v -> toggleVisibility(fitText));
        findViewById(R.id.btn_care).setOnClickListener(v -> toggleVisibility(careText));

        // Load item details from Firestore
        loadItemDetails(itemId);

        // Update view count
        incrementViewCount(itemId);

        getWindow().setStatusBarColor(ContextCompat.getColor(this, R.color.categoryButtonLightGrey));
    }

    private void loadItemDetails(String itemId) {
        db.collection("clothing_items").document(itemId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        currentItem = documentSnapshot.toObject(ClothingItem.class);
                        if (currentItem != null) {
                            updateUI();
                        }
                    }
                });
    }

    private void incrementViewCount(String itemId) {
        DocumentReference docRef = db.collection("clothing_items").document(itemId);
        docRef.update("viewCount", FieldValue.increment(1));
    }

    private void updateUI() {
        // Set text fields
        itemName.setText(currentItem.getName());
        fabricText.setText(currentItem.getFabric());
        fitText.setText(currentItem.getFit());
        careText.setText(currentItem.getCare());

        // Setup image slider
        if (currentItem.getImageUrls() != null && !currentItem.getImageUrls().isEmpty()) {
            setupImageSlider(currentItem.getImageUrls());
        }
    }

    private void setupImageSlider(List<String> imageUrls) {
        ImageAdapter adapter = new ImageAdapter(this, imageUrls);
        viewPager.setAdapter(adapter);

        // Clear existing dots
        dotsContainer.removeAllViews();

        // Create new dots
        ImageView[] dots = new ImageView[imageUrls.size()];
        for (int i = 0; i < dots.length; i++) {
            dots[i] = new ImageView(this);
            dots[i].setImageDrawable(ContextCompat.getDrawable(this, R.drawable.dot_unseen));
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );
            params.setMargins(8, 0, 8, 0);
            dotsContainer.addView(dots[i], params);
        }

        // Set first dot as active
        if (dots.length > 0) {
            dots[0].setImageDrawable(ContextCompat.getDrawable(this, R.drawable.dot_open));
        }

        // Dot change listener
        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                for (int i = 0; i < dots.length; i++) {
                    dots[i].setImageDrawable(ContextCompat.getDrawable(
                            DetailsActivity.this,
                            i == position ? R.drawable.dot_open : R.drawable.dot_unseen
                    ));
                }
            }
        });

        // Arrows navigation
        ImageView arrowLeft = findViewById(R.id.arrow_left);
        ImageView arrowRight = findViewById(R.id.arrow_right);

        arrowLeft.setOnClickListener(v -> {
            int currentItem = viewPager.getCurrentItem();
            if (currentItem > 0) {
                viewPager.setCurrentItem(currentItem - 1, true);
            }
        });

        arrowRight.setOnClickListener(v -> {
            int currentItem = viewPager.getCurrentItem();
            if (currentItem < imageUrls.size() - 1) {
                viewPager.setCurrentItem(currentItem + 1, true);
            }
        });
    }

    private void toggleVisibility(TextView textView) {
        boolean isVisible = textView.getVisibility() == View.VISIBLE;
        textView.setVisibility(isVisible ? View.GONE : View.VISIBLE);

        // Hide other text views
        if (!isVisible) {
            if (textView == fabricText) {
                fitText.setVisibility(View.GONE);
                careText.setVisibility(View.GONE);
            } else if (textView == fitText) {
                fabricText.setVisibility(View.GONE);
                careText.setVisibility(View.GONE);
            } else if (textView == careText) {
                fabricText.setVisibility(View.GONE);
                fitText.setVisibility(View.GONE);
            }
        }
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (toggle.onOptionsItemSelected(item)) return true;
        return super.onOptionsItemSelected(item);
    }
}