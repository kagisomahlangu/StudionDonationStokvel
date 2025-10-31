package com.example.studiondonationstokvel;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthInvalidUserException;

public class LoginActivity extends AppCompatActivity {
    private FirebaseAuth mAuth;
    private FirebaseAnalytics mFirebaseAnalytics;
    private EditText etEmail, etPassword;
    private CheckBox cbRememberMe;
    private TextView tvForgotPassword, tvSignUp;
    private Button btnLogin;
    private SharedPreferences sharedPreferences;
    private static final String PREF_NAME = "LoginPrefs";
    private static final String KEY_REMEMBER_ME = "rememberMe";
    private static final String KEY_EMAIL = "email";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        mAuth = FirebaseAuth.getInstance();
        mFirebaseAnalytics = FirebaseAnalytics.getInstance(this);
        sharedPreferences = getSharedPreferences(PREF_NAME, MODE_PRIVATE);

        Toolbar toolbar = findViewById(R.id.toolbar_login);
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v -> onBackPressed());

        // Check if user is already logged in and "Remember Me" was selected
        if (mAuth.getCurrentUser() != null && sharedPreferences.getBoolean(KEY_REMEMBER_ME, false)) {
            startActivity(new Intent(this, HomeActivity.class));
            finish();
            return;
        }

        etEmail = findViewById(R.id.et_email);
        etPassword = findViewById(R.id.et_password);
        cbRememberMe = findViewById(R.id.cb_remember_me);
        tvForgotPassword = findViewById(R.id.tv_forgot_password);
        tvSignUp = findViewById(R.id.tv_sign_up);
        btnLogin = findViewById(R.id.btn_login);

        // Load saved email if "Remember Me" was previously checked
        if (sharedPreferences.getBoolean(KEY_REMEMBER_ME, false)) {
            etEmail.setText(sharedPreferences.getString(KEY_EMAIL, ""));
            cbRememberMe.setChecked(true);
        }

        btnLogin.setOnClickListener(v -> login());

        tvSignUp.setOnClickListener(v -> {
            Bundle params = new Bundle();
            params.putString("button", "sign_up_from_login");
            mFirebaseAnalytics.logEvent("button_click", params);
            startActivity(new Intent(LoginActivity.this, SignUpActivity.class));
        });

        tvForgotPassword.setOnClickListener(v -> {
            String email = etEmail.getText().toString().trim();
            if (email.isEmpty()) {
                Toast.makeText(this, "Please enter your email to reset password", Toast.LENGTH_SHORT).show();
                return;
            }
            mAuth.sendPasswordResetEmail(email)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            Toast.makeText(this, "Password reset email sent", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(this, "Error: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
        });

        // Log screen view
        Bundle bundle = new Bundle();
        bundle.putString("screen", "login");
        mFirebaseAnalytics.logEvent("screen_view", bundle);
    }

    private void login() {
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Please enter both email and password", Toast.LENGTH_SHORT).show();
            return;
        }

        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(this, "Login successful", Toast.LENGTH_SHORT).show();

                        // Save "Remember Me" preference
                        SharedPreferences.Editor editor = sharedPreferences.edit();
                        editor.putBoolean(KEY_REMEMBER_ME, cbRememberMe.isChecked());
                        if (cbRememberMe.isChecked()) {
                            editor.putString(KEY_EMAIL, email);
                        } else {
                            editor.remove(KEY_EMAIL);
                        }
                        editor.apply();

                        Bundle params = new Bundle();
                        params.putString("user_id", mAuth.getCurrentUser().getUid());
                        mFirebaseAnalytics.logEvent("login_success", params);

                        // Redirect to Home Page
                        startActivity(new Intent(LoginActivity.this, HomeActivity.class));
                        finish();
                    } else {
                        String errorMessage = "Login failed";
                        if (task.getException() instanceof FirebaseAuthInvalidCredentialsException) {
                            errorMessage = "Invalid email or password";
                        } else if (task.getException() instanceof FirebaseAuthInvalidUserException) {
                            errorMessage = "No account found with this email";
                        } else if (task.getException() != null) {
                            errorMessage = task.getException().getMessage();
                        }
                        Toast.makeText(this, errorMessage, Toast.LENGTH_SHORT).show();

                        mFirebaseAnalytics.logEvent("login_failure", null);
                    }
                });
    }
}