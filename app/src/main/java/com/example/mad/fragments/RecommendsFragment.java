package com.example.mad.fragments;

import android.app.AlertDialog;
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

public class RecommendsFragment extends Fragment {
    private TextView tvRecommendResult;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_recommends, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        tvRecommendResult = view.findViewById(R.id.tvRecommendResult);

        view.findViewById(R.id.btnFindMatch).setOnClickListener(v ->
                BackendClient.getRecommendations(requireContext(), new BackendClient.JsonCallback() {
                    @Override
                    public void onSuccess(JSONArray data) {
                        if (getActivity() == null) return;
                        getActivity().runOnUiThread(() -> showRecommendationDialog(data));
                    }

                    @Override
                    public void onError(String message) {
                        if (getActivity() == null) return;
                        getActivity().runOnUiThread(() ->
                                Toast.makeText(getContext(), message, Toast.LENGTH_LONG).show());
                    }
                }));
    }

    private void showRecommendationDialog(JSONArray data) {
        if (data.length() == 0) {
            Toast.makeText(getContext(), "No recommendations found", Toast.LENGTH_SHORT).show();
            return;
        }
        JSONObject first = data.optJSONObject(0);
        if (first != null && tvRecommendResult != null) {
            tvRecommendResult.setText(first.optString("title", "Recommendation loaded"));
        }

        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < Math.min(3, data.length()); i++) {
            try {
                JSONObject item = data.getJSONObject(i);
                builder.append(i + 1).append(". ")
                        .append(item.optString("title", "Untitled"))
                        .append("\n")
                        .append(item.optString("description", ""))
                        .append("\n\n");
            } catch (Exception ignored) {
            }
        }

        new AlertDialog.Builder(requireContext())
                .setTitle("Top Recommendations")
                .setMessage(builder.toString().trim())
                .setPositiveButton("OK", null)
                .show();
    }
}
