package com.example.closet;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.List;

public class AvatarActivity extends AppCompatActivity {
    private DrawerLayout    drawerLayout;
    private FirebaseFirestore db;
    private String            currentUserId;

    private ImageView imgHeadwear, imgJewellery, imgShirts, imgPants, imgShoes;
    private Button    btnEnvision;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_avatar);

        // ─── Bottom Navigation ───
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
                return true; // we're already here
            } else if (id == R.id.nav_account) {
                startActivity(new Intent(this, AccountActivity.class));
                finish();
                return true;
            }
            return false;
        });

        // ─── Drawer + NavView ───
        drawerLayout = findViewById(R.id.drawer_layout);
        findViewById(R.id.hamburger_icon)
                .setOnClickListener(v -> drawerLayout.openDrawer(GravityCompat.START));

        NavigationView nav = findViewById(R.id.navigation_view);
        nav.setNavigationItemSelectedListener(item -> {
            Intent i = null;
            int id = item.getItemId();
            if (id == R.id.nav_home)          i = new Intent(this, MainActivity.class);
            else if (id == R.id.nav_top_picks)  i = new Intent(this, TopPicksActivity.class);
            else if (id == R.id.nav_most_viewed) i = new Intent(this, MostViewedActivity.class);
            else if (id == R.id.nav_favourites)  i = new Intent(this, FavouritesActivity.class);
            else if (id == R.id.nav_virtual_avatar) {
                drawerLayout.closeDrawer(GravityCompat.START);
                return true;
            }

            if (i != null) {
                i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(i);
                finish();
            }
            drawerLayout.closeDrawer(GravityCompat.START);
            return true;
        });

        // ─── Firestore + Auth ───
        db            = FirebaseFirestore.getInstance();
        currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        // ─── Find the “+” boxes ───
        imgHeadwear  = findViewById(R.id.img_headwear);
        imgJewellery = findViewById(R.id.img_jewellery);
        imgShirts    = findViewById(R.id.img_shirts);
        imgPants     = findViewById(R.id.img_pants);
        imgShoes     = findViewById(R.id.img_shoes);

        setupChooser("Headwear",  imgHeadwear);
        setupChooser("Jewellery", imgJewellery);
        setupChooser("Shirts",    imgShirts);
        setupChooser("Pants",     imgPants);
        setupChooser("Shoes",     imgShoes);

        // ─── Envision button ───
        btnEnvision = findViewById(R.id.btn_envision);
        btnEnvision.setOnClickListener(v ->
                        Toast.makeText(this, "Envisioning avatar…", Toast.LENGTH_SHORT).show()
                // TODO: composite your five layers here
        );
    }

    /**
     * When you tap any of the five ImageViews, query your “Clothes” collection
     * for that category where the current user is in likedUsers, then show
     * a little AlertDialog to pick “None” or one of the results.
     */
    private void setupChooser(String category, ImageView targetImg) {
        targetImg.setOnClickListener(v -> {
            Query q = db.collection("Clothes")
                    .whereEqualTo("Category", category)
                    .whereArrayContains("likedUsers", currentUserId);

            q.get().addOnSuccessListener(snapshot -> {
                        List<ClothingItem> items = snapshot.toObjects(ClothingItem.class);
                        List<String> names = new ArrayList<>();
                        names.add("None");
                        for (ClothingItem ci : items) names.add(ci.getName());

                        new AlertDialog.Builder(this)
                                .setTitle("Select " + category)
                                .setItems(names.toArray(new String[0]), (DialogInterface dlg, int which) -> {
                                    if (which == 0) {
                                        targetImg.setImageResource(R.drawable.ic_add);
                                    } else {
                                        ClothingItem sel = items.get(which - 1);
                                        Picasso.get()
                                                .load(sel.getImageUrl())
                                                .fit()
                                                .centerCrop()
                                                .into(targetImg);
                                    }
                                })
                                .show();
                    })
                    .addOnFailureListener(e ->
                            Toast.makeText(this, "Failed to load favorites", Toast.LENGTH_SHORT).show()
                    );
        });
    }
}
