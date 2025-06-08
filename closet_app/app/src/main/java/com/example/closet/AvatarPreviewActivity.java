package com.example.closet;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationView;
import com.squareup.picasso.Picasso;

public class AvatarPreviewActivity extends AppCompatActivity {
    private DrawerLayout drawerLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_avatar_preview);

        // Get URLs from intent
        String headwearUrl = getIntent().getStringExtra("headwear");
        String chainsUrl   = getIntent().getStringExtra("chains");
        String shirtsUrl   = getIntent().getStringExtra("shirts");
        String pantsUrl    = getIntent().getStringExtra("pants");
        String shoesUrl    = getIntent().getStringExtra("shoes");

        // Load images if URLs are not null
        loadImage(headwearUrl, R.id.img_headwear_preview);
        loadImage(chainsUrl,   R.id.img_chains_preview);
        loadImage(shirtsUrl,   R.id.img_shirts_preview);
        loadImage(pantsUrl,    R.id.img_pants_preview);
        loadImage(shoesUrl,    R.id.img_shoes_preview);

        // Bottom navigation
        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation);
        bottomNav.setSelectedItemId(R.id.nav_virtual_avatar);
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
                return true;
            } else if (id == R.id.nav_account) {
                startActivity(new Intent(this, AccountActivity.class));
                finish();
                return true;
            }
            return false;
        });

        // Navigation drawer
        drawerLayout = findViewById(R.id.drawer_layout);
        findViewById(R.id.hamburger_icon).setOnClickListener(v ->
                drawerLayout.openDrawer(GravityCompat.START));

        NavigationView nav = findViewById(R.id.navigation_view);
        nav.setNavigationItemSelectedListener(item -> {
            Intent intent = null;
            int id = item.getItemId();
            if (id == R.id.nav_home)            intent = new Intent(this, MainActivity.class);
            else if (id == R.id.nav_top_picks)  intent = new Intent(this, TopPicksActivity.class);
            else if (id == R.id.nav_most_viewed)intent = new Intent(this, MostViewedActivity.class);
            else if (id == R.id.nav_favourites) intent = new Intent(this, FavouritesActivity.class);
            else if (id == R.id.nav_virtual_avatar) {
                drawerLayout.closeDrawer(GravityCompat.START);
                return true;
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

    private void loadImage(String url, int imageViewId) {
        if (url != null && !url.isEmpty()) {
            ImageView img = findViewById(imageViewId);
            Picasso.get().load(url).fit().centerCrop().into(img);
        }
    }
}
