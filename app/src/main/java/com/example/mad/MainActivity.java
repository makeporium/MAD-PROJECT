package com.example.mad;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        TextView urgentHelpButton = findViewById(R.id.tvUrgentHelp);
        FloatingActionButton fabUrgent = findViewById(R.id.fabUrgent);
        BottomNavigationView bottomNavigationView = findViewById(R.id.bottomNavigation);

        View.OnClickListener urgentListener = v -> {
            BackendClient.sendSos(this, new BackendClient.SimpleCallback() {
                @Override
                public void onSuccess(String message) {
                    runOnUiThread(() ->
                            Toast.makeText(MainActivity.this, "Emergency support notified", Toast.LENGTH_LONG).show());
                }

                @Override
                public void onError(String message) {
                    runOnUiThread(() ->
                            Toast.makeText(MainActivity.this, message, Toast.LENGTH_LONG).show());
                }
            });
        };

        urgentHelpButton.setOnClickListener(urgentListener);
        fabUrgent.setOnClickListener(urgentListener);

        if (savedInstanceState == null) {
            if (BackendClient.hasAccessToken(this)) {
                showMainUi();
                loadFragment(new HomeFragment());
            } else {
                hideMainUi();
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

    public void onAuthSuccess() {
        showMainUi();
        loadFragment(new HomeFragment());
    }

    private void showMainUi() {
        findViewById(R.id.bottomNavigation).setVisibility(View.VISIBLE);
        findViewById(R.id.tvUrgentHelp).setVisibility(View.VISIBLE);
        findViewById(R.id.fabUrgent).setVisibility(View.VISIBLE);
    }

    private void hideMainUi() {
        findViewById(R.id.bottomNavigation).setVisibility(View.GONE);
        findViewById(R.id.tvUrgentHelp).setVisibility(View.GONE);
        findViewById(R.id.fabUrgent).setVisibility(View.GONE);
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
}