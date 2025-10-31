package com.example.studiondonationstokvel;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle; // Fixed: Use android.os.Bundle
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
import com.bumptech.glide.Glide;
import com.google.android.material.navigation.NavigationView;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import java.util.HashMap;
import java.util.Map;
import java.util.Date;
import android.Manifest;

public class MyProfileActivity extends AppCompatActivity {
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private FirebaseAnalytics mFirebaseAnalytics;
    private FirebaseStorage storage;
    private DrawerLayout drawerLayout;
    private ActionBarDrawerToggle toggle;
    private ActivityResultLauncher<String> imagePickerLauncher;
    private ActivityResultLauncher<String[]> permissionLauncher;
    private Student currentStudent;
    private boolean isEditing = false;
    private Uri imageUri;

    // UI components
    private ImageView ivProfileImage;
    private TextView tvName, tvSurname, tvInstitution, tvStudentNumber, tvAmountRequired, tvProgress;
    private ProgressBar progressBarFunding;
    private EditText etName, etSurname, etStudentNumber, etStory, etAmountRequired, etVideoLink;
    private AutoCompleteTextView autoCompleteInstitution;
    private Button btnEdit, btnSave, btnCancel;
    private ProgressBar progressBar;
    private TextInputLayout tilName, tilSurname, tilInstitution, tilStudentNumber, tilStory, tilAmountRequired, tilVideoLink;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_profile);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        storage = FirebaseStorage.getInstance();
        mFirebaseAnalytics = FirebaseAnalytics.getInstance(this);

        if (mAuth.getCurrentUser() == null) {
            startActivity(new Intent(this, MainActivity.class));
            finish();
            return;
        }

        // Initialize UI components
        Toolbar toolbar = findViewById(R.id.toolbar);
        ivProfileImage = findViewById(R.id.iv_profile_image);
        tvName = findViewById(R.id.tv_name);
        tvSurname = findViewById(R.id.tv_surname);
        tvInstitution = findViewById(R.id.tv_institution);
        tvStudentNumber = findViewById(R.id.tv_student_number);
        tvAmountRequired = findViewById(R.id.tv_amount_required);
        tvProgress = findViewById(R.id.tv_progress);
        progressBarFunding = findViewById(R.id.progress_bar_funding); // Fixed: Added findViewById
        etName = findViewById(R.id.et_name);
        etSurname = findViewById(R.id.et_surname);
        autoCompleteInstitution = findViewById(R.id.auto_complete_institution);
        etStudentNumber = findViewById(R.id.et_student_number);
        etStory = findViewById(R.id.et_story);
        etAmountRequired = findViewById(R.id.et_amount_required);
        etVideoLink = findViewById(R.id.et_video_link);
        btnEdit = findViewById(R.id.btn_edit);
        btnSave = findViewById(R.id.btn_save);
        btnCancel = findViewById(R.id.btn_cancel);
        progressBar = findViewById(R.id.progress_bar);
        tilName = findViewById(R.id.til_name);
        tilSurname = findViewById(R.id.til_surname);
        tilInstitution = findViewById(R.id.til_institution);
        tilStudentNumber = findViewById(R.id.til_student_number);
        tilStory = findViewById(R.id.til_story);
        tilAmountRequired = findViewById(R.id.til_amount_required);
        tilVideoLink = findViewById(R.id.til_video_link);
        drawerLayout = findViewById(R.id.drawer_layout);

        // Set up Toolbar
        setSupportActionBar(toolbar);
        toolbar.setNavigationIcon(R.drawable.ic_menu);
        getSupportActionBar().setTitle("My Profile");

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
                startActivity(new Intent(MyProfileActivity.this, HomeActivity.class));
                finish();
            } else if (id == R.id.nav_my_profile) {
                drawerLayout.closeDrawer(GravityCompat.START);
            } else if (id == R.id.nav_registration) {
                startActivity(new Intent(MyProfileActivity.this, RegistrationActivity.class));
            } else if (id == R.id.nav_wallet) {
                startActivity(new Intent(MyProfileActivity.this, WalletActivity.class));
            } else if (id == R.id.nav_communities) {
                startActivity(new Intent(MyProfileActivity.this, CommunitiesActivity.class));
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
                ivProfileImage.setImageURI(uri);
                Bundle params = new Bundle();
                params.putString("event", "profile_image_selected");
                mFirebaseAnalytics.logEvent("image_selection", params);
            }
        });

        // Edit button click
        btnEdit.setOnClickListener(v -> toggleEditMode(true));

        // Save button click
        btnSave.setOnClickListener(v -> saveProfile());

        // Cancel button click
        btnCancel.setOnClickListener(v -> toggleEditMode(false));

        // Image button click
        ivProfileImage.setOnClickListener(v -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                permissionLauncher.launch(new String[]{Manifest.permission.READ_MEDIA_IMAGES});
            } else {
                permissionLauncher.launch(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE});
            }
        });

        // Load profile
        loadProfile();

        // Log screen view
        Bundle bundle = new Bundle();
        bundle.putString("screen", "my_profile");
        mFirebaseAnalytics.logEvent("screen_view", bundle);
    }

    private void loadProfile() {
        String uid = mAuth.getCurrentUser().getUid();
        db.collection("users").document(uid).get().addOnSuccessListener(userDoc -> {
            if (userDoc.exists()) {
                boolean profileCreated = userDoc.getBoolean("profileCreated") != null ? userDoc.getBoolean("profileCreated") : false;
                if (!profileCreated) {
                    Toast.makeText(this, "Create a profile first in Registration", Toast.LENGTH_SHORT).show();
                    startActivity(new Intent(this, RegistrationActivity.class));
                    finish();
                    return;
                }
                if (!"student".equals(userDoc.getString("role"))) {
                    Toast.makeText(this, "Only students can access My Profile", Toast.LENGTH_SHORT).show();
                    finish();
                    return;
                }
            } else {
                Toast.makeText(this, "User profile not found", Toast.LENGTH_SHORT).show();
                finish();
                return;
            }

            db.collection("students").document(uid).get().addOnSuccessListener(studentDoc -> {
                if (studentDoc.exists()) {
                    currentStudent = studentDoc.toObject(Student.class);
                    currentStudent.setUid(uid);

                    // Populate view mode
                    tvName.setText(currentStudent.getName());
                    tvSurname.setText(currentStudent.getSurname());
                    tvInstitution.setText(currentStudent.getInstitution());
                    tvStudentNumber.setText(currentStudent.getStudentNumber());
                    tvAmountRequired.setText("R" + currentStudent.getAmountRequired());
                    tvProgress.setText("Progress: " + currentStudent.getProgress() + "%");
                    progressBarFunding.setProgress((int) currentStudent.getProgress()); // Fixed: Now resolves

                    // Load image
                    if (currentStudent.getImageUrl() != null && !currentStudent.getImageUrl().isEmpty()) {
                        Glide.with(this).load(currentStudent.getImageUrl()).placeholder(R.drawable.ic_student).into(ivProfileImage);
                    } else {
                        ivProfileImage.setImageResource(R.drawable.ic_student);
                    }
                } else {
                    Toast.makeText(this, "Student profile not found", Toast.LENGTH_SHORT).show();
                    startActivity(new Intent(this, RegistrationActivity.class));
                    finish();
                }
            }).addOnFailureListener(e -> {
                Toast.makeText(this, "Error loading profile: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            });
        }).addOnFailureListener(e -> {
            Toast.makeText(this, "Error checking user: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        });
    }

    private void toggleEditMode(boolean enable) {
        isEditing = enable;
        if (enable) {
            btnEdit.setVisibility(View.GONE);
            btnSave.setVisibility(View.VISIBLE);
            btnCancel.setVisibility(View.VISIBLE);
            etName.setVisibility(View.VISIBLE);
            etSurname.setVisibility(View.VISIBLE);
            autoCompleteInstitution.setVisibility(View.VISIBLE);
            etStudentNumber.setVisibility(View.VISIBLE);
            etStory.setVisibility(View.VISIBLE);
            etAmountRequired.setVisibility(View.VISIBLE);
            etVideoLink.setVisibility(View.VISIBLE);
            tvName.setVisibility(View.GONE);
            tvSurname.setVisibility(View.GONE);
            tvInstitution.setVisibility(View.GONE);
            tvStudentNumber.setVisibility(View.GONE);
            tvAmountRequired.setVisibility(View.GONE);
            tvProgress.setVisibility(View.GONE);
            progressBarFunding.setVisibility(View.GONE); // Fixed: Now resolves

            // Populate edit fields
            etName.setText(currentStudent.getName());
            etSurname.setText(currentStudent.getSurname());
            autoCompleteInstitution.setText(currentStudent.getInstitution());
            etStudentNumber.setText(currentStudent.getStudentNumber());
            etStory.setText(currentStudent.getStory());
            etAmountRequired.setText(String.valueOf(currentStudent.getAmountRequired()));
            etVideoLink.setText(currentStudent.getVideoLink() != null ? currentStudent.getVideoLink() : ""); // Fixed: Now resolves
        } else {
            btnEdit.setVisibility(View.VISIBLE);
            btnSave.setVisibility(View.GONE);
            btnCancel.setVisibility(View.GONE);
            etName.setVisibility(View.GONE);
            etSurname.setVisibility(View.GONE);
            autoCompleteInstitution.setVisibility(View.GONE);
            etStudentNumber.setVisibility(View.GONE);
            etStory.setVisibility(View.GONE);
            etAmountRequired.setVisibility(View.GONE);
            etVideoLink.setVisibility(View.GONE);
            tvName.setVisibility(View.VISIBLE);
            tvSurname.setVisibility(View.VISIBLE);
            tvInstitution.setVisibility(View.VISIBLE);
            tvStudentNumber.setVisibility(View.VISIBLE);
            tvAmountRequired.setVisibility(View.VISIBLE);
            tvProgress.setVisibility(View.VISIBLE);
            progressBarFunding.setVisibility(View.VISIBLE); // Fixed: Now resolves

            // Reload profile to refresh view
            loadProfile();
        }
    }

    private void saveProfile() {
        if (validateEditInput()) {
            progressBar.setVisibility(View.VISIBLE);
            btnSave.setEnabled(false);

            String uid = mAuth.getCurrentUser().getUid();
            String name = etName.getText().toString().trim();
            String surname = etSurname.getText().toString().trim();
            String institution = autoCompleteInstitution.getText().toString().trim();
            String studentNumber = etStudentNumber.getText().toString().trim();
            String story = etStory.getText().toString().trim();
            double amountRequired = Double.parseDouble(etAmountRequired.getText().toString().trim());
            String videoLink = etVideoLink.getText().toString().trim();

            // Check edit limit for amount
            db.collection("students").document(uid).get().addOnSuccessListener(documentSnapshot -> {
                long lastEdited = documentSnapshot.getLong("lastEdited") != null ? documentSnapshot.getLong("lastEdited") : 0;
                long currentTime = System.currentTimeMillis();
                if (lastEdited > 0 && (currentTime - lastEdited) < 7 * 24 * 60 * 60 * 1000) { // 1 week in ms
                    Toast.makeText(this, "Amount can only be changed once per week", Toast.LENGTH_SHORT).show();
                    progressBar.setVisibility(View.GONE);
                    btnSave.setEnabled(true);
                    return;
                }

                Map<String, Object> updates = new HashMap<>();
                updates.put("name", name);
                updates.put("surname", surname);
                updates.put("institution", institution);
                updates.put("studentNumber", studentNumber);
                updates.put("story", story);
                updates.put("amountRequired", amountRequired);
                updates.put("videoLink", videoLink); // Fixed: Now resolves
                updates.put("lastEdited", currentTime);

                if (imageUri != null) {
                    StorageReference imageRef = storage.getReference().child("student_images/" + uid + ".jpg");
                    imageRef.putFile(imageUri)
                            .addOnSuccessListener(taskSnapshot -> {
                                imageRef.getDownloadUrl().addOnSuccessListener(uri -> {
                                    updates.put("imageUrl", uri.toString());
                                    updateProfile(updates, uid);
                                }).addOnFailureListener(e -> {
                                    progressBar.setVisibility(View.GONE);
                                    btnSave.setEnabled(true);
                                    Toast.makeText(this, "Failed to update image: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                });
                            })
                            .addOnFailureListener(e -> {
                                progressBar.setVisibility(View.GONE);
                                btnSave.setEnabled(true);
                                Toast.makeText(this, "Image upload failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                            });
                } else {
                    updateProfile(updates, uid);
                }
            }).addOnFailureListener(e -> {
                progressBar.setVisibility(View.GONE);
                btnSave.setEnabled(true);
                Toast.makeText(this, "Error checking edit limit: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            });
        }
    }

    private void updateProfile(Map<String, Object> updates, String uid) {
        db.collection("students").document(uid).update(updates).addOnSuccessListener(aVoid -> {
            progressBar.setVisibility(View.GONE);
            btnSave.setEnabled(true);
            Toast.makeText(this, "Profile updated successfully", Toast.LENGTH_SHORT).show();

            Bundle params = new Bundle();
            params.putString("event", "profile_updated");
            mFirebaseAnalytics.logEvent("profile_update", params);

            toggleEditMode(false);
        }).addOnFailureListener(e -> {
            progressBar.setVisibility(View.GONE);
            btnSave.setEnabled(true);
            Toast.makeText(this, "Error updating profile: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        });
    }

    private boolean validateEditInput() {
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

    private void checkProfileAccess() {
        db.collection("users").document(mAuth.getCurrentUser().getUid())
                .get()
                .addOnSuccessListener(document -> {
                    if (document.exists() && document.getBoolean("profileCreated")) {
                        drawerLayout.closeDrawer(GravityCompat.START);
                    } else {
                        Toast.makeText(this, "Only students with profiles can access this page", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error checking profile: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void logout() {
        SharedPreferences sharedPreferences = getSharedPreferences("LoginPrefs", MODE_PRIVATE); // Fixed: Now resolves
        SharedPreferences.Editor editor = sharedPreferences.edit(); // Fixed: Now resolves
        editor.clear();
        editor.apply();

        mAuth.signOut();
        Toast.makeText(this, "Logged out successfully", Toast.LENGTH_SHORT).show();
        startActivity(new Intent(MyProfileActivity.this, MainActivity.class));
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