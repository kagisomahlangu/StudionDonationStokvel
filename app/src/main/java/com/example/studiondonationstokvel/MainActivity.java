package com.example.studiondonationstokvel;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.analytics.FirebaseAnalytics;

public class MainActivity extends AppCompatActivity {
    private FirebaseAnalytics mFirebaseAnalytics;
    private DrawerLayout drawerLayout;
    private ActionBarDrawerToggle toggle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mFirebaseAnalytics = FirebaseAnalytics.getInstance(this);

        // Log welcome screen view
        Bundle bundle = new Bundle();
        bundle.putString("screen", "welcome");
        mFirebaseAnalytics.logEvent("screen_view", bundle);

        // Set up DrawerLayout
        drawerLayout = findViewById(R.id.drawer_layout);
        NavigationView navigationView = findViewById(R.id.nav_view);

        toggle = new ActionBarDrawerToggle(this, drawerLayout, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        navigationView.setNavigationItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_sign_up) {
                Bundle params = new Bundle();
                params.putString("button", "sign_up_via_drawer");
                mFirebaseAnalytics.logEvent("button_click", params);
                startActivity(new Intent(MainActivity.this, SignUpActivity.class));
            } else if (id == R.id.nav_login) {
                Bundle params = new Bundle();
                params.putString("button", "login_via_drawer");
                mFirebaseAnalytics.logEvent("button_click", params);
                startActivity(new Intent(MainActivity.this, LoginActivity.class));
            } else if (id == R.id.nav_home) {
                drawerLayout.closeDrawer(GravityCompat.START);
            }
            return true;
        });

        // Button clicks
        findViewById(R.id.btn_sign_up).setOnClickListener(v -> {
            Bundle params = new Bundle();
            params.putString("button", "sign_up");
            mFirebaseAnalytics.logEvent("button_click", params);
            startActivity(new Intent(MainActivity.this, SignUpActivity.class));
        });

        findViewById(R.id.btn_login).setOnClickListener(v -> {
            Bundle params = new Bundle();
            params.putString("button", "login");
            mFirebaseAnalytics.logEvent("button_click", params);
            startActivity(new Intent(MainActivity.this, LoginActivity.class));
        });

        if (mFirebaseAnalytics != null) {
            Log.d("FirebaseCheck", "Firebase Analytics is connected successfully!");
        } else {
            Log.e("FirebaseCheck", "Firebase Analytics initialization failed!");
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (toggle.onOptionsItemSelected(item)) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}