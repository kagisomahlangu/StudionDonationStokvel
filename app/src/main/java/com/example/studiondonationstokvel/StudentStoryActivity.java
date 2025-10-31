package com.example.studiondonationstokvel;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import com.bumptech.glide.Glide;
import com.google.android.material.navigation.NavigationView;
import android.os.Bundle;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import java.util.HashMap;
import java.util.Map;

public class StudentStoryActivity extends AppCompatActivity {
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private FirebaseAnalytics mFirebaseAnalytics;
    private FirebaseStorage storage;
    private DrawerLayout drawerLayout;
    private ActionBarDrawerToggle toggle;
    private String studentId;
    private Student currentStudent;
    private SharedPreferences sharedPreferences;
    private static final String PREF_NAME = "LoginPrefs";

    // UI components
    private ImageView ivStudentImage;
    private TextView tvStudentName, tvInstitution, tvStudentNumber, tvStory, tvAmountRequired;
    private Button btnLike, btnDonate;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_student_story);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        storage = FirebaseStorage.getInstance();
        mFirebaseAnalytics = FirebaseAnalytics.getInstance(this);
        sharedPreferences = getSharedPreferences(PREF_NAME, MODE_PRIVATE);

        // Get student ID from intent
        studentId = getIntent().getStringExtra("student_id");
        if (studentId == null) {
            Toast.makeText(this, "Student ID not found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Initialize UI components
        Toolbar toolbar = findViewById(R.id.toolbar);
        ivStudentImage = findViewById(R.id.iv_student_image);
        tvStudentName = findViewById(R.id.tv_student_name);
        tvInstitution = findViewById(R.id.tv_institution);
        tvStudentNumber = findViewById(R.id.tv_student_number);
        tvStory = findViewById(R.id.tv_story);
        tvAmountRequired = findViewById(R.id.tv_amount_required);
        btnLike = findViewById(R.id.btn_like);
        btnDonate = findViewById(R.id.btn_donate);
        drawerLayout = findViewById(R.id.drawer_layout);

        // Set up Toolbar
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle("Student Story");

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
                startActivity(new Intent(StudentStoryActivity.this, HomeActivity.class));
                finish();
            } else if (id == R.id.nav_my_profile) {
                checkProfileAccess();
            } else if (id == R.id.nav_registration) {
                startActivity(new Intent(StudentStoryActivity.this, RegistrationActivity.class));
            } else if (id == R.id.nav_wallet) {
                startActivity(new Intent(StudentStoryActivity.this, WalletActivity.class));
            } else if (id == R.id.nav_communities) {
                startActivity(new Intent(StudentStoryActivity.this, CommunitiesActivity.class));
            } else if (id == R.id.nav_logout) {
                logout();
            }
            drawerLayout.closeDrawer(GravityCompat.START);
            return true;
        });

        // Load student details
        loadStudentDetails();

        // Like button click
        btnLike.setOnClickListener(v -> likeStudent());

        // Donate button click
        btnDonate.setOnClickListener(v -> donateToStudent());

        // Log screen view
        Bundle bundle = new Bundle();
        bundle.putString("screen", "student_story");
        mFirebaseAnalytics.logEvent("screen_view", bundle);
    }

    private void loadStudentDetails() {
        db.collection("students").document(studentId).get().addOnSuccessListener(documentSnapshot -> {
            if (documentSnapshot.exists()) {
                currentStudent = documentSnapshot.toObject(Student.class);
                currentStudent.setUid(studentId);

                // Populate UI
                tvStudentName.setText(currentStudent.getName() + " " + currentStudent.getSurname());
                tvInstitution.setText(currentStudent.getInstitution());
                tvStudentNumber.setText(currentStudent.getStudentNumber());
                tvStory.setText(currentStudent.getStory());
                tvAmountRequired.setText("R" + currentStudent.getAmountRequired());

                // Load image
                if (currentStudent.getImageUrl() != null && !currentStudent.getImageUrl().isEmpty()) {
                    Glide.with(this).load(currentStudent.getImageUrl()).placeholder(R.drawable.ic_student).into(ivStudentImage);
                } else {
                    ivStudentImage.setImageResource(R.drawable.ic_student);
                }
            } else {
                Toast.makeText(this, "Student not found", Toast.LENGTH_SHORT).show();
                finish();
            }
        }).addOnFailureListener(e -> {
            Toast.makeText(this, "Error loading student details: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        });
    }

    private void likeStudent() {
        if (mAuth.getCurrentUser() == null) {
            Toast.makeText(this, "Please log in to like", Toast.LENGTH_SHORT).show();
            return;
        }

        db.collection("users").document(mAuth.getCurrentUser().getUid()).get().addOnSuccessListener(userDoc -> {
            double walletBalance = userDoc.getDouble("walletBalance") != null ? userDoc.getDouble("walletBalance") : 0.0;
            if (walletBalance < 10) {
                Toast.makeText(this, "Insufficient funds. Top up your wallet to like.", Toast.LENGTH_SHORT).show();
                return;
            }

            // Deduct R10 from user's wallet
            db.collection("users").document(mAuth.getCurrentUser().getUid())
                    .update("walletBalance", walletBalance - 10);

            // Update student's amountRaised and likes
            db.collection("students").document(studentId)
                    .update("amountRaised", currentStudent.getAmountRaised() + 10, "likes", currentStudent.getLikes() + 1);

            // Update progress
            double newProgress = (currentStudent.getAmountRaised() + 10) / currentStudent.getAmountRequired() * 100;
            db.collection("students").document(studentId).update("progress", Math.min(newProgress, 100));

            Toast.makeText(this, "Liked! -R10 deducted from wallet.", Toast.LENGTH_SHORT).show();

            Bundle params = new Bundle();
            params.putString("event", "student_liked");
            params.putString("student_id", studentId);
            mFirebaseAnalytics.logEvent("like_event", params);
        }).addOnFailureListener(e -> {
            Toast.makeText(this, "Error processing like: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        });
    }

    private void donateToStudent() {
        if (mAuth.getCurrentUser() == null) {
            Toast.makeText(this, "Please log in to donate", Toast.LENGTH_SHORT).show();
            return;
        }

        // Placeholder for institution dropdown - in real implementation, show a dialog or bottom sheet
        String selectedInstitution = currentStudent.getInstitution(); // Default to student's institution
        String message = "Be The Reason I Graduate";
        String amount = "100"; // Default amount; in real app, show input dialog

        // Trigger PayFast payment with custom data (institution, studentId, accountNumber placeholder)
        String payFastUrl = "https://sandbox.payfast.co.za/eng/process?merchant_id=10000100&merchant_key=46f0cd694581a&return_url=" + Uri.encode("your-return-url") + "&cancel_url=" + Uri.encode("your-cancel-url") + "&notify_url=" + Uri.encode("your-notify-url") + "&name=" + Uri.encode("Donate to " + currentStudent.getName()) + "&amount=" + amount + "&custom_str1=" + Uri.encode(selectedInstitution) + "&custom_str2=" + Uri.encode(studentId);

        // Open PayFast in browser
        Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(payFastUrl));
        startActivity(browserIntent);

        Toast.makeText(this, message, Toast.LENGTH_LONG).show();

        Bundle params = new Bundle();
        params.putString("event", "donation_started");
        params.putString("student_id", studentId);
        params.putString("institution", selectedInstitution);
        mFirebaseAnalytics.logEvent("donation_event", params);
    }

    private void checkProfileAccess() {
        db.collection("users").document(mAuth.getCurrentUser().getUid())
                .get()
                .addOnSuccessListener(document -> {
                    if (document.exists() && document.getBoolean("profileCreated")) {
                        startActivity(new Intent(StudentStoryActivity.this, MyProfileActivity.class));
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
        startActivity(new Intent(StudentStoryActivity.this, MainActivity.class));
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