package com.example.studiondonationstokvel;

import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.navigation.NavigationView;
import android.content.SharedPreferences;
import android.os.Bundle;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.ArrayList;
import java.util.List;

public class CommunitiesActivity extends AppCompatActivity {
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private FirebaseAnalytics mFirebaseAnalytics;
    private DrawerLayout drawerLayout;
    private ActionBarDrawerToggle toggle;

    // UI components
    private EditText searchBar;
    private RecyclerView rvInstitutions;
    private TextView tvUserInstitution, tvNoInstitutions;
    private InstitutionAdapter institutionAdapter;
    private List<Institution> institutionList;
    private String userInstitution = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_communities);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        mFirebaseAnalytics = FirebaseAnalytics.getInstance(this);

        if (mAuth.getCurrentUser() == null) {
            startActivity(new Intent(this, MainActivity.class));
            finish();
            return;
        }

        // Initialize UI components
        Toolbar toolbar = findViewById(R.id.toolbar);
        searchBar = findViewById(R.id.search_bar);
        rvInstitutions = findViewById(R.id.rv_institutions);
        tvUserInstitution = findViewById(R.id.tv_user_institution);
        tvNoInstitutions = findViewById(R.id.tv_no_institutions);
        drawerLayout = findViewById(R.id.drawer_layout);

        // Set up Toolbar
        setSupportActionBar(toolbar);
        toolbar.setNavigationIcon(R.drawable.ic_menu);
        getSupportActionBar().setTitle("Communities");

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
                startActivity(new Intent(CommunitiesActivity.this, HomeActivity.class));
                finish();
            } else if (id == R.id.nav_my_profile) {
                checkProfileAccess();
            } else if (id == R.id.nav_registration) {
                startActivity(new Intent(CommunitiesActivity.this, RegistrationActivity.class));
            } else if (id == R.id.nav_wallet) {
                startActivity(new Intent(CommunitiesActivity.this, WalletActivity.class));
            } else if (id == R.id.nav_communities) {
                drawerLayout.closeDrawer(GravityCompat.START);
            } else if (id == R.id.nav_logout) {
                logout();
            }
            drawerLayout.closeDrawer(GravityCompat.START);
            return true;
        });

        // Set up RecyclerView
        institutionList = new ArrayList<>();
        institutionAdapter = new InstitutionAdapter(institutionList, institution -> showInstitutionDetails(institution));
        rvInstitutions.setLayoutManager(new LinearLayoutManager(this));
        rvInstitutions.setAdapter(institutionAdapter);

        // Load user's institution
        loadUserInstitution();

        // Load institutions and set up search
        loadInstitutions();

        searchBar.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                filterInstitutions(s.toString().trim());
            }
        });

        // Log screen view
        Bundle bundle = new Bundle();
        bundle.putString("screen", "communities");
        mFirebaseAnalytics.logEvent("screen_view", bundle);
    }

    private void loadUserInstitution() {
        db.collection("students").document(mAuth.getCurrentUser().getUid()).get().addOnSuccessListener(documentSnapshot -> {
            if (documentSnapshot.exists()) {
                userInstitution = documentSnapshot.getString("institution");
                tvUserInstitution.setText("Your Group: " + userInstitution);
                tvUserInstitution.setVisibility(View.VISIBLE);
            } else {
                tvUserInstitution.setText("No profile yet. Register to join a group.");
                tvUserInstitution.setVisibility(View.VISIBLE);
            }
        }).addOnFailureListener(e -> {
            tvUserInstitution.setText("Error loading your group");
            tvUserInstitution.setVisibility(View.VISIBLE);
        });
    }

    private void loadInstitutions() {
        // Hardcoded list per A1 with account numbers
        institutionList.add(new Institution("Wits", "https://wa.me/placeholder-wits", "#123456789"));
        institutionList.add(new Institution("UJ", "https://wa.me/placeholder-uj", "#987654321"));
        institutionList.add(new Institution("UCT", "https://wa.me/placeholder-uct", "#456789123"));
        institutionList.add(new Institution("Richfield", "https://wa.me/placeholder-richfield", "#321654987"));
        institutionList.add(new Institution("Limpopo", "https://wa.me/placeholder-limpopo", "#654321987"));
        institutionList.add(new Institution("North West", "https://wa.me/placeholder-northwest", "#789123456"));
        institutionAdapter.notifyDataSetChanged();

        if (institutionList.isEmpty()) {
            tvNoInstitutions.setVisibility(View.VISIBLE);
            rvInstitutions.setVisibility(View.GONE);
        } else {
            tvNoInstitutions.setVisibility(View.GONE);
            rvInstitutions.setVisibility(View.VISIBLE);
        }
    }

    private void filterInstitutions(String query) {
        List<Institution> filteredList = new ArrayList<>();
        for (Institution institution : institutionList) {
            if (institution.getName().toLowerCase().contains(query.toLowerCase())) {
                filteredList.add(institution);
            }
        }
        institutionAdapter.updateList(filteredList);

        if (filteredList.isEmpty()) {
            tvNoInstitutions.setVisibility(View.VISIBLE);
            rvInstitutions.setVisibility(View.GONE);
        } else {
            tvNoInstitutions.setVisibility(View.GONE);
            rvInstitutions.setVisibility(View.VISIBLE);
        }
    }

    private void showInstitutionDetails(Institution institution) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(institution.getName());
        builder.setMessage("WhatsApp Link: " + institution.getWhatsAppLink() + "\nAccount Number: " + institution.getAccountNumber());
        builder.setPositiveButton("Open WhatsApp", (dialog, which) -> {
            String url = "https://wa.me/" + Uri.encode(institution.getWhatsAppLink());
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            startActivity(intent);

            Bundle params = new Bundle();
            params.putString("event", "whatsapp_opened");
            params.putString("institution", institution.getName());
            mFirebaseAnalytics.logEvent("whatsapp_event", params);
        });
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private void checkProfileAccess() {
        db.collection("users").document(mAuth.getCurrentUser().getUid())
                .get()
                .addOnSuccessListener(document -> {
                    if (document.exists() && document.getBoolean("profileCreated")) {
                        startActivity(new Intent(CommunitiesActivity.this, MyProfileActivity.class));
                    } else {
                        Toast.makeText(this, "Only students with profiles can access this page", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error checking profile: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void logout() {
        SharedPreferences sharedPreferences = getSharedPreferences("LoginPrefs", MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.clear();
        editor.apply();

        mAuth.signOut();
        Toast.makeText(this, "Logged out successfully", Toast.LENGTH_SHORT).show();
        startActivity(new Intent(CommunitiesActivity.this, MainActivity.class));
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