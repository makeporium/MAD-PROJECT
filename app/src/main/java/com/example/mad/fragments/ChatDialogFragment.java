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

import com.example.mad.R;
import com.example.mad.network.BackendClient;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import org.json.JSONArray;
import org.json.JSONObject;

public class ChatDialogFragment extends BottomSheetDialogFragment {

    private long roomId;
    private String title;
    private String description;
    private LinearLayout messagesContainer;
    private EditText etChatMessage;

    public static ChatDialogFragment newInstance(long roomId, String title, String description) {
        ChatDialogFragment fragment = new ChatDialogFragment();
        Bundle args = new Bundle();
        args.putLong("roomId", roomId);
        args.putString("title", title);
        args.putString("description", description);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            roomId = getArguments().getLong("roomId", -1);
            title = getArguments().getString("title");
            description = getArguments().getString("description");
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.dialog_chatroom, container, false);
        messagesContainer = view.findViewById(R.id.messagesContainer);
        etChatMessage = view.findViewById(R.id.etChatMessage);

        ((TextView) view.findViewById(R.id.chatTitle)).setText(title);
        ((TextView) view.findViewById(R.id.chatDesc)).setText(description);

        loadMessages();

        view.findViewById(R.id.btnSendChat).setOnClickListener(v -> {
            String message = etChatMessage.getText().toString().trim();
            if (message.isEmpty()) {
                Toast.makeText(getContext(), "Type a message first", Toast.LENGTH_SHORT).show();
                return;
            }
            if (roomId <= 0) {
                Toast.makeText(getContext(), "Room is not available", Toast.LENGTH_SHORT).show();
                return;
            }
            BackendClient.sendRoomMessage(requireContext(), roomId, message, new BackendClient.SimpleCallback() {
                @Override
                public void onSuccess(String msg) {
                    if (getActivity() == null) return;
                    getActivity().runOnUiThread(() -> {
                        etChatMessage.setText("");
                        loadMessages();
                    });
                }

                @Override
                public void onError(String msg) {
                    if (getActivity() == null) return;
                    getActivity().runOnUiThread(() ->
                            Toast.makeText(getContext(), msg, Toast.LENGTH_LONG).show());
                }
            });
        });

        return view;
    }

    private void loadMessages() {
        if (roomId <= 0) return;
        BackendClient.getRoomMessages(requireContext(), roomId, new BackendClient.JsonCallback() {
            @Override
            public void onSuccess(JSONArray data) {
                if (getActivity() == null) return;
                getActivity().runOnUiThread(() -> renderMessages(data));
            }

            @Override
            public void onError(String message) {
                if (getActivity() == null) return;
                getActivity().runOnUiThread(() ->
                        Toast.makeText(getContext(), message, Toast.LENGTH_LONG).show());
            }
        });
    }

    private void renderMessages(JSONArray data) {
        messagesContainer.removeAllViews();
        for (int i = 0; i < data.length(); i++) {
            JSONObject item = data.optJSONObject(i);
            if (item == null) continue;
            String user = item.optString("user_name", "User");
            String msg = item.optString("message", "");

            TextView tv = new TextView(requireContext());
            tv.setText(user + ": " + msg);
            tv.setTextSize(14f);
            tv.setPadding(0, 12, 0, 12);
            messagesContainer.addView(tv);
        }
    }
}
