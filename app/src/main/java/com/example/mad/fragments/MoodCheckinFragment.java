package com.example.mad.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.SeekBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.mad.R;
import com.example.mad.network.BackendClient;

public class MoodCheckinFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_mood_checkin, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        SeekBar moodSeekBar = view.findViewById(R.id.seekMoodLevel);

        view.findViewById(R.id.btnMoodNote).setOnClickListener(v -> 
            Toast.makeText(getContext(), "Note feature coming soon!", Toast.LENGTH_SHORT).show());

        view.findViewById(R.id.btnMoodSave).setOnClickListener(v -> {
            int moodLevel = moodSeekBar.getProgress() + 1; // SeekBar 0-4, API needs 1-5
            BackendClient.saveMoodEntry(requireContext(), moodLevel, "", new BackendClient.SimpleCallback() {
                @Override
                public void onSuccess(String message) {
                    if (getActivity() == null) return;
                    getActivity().runOnUiThread(() -> {
                        Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
                        getActivity().getSupportFragmentManager().popBackStack();
                    });
                }

                @Override
                public void onError(String message) {
                    if (getActivity() == null) return;
                    getActivity().runOnUiThread(() ->
                            Toast.makeText(getContext(), message, Toast.LENGTH_LONG).show());
                }
            });
        });
    }
}
