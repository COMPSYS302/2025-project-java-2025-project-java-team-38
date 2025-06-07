package com.example.closet;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.auth.api.signin.*;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.CommonStatusCodes;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.*;
import com.google.android.gms.common.api.CommonStatusCodes;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.tasks.Task;

public class LoginActivity extends AppCompatActivity {
    private static final String TAG = "LoginActivity";

    private FirebaseAuth       mAuth;
    private GoogleSignInClient mGoogleSignInClient;

    // Launcher for the Google Sign-In intent
    private final ActivityResultLauncher<Intent> signInLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.StartActivityForResult(),
                    this::onSignInResult
            );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Use your provided layout
        setContentView(R.layout.login);

        // 1) Firebase Auth instance
        mAuth = FirebaseAuth.getInstance();

        // 2) If already signed in, skip straight to MainActivity
        if (mAuth.getCurrentUser() != null) {
            goToMain();
            return;
        }

        // 3) Configure Google Sign-In
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(
                GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

        // 4) Wire up the button
        findViewById(R.id.btn_google_signin).setOnClickListener(v -> {
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
