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

import com.example.mad.R;

public class InfoHubFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_info_hub, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Topic Chip Clicks
        View.OnClickListener chipListener = v -> {
            String topic = ((TextView) v).getText().toString();
            Toast.makeText(getContext(), "Filtering by: " + topic, Toast.LENGTH_SHORT).show();
        };

        view.findViewById(R.id.chipPPD).setOnClickListener(chipListener);
        view.findViewById(R.id.chipCoping).setOnClickListener(chipListener);
        view.findViewById(R.id.chipPartner).setOnClickListener(chipListener);
        view.findViewById(R.id.chipSleep).setOnClickListener(chipListener);
        view.findViewById(R.id.chipFeeding).setOnClickListener(chipListener);
        view.findViewById(R.id.chipSelfCare).setOnClickListener(chipListener);

        // Article Clicks
        view.findViewById(R.id.cardArticle1).setOnClickListener(v -> 
            Toast.makeText(getContext(), "Opening full article...", Toast.LENGTH_SHORT).show());
    }
}
