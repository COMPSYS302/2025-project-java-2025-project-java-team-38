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
import androidx.activity.result.IntentSenderRequest;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.auth.api.identity.BeginSignInRequest;
import com.google.android.gms.auth.api.identity.BeginSignInRequest.GoogleIdTokenRequestOptions;
import com.google.android.gms.auth.api.identity.Identity;
import com.google.android.gms.auth.api.identity.SignInClient;
import com.google.android.gms.auth.api.identity.SignInCredential;
import com.google.android.gms.common.SignInButton;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.GoogleAuthProvider;

public class LoginActivity extends AppCompatActivity {
    private static final String TAG = "LoginActivity";

    private FirebaseAuth mAuth;
    private SignInClient oneTapClient;

    // UI
    private EditText etEmail, etPassword;
    private Button btnLogin, btnRegister;
    private SignInButton btnGoogle;

    // One-Tap launcher
    private final ActivityResultLauncher<IntentSenderRequest> oneTapLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.StartIntentSenderForResult(),
                    this::onOneTapResult
            );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.login);

        // Firebase
        mAuth = FirebaseAuth.getInstance();
        if (mAuth.getCurrentUser() != null) {
            goToMain();
            return;
        }

        // Wire up email/password
        etEmail    = findViewById(R.id.et_email);
        etPassword = findViewById(R.id.et_password);
        btnLogin   = findViewById(R.id.btn_login);
        btnRegister= findViewById(R.id.btn_register);
        btnGoogle  = findViewById(R.id.btn_google_signin);

        btnLogin.setOnClickListener(v -> {
            String email = etEmail.getText().toString().trim();
            String pass  = etPassword.getText().toString();
            if (email.isEmpty() || pass.isEmpty()) {
                Toast.makeText(this, "Enter email & password", Toast.LENGTH_SHORT).show();
                return;
            }
            mAuth.signInWithEmailAndPassword(email, pass)
                    .addOnCompleteListener(this, task -> {
                        if (task.isSuccessful()) goToMain();
                        else Toast.makeText(this,
                                "Login failed: " + task.getException().getMessage(),
                                Toast.LENGTH_LONG).show();
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

        // Configure One-Tap
        oneTapClient = Identity.getSignInClient(this);
        BeginSignInRequest signInRequest = BeginSignInRequest.builder()
                .setGoogleIdTokenRequestOptions(
                        GoogleIdTokenRequestOptions.builder()
                                .setSupported(true)
                                .setServerClientId(getString(R.string.default_web_client_id))
                                .setFilterByAuthorizedAccounts(false)
                                .build()
                )
                .build();

        // Trigger One-Tap on Google button click
        btnGoogle.setSize(SignInButton.SIZE_WIDE);
        btnGoogle.setOnClickListener(v ->
                oneTapClient.beginSignIn(signInRequest)
                        .addOnSuccessListener(res -> {
                            IntentSenderRequest req = new IntentSenderRequest.Builder(
                                    res.getPendingIntent().getIntentSender()
                            ).build();
                            oneTapLauncher.launch(req);
                        })
                        .addOnFailureListener(e -> {
                            Log.w(TAG, "One-Tap failed to start", e);
                            // fallback if needed
                        })
        );
    }

    private void onOneTapResult(ActivityResult result) {
        if (result.getResultCode() != RESULT_OK || result.getData() == null) {
            Toast.makeText(this, "Sign-in canceled.", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            // Extract One-Tap credential
            SignInCredential credential =
                    oneTapClient.getSignInCredentialFromIntent(result.getData());
            String idToken = credential.getGoogleIdToken();
            if (idToken == null) {
                Toast.makeText(this, "No ID token returned.", Toast.LENGTH_SHORT).show();
                return;
            }

            // Exchange for Firebase credential
            AuthCredential firebaseCred =
                    GoogleAuthProvider.getCredential(idToken, null);
            mAuth.signInWithCredential(firebaseCred)
                    .addOnCompleteListener(this, task -> {
                        if (task.isSuccessful()) {
                            goToMain();
                        } else {
                            Log.w(TAG, "Firebase auth failed", task.getException());
                            Toast.makeText(this,
                                    "Authentication failed.", Toast.LENGTH_SHORT).show();
                        }
                    });
        } catch (Exception e) {
            Log.w(TAG, "One-Tap result handling failed", e);
            Toast.makeText(this,
                    "Google Sign-In failed.", Toast.LENGTH_SHORT).show();
        }
    }

    private void goToMain() {
        startActivity(new Intent(this, MainActivity.class));
        finish();
    }
}
