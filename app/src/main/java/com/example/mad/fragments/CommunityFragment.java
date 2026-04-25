package com.example.mad.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.mad.R;

public class CommunityFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_community, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        view.findViewById(R.id.itemNightFeeding).setOnClickListener(v -> 
            showChat("Night Feeding Support", "Share tips and support for late-night feeding"));

        view.findViewById(R.id.itemFirstWeek).setOnClickListener(v -> 
            showChat("First Week Postpartum", "For mothers in their first week after delivery"));

        view.findViewById(R.id.itemFeelingLonely).setOnClickListener(v -> 
            showChat("Feeling Lonely", "A safe space to share when you feel isolated"));

        view.findViewById(R.id.itemPartnerSupport).setOnClickListener(v -> 
            showChat("Partner Support", "Navigating relationships during postpartum"));

        view.findViewById(R.id.itemSelfCare).setOnClickListener(v -> 
            showChat("Self-Care Corner", "Tips and encouragement for taking care of yourself"));

        view.findViewById(R.id.cardSuggestTopic).setOnClickListener(v -> 
            Toast.makeText(getContext(), "Thank you for your suggestion! We'll review it soon.", Toast.LENGTH_SHORT).show());
    }

    private void showChat(String title, String desc) {
        ChatDialogFragment.newInstance(title, desc)
                .show(getChildFragmentManager(), "chat_dialog");
    }
}
