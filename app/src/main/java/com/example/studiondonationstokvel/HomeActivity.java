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
import androidx.core.widget.NestedScrollView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class HomeActivity extends AppCompatActivity {
    private FirebaseAuth mAuth;
    private FirebaseAnalytics mFirebaseAnalytics;
    private FirebaseFirestore db;
    private DrawerLayout drawerLayout;
    private ActionBarDrawerToggle toggle;
    private EditText searchBar;
    private RecyclerView rvStudents;
    private RecyclerView rvLeaderboard;
    private TextView tvNoStudents;
    private StudentAdapter studentAdapter;
    private LeaderboardAdapter leaderboardAdapter;
    private ChipGroup chipGroupFilters;
    private Chip chipTrending;
    private Chip chipNew;
    private Chip chipTopRated;
    private List<Student> studentList;
    private List<Student> allStudents;
    private List<Student> leaderboardList;
    private View leaderboardCard;
    private SharedPreferences sharedPreferences;
    private String currentSearchQuery = "";
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
        rvLeaderboard = findViewById(R.id.rv_leaderboard);
        tvNoStudents = findViewById(R.id.tv_no_students);
        FloatingActionButton fabRegister = findViewById(R.id.fab_register);
        MaterialButton btnViewAllLeaderboard = findViewById(R.id.btn_view_all_leaderboard);
        NestedScrollView homeScroll = findViewById(R.id.home_scroll);
        leaderboardCard = findViewById(R.id.card_leaderboard);
        chipGroupFilters = findViewById(R.id.chip_group_filters);
        chipTrending = findViewById(R.id.chip_trending);
        chipNew = findViewById(R.id.chip_new);
        chipTopRated = findViewById(R.id.chip_top_rated);

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
        allStudents = new ArrayList<>();
        studentList = new ArrayList<>();
        leaderboardList = new ArrayList<>();

        studentAdapter = new StudentAdapter(studentList, student -> {
            Intent intent = new Intent(HomeActivity.this, StudentStoryActivity.class);
            intent.putExtra("student_id", student.getUid());
            startActivity(intent);
        });
        rvStudents.setLayoutManager(new LinearLayoutManager(this));
        rvStudents.setAdapter(studentAdapter);

        leaderboardAdapter = new LeaderboardAdapter(leaderboardList, student -> {
            Intent intent = new Intent(HomeActivity.this, StudentStoryActivity.class);
            intent.putExtra("student_id", student.getUid());
            startActivity(intent);
        });
        LinearLayoutManager leaderboardLayoutManager = new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false);
        rvLeaderboard.setLayoutManager(leaderboardLayoutManager);
        rvLeaderboard.setAdapter(leaderboardAdapter);

        // Load students with real-time listener
        loadStudents();

        // Search bar listener
        searchBar.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override
            public void afterTextChanged(Editable s) {
                currentSearchQuery = s.toString().trim();
                applyFilters();
            }
        });

        chipGroupFilters.setOnCheckedChangeListener((group, checkedId) -> applyFilters());

        // Floating Action Button for Registration
        fabRegister.setOnClickListener(v -> {
            startActivity(new Intent(HomeActivity.this, RegistrationActivity.class));
            Bundle params = new Bundle();
            params.putString("button", "fab_register");
            mFirebaseAnalytics.logEvent("button_click", params);
        });

        btnViewAllLeaderboard.setOnClickListener(v -> {
            Bundle params = new Bundle();
            params.putString("cta", "leaderboard_view_all");
            mFirebaseAnalytics.logEvent("leaderboard_cta", params);
            homeScroll.post(() -> homeScroll.smoothScrollTo(0, rvStudents.getTop()));
        });

        // Log screen view
        Bundle bundle = new Bundle();
        bundle.putString("screen", "home");
        mFirebaseAnalytics.logEvent("screen_view", bundle);
    }

    private void loadStudents() {
        db.collection("students")
                .orderBy("name")
                .limit(50)
                .addSnapshotListener((snapshots, e) -> {
            if (e != null) {
                Toast.makeText(this, "Error loading students: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                return;
            }
            allStudents.clear();
            if (snapshots != null) {
                for (DocumentSnapshot doc : snapshots) {
                    Student student = doc.toObject(Student.class);
                    if (student != null) {
                        student.setUid(doc.getId());
                        allStudents.add(student);
                    }
                }
            }
            applyFilters();
        });
    }

    private void applyFilters() {
        List<Student> filtered = new ArrayList<>();
        for (Student student : allStudents) {
            String fullName = (student.getName() + " " + student.getSurname()).toLowerCase();
            if (!currentSearchQuery.isEmpty() && !fullName.contains(currentSearchQuery.toLowerCase())) {
                continue;
            }
            filtered.add(student);
        }

        Collections.sort(filtered, (a, b) -> a.getName().compareToIgnoreCase(b.getName()));

        int checkedId = chipGroupFilters.getCheckedChipId();
        if (checkedId == R.id.chip_trending) {
            Collections.sort(filtered, (a, b) -> Integer.compare(b.getLikes(), a.getLikes()));
        } else if (checkedId == R.id.chip_new) {
            Collections.sort(filtered, (a, b) -> b.getUid().compareToIgnoreCase(a.getUid()));
        } else if (checkedId == R.id.chip_top_rated) {
            Collections.sort(filtered, (a, b) -> Double.compare(b.getProgress(), a.getProgress()));
        }

        studentList.clear();
        studentList.addAll(filtered);
        studentAdapter.notifyDataSetChanged();

        boolean hasStudents = !studentList.isEmpty();
        tvNoStudents.setVisibility(hasStudents ? View.GONE : View.VISIBLE);
        rvStudents.setVisibility(hasStudents ? View.VISIBLE : View.GONE);

        updateLeaderboard();
    }

    private void updateLeaderboard() {
        List<Student> sorted = new ArrayList<>(studentList);
        Collections.sort(sorted, (a, b) -> Double.compare(b.getProgress(), a.getProgress()));

        leaderboardList.clear();
        int limit = Math.min(5, sorted.size());
        for (int i = 0; i < limit; i++) {
            leaderboardList.add(sorted.get(i));
        }
        leaderboardAdapter.notifyDataSetChanged();
        int visibility = leaderboardList.isEmpty() ? View.GONE : View.VISIBLE;
        rvLeaderboard.setVisibility(visibility);
        if (leaderboardCard != null) {
            leaderboardCard.setVisibility(visibility);
        }
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