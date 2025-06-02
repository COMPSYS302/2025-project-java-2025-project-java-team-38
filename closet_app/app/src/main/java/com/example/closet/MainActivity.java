package com.example.closet;

import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.RecyclerView;
import androidx.appcompat.widget.SearchView;
import androidx.drawerlayout.widget.DrawerLayout;
import android.widget.ImageView;
import android.content.Intent;

import androidx.appcompat.app.ActionBarDrawerToggle;

import androidx.core.view.GravityCompat;


import com.google.android.material.navigation.NavigationView;

public class MainActivity extends AppCompatActivity {

    private RecyclerView topPicksRecyclerView;
    private SearchView searchView;

    DrawerLayout drawerLayout;
    ActionBarDrawerToggle toggle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main); // or whatever your main XML is

        // Initialize drawer layout
        drawerLayout = findViewById(R.id.drawer_layout);

        // Hamburger menu set up
        ImageView hamburger = findViewById(R.id.hamburger_icon);
        hamburger.setOnClickListener(v -> drawerLayout.openDrawer(GravityCompat.START));

        // Navigation View logic
        NavigationView navigationView = findViewById(R.id.navigation_view);
        navigationView.setNavigationItemSelectedListener(item -> {
            int id = item.getItemId();

            // Example action
            if (id == R.id.nav_top_picks || id == R.id.nav_new_in || id == R.id.nav_most_viewed) {
                startActivity(new Intent(MainActivity.this, MainActivity.class));
            }

            drawerLayout.closeDrawers();
            return true;
        });
    }

}