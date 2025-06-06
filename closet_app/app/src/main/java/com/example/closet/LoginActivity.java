// app/src/main/java/com/example/closet/LoginActivity.java

package com.example.closet;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;

import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;

import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.HashMap;
import java.util.Map;

public class LoginActivity extends AppCompatActivity {
    private static final String TAG = "LoginActivity";

    private FirebaseAuth mAuth;
    private GoogleSignInClient mGoogleSignInClient;

    private final ActivityResultLauncher<Intent> googleSignInLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.StartActivityForResult(),
                    new ActivityResultCallback<ActivityResult>() {
                        @Override
                        public void onActivityResult(ActivityResult result) {
                            if (result.getResultCode() == RESULT_OK) {
                                Intent data = result.getData();
                                if (data != null) {
                                    handleGoogleSignInResult(data);
                                }
                            } else {
                                Log.w(TAG, "Google Sign-In was canceled or failed with code: "
                                        + result.getResultCode());
                                Toast.makeText(LoginActivity.this,
                                        "Google Sign-In canceled or failed.",
                                        Toast.LENGTH_SHORT).show();
                            }
                        }
                    }
            );

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        mAuth = FirebaseAuth.getInstance();
        configureGoogleSignIn();

        findViewById(R.id.btn_google_signin).setOnClickListener(v -> {
            Intent signInIntent = mGoogleSignInClient.getSignInIntent();
            googleSignInLauncher.launch(signInIntent);
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            goToMain();
        }
    }

    @SuppressWarnings("deprecation")
    private void configureGoogleSignIn() {
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(
                GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();

        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);
    }

    private void handleGoogleSignInResult(Intent data) {
        Task<GoogleSignInAccount> task =
                GoogleSignIn.getSignedInAccountFromIntent(data);
        try {
            GoogleSignInAccount account = task.getResult(ApiException.class);
            if (account != null && account.getIdToken() != null) {
                firebaseAuthWithGoogle(account.getIdToken());
            } else {
                Log.w(TAG, "GoogleSignInAccount or ID token is null");
                Toast.makeText(this,
                        "Google Sign-In failed to provide an ID token.",
                        Toast.LENGTH_LONG).show();
            }
        } catch (ApiException e) {
            Log.w(TAG, "Google sign-in failed", e);
            Toast.makeText(this,
                    "Google Sign-In failed: " + e.getMessage(),
                    Toast.LENGTH_LONG).show();
        }
    }

    private void firebaseAuthWithGoogle(String idToken) {
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            writeUserToFirestoreIfFirstLogin(user);
                        }
                        goToMain();
                    } else {
                        Log.w(TAG, "signInWithCredential:failure", task.getException());
                        Toast.makeText(LoginActivity.this,
                                "Firebase Authentication failed.",
                                Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void writeUserToFirestoreIfFirstLogin(FirebaseUser user) {
        String uid = user.getUid();
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        DocumentReference userDocRef = db.collection("Users").document(uid);

        userDocRef.get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Log.d(TAG, "User document already exists for UID: " + uid);
                    } else {
                        Map<String, Object> profileData = new HashMap<>();
                        profileData.put("displayName", user.getDisplayName());
                        profileData.put("email", user.getEmail());
                        profileData.put("joinedAt", FieldValue.serverTimestamp());

                        userDocRef.set(profileData, SetOptions.merge())
                                .addOnSuccessListener(aVoid -> {
                                    Log.d(TAG, "Created new user document for UID: " + uid);
                                })
                                .addOnFailureListener(e -> {
                                    Log.w(TAG, "Error creating user document for UID: " + uid, e);
                                });
                    }
                })
                .addOnFailureListener(e -> {
                    Log.w(TAG, "Error checking user document for UID: " + uid, e);
                });
    }

    private void goToMain() {
        startActivity(new Intent(LoginActivity.this, MainActivity.class));
        finish();
    }
}
