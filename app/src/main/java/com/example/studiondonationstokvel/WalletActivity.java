package com.example.studiondonationstokvel;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import com.google.android.material.navigation.NavigationView;
import android.os.Bundle;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.android.material.card.MaterialCardView;
import java.util.HashMap;
import java.util.Map;

public class WalletActivity extends AppCompatActivity {
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private FirebaseAnalytics mFirebaseAnalytics;
    private DrawerLayout drawerLayout;
    private ActionBarDrawerToggle toggle;
    private SharedPreferences sharedPreferences;
    private static final String PREF_NAME = "LoginPrefs";

    // UI components
    private TextView tvWalletBalance;
    private Button btnTopUp;
    private MaterialCardView cardBalance;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_wallet);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        mFirebaseAnalytics = FirebaseAnalytics.getInstance(this);
        sharedPreferences = getSharedPreferences(PREF_NAME, MODE_PRIVATE);

        if (mAuth.getCurrentUser() == null) {
            startActivity(new Intent(this, MainActivity.class));
            finish();
            return;
        }

        // Initialize UI components
        Toolbar toolbar = findViewById(R.id.toolbar);
        tvWalletBalance = findViewById(R.id.tv_wallet_balance);
        btnTopUp = findViewById(R.id.btn_top_up);
        cardBalance = findViewById(R.id.card_balance);
        drawerLayout = findViewById(R.id.drawer_layout);

        // Set up Toolbar
        setSupportActionBar(toolbar);
        toolbar.setNavigationIcon(R.drawable.ic_menu);
        getSupportActionBar().setTitle("Wallet");

        // Set up Navigation Drawer
        toggle = new ActionBarDrawerToggle(this, drawerLayout, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = findViewById(R.id.nav_view);
        TextView navHeaderEmail = navigationView.getHeaderView(0).findViewById(R.id.nav_header_email);
        navHeaderEmail.setText(mAuth.getCurrentUser().getEmail());

        navigationView.setNavigationItemSelectedListener(item -> {
            int id = item.getItemId();
            Bundle params = new Bundle();
            params.putString("button", "nav_" + item.getTitle());
            mFirebaseAnalytics.logEvent("nav_click", params);

            if (id == R.id.nav_home) {
                startActivity(new Intent(WalletActivity.this, HomeActivity.class));
                finish();
            } else if (id == R.id.nav_my_profile) {
                checkProfileAccess();
            } else if (id == R.id.nav_registration) {
                startActivity(new Intent(WalletActivity.this, RegistrationActivity.class));
            } else if (id == R.id.nav_wallet) {
                drawerLayout.closeDrawer(GravityCompat.START);
            } else if (id == R.id.nav_communities) {
                startActivity(new Intent(WalletActivity.this, CommunitiesActivity.class));
            } else if (id == R.id.nav_logout) {
                logout();
            }
            drawerLayout.closeDrawer(GravityCompat.START);
            return true;
        });

        // Load wallet balance
        loadWalletBalance();

        // Top Up button click
        btnTopUp.setOnClickListener(v -> topUpWallet());

        // Log screen view
        Bundle bundle = new Bundle();
        bundle.putString("screen", "wallet");
        mFirebaseAnalytics.logEvent("screen_view", bundle);
    }

    private void loadWalletBalance() {
        db.collection("users").document(mAuth.getCurrentUser().getUid()).get().addOnSuccessListener(documentSnapshot -> {
            if (documentSnapshot.exists()) {
                double balance = documentSnapshot.getDouble("walletBalance") != null ? documentSnapshot.getDouble("walletBalance") : 0.0;
                tvWalletBalance.setText("R" + String.format("%.2f", balance));
            } else {
                Toast.makeText(this, "Wallet not found", Toast.LENGTH_SHORT).show();
                finish();
            }
        }).addOnFailureListener(e -> {
            Toast.makeText(this, "Error loading wallet: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        });
    }

    private void topUpWallet() {
        // Minimum top-up R10
        double amount = 10.0; // Default minimum; in real app, show input dialog

        // Trigger PayFast top-up
        String payFastUrl = "https://sandbox.payfast.co.za/eng/process?merchant_id=10000100&merchant_key=46f0cd694581a&return_url=" + Uri.encode("your-return-url") + "&cancel_url=" + Uri.encode("your-cancel-url") + "&notify_url=" + Uri.encode("your-notify-url") + "&name=" + Uri.encode("Top Up Wallet") + "&amount=" + amount + "&custom_str1=" + Uri.encode("top_up");

        Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(payFastUrl));
        startActivity(browserIntent);

        Toast.makeText(this, "Top up initiated. Minimum R10.", Toast.LENGTH_SHORT).show();

        Bundle params = new Bundle();
        params.putString("event", "wallet_top_up_started");
        params.putDouble("amount", amount);
        mFirebaseAnalytics.logEvent("top_up_event", params);
    }

    private void checkProfileAccess() {
        db.collection("users").document(mAuth.getCurrentUser().getUid())
                .get()
                .addOnSuccessListener(document -> {
                    if (document.exists() && document.getBoolean("profileCreated")) {
                        startActivity(new Intent(WalletActivity.this, MyProfileActivity.class));
                    } else {
                        Toast.makeText(this, "Only students with profiles can access this page", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error checking profile: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void logout() {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.clear();
        editor.apply();

        mAuth.signOut();
        Toast.makeText(this, "Logged out successfully", Toast.LENGTH_SHORT).show();
        startActivity(new Intent(WalletActivity.this, MainActivity.class));
        finish();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (toggle.onOptionsItemSelected(item)) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}