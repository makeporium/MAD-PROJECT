package com.example.mad.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.mad.R;
import com.example.mad.network.BackendClient;

public class AiSupportFragment extends Fragment {
    private LinearLayout messagesContainer;
    private EditText etAiMessage;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_ai_support, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        messagesContainer = view.findViewById(R.id.aiMessagesContainer);
        etAiMessage = view.findViewById(R.id.etAiMessage);
        View btnSend = view.findViewById(R.id.btnSendAiSupport);

        view.findViewById(R.id.btnCloseChat).setOnClickListener(v -> {
            if (getActivity() != null) {
                getActivity().getSupportFragmentManager().popBackStack();
            }
        });

        loadHistory();

        btnSend.setOnClickListener(v -> {
            String userMessage = etAiMessage.getText().toString().trim();
            if (userMessage.isEmpty()) {
                Toast.makeText(getContext(), "Type your message first", Toast.LENGTH_SHORT).show();
                return;
            }

            // UI feedback
            btnSend.setEnabled(false);
            btnSend.setAlpha(0.5f);
            etAiMessage.setText("");
            addChatLine("You: " + userMessage);

            BackendClient.sendAiMessage(requireContext(), userMessage, new BackendClient.ObjectCallback() {
                @Override
                public void onSuccess(org.json.JSONObject data) {
                    if (getActivity() == null) return;
                    getActivity().runOnUiThread(() -> {
                        String reply = data.optString("reply", "Thanks for sharing.");
                        addChatLine("Support: " + reply);
                        resetButton(btnSend);
                    });
                }

                @Override
                public void onError(String message) {
                    if (getActivity() == null) return;
                    getActivity().runOnUiThread(() -> {
                        Toast.makeText(getContext(), message, Toast.LENGTH_LONG).show();
                        resetButton(btnSend);
                    });
                }
            });
        });
    }

    private void resetButton(View btnSend) {
        btnSend.setEnabled(true);
        btnSend.setAlpha(1.0f);
    }

    private void loadHistory() {
        BackendClient.getAiHistory(requireContext(), new BackendClient.JsonCallback() {
            @Override
            public void onSuccess(org.json.JSONArray data) {
                if (getActivity() == null) return;
                getActivity().runOnUiThread(() -> {
                    messagesContainer.removeAllViews();
                    for (int i = 0; i < data.length(); i++) {
                        try {
                            org.json.JSONObject msg = data.getJSONObject(i);
                            String userMsg = msg.optString("user_message", "");
                            String aiReply = msg.optString("ai_reply", "");
                            if (!userMsg.isEmpty()) addChatLine("You: " + userMsg);
                            if (!aiReply.isEmpty()) addChatLine("Support: " + aiReply);
                        } catch (Exception ignored) {}
                    }
                });
            }

            @Override
            public void onError(String message) {
                if (getActivity() == null) return;
                getActivity().runOnUiThread(() -> 
                    Toast.makeText(getContext(), "Could not load history", Toast.LENGTH_SHORT).show()
                );
            }
        });
    }

    private void addChatLine(String text) {
        TextView tv = new TextView(requireContext());
        tv.setText(text);
        tv.setTextSize(15f);
        tv.setPadding(0, 8, 0, 8);
        messagesContainer.addView(tv);
    }
}
