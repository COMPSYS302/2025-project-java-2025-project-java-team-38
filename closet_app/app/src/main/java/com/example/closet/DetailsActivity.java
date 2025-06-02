package com.example.closet;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.ArrayAdapter;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.google.android.material.navigation.NavigationView;

public class DetailsActivity extends AppCompatActivity {

    DrawerLayout drawerLayout;
    ActionBarDrawerToggle toggle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_details);

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

        TextView fabricText = findViewById(R.id.fabric_text);
        TextView fitText = findViewById(R.id.fit_text);
        TextView careText = findViewById(R.id.care_text);

        // FABRIC toggle
        findViewById(R.id.btn_fabric).setOnClickListener(v -> {
            fabricText.setVisibility(fabricText.getVisibility() == View.VISIBLE ? View.GONE : View.VISIBLE);
            fitText.setVisibility(View.GONE);
            careText.setVisibility(View.GONE);
        });

// FIT toggle
        findViewById(R.id.btn_fit).setOnClickListener(v -> {
            fitText.setVisibility(fitText.getVisibility() == View.VISIBLE ? View.GONE : View.VISIBLE);
            fabricText.setVisibility(View.GONE);
            careText.setVisibility(View.GONE);
        });

// CARE toggle
        findViewById(R.id.btn_care).setOnClickListener(v -> {
            careText.setVisibility(careText.getVisibility() == View.VISIBLE ? View.GONE : View.VISIBLE);
            fabricText.setVisibility(View.GONE);
            fitText.setVisibility(View.GONE);
        });

    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (toggle.onOptionsItemSelected(item)) return true;
        return super.onOptionsItemSelected(item);
    }
}