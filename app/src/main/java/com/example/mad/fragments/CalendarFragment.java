package com.example.mad.fragments;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.mad.MainActivity;
import com.example.mad.R;
import com.example.mad.network.BackendClient;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class CalendarFragment extends Fragment {
    private View rootView;
    private boolean moodDoneToday = false;
    private final android.os.Handler pollHandler = new android.os.Handler(android.os.Looper.getMainLooper());
    private final Runnable pollRunnable = this::loadCalendarData;

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
        rootView = view;

        MainActivity activity = (MainActivity) getActivity();
        if (activity == null) return;

        view.findViewById(R.id.cardDailyMood).setOnClickListener(v -> 
                activity.loadFragment(new MoodCheckinFragment()));

        view.findViewById(R.id.btnAddReminder).setOnClickListener(v -> showAddReminderDialog());
        loadCalendarData();
        pollHandler.postDelayed(pollRunnable, 8000);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        pollHandler.removeCallbacks(pollRunnable);
    }

    private void showAddReminderDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("Add Reminder");

        LinearLayout layout = new LinearLayout(requireContext());
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(40, 20, 40, 20);

        EditText etTitle = new EditText(requireContext());
        etTitle.setHint("Reminder title");
        etTitle.setText("Daily mood check-in");
        layout.addView(etTitle);

        EditText etDateTime = new EditText(requireContext());
        etDateTime.setHint("Select date & time");
        etDateTime.setFocusable(false);
        etDateTime.setClickable(true);
        etDateTime.setLongClickable(false);
        layout.addView(etDateTime);

        etDateTime.setOnClickListener(v -> showDateTimePicker(etDateTime));
        prefillNextHour(etDateTime);

        builder.setView(layout);
        builder.setPositiveButton("Save", (dialog, which) -> {
            String title = etTitle.getText().toString().trim();
            String remindAt = etDateTime.getText().toString().trim();

            if (title.isEmpty() || remindAt.isEmpty()) {
                if (getActivity() == null) return;
                getActivity().runOnUiThread(() ->
                        Toast.makeText(getContext(), "Please select title and date/time", Toast.LENGTH_SHORT).show());
                return;
            }

            BackendClient.createQuickReminder(requireContext(), title, remindAt, new BackendClient.SimpleCallback() {
                @Override
                public void onSuccess(String message) {
                    if (getActivity() == null) return;
                    getActivity().runOnUiThread(() -> {
                        Toast.makeText(getContext(), "Reminder added", Toast.LENGTH_SHORT).show();
                        loadCalendarData();
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
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private void prefillNextHour(EditText target) {
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.HOUR_OF_DAY, 1);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US);
        target.setText(formatter.format(calendar.getTime()));
    }

    private void showDateTimePicker(EditText target) {
        Calendar now = Calendar.getInstance();
        DatePickerDialog datePickerDialog = new DatePickerDialog(
                requireContext(),
                (view, year, month, dayOfMonth) -> {
                    Calendar selected = Calendar.getInstance();
                    selected.set(Calendar.YEAR, year);
                    selected.set(Calendar.MONTH, month);
                    selected.set(Calendar.DAY_OF_MONTH, dayOfMonth);

                    new android.app.TimePickerDialog(
                            requireContext(),
                            (timeView, hourOfDay, minute) -> {
                                selected.set(Calendar.HOUR_OF_DAY, hourOfDay);
                                selected.set(Calendar.MINUTE, minute);
                                selected.set(Calendar.SECOND, 0);
                                selected.set(Calendar.MILLISECOND, 0);
                                SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US);
                                target.setText(formatter.format(selected.getTime()));
                            },
                            now.get(Calendar.HOUR_OF_DAY),
                            now.get(Calendar.MINUTE),
                            false
                    ).show();
                },
                now.get(Calendar.YEAR),
                now.get(Calendar.MONTH),
                now.get(Calendar.DAY_OF_MONTH)
        );
        datePickerDialog.getDatePicker().setMinDate(System.currentTimeMillis() - 1000L);
        datePickerDialog.show();
    }

    private void loadCalendarData() {
        loadUpcomingEntries();
        loadProgressAndAffirmation();
        pollHandler.removeCallbacks(pollRunnable);
        pollHandler.postDelayed(pollRunnable, 8000);
    }

    private void loadProgressAndAffirmation() {
        BackendClient.getCalendarProgress(requireContext(), new BackendClient.ObjectCallback() {
            @Override
            public void onSuccess(JSONObject data) {
                if (getActivity() == null || rootView == null) return;
                getActivity().runOnUiThread(() -> {
                    setTextSafe(R.id.tvTherapySessions, String.valueOf(data.optInt("therapy_sessions_this_month", 0)));
                    setTextSafe(R.id.tvCommunitySessions, String.valueOf(data.optInt("community_sessions_joined", 0)));
                    setTextSafe(R.id.tvMoodCheckins, String.valueOf(data.optInt("mood_checkins_this_month", 0)));
                    JSONObject affirmation = data.optJSONObject("affirmation");
                    String text = "You're showing up for yourself. That's beautiful.";
                    if (affirmation != null) {
                        String content = affirmation.optString("content", "");
                        String author = affirmation.optString("user_name", "");
                        if (!content.isEmpty()) {
                            text = "\"" + content + "\"";
                            if (!author.isEmpty()) text += " — " + author;
                        }
                    }
                    setTextSafe(R.id.tvAffirmation, text);

                    // Update instance field so renderUpcoming() can re-apply it after removeAllViews()
                    moodDoneToday = data.optBoolean("mood_done_today", false);
                    renderMoodTodayCard(moodDoneToday);
                });
            }

            @Override
            public void onError(String message) {
                // Keep last values on UI if request fails.
            }
        });
    }

    private void renderMoodTodayCard(boolean done) {
        if (rootView == null) return;
        // Update the fixed cardDailyMood card in the layout — no upcomingContainer insert needed
        android.widget.TextView tvSubtitle = rootView.findViewById(R.id.tvMoodCardSubtitle);
        android.widget.TextView tvTime     = rootView.findViewById(R.id.tvMoodCardTime);
        android.widget.TextView tvBadge    = rootView.findViewById(R.id.tvMoodDoneBadge);
        if (done) {
            if (tvSubtitle != null) tvSubtitle.setText("Great job checking in today! ✨");
            if (tvTime     != null) tvTime.setText("📊 Today's entry logged");
            if (tvBadge    != null) tvBadge.setVisibility(View.VISIBLE);
        } else {
            if (tvSubtitle != null) tvSubtitle.setText("Take a moment to reflect");
            if (tvTime     != null) tvTime.setText("🕒 Anytime");
            if (tvBadge    != null) tvBadge.setVisibility(View.GONE);
        }
    }

    private void loadUpcomingEntries() {
        BackendClient.getEvents(requireContext(), new BackendClient.JsonCallback() {
            @Override
            public void onSuccess(JSONArray events) {
                BackendClient.getReminders(requireContext(), new BackendClient.JsonCallback() {
                    @Override
                    public void onSuccess(JSONArray reminders) {
                        if (getActivity() == null || rootView == null) return;
                        getActivity().runOnUiThread(() -> renderUpcoming(events, reminders));
                    }

                    @Override
                    public void onError(String message) {
                        if (getActivity() == null || rootView == null) return;
                        getActivity().runOnUiThread(() -> renderUpcoming(events, new JSONArray()));
                    }
                });
            }

            @Override
            public void onError(String message) {
                if (getActivity() == null || rootView == null) return;
                getActivity().runOnUiThread(() -> {
                    LinearLayout container = rootView.findViewById(R.id.upcomingContainer);
                    if (container != null) container.removeAllViews();
                });
            }
        });
    }

    private void renderUpcoming(JSONArray events, JSONArray reminders) {
        if (rootView == null) return;
        LinearLayout container = rootView.findViewById(R.id.upcomingContainer);
        if (container == null) return;
        container.removeAllViews();

        List<JSONObject> entries = new ArrayList<>();
        long now = System.currentTimeMillis();

        for (int i = 0; i < events.length(); i++) {
            JSONObject event = events.optJSONObject(i);
            if (event == null) continue;
            boolean isBooked = event.optInt("is_booked", 0) == 1 || event.optBoolean("is_booked", false);
            if (!isBooked) continue;
            String eventDate = event.optString("event_date", "");
            Date parsed = parseBackendDate(eventDate);
            if (parsed == null) continue;
            // Show booked events that are today or in the future
            Calendar evtCal = Calendar.getInstance();
            evtCal.setTime(parsed);
            Calendar todayCal = Calendar.getInstance();
            boolean eventIsToday   = isSameDay(evtCal, todayCal);
            boolean eventIsFuture  = parsed.getTime() > now;
            if (!eventIsToday && !eventIsFuture) continue;
            try {
                JSONObject e = new JSONObject();
                e.put("type", "Session");
                e.put("title", event.optString("title", "Booked session"));
                e.put("when", eventDate);
                e.put("ts", parsed.getTime());
                entries.add(e);
            } catch (Exception ignored) {}
        }

        for (int i = 0; i < reminders.length(); i++) {
            JSONObject reminder = reminders.optJSONObject(i);
            if (reminder == null) continue;
            String remindAt = reminder.optString("remind_at", "");
            Date parsed = parseBackendDate(remindAt);
            // Include future reminders AND reminders from today (even if earlier today)
            if (parsed == null) continue;
            Calendar reminderCal = Calendar.getInstance();
            reminderCal.setTime(parsed);
            Calendar nowCal = Calendar.getInstance();
            boolean isToday = isSameDay(reminderCal, nowCal);
            boolean isFuture = parsed.getTime() > now;
            if (!isToday && !isFuture) continue;
            try {
                JSONObject e = new JSONObject();
                e.put("type", "Reminder");
                e.put("title", reminder.optString("title", "Reminder"));
                e.put("when", remindAt);
                e.put("ts", parsed.getTime());
                e.put("reminder_id", reminder.optLong("id", -1));
                entries.add(e);
            } catch (Exception ignored) {}
        }

        entries.sort(Comparator.comparingLong(o -> o.optLong("ts", Long.MAX_VALUE)));

        LayoutInflater inflater = LayoutInflater.from(getContext());
        int maxItems = Math.min(entries.size(), 8);
        for (int i = 0; i < maxItems; i++) {
            JSONObject item = entries.get(i);
            View itemView = inflater.inflate(R.layout.item_calendar_entry, container, false);
            android.widget.TextView tvType  = itemView.findViewById(R.id.tvEntryType);
            android.widget.TextView tvTitle = itemView.findViewById(R.id.tvEntryTitle);
            android.widget.TextView tvMeta  = itemView.findViewById(R.id.tvEntryMeta);
            android.view.ViewGroup llActions = itemView.findViewById(R.id.llReminderActions);
            String when = item.optString("when");
            tvType.setText(item.optString("type").toUpperCase(Locale.US));
            tvTitle.setText(item.optString("title"));
            tvMeta.setText(formatFriendlyDate(when));

            // Show edit/delete only for reminders
            boolean isReminder = "Reminder".equals(item.optString("type"));
            long reminderId = item.optLong("reminder_id", -1);
            if (isReminder && reminderId > 0 && llActions != null) {
                llActions.setVisibility(View.VISIBLE);
                android.widget.Button btnEdit   = itemView.findViewById(R.id.btnEditEntry);
                android.widget.Button btnDelete = itemView.findViewById(R.id.btnDeleteEntry);
                btnEdit.setOnClickListener(v -> showEditReminderDialog(reminderId, item.optString("title"), when));
                btnDelete.setOnClickListener(v -> confirmDeleteReminder(reminderId));
            }

            container.addView(itemView);
        }

        if (entries.isEmpty()) {
            View itemView = inflater.inflate(R.layout.item_calendar_entry, container, false);
            ((android.widget.TextView) itemView.findViewById(R.id.tvEntryType)).setText("UPCOMING");
            ((android.widget.TextView) itemView.findViewById(R.id.tvEntryTitle)).setText("No booked sessions or reminders yet");
            ((android.widget.TextView) itemView.findViewById(R.id.tvEntryMeta)).setText("Book a session or add a reminder");
            container.addView(itemView);
        }

        // Re-apply mood card state after removeAllViews() — eliminates race condition
        renderMoodTodayCard(moodDoneToday);
    }

    private Date parseBackendDate(String dateTime) {
        if (dateTime == null || dateTime.trim().isEmpty()) return null;
        String value = dateTime.trim().replace("T", " ");
        if (value.length() >= 19) value = value.substring(0, 19);
        try {
            return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).parse(value);
        } catch (ParseException e) {
            return null;
        }
    }

    private String formatFriendlyDate(String dateTime) {
        Date parsed = parseBackendDate(dateTime);
        if (parsed == null) return dateTime;
        Calendar now = Calendar.getInstance();
        Calendar target = Calendar.getInstance();
        target.setTime(parsed);
        String prefix = isSameDay(now, target) ? "Today" : new SimpleDateFormat("dd MMM yyyy", Locale.US).format(parsed);
        return prefix + "  •  " + new SimpleDateFormat("hh:mm a", Locale.US).format(parsed);
    }

    private boolean isSameDay(Calendar a, Calendar b) {
        return a.get(Calendar.YEAR) == b.get(Calendar.YEAR)
                && a.get(Calendar.DAY_OF_YEAR) == b.get(Calendar.DAY_OF_YEAR);
    }

    private void setTextSafe(int id, String value) {
        if (rootView == null) return;
        android.widget.TextView tv = rootView.findViewById(id);
        if (tv != null) tv.setText(value);
    }

    private void showEditReminderDialog(long reminderId, String currentTitle, String currentRemindAt) {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("Edit Reminder");

        LinearLayout layout = new LinearLayout(requireContext());
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(40, 20, 40, 20);

        EditText etTitle = new EditText(requireContext());
        etTitle.setHint("Reminder title");
        etTitle.setText(currentTitle);
        layout.addView(etTitle);

        EditText etDateTime = new EditText(requireContext());
        etDateTime.setHint("Select date & time");
        etDateTime.setFocusable(false);
        etDateTime.setClickable(true);
        etDateTime.setLongClickable(false);
        etDateTime.setText(currentRemindAt);
        layout.addView(etDateTime);
        etDateTime.setOnClickListener(v -> showDateTimePicker(etDateTime));

        builder.setView(layout);
        builder.setPositiveButton("Save", (dialog, which) -> {
            String newTitle = etTitle.getText().toString().trim();
            String newTime  = etDateTime.getText().toString().trim();
            if (newTitle.isEmpty() || newTime.isEmpty()) {
                Toast.makeText(getContext(), "Title and time required", Toast.LENGTH_SHORT).show();
                return;
            }
            BackendClient.updateReminder(requireContext(), reminderId, newTitle, newTime, new BackendClient.SimpleCallback() {
                @Override public void onSuccess(String msg) {
                    if (getActivity() == null) return;
                    getActivity().runOnUiThread(() -> {
                        Toast.makeText(getContext(), "Reminder updated", Toast.LENGTH_SHORT).show();
                        loadCalendarData();
                    });
                }
                @Override public void onError(String msg) {
                    if (getActivity() == null) return;
                    getActivity().runOnUiThread(() -> Toast.makeText(getContext(), msg, Toast.LENGTH_LONG).show());
                }
            });
        });
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private void confirmDeleteReminder(long reminderId) {
        new AlertDialog.Builder(requireContext())
                .setTitle("Delete Reminder")
                .setMessage("Remove this reminder?")
                .setPositiveButton("Delete", (dialog, which) ->
                        BackendClient.deleteReminder(requireContext(), reminderId, new BackendClient.SimpleCallback() {
                            @Override public void onSuccess(String msg) {
                                if (getActivity() == null) return;
                                getActivity().runOnUiThread(() -> {
                                    Toast.makeText(getContext(), "Reminder deleted", Toast.LENGTH_SHORT).show();
                                    loadCalendarData();
                                });
                            }
                            @Override public void onError(String msg) {
                                if (getActivity() == null) return;
                                getActivity().runOnUiThread(() -> Toast.makeText(getContext(), msg, Toast.LENGTH_LONG).show());
                            }
                        }))
                .setNegativeButton("Cancel", null)
                .show();
    }
}
