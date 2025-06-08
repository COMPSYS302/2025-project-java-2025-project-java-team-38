package com.example.closet;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class AccountActivity extends AppCompatActivity {
    private FirebaseAuth      mAuth;
    private MaterialButton    btnLogout, btnDelete;
    private MaterialCardView  cardFaq, cardSupport;
    private TextView          tvFaqContent, tvSupportContent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_account);

        // inside onCreate(), after setContentView(...)
        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation);
// highlight the “Account” tab
        bottomNav.setSelectedItemId(R.id.nav_account);

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
                // already here
                return true;
            }
            return false;
        });


        mAuth = FirebaseAuth.getInstance();

        // bind views
        btnLogout        = findViewById(R.id.btn_logout_account);
        btnDelete        = findViewById(R.id.btn_delete_account);
        cardFaq          = findViewById(R.id.card_faq);
        cardSupport      = findViewById(R.id.card_support);
        tvFaqContent     = findViewById(R.id.tv_faq_content);
        tvSupportContent = findViewById(R.id.tv_support_content);

        // Toggle section buttons
        findViewById(R.id.card_faq).setOnClickListener(v -> toggleSection(cardFaq));
        findViewById(R.id.card_support).setOnClickListener(v -> toggleSection(cardSupport));

        // Log out
        btnLogout.setOnClickListener(v -> {
            mAuth.signOut();
            startActivity(new Intent(this, LoginActivity.class));
            finish();
        });

        // Delete account (with confirmation)
        btnDelete.setOnClickListener(v -> new AlertDialog.Builder(this)
                .setTitle("Delete Account")
                .setMessage("This will permanently delete your account. Continue?")
                .setPositiveButton("Delete", (dialog, which) -> deleteAndSignOut())
                .setNegativeButton("Cancel", null)
                .show()
        );

        // Toggle FAQ content
        cardFaq.setOnClickListener(v -> {
            tvFaqContent.setVisibility(
                    tvFaqContent.getVisibility() == View.VISIBLE
                            ? View.GONE
                            : View.VISIBLE
            );
        });

        // Toggle Support content
        cardSupport.setOnClickListener(v -> {
            tvSupportContent.setVisibility(
                    tvSupportContent.getVisibility() == View.VISIBLE
                            ? View.GONE
                            : View.VISIBLE
            );
        });
    }

    private void deleteAndSignOut() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) return;

        user.delete().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                Toast.makeText(this,
                        "Account deleted.", Toast.LENGTH_SHORT).show();
                mAuth.signOut();
                startActivity(new Intent(this, LoginActivity.class));
                finish();
            } else {
                Toast.makeText(this,
                        "Deletion failed: " + task.getException().getMessage(),
                        Toast.LENGTH_LONG).show();
            }
        });
    }

    private void toggleSection(MaterialCardView view) {
        boolean visible = view.getVisibility() == View.VISIBLE;
        cardFaq.setVisibility(View.GONE);
        cardSupport.setVisibility(View.GONE);
        if (!visible) view.setVisibility(View.VISIBLE);
    }
}
