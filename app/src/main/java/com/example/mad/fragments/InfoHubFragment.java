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

public class InfoHubFragment extends Fragment {
    private String firstArticleUrl = "";
    private TextView tvResourceTopic;
    private TextView tvResourceTitle;
    private TextView tvResourceExcerpt;

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
        tvResourceTopic = view.findViewById(R.id.tvResourceTopic);
        tvResourceTitle = view.findViewById(R.id.tvResourceTitle);
        tvResourceExcerpt = view.findViewById(R.id.tvResourceExcerpt);

        loadResources("");

        view.findViewById(R.id.chipPPD).setOnClickListener(v -> loadResources("PPD"));
        view.findViewById(R.id.chipCoping).setOnClickListener(v -> loadResources("Coping"));
        view.findViewById(R.id.chipPartner).setOnClickListener(v -> loadResources("Partner"));
        view.findViewById(R.id.chipSleep).setOnClickListener(v -> loadResources("Sleep"));
        view.findViewById(R.id.chipFeeding).setOnClickListener(v -> loadResources("Feeding"));
        view.findViewById(R.id.chipSelfCare).setOnClickListener(v -> loadResources("Self-care"));

        view.findViewById(R.id.cardArticle1).setOnClickListener(v -> {
            if (firstArticleUrl == null || firstArticleUrl.isEmpty()) {
                Toast.makeText(getContext(), "No article link available", Toast.LENGTH_SHORT).show();
                return;
            }
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(firstArticleUrl)));
        });
    }

    private void loadResources(String topic) {
        BackendClient.getResources(requireContext(), topic, new BackendClient.JsonCallback() {
            @Override
            public void onSuccess(JSONArray data) {
                if (getActivity() == null) return;
                getActivity().runOnUiThread(() -> {
                    if (data.length() == 0) {
                        Toast.makeText(getContext(), "No resources for this topic", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    JSONObject first = data.optJSONObject(0);
                    if (first != null) {
                        firstArticleUrl = first.optString("content_url", "");
                        tvResourceTopic.setText(first.optString("topic", "Topic"));
                        tvResourceTitle.setText(first.optString("title", "Article"));
                        tvResourceExcerpt.setText(first.optString("excerpt", "No summary available"));
                    }
                });
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
