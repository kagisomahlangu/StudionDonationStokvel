package com.example.studiondonationstokvel;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.MenuItem;
import android.view.View; // Added import
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
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import java.util.ArrayList;
import java.util.List;

public class HomeActivity extends AppCompatActivity {
    private FirebaseAuth mAuth;
    private FirebaseAnalytics mFirebaseAnalytics;
    private FirebaseFirestore db;
    private DrawerLayout drawerLayout;
    private ActionBarDrawerToggle toggle;
    private EditText searchBar;
    private RecyclerView rvStudents;
    private TextView tvNoStudents;
    private StudentAdapter studentAdapter;
    private List<Student> studentList;
    private SharedPreferences sharedPreferences;
    private static final String PREF_NAME = "LoginPrefs";
    private static final String KEY_REMEMBER_ME = "rememberMe";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        mAuth = FirebaseAuth.getInstance();
        mFirebaseAnalytics = FirebaseAnalytics.getInstance(this);
        db = FirebaseFirestore.getInstance();
        sharedPreferences = getSharedPreferences(PREF_NAME, MODE_PRIVATE);

        // Check if user is authenticated
        if (mAuth.getCurrentUser() == null) {
            startActivity(new Intent(this, MainActivity.class));
            finish();
            return;
        }

        // Initialize UI components
        drawerLayout = findViewById(R.id.drawer_layout);
        Toolbar toolbar = findViewById(R.id.toolbar);
        NavigationView navigationView = findViewById(R.id.nav_view);
        searchBar = findViewById(R.id.search_bar);
        rvStudents = findViewById(R.id.rv_students);
        tvNoStudents = findViewById(R.id.tv_no_students);
        FloatingActionButton fabRegister = findViewById(R.id.fab_register);

        // Set up Toolbar
        setSupportActionBar(toolbar);

        // Set up Navigation Drawer
        toggle = new ActionBarDrawerToggle(this, drawerLayout, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();

        // Update Navigation Header
        TextView navHeaderEmail = navigationView.getHeaderView(0).findViewById(R.id.nav_header_email);
        navHeaderEmail.setText(mAuth.getCurrentUser().getEmail());

        // Navigation Menu handling
        navigationView.setNavigationItemSelectedListener(item -> {
            int id = item.getItemId();
            Bundle params = new Bundle();
            params.putString("button", "nav_" + item.getTitle());
            mFirebaseAnalytics.logEvent("nav_click", params);

            if (id == R.id.nav_home) {
                drawerLayout.closeDrawer(GravityCompat.START);
            } else if (id == R.id.nav_my_profile) {
                checkProfileAccess();
            } else if (id == R.id.nav_registration) {
                startActivity(new Intent(HomeActivity.this, RegistrationActivity.class));
            } else if (id == R.id.nav_wallet) {
                startActivity(new Intent(HomeActivity.this, WalletActivity.class));
            } else if (id == R.id.nav_communities) {
                startActivity(new Intent(HomeActivity.this, CommunitiesActivity.class));
            } else if (id == R.id.nav_logout) {
                logout();
            }
            drawerLayout.closeDrawer(GravityCompat.START);
            return true;
        });

        // Set up RecyclerView
        studentList = new ArrayList<>();
        studentAdapter = new StudentAdapter(studentList, student -> {
            Intent intent = new Intent(HomeActivity.this, StudentStoryActivity.class);
            intent.putExtra("student_id", student.getUid());
            startActivity(intent);
        });
        rvStudents.setLayoutManager(new LinearLayoutManager(this));
        rvStudents.setAdapter(studentAdapter);

        // Load students with real-time listener
        loadStudents("");

        // Search bar listener
        searchBar.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override
            public void afterTextChanged(Editable s) {
                loadStudents(s.toString().trim());
            }
        });

        // Floating Action Button for Registration
        fabRegister.setOnClickListener(v -> {
            startActivity(new Intent(HomeActivity.this, RegistrationActivity.class));
            Bundle params = new Bundle();
            params.putString("button", "fab_register");
            mFirebaseAnalytics.logEvent("button_click", params);
        });

        // Log screen view
        Bundle bundle = new Bundle();
        bundle.putString("screen", "home");
        mFirebaseAnalytics.logEvent("screen_view", bundle);
    }

    private void loadStudents(String query) {
        Query firestoreQuery = db.collection("students")
                .orderBy("name")
                .limit(20);

        if (!query.isEmpty()) {
            firestoreQuery = db.collection("students")
                    .whereGreaterThanOrEqualTo("name", query)
                    .whereLessThanOrEqualTo("name", query + "\uf8ff")
                    .orderBy("name")
                    .limit(20);
        }

        firestoreQuery.addSnapshotListener((snapshots, e) -> {
            if (e != null) {
                Toast.makeText(this, "Error loading students: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                return;
            }
            studentList.clear();
            if (snapshots.isEmpty()) {
                tvNoStudents.setVisibility(View.VISIBLE);
                rvStudents.setVisibility(View.GONE);
            } else {
                tvNoStudents.setVisibility(View.GONE);
                rvStudents.setVisibility(View.VISIBLE);
                for (DocumentSnapshot doc : snapshots) {
                    Student student = doc.toObject(Student.class);
                    student.setUid(doc.getId());
                    studentList.add(student);
                }
            }
            studentAdapter.notifyDataSetChanged();
        });
    }

    private void checkProfileAccess() {
        db.collection("users").document(mAuth.getCurrentUser().getUid())
                .get()
                .addOnSuccessListener(document -> {
                    if (document.exists() && document.getBoolean("profileCreated")) {
                        startActivity(new Intent(HomeActivity.this, MyProfileActivity.class));
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
        startActivity(new Intent(HomeActivity.this, MainActivity.class));
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