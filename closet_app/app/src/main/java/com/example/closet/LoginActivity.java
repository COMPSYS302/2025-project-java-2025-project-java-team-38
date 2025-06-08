package com.example.closet;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.auth.api.signin.*;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.CommonStatusCodes;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.*;

public class LoginActivity extends AppCompatActivity {
    private static final String TAG = "LoginActivity";

    private FirebaseAuth       mAuth;
    private GoogleSignInClient mGoogleSignInClient;

    // UI
    private EditText etEmail, etPassword;
    private Button   btnLogin, btnRegister;

    // Launcher for the Google Sign-In intent
    private final ActivityResultLauncher<Intent> signInLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.StartActivityForResult(),
                    this::onSignInResult
            );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.login);  // your XML with et_email, et_password, btn_login, btn_register, btn_google_signin

        // 1) Firebase Auth
        mAuth = FirebaseAuth.getInstance();

        // 2) If already signed in, skip straight to MainActivity
        if (mAuth.getCurrentUser() != null) {
            goToMain();
            return;
        }

        // 3) Wire up Email/Password UI
        etEmail    = findViewById(R.id.et_email);
        etPassword = findViewById(R.id.et_password);
        btnLogin   = findViewById(R.id.btn_login);
        btnRegister= findViewById(R.id.btn_register);

        btnLogin.setOnClickListener(v -> {
            String email = etEmail.getText().toString().trim();
            String pass  = etPassword.getText().toString();

            if (email.isEmpty() || pass.isEmpty()) {
                Toast.makeText(this, "Enter email & password", Toast.LENGTH_SHORT).show();
                return;
            }

            mAuth.signInWithEmailAndPassword(email, pass)
                    .addOnCompleteListener(this, task -> {
                        if (task.isSuccessful()) {
                            goToMain();
                        } else {
                            Toast.makeText(this,
                                    "Login failed: " + task.getException().getMessage(),
                                    Toast.LENGTH_LONG).show();
                        }
                    });
        });

        btnRegister.setOnClickListener(v -> {
            String email = etEmail.getText().toString().trim();
            String pass  = etPassword.getText().toString();

            if (email.isEmpty() || pass.length() < 6) {
                Toast.makeText(this,
                        "Provide valid email & at least 6-char password",
                        Toast.LENGTH_SHORT).show();
                return;
            }

            mAuth.createUserWithEmailAndPassword(email, pass)
                    .addOnCompleteListener(this, task -> {
                        if (task.isSuccessful()) {
                            Toast.makeText(this,
                                    "Registered! Please log in.",
                                    Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(this,
                                    "Registration failed: " + task.getException().getMessage(),
                                    Toast.LENGTH_LONG).show();
                        }
                    });
        });

        // 4) Configure Google Sign-In
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(
                GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

        // 5) Wire up Google button
        findViewById(R.id.btn_google_signin)
                .setOnClickListener(v -> {
                    Intent signInIntent = mGoogleSignInClient.getSignInIntent();
                    signInLauncher.launch(signInIntent);
                });
    }

    private void onSignInResult(ActivityResult result) {
        if (result.getResultCode() != RESULT_OK || result.getData() == null) {
            Toast.makeText(this, "Sign-in canceled.", Toast.LENGTH_SHORT).show();
            return;
        }

        Task<GoogleSignInAccount> task =
                GoogleSignIn.getSignedInAccountFromIntent(result.getData());
        try {
            GoogleSignInAccount acct = task.getResult(ApiException.class);
            if (acct == null || acct.getIdToken() == null) {
                throw new ApiException(new Status(CommonStatusCodes.INTERNAL_ERROR));
            }

            // Exchange the ID token for a Firebase credential
            AuthCredential credential =
                    GoogleAuthProvider.getCredential(acct.getIdToken(), null);

            mAuth.signInWithCredential(credential)
                    .addOnCompleteListener(this, authTask -> {
                        if (authTask.isSuccessful()) {
                            goToMain();
                        } else {
                            Log.w(TAG, "Firebase auth failed", authTask.getException());
                            Toast.makeText(this,
                                    "Authentication failed.",
                                    Toast.LENGTH_SHORT).show();
                        }
                    });
        } catch (ApiException e) {
            Log.w(TAG, "Google sign-in failed", e);
            Toast.makeText(this,
                    "Google Sign-In failed.",
                    Toast.LENGTH_SHORT).show();
        }
    }

    private void goToMain() {
        startActivity(new Intent(this, MainActivity.class));
        finish();
    }


}