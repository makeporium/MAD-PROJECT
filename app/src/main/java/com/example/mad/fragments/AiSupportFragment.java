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

        view.findViewById(R.id.btnCloseChat).setOnClickListener(v -> {
            if (getActivity() != null) {
                getActivity().getSupportFragmentManager().popBackStack();
            }
        });

        view.findViewById(R.id.btnSendAiSupport).setOnClickListener(v -> {
            String userMessage = etAiMessage.getText().toString().trim();
            if (userMessage.isEmpty()) {
                Toast.makeText(getContext(), "Type your message first", Toast.LENGTH_SHORT).show();
                return;
            }

            BackendClient.sendAiMessage(requireContext(), userMessage, new BackendClient.ObjectCallback() {
                @Override
                public void onSuccess(org.json.JSONObject data) {
                    if (getActivity() == null) return;
                    getActivity().runOnUiThread(() -> {
                        String reply = data.optString("reply", "Thanks for sharing.");
                        addChatLine("You: " + userMessage);
                        addChatLine("Support: " + reply);
                        etAiMessage.setText("");
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

    private void addChatLine(String text) {
        TextView tv = new TextView(requireContext());
        tv.setText(text);
        tv.setTextSize(15f);
        tv.setPadding(0, 8, 0, 8);
        messagesContainer.addView(tv);
    }
}
