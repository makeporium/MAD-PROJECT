package com.example.mad.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.mad.MainActivity;
import com.example.mad.R;
import com.example.mad.network.BackendClient;
import com.google.firebase.auth.FirebaseAuth;

public class HomeFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        MainActivity activity = (MainActivity) getActivity();
        if (activity == null) return;

        TextView tvWelcome = view.findViewById(R.id.tvHomeWelcome);
        String name = "there";
        if (FirebaseAuth.getInstance().getCurrentUser() != null &&
                FirebaseAuth.getInstance().getCurrentUser().getDisplayName() != null) {
            name = FirebaseAuth.getInstance().getCurrentUser().getDisplayName();
        }
        tvWelcome.setText("Welcome back, " + name);

        BackendClient.checkHealth(new BackendClient.HealthCallback() {
            @Override
            public void onSuccess() {
                // Connected.
            }

            @Override
            public void onError() {
                if (getActivity() == null) return;
                getActivity().runOnUiThread(() ->
                        Toast.makeText(getContext(), "Backend not reachable", Toast.LENGTH_SHORT).show());
            }
        });

        view.findViewById(R.id.cardRecommends).setOnClickListener(v -> 
                activity.loadFragment(new RecommendsFragment()));

        view.findViewById(R.id.cardMoodCheckin).setOnClickListener(v -> 
                activity.loadFragment(new MoodCheckinFragment()));

        view.findViewById(R.id.cardAiSupport).setOnClickListener(v -> 
                activity.loadFragment(new AiSupportFragment()));

        view.findViewById(R.id.cardLearn).setOnClickListener(v -> 
                activity.navigateToTab(R.id.nav_info_hub));

        view.findViewById(R.id.cardFeaturedArticle).setOnClickListener(v -> 
                activity.navigateToTab(R.id.nav_info_hub));
    }
}
