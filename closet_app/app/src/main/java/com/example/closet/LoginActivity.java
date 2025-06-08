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

import com.google.android.gms.auth.api.identity.BeginSignInRequest;
import com.google.android.gms.auth.api.identity.Identity;
import com.google.android.gms.auth.api.identity.SignInClient;
import com.google.android.gms.auth.api.identity.BeginSignInRequest.GoogleIdTokenRequestOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.CommonStatusCodes;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;

import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.GoogleAuthProvider;

import androidx.activity.result.IntentSenderRequest;

public class LoginActivity extends AppCompatActivity {
    private static final String TAG = "LoginActivity";

    private FirebaseAuth mAuth;
    private SignInClient oneTapClient;

    // UI
    private EditText etEmail, etPassword;
    private Button btnLogin, btnRegister, btnGoogle;

    // Launcher for One-Tap IntentSender
    private final ActivityResultLauncher<IntentSenderRequest> oneTapLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.StartIntentSenderForResult(),
                    this::onOneTapResult
            );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.login);

        // 1) Firebase Auth
        mAuth = FirebaseAuth.getInstance();

        // 2) If already signed in, go straight to main
        if (mAuth.getCurrentUser() != null) {
            goToMain();
            return;
        }

        // 3) Wire up Email/Password UI
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

        // 4) Configure One-Tap
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

        // 5) Launch One-Tap when user taps the Google button
        btnGoogle.setOnClickListener(v ->
                oneTapClient.beginSignIn(signInRequest)
                        .addOnSuccessListener(beginResult -> {
                            IntentSenderRequest req = new IntentSenderRequest.Builder(
                                    beginResult.getPendingIntent().getIntentSender()
                            ).build();
                            oneTapLauncher.launch(req);
                        })
                        .addOnFailureListener(e -> {
                            Log.w(TAG, "One-Tap failed, falling back", e);
                            // You could fall back here to another sign-in method if desired
                        })
        );
    }

    private void onOneTapResult(ActivityResult result) {
        if (result.getResultCode() != RESULT_OK || result.getData() == null) {
            Toast.makeText(this, "Sign-in canceled.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Extract the GoogleSignInAccount and exchange for a Firebase credential
        Task<GoogleSignInAccount> task =
                GoogleSignIn.getSignedInAccountFromIntent(result.getData());
        try {
            GoogleSignInAccount acct = task.getResult(ApiException.class);
            if (acct == null || acct.getIdToken() == null) {
                throw new ApiException(new com.google.android.gms.common.api.Status(
                        CommonStatusCodes.INTERNAL_ERROR));
            }

            AuthCredential credential =
                    GoogleAuthProvider.getCredential(acct.getIdToken(), null);

            mAuth.signInWithCredential(credential)
                    .addOnCompleteListener(this, authTask -> {
                        if (authTask.isSuccessful()) {
                            goToMain();
                        } else {
                            Log.w(TAG, "Firebase auth failed", authTask.getException());
                            Toast.makeText(this,
                                    "Authentication failed.", Toast.LENGTH_SHORT).show();
                        }
                    });
        } catch (ApiException e) {
            Log.w(TAG, "Google sign-in failed", e);
            Toast.makeText(this,
                    "Google Sign-In failed.", Toast.LENGTH_SHORT).show();
        }
    }

    private void goToMain() {
        startActivity(new Intent(this, MainActivity.class));
        finish();
    }
}
