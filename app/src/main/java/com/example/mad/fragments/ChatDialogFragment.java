package com.example.mad.fragments;

import android.app.Dialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.mad.R;
import com.example.mad.network.BackendClient;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Full-height bottom-sheet chatroom.
 * – Swipe down to dismiss (built-in BottomSheet behaviour).
 * – Polls for new messages every 3 seconds while open.
 */
public class ChatDialogFragment extends BottomSheetDialogFragment {

    private long roomId;
    private String title;
    private String description;
    private LinearLayout messagesContainer;
    private ScrollView scrollView;
    private EditText etChatMessage;
    private CheckBox cbAnonymous;

    // ── Real-time polling ─────────────────────────────────────────────────────
    private final Handler pollHandler = new Handler(Looper.getMainLooper());
    private final Runnable pollRunnable = new Runnable() {
        @Override
        public void run() {
            loadMessages(false);           // silent refresh (no scroll-to-bottom on poll)
            pollHandler.postDelayed(this, 3000);
        }
    };

    // ── Factory ───────────────────────────────────────────────────────────────

    public static ChatDialogFragment newInstance(long roomId, String title, String description) {
        ChatDialogFragment f = new ChatDialogFragment();
        Bundle args = new Bundle();
        args.putLong("roomId", roomId);
        args.putString("title", title);
        args.putString("description", description);
        f.setArguments(args);
        return f;
    }

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            roomId      = getArguments().getLong("roomId", -1);
            title       = getArguments().getString("title", "Chat");
            description = getArguments().getString("description", "");
        }
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        BottomSheetDialog dialog = (BottomSheetDialog) super.onCreateDialog(savedInstanceState);
        dialog.setOnShowListener(d -> {
            BottomSheetDialog bsd = (BottomSheetDialog) d;
            View sheet = bsd.findViewById(com.google.android.material.R.id.design_bottom_sheet);
            if (sheet != null) {
                // Expand to full height and skip the half-expanded state
                BottomSheetBehavior<View> behavior = BottomSheetBehavior.from(sheet);
                sheet.getLayoutParams().height = ViewGroup.LayoutParams.MATCH_PARENT;
                behavior.setSkipCollapsed(true);
                behavior.setState(BottomSheetBehavior.STATE_EXPANDED);
                // Allow swipe down to dismiss
                behavior.setHideable(true);
            }
        });
        return dialog;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.dialog_chatroom, container, false);

        messagesContainer = view.findViewById(R.id.messagesContainer);
        scrollView        = view.findViewById(R.id.scrollView);
        etChatMessage     = view.findViewById(R.id.etChatMessage);
        cbAnonymous       = view.findViewById(R.id.cbAnonymous);

        ((TextView) view.findViewById(R.id.chatTitle)).setText(title);
        ((TextView) view.findViewById(R.id.chatDesc)).setText(description);

        // Send
        view.findViewById(R.id.btnSendChat).setOnClickListener(v -> sendMessage());

        // First load (scroll to bottom afterwards)
        loadMessages(true);

        // Start polling
        pollHandler.postDelayed(pollRunnable, 3000);

        return view;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        pollHandler.removeCallbacks(pollRunnable);
    }

    // ── Message I/O ───────────────────────────────────────────────────────────

    private void sendMessage() {
        String message = etChatMessage.getText().toString().trim();
        if (message.isEmpty()) {
            Toast.makeText(getContext(), "Type a message first", Toast.LENGTH_SHORT).show();
            return;
        }
        if (roomId <= 0) {
            Toast.makeText(getContext(), "Room unavailable", Toast.LENGTH_SHORT).show();
            return;
        }
        boolean isAnonymous = cbAnonymous.isChecked();
        BackendClient.sendRoomMessage(requireContext(), roomId, message, isAnonymous,
                new BackendClient.SimpleCallback() {
                    @Override public void onSuccess(String msg) {
                        if (getActivity() == null) return;
                        getActivity().runOnUiThread(() -> {
                            etChatMessage.setText("");
                            loadMessages(true);   // scroll to bottom after own send
                        });
                    }
                    @Override public void onError(String msg) {
                        if (getActivity() == null) return;
                        getActivity().runOnUiThread(() ->
                                Toast.makeText(getContext(), msg, Toast.LENGTH_LONG).show());
                    }
                });
    }

    private void loadMessages(boolean scrollToBottom) {
        if (roomId <= 0 || getContext() == null) return;
        BackendClient.getRoomMessages(requireContext(), roomId, new BackendClient.JsonCallback() {
            @Override public void onSuccess(JSONArray data) {
                if (getActivity() == null || messagesContainer == null) return;
                getActivity().runOnUiThread(() -> renderMessages(data, scrollToBottom));
            }
            @Override public void onError(String message) { /* silent */ }
        });
    }

    private void renderMessages(JSONArray data, boolean scrollToBottom) {
        // Track currently visible message count to decide whether to scroll
        int prevCount = messagesContainer.getChildCount();
        boolean newMessages = data.length() > prevCount;

        messagesContainer.removeAllViews();
        for (int i = 0; i < data.length(); i++) {
            JSONObject item = data.optJSONObject(i);
            if (item == null) continue;

            String  user   = item.optString("user_name", "User");
            String  msg    = item.optString("message", "");
            long    userId = item.optLong("user_id", -1);
            boolean isAnon = item.optInt("is_anonymous", 0) == 1
                          || item.optBoolean("is_anonymous", false);

            // Build message bubble card
            LinearLayout bubble = buildBubble(user, msg, isAnon);

            if (!isAnon && userId > 0) {
                final long uid = userId;
                bubble.setOnClickListener(v -> {
                    if (getActivity() instanceof com.example.mad.MainActivity) {
                        dismiss();
                        ((com.example.mad.MainActivity) getActivity())
                                .openOtherProfileDrawer(user, uid);
                    }
                });
            }
            messagesContainer.addView(bubble);
        }

        // Scroll to bottom when: explicitly requested, or new messages arrived
        if (scrollToBottom || newMessages) {
            scrollView.post(() -> scrollView.fullScroll(ScrollView.FOCUS_DOWN));
        }
    }

    /** Creates a styled message bubble. */
    private LinearLayout buildBubble(String user, String msg, boolean isAnon) {
        LinearLayout card = new LinearLayout(requireContext());
        card.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams cardLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        cardLp.setMargins(0, 0, 0, dpToPx(10));
        card.setLayoutParams(cardLp);
        card.setBackgroundColor(Color.WHITE);
        card.setPadding(dpToPx(14), dpToPx(12), dpToPx(14), dpToPx(12));

        // Try to set rounded corners programmatically via a simple shape
        android.graphics.drawable.GradientDrawable bg = new android.graphics.drawable.GradientDrawable();
        bg.setColor(Color.WHITE);
        bg.setCornerRadius(dpToPx(12));
        card.setBackground(bg);

        // Author name row
        TextView tvName = new TextView(requireContext());
        tvName.setText(isAnon ? "🔒 Anonymous" : "👤 " + user);
        tvName.setTextSize(12f);
        tvName.setTextColor(isAnon ? Color.parseColor("#9E9E9E") : Color.parseColor("#C1440E"));
        tvName.setTypeface(null, android.graphics.Typeface.BOLD);
        card.addView(tvName);

        // Message text
        TextView tvMsg = new TextView(requireContext());
        tvMsg.setText(msg);
        tvMsg.setTextSize(14f);
        tvMsg.setTextColor(Color.parseColor("#333333"));
        LinearLayout.LayoutParams msgLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        msgLp.setMargins(0, dpToPx(4), 0, 0);
        tvMsg.setLayoutParams(msgLp);
        card.addView(tvMsg);

        return card;
    }

    private int dpToPx(int dp) {
        return Math.round(dp * getResources().getDisplayMetrics().density);
    }
}
