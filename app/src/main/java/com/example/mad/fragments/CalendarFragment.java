package com.example.mad.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.mad.MainActivity;
import com.example.mad.R;

public class CalendarFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_calendar, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        MainActivity activity = (MainActivity) getActivity();
        if (activity == null) return;

        view.findViewById(R.id.cardDailyMood).setOnClickListener(v -> 
                activity.loadFragment(new MoodCheckinFragment()));

        view.findViewById(R.id.btnAddReminder).setOnClickListener(v -> 
                Toast.makeText(getContext(), "Reminder feature coming soon!", Toast.LENGTH_SHORT).show());
    }
}
