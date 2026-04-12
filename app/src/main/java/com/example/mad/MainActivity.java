package com.example.mad;

import android.os.Bundle;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.example.mad.fragments.CalendarFragment;
import com.example.mad.fragments.CommunityFragment;
import com.example.mad.fragments.EventsFragment;
import com.example.mad.fragments.HomeFragment;
import com.example.mad.fragments.InfoHubFragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        TextView urgentHelpButton = findViewById(R.id.tvUrgentHelp);
        BottomNavigationView bottomNavigationView = findViewById(R.id.bottomNavigation);

        urgentHelpButton.setOnClickListener(v -> {
            // Hook emergency action here (call, SOS screen, etc.).
        });

        if (savedInstanceState == null) {
            loadFragment(new HomeFragment());
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

    private void loadFragment(Fragment fragment) {
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragmentContainer, fragment)
                .commit();
    }
}