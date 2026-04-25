package com.example.mad;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import com.example.mad.fragments.CalendarFragment;
import com.example.mad.fragments.CommunityFragment;
import com.example.mad.fragments.EventsFragment;
import com.example.mad.fragments.HomeFragment;
import com.example.mad.fragments.InfoHubFragment;
import com.example.mad.fragments.SignInFragment;
import com.example.mad.network.BackendClient;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

public class MainActivity extends AppCompatActivity {

    private DrawerLayout drawerLayout;
    private EditText etFirstName, etSurname, etPhone, etEmail;
    private static final String PREFS_NAME = "MotherNestProfile";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        // DrawerLayout
        drawerLayout = findViewById(R.id.drawerLayout);

        // Profile fields
        etFirstName = findViewById(R.id.etFirstName);
        etSurname = findViewById(R.id.etSurname);
        etPhone = findViewById(R.id.etPhone);
        etEmail = findViewById(R.id.etEmail);

        // Load saved profile data
        loadProfile();

        // Save Profile button
        findViewById(R.id.btnSaveProfile).setOnClickListener(v -> saveProfile());

        // Sign Out button
        findViewById(R.id.btnSignOut).setOnClickListener(v -> showSignOutDialog());

        // Urgent Help
        TextView urgentHelpButton = findViewById(R.id.tvUrgentHelp);
        FloatingActionButton fabUrgent = findViewById(R.id.fabUrgent);
        BottomNavigationView bottomNavigationView = findViewById(R.id.bottomNavigation);

        View.OnClickListener urgentListener = v -> {
            Toast.makeText(this, "Emergency support is being notified...", Toast.LENGTH_LONG).show();
            // In a real app, this would trigger an SOS alert or call.
        };

        urgentHelpButton.setOnClickListener(urgentListener);
        fabUrgent.setOnClickListener(urgentListener);

        if (savedInstanceState == null) {
            if (BackendClient.hasAccessToken(this)) {
                loadFragment(new HomeFragment());
            } else {
                loadFragment(new SignInFragment());
            }
        }

        bottomNavigationView.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.nav_home) {
                loadFragment(new HomeFragment());
                return true;
            } else if (itemId == R.id.nav_community) {
                loadFragment(new CommunityFragment());
                return true;
            } else if (itemId == R.id.nav_calendar) {
                loadFragment(new CalendarFragment());
                return true;
            } else if (itemId == R.id.nav_info_hub) {
                loadFragment(new InfoHubFragment());
                return true;
            } else if (itemId == R.id.nav_events) {
                loadFragment(new EventsFragment());
                return true;
            }
            return false;
        });
    }

    public void loadFragment(Fragment fragment) {
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragmentContainer, fragment)
                .addToBackStack(null)
                .commit();
    }

    public void navigateToTab(int itemId) {
        BottomNavigationView bottomNavigationView = findViewById(R.id.bottomNavigation);
        bottomNavigationView.setSelectedItemId(itemId);
    }

    public void onAuthSuccess() {
        // Clear back stack to prevent going back to sign-in screen
        getSupportFragmentManager().popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
        navigateToTab(R.id.nav_home);
    }

    public void openProfileDrawer() {
        if (drawerLayout != null) {
            drawerLayout.openDrawer(findViewById(R.id.drawerContent));
        }
    }

    private void loadProfile() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        etFirstName.setText(prefs.getString("firstName", ""));
        etSurname.setText(prefs.getString("surname", ""));
        etPhone.setText(prefs.getString("phone", ""));
        etEmail.setText(prefs.getString("email", ""));
    }

    private void saveProfile() {
        SharedPreferences.Editor editor = getSharedPreferences(PREFS_NAME, MODE_PRIVATE).edit();
        editor.putString("firstName", etFirstName.getText().toString().trim());
        editor.putString("surname", etSurname.getText().toString().trim());
        editor.putString("phone", etPhone.getText().toString().trim());
        editor.putString("email", etEmail.getText().toString().trim());
        editor.apply();
        Toast.makeText(this, getString(R.string.profile_saved), Toast.LENGTH_SHORT).show();
    }

    private void showSignOutDialog() {
        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.sign_out_confirm_title))
                .setMessage(getString(R.string.sign_out_confirm_message))
                .setPositiveButton("Yes", (dialog, which) -> {
                    // Clear profile and auth data
                    getSharedPreferences(PREFS_NAME, MODE_PRIVATE).edit().clear().apply();
                    BackendClient.clearAccessToken(this);
                    etFirstName.setText("");
                    etSurname.setText("");
                    etPhone.setText("");
                    etEmail.setText("");
                    drawerLayout.closeDrawers();
                    Toast.makeText(this, getString(R.string.sign_out_success), Toast.LENGTH_SHORT).show();
                    
                    // Redirect to sign in
                    getSupportFragmentManager().popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
                    loadFragment(new SignInFragment());
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    public String getSavedFirstName() {
        return getSharedPreferences(PREFS_NAME, MODE_PRIVATE).getString("firstName", "");
    }
}
