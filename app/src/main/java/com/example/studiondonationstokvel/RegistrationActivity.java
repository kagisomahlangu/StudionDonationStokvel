package com.example.studiondonationstokvel;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import com.google.android.material.navigation.NavigationView;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import java.util.HashMap;
import java.util.Map;
import android.Manifest;

public class RegistrationActivity extends AppCompatActivity {
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private FirebaseAnalytics mFirebaseAnalytics;
    private FirebaseStorage storage;
    private EditText etName, etSurname, etStudentNumber, etStory, etAmountRequired;
    private AutoCompleteTextView autoCompleteInstitution;
    private Button btnRegister, btnSelectImage;
    private ImageView ivProfilePreview;
    private ProgressBar progressBar;
    private TextInputLayout tilName, tilSurname, tilInstitution, tilStudentNumber, tilStory, tilAmountRequired;
    private Uri imageUri;
    private ActivityResultLauncher<String> imagePickerLauncher;
    private ActivityResultLauncher<String[]> permissionLauncher;
    private DrawerLayout drawerLayout;
    private ActionBarDrawerToggle toggle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_registration);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        storage = FirebaseStorage.getInstance();
        mFirebaseAnalytics = FirebaseAnalytics.getInstance(this);

        // Initialize UI components
        Toolbar toolbar = findViewById(R.id.toolbar);
        etName = findViewById(R.id.et_name);
        etSurname = findViewById(R.id.et_surname);
        autoCompleteInstitution = findViewById(R.id.auto_complete_institution);
        etStudentNumber = findViewById(R.id.et_student_number);
        etStory = findViewById(R.id.et_story);
        etAmountRequired = findViewById(R.id.et_amount_required);
        btnRegister = findViewById(R.id.btn_register);
        btnSelectImage = findViewById(R.id.btn_select_image);
        ivProfilePreview = findViewById(R.id.iv_profile_preview);
        progressBar = findViewById(R.id.progress_bar);
        tilName = findViewById(R.id.til_name);
        tilSurname = findViewById(R.id.til_surname);
        tilInstitution = findViewById(R.id.til_institution);
        tilStudentNumber = findViewById(R.id.til_student_number);
        tilStory = findViewById(R.id.til_story);
        tilAmountRequired = findViewById(R.id.til_amount_required);
        drawerLayout = findViewById(R.id.drawer_layout);

        // Set up Toolbar
        setSupportActionBar(toolbar);
        toolbar.setNavigationIcon(R.drawable.ic_menu);
        getSupportActionBar().setTitle("Register as Student");

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
                startActivity(new Intent(RegistrationActivity.this, HomeActivity.class));
                finish();
            } else if (id == R.id.nav_my_profile) {
                checkProfileAccess();
            } else if (id == R.id.nav_registration) {
                drawerLayout.closeDrawer(GravityCompat.START);
            } else if (id == R.id.nav_wallet) {
                startActivity(new Intent(RegistrationActivity.this, WalletActivity.class));
            } else if (id == R.id.nav_communities) {
                startActivity(new Intent(RegistrationActivity.this, CommunitiesActivity.class));
            } else if (id == R.id.nav_logout) {
                logout();
            }
            drawerLayout.closeDrawer(GravityCompat.START);
            return true;
        });

        // Populate institution dropdown
        String[] institutions = {"Wits", "UJ", "UCT", "Richfield", "Limpopo", "North West"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, institutions);
        autoCompleteInstitution.setAdapter(adapter);

        // Initialize permission launcher
        permissionLauncher = registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), permissions -> {
            boolean granted = permissions.entrySet().stream().allMatch(Map.Entry::getValue);
            if (granted) {
                imagePickerLauncher.launch("image/*");
            } else {
                Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show();
            }
        });

        // Initialize image picker
        imagePickerLauncher = registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
            if (uri != null) {
                imageUri = uri;
                ivProfilePreview.setImageURI(uri);
                Bundle params = new Bundle();
                params.putString("event", "image_selected");
                mFirebaseAnalytics.logEvent("image_selection", params);
            }
        });

        // Select image button click
        btnSelectImage.setOnClickListener(v -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                permissionLauncher.launch(new String[]{Manifest.permission.READ_MEDIA_IMAGES});
            } else {
                permissionLauncher.launch(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE});
            }
        });

        // Register button click
        btnRegister.setOnClickListener(v -> {
            if (validateInput()) {
                registerStudent();
            }
        });

        // Log screen view
        Bundle bundle = new Bundle();
        bundle.putString("screen", "registration");
        mFirebaseAnalytics.logEvent("screen_view", bundle);
    }

    private boolean validateInput() {
        String name = etName.getText().toString().trim();
        String surname = etSurname.getText().toString().trim();
        String institution = autoCompleteInstitution.getText().toString().trim();
        String studentNumber = etStudentNumber.getText().toString().trim();
        String story = etStory.getText().toString().trim();
        String amountRequired = etAmountRequired.getText().toString().trim();

        boolean isValid = true;

        if (name.isEmpty()) {
            tilName.setError("Name is required");
            isValid = false;
        } else {
            tilName.setError(null);
        }

        if (surname.isEmpty()) {
            tilSurname.setError("Surname is required");
            isValid = false;
        } else {
            tilSurname.setError(null);
        }

        if (institution.isEmpty()) {
            tilInstitution.setError("Institution is required");
            isValid = false;
        } else {
            tilInstitution.setError(null);
        }

        if (studentNumber.isEmpty()) {
            tilStudentNumber.setError("Student Number is required");
            isValid = false;
        } else {
            tilStudentNumber.setError(null);
        }

        if (story.isEmpty()) {
            tilStory.setError("Story is required");
            isValid = false;
        } else {
            tilStory.setError(null);
        }

        if (amountRequired.isEmpty()) {
            tilAmountRequired.setError("Amount Required is required");
            isValid = false;
        } else {
            try {
                double amount = Double.parseDouble(amountRequired);
                if (amount <= 0) {
                    tilAmountRequired.setError("Amount must be greater than 0");
                    isValid = false;
                } else {
                    tilAmountRequired.setError(null);
                }
            } catch (NumberFormatException e) {
                tilAmountRequired.setError("Invalid amount");
                isValid = false;
            }
        }

        return isValid;
    }

    private void registerStudent() {
        progressBar.setVisibility(View.VISIBLE);
        btnRegister.setEnabled(false);
        btnSelectImage.setEnabled(false);

        String uid = mAuth.getCurrentUser().getUid();
        String name = etName.getText().toString().trim();
        String surname = etSurname.getText().toString().trim();
        String institution = autoCompleteInstitution.getText().toString().trim();
        String studentNumber = etStudentNumber.getText().toString().trim();
        String story = etStory.getText().toString().trim();
        double amountRequired = Double.parseDouble(etAmountRequired.getText().toString().trim());

        Map<String, Object> student = new HashMap<>();
        student.put("name", name);
        student.put("surname", surname);
        student.put("institution", institution);
        student.put("studentNumber", studentNumber);
        student.put("story", story);
        student.put("amountRequired", amountRequired);
        student.put("amountRaised", 0.0);
        student.put("likes", 0);
        student.put("progress", 0.0);

        if (imageUri != null) {
            // Upload image to Firebase Storage
            StorageReference imageRef = storage.getReference().child("student_images/" + uid + ".jpg");
            imageRef.putFile(imageUri)
                    .addOnSuccessListener(taskSnapshot -> {
                        imageRef.getDownloadUrl().addOnSuccessListener(uri -> {
                            student.put("imageUrl", uri.toString());
                            saveStudentToFirestore(student, uid);
                        }).addOnFailureListener(e -> {
                            progressBar.setVisibility(View.GONE);
                            btnRegister.setEnabled(true);
                            btnSelectImage.setEnabled(true);
                            Toast.makeText(RegistrationActivity.this, "Failed to get image URL: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        });
                    })
                    .addOnFailureListener(e -> {
                        progressBar.setVisibility(View.GONE);
                        btnRegister.setEnabled(true);
                        btnSelectImage.setEnabled(true);
                        Toast.makeText(RegistrationActivity.this, "Image upload failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
        } else {
            // No image selected, save without imageUrl
            saveStudentToFirestore(student, uid);
        }
    }

    private void saveStudentToFirestore(Map<String, Object> student, String uid) {
        db.collection("students").document(uid).set(student)
                .addOnSuccessListener(aVoid -> {
                    // Update users collection to mark profile as created
                    db.collection("users").document(uid)
                            .update("profileCreated", true)
                            .addOnSuccessListener(aVoid1 -> {
                                progressBar.setVisibility(View.GONE);
                                btnRegister.setEnabled(true);
                                btnSelectImage.setEnabled(true);
                                Toast.makeText(RegistrationActivity.this, "Profile created successfully", Toast.LENGTH_SHORT).show();
                                Bundle params = new Bundle();
                                params.putString("event", "student_registered");
                                mFirebaseAnalytics.logEvent("registration_success", params);
                                startActivity(new Intent(RegistrationActivity.this, HomeActivity.class));
                                finish();
                            })
                            .addOnFailureListener(e -> {
                                progressBar.setVisibility(View.GONE);
                                btnRegister.setEnabled(true);
                                btnSelectImage.setEnabled(true);
                                Toast.makeText(RegistrationActivity.this, "Error updating profile: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                            });
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    btnRegister.setEnabled(true);
                    btnSelectImage.setEnabled(true);
                    Toast.makeText(RegistrationActivity.this, "Error creating profile: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void checkProfileAccess() {
        db.collection("users").document(mAuth.getCurrentUser().getUid())
                .get()
                .addOnSuccessListener(document -> {
                    if (document.exists() && document.getBoolean("profileCreated")) {
                        startActivity(new Intent(RegistrationActivity.this, MyProfileActivity.class));
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
        startActivity(new Intent(RegistrationActivity.this, MainActivity.class));
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