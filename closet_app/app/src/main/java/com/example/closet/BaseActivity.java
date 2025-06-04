// File: app/src/main/java/com/example/closet/BaseActivity.java
package com.example.closet;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.google.android.material.navigation.NavigationView;

/**
 * BaseActivity sets up a common navigation drawer (hamburger + menu) for all screens.
 *
 * Each subclass must:
 *   1) Call setContentView(...) in onCreate(...)
 *   2) Have a DrawerLayout with ID @+id/drawer_layout
 *   3) Inside that DrawerLayout, have a NavigationView with ID @+id/navigation_view
 *      and set app:headerLayout="@layout/nav_header" and app:menu="@menu/drawer_menu"
 *   4) Have a “hamburger” View (ImageView or similar) with ID @+id/hamburger_icon
 *   5) After setContentView(...), call setupDrawer() to wire it up
 */
public abstract class BaseActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    protected DrawerLayout drawerLayout;
    protected NavigationView navigationView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Note: Do NOT call setContentView(...) here. Each child Activity calls setContentView(...)
        //       and then calls setupDrawer() afterward.
    }

    /**
     * Call this method immediately after your Activity’s setContentView(...) call.
     * It assumes your layout contains:
     *   • a DrawerLayout with android:id="@+id/drawer_layout"
     *   • a NavigationView with android:id="@+id/navigation_view" (and XML attributes
     *       app:headerLayout="@layout/nav_header", app:menu="@menu/drawer_menu")
     *   • a “hamburger” View (ImageView, Toolbar icon, etc.) with android:id="@+id/hamburger_icon"
     */
    protected void setupDrawer() {
        // 1) Find our DrawerLayout + NavigationView
        drawerLayout = findViewById(R.id.drawer_layout);
        navigationView = findViewById(R.id.navigation_view);

        // 2) Tell the NavigationView that this BaseActivity will handle item clicks
        navigationView.setNavigationItemSelectedListener(this);

        // 3) Wire up the “hamburger” so that tapping it opens/closes the drawer
        View hamburger = findViewById(R.id.hamburger_icon);
        if (hamburger != null) {
            hamburger.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
                        drawerLayout.closeDrawer(GravityCompat.START);
                    } else {
                        drawerLayout.openDrawer(GravityCompat.START);
                    }
                }
            });
        }
    }

    /**
     * This is invoked whenever a user taps one of the menu items in drawer_menu.xml.
     * We always close the drawer first, then launch the requested Activity—unless we’re
     * already in that Activity (checked with instanceof).
     */
/*
@Override
public boolean onNavigationItemSelected(@NonNull MenuItem item) {
    // Close the drawer immediately
    drawerLayout.closeDrawer(GravityCompat.START);

    int id = item.getItemId();

    // “Home” item
    if (id == R.id.nav_home) {
        // If this Activity is NOT already MainActivity, then start it
        if (!(this instanceof MainActivity)) {
            startActivity(new Intent(this, MainActivity.class));
        }
        return true;
    }

    // “Hot Deals” item
    if (id == R.id.nav_hot_deals) {
        if (!(this instanceof HotDealsActivity)) {
            startActivity(new Intent(this, HotDealsActivity.class));
        }
        return true;
    }

    // “Most Viewed” item
    if (id == R.id.nav_most_viewed) {
        if (!(this instanceof MostViewedActivity)) {
            startActivity(new Intent(this, MostViewedActivity.class));
        }
        return true;
    }

    // “New In” item
    if (id == R.id.nav_new_in) {
        if (!(this instanceof NewInActivity)) {
            startActivity(new Intent(this, NewInActivity.class));
        }
        return true;
    }

    // “Categories” item
    if (id == R.id.nav_categories) {
        if (!(this instanceof CategoriesActivity)) {
            startActivity(new Intent(this, CategoriesActivity.class));
        }
        return true;
    }

    // “Favourites” item
    if (id == R.id.nav_favourites) {
        if (!(this instanceof FavouritesActivity)) {
            startActivity(new Intent(this, FavouritesActivity.class));
        }
        return true;
    }

    // “Cart” item
    if (id == R.id.nav_cart) {
        if (!(this instanceof CartActivity)) {
            startActivity(new Intent(this, CartActivity.class));
        }
        return true;
    }

    // If none of the above matched, do nothing
    return false;
}
*/

}
