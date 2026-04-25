package com.example.mad.fragments;

import android.content.Intent;
import android.net.Uri;
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
import com.example.mad.network.BackendClient;

import org.json.JSONArray;
import org.json.JSONObject;

public class EventsFragment extends Fragment {
    private JSONObject firstEvent;
    private TextView tvEventTitle;
    private TextView tvEventDescription;
    private TextView tvEventDate;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_events, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        tvEventTitle = view.findViewById(R.id.tvEventTitle);
        tvEventDescription = view.findViewById(R.id.tvEventDescription);
        tvEventDate = view.findViewById(R.id.tvEventDate);

        loadEvents();

        view.findViewById(R.id.btnJoinSession).setOnClickListener(v -> {
            if (firstEvent == null) {
                Toast.makeText(getContext(), "No session available", Toast.LENGTH_SHORT).show();
                return;
            }
            String joinLink = firstEvent.optString("join_link", "");
            if (joinLink.isEmpty()) {
                Toast.makeText(getContext(), "Join link not available", Toast.LENGTH_SHORT).show();
                return;
            }
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(joinLink)));
        });

        view.findViewById(R.id.btnBookSession).setOnClickListener(v ->
                BackendClient.createQuickReminder(requireContext(), "Upcoming support session", new BackendClient.SimpleCallback() {
                    @Override
                    public void onSuccess(String message) {
                        if (getActivity() == null) return;
                        getActivity().runOnUiThread(() ->
                                Toast.makeText(getContext(), "Reminder added", Toast.LENGTH_SHORT).show());
                    }

                    @Override
                    public void onError(String message) {
                        if (getActivity() == null) return;
                        getActivity().runOnUiThread(() ->
                                Toast.makeText(getContext(), message, Toast.LENGTH_LONG).show());
                    }
                }));
    }

    private void loadEvents() {
        BackendClient.getEvents(requireContext(), new BackendClient.JsonCallback() {
            @Override
            public void onSuccess(JSONArray data) {
                if (data.length() > 0) {
                    firstEvent = data.optJSONObject(0);
                    if (firstEvent != null && getActivity() != null) {
                        getActivity().runOnUiThread(() -> {
                            tvEventTitle.setText(firstEvent.optString("title", "Upcoming Session"));
                            tvEventDescription.setText(firstEvent.optString("description", "Session details"));
                            tvEventDate.setText(firstEvent.optString("event_date", ""));
                        });
                    }
                }
            }

            @Override
            public void onError(String message) {
                if (getActivity() == null) return;
                getActivity().runOnUiThread(() ->
                        Toast.makeText(getContext(), message, Toast.LENGTH_LONG).show());
            }
        });
    }
}
