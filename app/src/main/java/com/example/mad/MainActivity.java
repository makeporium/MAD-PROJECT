package com.example.mad;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import org.json.JSONObject;

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
    // ID of the user whose profile is shown in the left (other-profile) drawer
    private long otherProfileUserId = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        drawerLayout = findViewById(R.id.drawerLayout);

        // Increase swipe edge size for the right drawer (profile) to 100% of screen width (full screen)
        setDrawerEdgeSize(this, drawerLayout, 1.0f);

        // Lock the left drawer so it can only be opened programmatically
        drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED,
                findViewById(R.id.drawerLeftContent));

        etFirstName = findViewById(R.id.etFirstName);
        etSurname   = findViewById(R.id.etSurname);
        etPhone     = findViewById(R.id.etPhone);
        etEmail     = findViewById(R.id.etEmail);

        // Save Profile
        findViewById(R.id.btnSaveProfile).setOnClickListener(v -> saveProfile());

        // Add Testimonial (experts only, button is GONE otherwise)
        findViewById(R.id.btnManageTestimonials).setOnClickListener(v -> {
            android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
            builder.setTitle("Add Testimonial");
            android.widget.EditText input = new android.widget.EditText(this);
            input.setHint("Write testimonial...");
            builder.setView(input);
            builder.setPositiveButton("Submit", (dialog, which) -> {
                String text = input.getText().toString().trim();
                if (!text.isEmpty()) {
                    try {
                        org.json.JSONObject body = new org.json.JSONObject();
                        body.put("content", text);
                        okhttp3.Request req = BackendClient.authorizedBuilder(this, "/api/testimonials")
                                .post(okhttp3.RequestBody.create(body.toString(), BackendClient.JSON))
                                .build();
                        BackendClient.executeSimpleRequest(req, new BackendClient.SimpleCallback() {
                            @Override public void onSuccess(String message) {
                                runOnUiThread(() -> Toast.makeText(MainActivity.this,
                                        "Testimonial added!", Toast.LENGTH_SHORT).show());
                            }
                            @Override public void onError(String message) {
                                runOnUiThread(() -> Toast.makeText(MainActivity.this,
                                        message, Toast.LENGTH_LONG).show());
                            }
                        });
                    } catch (Exception ignored) {}
                }
            });
            builder.setNegativeButton("Cancel", null).show();
        });

        // Sign Out
        findViewById(R.id.btnSignOut).setOnClickListener(v -> showSignOutDialog());

        BottomNavigationView bottomNav = findViewById(R.id.bottomNavigation);
        FloatingActionButton fabUrgent = findViewById(R.id.fabUrgent);
        TextView tvUrgentHelp = findViewById(R.id.tvUrgentHelp);

        View.OnClickListener urgentListener = v ->
                Toast.makeText(this, "Emergency support is being notified...", Toast.LENGTH_LONG).show();
        tvUrgentHelp.setOnClickListener(urgentListener);
        fabUrgent.setOnClickListener(urgentListener);

        if (savedInstanceState == null) {
            if (BackendClient.hasAccessToken(this)) {
                showAuthenticatedUI(bottomNav);
                loadFragment(new HomeFragment());
            } else {
                hideAuthenticatedUI(bottomNav);
                loadFragment(new SignInFragment());
            }
        }

        bottomNav.setOnItemSelectedListener(item -> {
            // Only allow navigation when signed in
            if (!BackendClient.hasAccessToken(this)) return false;
            int id = item.getItemId();
            if (id == R.id.nav_home)       { loadFragment(new HomeFragment());      return true; }
            if (id == R.id.nav_community)  { loadFragment(new CommunityFragment()); return true; }
            if (id == R.id.nav_calendar)   { loadFragment(new CalendarFragment());  return true; }
            if (id == R.id.nav_info_hub)   { loadFragment(new InfoHubFragment());   return true; }
            if (id == R.id.nav_events)     { loadFragment(new EventsFragment());    return true; }
            return false;
        });
    }

    // ── Auth state helpers ────────────────────────────────────────────────────

    private void showAuthenticatedUI(BottomNavigationView bottomNav) {
        bottomNav.setVisibility(View.VISIBLE);
        loadProfile();
    }

    private void hideAuthenticatedUI(BottomNavigationView bottomNav) {
        bottomNav.setVisibility(View.GONE);
    }

    // ── Fragment navigation ───────────────────────────────────────────────────

    public void loadFragment(Fragment fragment) {
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragmentContainer, fragment)
                .addToBackStack(null)
                .commit();
    }

    public void navigateToTab(int itemId) {
        BottomNavigationView nav = findViewById(R.id.bottomNavigation);
        nav.setSelectedItemId(itemId);
    }

    /** Called by SignInFragment / SignUpFragment on successful auth. */
    public void onAuthSuccess() {
        getSupportFragmentManager().popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
        BottomNavigationView bottomNav = findViewById(R.id.bottomNavigation);
        showAuthenticatedUI(bottomNav);
        navigateToTab(R.id.nav_home);
    }

    // ── Drawer helpers ────────────────────────────────────────────────────────

    /** Open the right-side MY profile drawer */
    public void openProfileDrawer() {
        if (drawerLayout != null) drawerLayout.openDrawer(findViewById(R.id.drawerRightContent));
    }

    /**
     * Open the left-side OTHER profile drawer populated with the given user.
     * The drawer is normally locked – we unlock it momentarily, open, then re-lock.
     */
    public void openOtherProfileDrawer(String userName, long userId) {
        otherProfileUserId = userId;
        if (drawerLayout == null) return;
        View leftDrawer = findViewById(R.id.drawerLeftContent);
        TextView tvName = leftDrawer.findViewById(R.id.tvOtherProfileName);
        if (tvName != null) tvName.setText(userName);

        // Show "Promote to Expert" button for experts
        android.widget.Button btnPromote = leftDrawer.findViewById(R.id.btnPromoteExpert);
        if (btnPromote != null) {
            String myRole = getUserRole();
            if ("expert".equals(myRole) && userId > 0) {
                btnPromote.setVisibility(View.VISIBLE);
                btnPromote.setOnClickListener(v -> promoteUserToExpert(userId, userName));
            } else {
                btnPromote.setVisibility(View.GONE);
            }
        }

        drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED, leftDrawer);
        drawerLayout.openDrawer(leftDrawer);
        // Re-lock after it opens (so user can't swipe it open themselves)
        drawerLayout.addDrawerListener(new DrawerLayout.SimpleDrawerListener() {
            @Override public void onDrawerClosed(View v) {
                drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED, leftDrawer);
                drawerLayout.removeDrawerListener(this);
            }
        });
    }

    private void promoteUserToExpert(long userId, String userName) {
        new AlertDialog.Builder(this)
                .setTitle("Promote to Expert")
                .setMessage("Make " + userName + " an expert? They will be able to create articles and testimonials.")
                .setPositiveButton("Promote", (dialog, which) -> {
                    try {
                        org.json.JSONObject body = new org.json.JSONObject();
                        body.put("role", "expert");
                        okhttp3.Request req = BackendClient.authorizedBuilder(this, "/api/auth/users/" + userId + "/role")
                                .put(okhttp3.RequestBody.create(body.toString(), BackendClient.JSON))
                                .build();
                        BackendClient.executeSimpleRequest(req, new BackendClient.SimpleCallback() {
                            @Override public void onSuccess(String message) {
                                runOnUiThread(() -> {
                                    Toast.makeText(MainActivity.this,
                                            userName + " is now an Expert!", Toast.LENGTH_SHORT).show();
                                    drawerLayout.closeDrawers();
                                });
                            }
                            @Override public void onError(String message) {
                                runOnUiThread(() -> Toast.makeText(MainActivity.this,
                                        message, Toast.LENGTH_LONG).show());
                            }
                        });
                    } catch (Exception ignored) {}
                })
                .setNegativeButton("Cancel", null).show();
    }

    // ── Profile load / save ───────────────────────────────────────────────────

    private void loadProfile() {
        if (!BackendClient.hasAccessToken(this)) return;
        BackendClient.getProfile(this, new BackendClient.ObjectCallback() {
            @Override
            public void onSuccess(JSONObject data) {
                runOnUiThread(() -> {
                    try {
                        String name  = data.optString("name", "");
                        String email = data.optString("email", "");
                        String role  = data.optString("role", "user");
                        String[] parts = name.split(" ", 2);

                        // Fill edit fields
                        etFirstName.setText(parts.length > 0 ? parts[0] : "");
                        etSurname.setText(parts.length   > 1 ? parts[1] : "");
                        etEmail.setText(email);

                        // Update profile name header
                        TextView tvProfileName = findViewById(R.id.tvProfileName);
                        if (tvProfileName != null) tvProfileName.setText(name.isEmpty() ? "My Profile" : name);

                        // Role badge
                        TextView tvRoleBadge = findViewById(R.id.tvRoleBadge);
                        if (tvRoleBadge != null) {
                            tvRoleBadge.setVisibility(View.VISIBLE);
                            if ("expert".equals(role)) {
                                tvRoleBadge.setText("✦ Expert");
                                tvRoleBadge.setBackgroundTintList(
                                        android.content.res.ColorStateList.valueOf(
                                                android.graphics.Color.parseColor("#FFF3E0")));
                                tvRoleBadge.setTextColor(android.graphics.Color.parseColor("#E65100"));
                            } else {
                                tvRoleBadge.setText("Member");
                            }
                        }

                        // Persist
                        SharedPreferences.Editor editor = getSharedPreferences(PREFS_NAME, MODE_PRIVATE).edit();
                        editor.putString("firstName", parts.length > 0 ? parts[0] : "");
                        editor.putString("surname",   parts.length > 1 ? parts[1] : "");
                        editor.putString("email", email);
                        editor.putString("role", role);
                        editor.apply();

                        // Expert-only manage testimonials
                        findViewById(R.id.btnManageTestimonials)
                                .setVisibility("expert".equals(role) ? View.VISIBLE : View.GONE);

                    } catch (Exception ignored) {}
                });
            }
            @Override public void onError(String message) {}
        });
    }

    private void saveProfile() {
        String fullName = etFirstName.getText().toString().trim()
                + " " + etSurname.getText().toString().trim();
        try {
            JSONObject body = new JSONObject();
            body.put("name", fullName.trim());
            okhttp3.Request request = BackendClient.authorizedBuilder(this, "/api/auth/me")
                    .put(okhttp3.RequestBody.create(body.toString(),
                            okhttp3.MediaType.parse("application/json; charset=utf-8")))
                    .build();
            new okhttp3.OkHttpClient().newCall(request).enqueue(new okhttp3.Callback() {
                @Override public void onFailure(okhttp3.Call call, java.io.IOException e) {}
                @Override public void onResponse(okhttp3.Call call, okhttp3.Response response)
                        throws java.io.IOException {
                    runOnUiThread(() -> {
                        Toast.makeText(MainActivity.this,
                                getString(R.string.profile_saved), Toast.LENGTH_SHORT).show();
                        // Update in-drawer name immediately
                        TextView tvProfileName = findViewById(R.id.tvProfileName);
                        if (tvProfileName != null) tvProfileName.setText(fullName.trim());
                        SharedPreferences.Editor ed = getSharedPreferences(PREFS_NAME, MODE_PRIVATE).edit();
                        String[] p = fullName.trim().split(" ", 2);
                        ed.putString("firstName", p.length > 0 ? p[0] : "");
                        ed.putString("surname",   p.length > 1 ? p[1] : "");
                        ed.apply();
                    });
                }
            });
        } catch (Exception ignored) {}
    }

    // ── Sign-out ──────────────────────────────────────────────────────────────

    private void showSignOutDialog() {
        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.sign_out_confirm_title))
                .setMessage(getString(R.string.sign_out_confirm_message))
                .setPositiveButton("Yes", (dialog, which) -> {
                    // Sign out from Firebase and Google completely
                    com.google.firebase.auth.FirebaseAuth.getInstance().signOut();
                    com.google.android.gms.auth.api.signin.GoogleSignInOptions gso =
                        new com.google.android.gms.auth.api.signin.GoogleSignInOptions.Builder(
                            com.google.android.gms.auth.api.signin.GoogleSignInOptions.DEFAULT_SIGN_IN)
                        .build();
                    com.google.android.gms.auth.api.signin.GoogleSignIn.getClient(MainActivity.this, gso).signOut();

                    getSharedPreferences(PREFS_NAME, MODE_PRIVATE).edit().clear().apply();
                    BackendClient.clearAccessToken(this);
                    etFirstName.setText(""); etSurname.setText("");
                    etPhone.setText(""); etEmail.setText("");
                    drawerLayout.closeDrawers();
                    Toast.makeText(this, getString(R.string.sign_out_success), Toast.LENGTH_SHORT).show();
                    BottomNavigationView bottomNav = findViewById(R.id.bottomNavigation);
                    hideAuthenticatedUI(bottomNav);
                    getSupportFragmentManager().popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
                    loadFragment(new SignInFragment());
                })
                .setNegativeButton("Cancel", null).show();
    }

    // ── Public helpers for fragments ──────────────────────────────────────────

    public String getSavedFirstName() {
        return getSharedPreferences(PREFS_NAME, MODE_PRIVATE).getString("firstName", "");
    }

    public String getUserRole() {
        return getSharedPreferences(PREFS_NAME, MODE_PRIVATE).getString("role", "user");
    }

    /**
     * Increases the edge swipe area of the DrawerLayout using reflection.
     */
    public static void setDrawerEdgeSize(android.app.Activity activity, androidx.drawerlayout.widget.DrawerLayout drawerLayout, float displayWidthPercentage) {
        try {
            // The drawer layout has a field mRightDragger for the right side
            java.lang.reflect.Field rightDraggerField = drawerLayout.getClass().getDeclaredField("mRightDragger");
            rightDraggerField.setAccessible(true);
            androidx.customview.widget.ViewDragHelper rightDragger = (androidx.customview.widget.ViewDragHelper) rightDraggerField.get(drawerLayout);

            java.lang.reflect.Field edgeSizeField = rightDragger.getClass().getDeclaredField("mEdgeSize");
            edgeSizeField.setAccessible(true);
            int edgeSize = edgeSizeField.getInt(rightDragger);

            android.util.DisplayMetrics dm = new android.util.DisplayMetrics();
            activity.getWindowManager().getDefaultDisplay().getMetrics(dm);
            int newEdgeSize = Math.max(edgeSize, (int) (dm.widthPixels * displayWidthPercentage));
            edgeSizeField.setInt(rightDragger, newEdgeSize);
        } catch (Exception e) {
            android.util.Log.e("DrawerLayout", "Could not set drawer edge size", e);
        }
    }
}
