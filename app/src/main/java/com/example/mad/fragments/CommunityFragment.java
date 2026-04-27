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
import com.example.mad.network.BackendClient;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class CommunityFragment extends Fragment {
    private final Map<String, Long> roomIdsByName = new HashMap<>();

    // Auto-refresh rooms list every 5 seconds
    private final android.os.Handler pollHandler = new android.os.Handler(android.os.Looper.getMainLooper());
    private final Runnable pollRunnable = new Runnable() {
        @Override
        public void run() {
            loadRooms();
            pollHandler.postDelayed(this, 5000);
        }
    };

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
        loadRooms();
        
        // Start polling
        pollHandler.postDelayed(pollRunnable, 5000);

        view.findViewById(R.id.cardSuggestTopic).setOnClickListener(v -> showCreateRoomDialog());
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // Stop polling when fragment is destroyed
        pollHandler.removeCallbacks(pollRunnable);
    }

    private void showCreateRoomDialog() {
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(getContext());
        builder.setTitle("Create New Chatroom");

        android.widget.LinearLayout layout = new android.widget.LinearLayout(getContext());
        layout.setOrientation(android.widget.LinearLayout.VERTICAL);
        layout.setPadding(40, 20, 40, 20);

        final android.widget.EditText nameInput = new android.widget.EditText(getContext());
        nameInput.setHint("Topic Name");
        layout.addView(nameInput);

        final android.widget.EditText descInput = new android.widget.EditText(getContext());
        descInput.setHint("Topic Description");
        layout.addView(descInput);

        builder.setView(layout);
        builder.setPositiveButton("Create", (dialog, which) -> {
            String name = nameInput.getText().toString();
            String desc = descInput.getText().toString();
            if (!name.isEmpty() && !desc.isEmpty()) {
                BackendClient.createCommunityRoom(requireContext(), name, desc, new BackendClient.SimpleCallback() {
                    @Override
                    public void onSuccess(String message) {
                        if (getActivity() == null) return;
                        getActivity().runOnUiThread(() -> {
                            Toast.makeText(getContext(), "Room created", Toast.LENGTH_SHORT).show();
                            loadRooms();
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
        });
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());
        builder.show();
    }

    private void loadRooms() {
        BackendClient.getCommunityRooms(requireContext(), new BackendClient.JsonCallback() {
            @Override
            public void onSuccess(JSONArray data) {
                if (getActivity() == null) return;
                getActivity().runOnUiThread(() -> {
                    android.widget.LinearLayout container = getView().findViewById(R.id.roomsContainer);
                    if (container == null) return;
                    container.removeAllViews();
                    roomIdsByName.clear();

                    for (int i = 0; i < data.length(); i++) {
                        JSONObject room = data.optJSONObject(i);
                        if (room == null) continue;

                        long roomId = room.optLong("id", -1);
                        String name = room.optString("name", "");
                        String description = room.optString("description", "");
                        int count = room.optInt("participants_count", 0);
                        String lastMsg = room.optString("last_message", "No messages yet");
                        String lastTime = room.optString("last_message_at", "");
                        if (lastTime.length() > 10) {
                            lastTime = lastTime.substring(11, 16); // basic time extraction
                        }
                        roomIdsByName.put(name, roomId);

                        View roomView = LayoutInflater.from(getContext()).inflate(R.layout.item_chatroom, container, false);
                        ((android.widget.TextView) roomView.findViewById(R.id.tvRoomName)).setText(name);
                        ((android.widget.TextView) roomView.findViewById(R.id.tvRoomDesc)).setText(description);
                        ((android.widget.TextView) roomView.findViewById(R.id.tvParticipants)).setText("👥 " + count);
                        ((android.widget.TextView) roomView.findViewById(R.id.tvLastMessage)).setText(lastMsg.isEmpty() || lastMsg.equals("null") ? "No messages yet" : "\"" + lastMsg + "\"");
                        ((android.widget.TextView) roomView.findViewById(R.id.tvLastMessageTime)).setText(lastTime);

                        roomView.setOnClickListener(v -> showChat(roomId, name, description));
                        roomView.setOnLongClickListener(v -> {
                            showDeleteRoomDialog(roomId, name);
                            return true;
                        });
                        container.addView(roomView);
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

    private void showDeleteRoomDialog(long roomId, String name) {
        new android.app.AlertDialog.Builder(getContext())
                .setTitle("Delete Topic")
                .setMessage("Are you sure you want to delete '" + name + "'? Only the creator or an expert can do this.")
                .setPositiveButton("Delete", (dialog, which) -> {
                    BackendClient.deleteCommunityRoom(requireContext(), roomId, new BackendClient.SimpleCallback() {
                        @Override
                        public void onSuccess(String message) {
                            if (getActivity() == null) return;
                            getActivity().runOnUiThread(() -> {
                                Toast.makeText(getContext(), "Room deleted", Toast.LENGTH_SHORT).show();
                                loadRooms();
                            });
                        }

                        @Override
                        public void onError(String message) {
                            if (getActivity() == null) return;
                            getActivity().runOnUiThread(() ->
                                    Toast.makeText(getContext(), message, Toast.LENGTH_LONG).show());
                        }
                    });
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showChat(long roomId, String title, String desc) {
        ChatDialogFragment.newInstance(roomId, title, desc)
                .show(getChildFragmentManager(), "chat_dialog");
    }
}
