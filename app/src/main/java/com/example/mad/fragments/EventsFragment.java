package com.example.mad.fragments;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.mad.MainActivity;
import com.example.mad.R;
import com.example.mad.network.BackendClient;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class EventsFragment extends Fragment {
    private LinearLayout llUpcomingSessions;
    private LinearLayout llProfessionalSupport;
    private Switch switchExpertMode;
    private Button btnAddSession;
    private Button btnUpdateProfile;
    private boolean isExpertMode = false;
    private JSONArray latestEvents = new JSONArray();
    private final android.os.Handler pollHandler = new android.os.Handler(android.os.Looper.getMainLooper());
    private final Runnable pollRunnable = this::refreshData;

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
        
        llUpcomingSessions = view.findViewById(R.id.llUpcomingSessions);
        llProfessionalSupport = view.findViewById(R.id.llProfessionalSupport);
        switchExpertMode = view.findViewById(R.id.switchExpertMode);
        btnAddSession = view.findViewById(R.id.btnAddSession);
        btnUpdateProfile = view.findViewById(R.id.btnUpdateProfile);

        if (getActivity() instanceof MainActivity) {
            String role = ((MainActivity) getActivity()).getUserRole();
            if ("expert".equals(role)) {
                switchExpertMode.setVisibility(View.VISIBLE);
                btnAddSession.setVisibility(View.VISIBLE);
                btnUpdateProfile.setVisibility(View.VISIBLE);
            }
        }

        switchExpertMode.setOnCheckedChangeListener((buttonView, isChecked) -> {
            isExpertMode = isChecked;
            loadEvents();
        });

        btnAddSession.setOnClickListener(v -> showAddSessionDialog());
        btnUpdateProfile.setOnClickListener(v -> showUpdateProfileDialog());

        loadEvents();
        loadExperts();
        pollHandler.postDelayed(pollRunnable, 8000);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        pollHandler.removeCallbacks(pollRunnable);
    }

    private void refreshData() {
        loadEvents();
        loadExperts();
        pollHandler.removeCallbacks(pollRunnable);
        pollHandler.postDelayed(pollRunnable, 8000);
    }

    private void loadEvents() {
        llUpcomingSessions.removeAllViews();
        BackendClient.JsonCallback callback = new BackendClient.JsonCallback() {
            @Override
            public void onSuccess(JSONArray data) {
                latestEvents = data;
                if (getActivity() == null) return;
                getActivity().runOnUiThread(() -> {
                    LayoutInflater inflater = LayoutInflater.from(getContext());
                    for (int i = 0; i < data.length(); i++) {
                        JSONObject event = data.optJSONObject(i);
                        if (event == null) continue;
                        
                        View itemView = inflater.inflate(R.layout.item_event, llUpcomingSessions, false);
                        TextView tvTitle = itemView.findViewById(R.id.tvTitle);
                        TextView tvDesc = itemView.findViewById(R.id.tvDescription);
                        TextView tvDate = itemView.findViewById(R.id.tvDate);
                        TextView tvMode = itemView.findViewById(R.id.tvMode);
                        TextView tvBookingCount = itemView.findViewById(R.id.tvBookingCount);
                        TextView tvBookings = itemView.findViewById(R.id.tvBookings);
                        Button btnAction = itemView.findViewById(R.id.btnAction);

                        tvTitle.setText(event.optString("title"));
                        tvDesc.setText(event.optString("description"));
                        tvMode.setText(event.optString("mode", "Online").toUpperCase());

                        // Format date as "28 Apr  •  06:30 PM"
                        String rawDate = event.optString("event_date", "");
                        tvDate.setText("🕐 " + formatEventDate(rawDate));

                        // Booking count — visible to everyone
                        int count = event.optInt("booking_count", 0);
                        tvBookingCount.setText("👥 " + count + (count == 1 ? " person booked" : " people booked"));

                        if (isExpertMode) {
                            tvBookings.setVisibility(View.VISIBLE);
                            JSONArray bookings = event.optJSONArray("bookings");
                            if (bookings != null && bookings.length() > 0) {
                                StringBuilder sb = new StringBuilder("Bookings:\n");
                                for (int j = 0; j < bookings.length(); j++) {
                                    JSONObject b = bookings.optJSONObject(j);
                                    if (b != null) {
                                        sb.append("- ").append(b.optString("name")).append("\n");
                                    }
                                }
                                tvBookings.setText(sb.toString().trim());
                            } else {
                                tvBookings.setText("No bookings yet.");
                            }
                            btnAction.setText("Start Session");
                            btnAction.setOnClickListener(v -> {
                                String joinLink = event.optString("join_link", "");
                                if (!joinLink.isEmpty()) {
                                    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(joinLink)));
                                } else {
                                    Toast.makeText(getContext(), "No link available", Toast.LENGTH_SHORT).show();
                                }
                            });
                        } else {
                            boolean isBooked = event.optInt("is_booked", 0) == 1 || event.optBoolean("is_booked", false);
                            btnAction.setText(isBooked ? "Join" : "Book");
                            btnAction.setOnClickListener(v -> {
                                boolean bookedNow = event.optInt("is_booked", 0) == 1 || event.optBoolean("is_booked", false);
                                if (bookedNow) {
                                    String joinLink = event.optString("join_link", "");
                                    if (!joinLink.isEmpty()) {
                                        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(joinLink)));
                                    } else {
                                        Toast.makeText(getContext(), "No link available", Toast.LENGTH_SHORT).show();
                                    }
                                    return;
                                }

                                BackendClient.bookSession(requireContext(), event.optLong("id"), new BackendClient.SimpleCallback() {
                                    @Override
                                    public void onSuccess(String message) {
                                        if (getActivity() == null) return;
                                        getActivity().runOnUiThread(() -> {
                                            event.remove("is_booked");
                                            try { event.put("is_booked", 1); } catch (Exception ignored) {}
                                            btnAction.setText("Join");
                                            Toast.makeText(getContext(), "Session booked successfully", Toast.LENGTH_SHORT).show();
                                            refreshData();
                                        });
                                    }
                                    @Override
                                    public void onError(String message) {
                                        if (getActivity() == null) return;
                                        getActivity().runOnUiThread(() -> {
                                            if (message.contains("Already booked")) {
                                                event.remove("is_booked");
                                                try { event.put("is_booked", 1); } catch (Exception ignored) {}
                                                btnAction.setText("Join");
                                                Toast.makeText(getContext(), "Already booked. You can join now.", Toast.LENGTH_SHORT).show();
                                            } else {
                                                Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
                                            }
                                        });
                                    }
                                });
                            });
                        }
                        llUpcomingSessions.addView(itemView);
                    }
                });
            }

            @Override
            public void onError(String message) {
                if (getActivity() == null) return;
                getActivity().runOnUiThread(() ->
                        Toast.makeText(getContext(), message, Toast.LENGTH_LONG).show());
            }
        };

        if (isExpertMode) {
            BackendClient.getMySessions(requireContext(), callback);
        } else {
            BackendClient.getEvents(requireContext(), callback);
        }
    }

    private void loadExperts() {
        llProfessionalSupport.removeAllViews();
        BackendClient.getExpertProfiles(requireContext(), new BackendClient.JsonCallback() {
            @Override
            public void onSuccess(JSONArray data) {
                if (getActivity() == null) return;
                getActivity().runOnUiThread(() -> {
                    LayoutInflater inflater = LayoutInflater.from(getContext());
                    for (int i = 0; i < data.length(); i++) {
                        JSONObject expert = data.optJSONObject(i);
                        if (expert == null) continue;
                        
                        View itemView = inflater.inflate(R.layout.item_expert, llProfessionalSupport, false);
                        TextView tvInitials = itemView.findViewById(R.id.tvInitials);
                        TextView tvName = itemView.findViewById(R.id.tvName);
                        TextView tvSpecialty = itemView.findViewById(R.id.tvSpecialty);
                        TextView tvLocation = itemView.findViewById(R.id.tvLocation);
                        TextView tvFormat = itemView.findViewById(R.id.tvFormat);
                        TextView tvAvailability = itemView.findViewById(R.id.tvAvailability);
                        TextView tvFee = itemView.findViewById(R.id.tvFee);
                        Button btnBookExpert = itemView.findViewById(R.id.btnBookExpert);

                        String name = expert.optString("expert_name", "Expert");
                        tvName.setText(name);
                        if (name.length() > 0) tvInitials.setText(String.valueOf(name.charAt(0)));
                        tvSpecialty.setText(expert.optString("specialty", "Specialty"));
                        tvLocation.setText("📍 " + expert.optString("location", "Location"));
                        tvFormat.setText("📹 " + expert.optString("format", "Online"));
                        tvAvailability.setText("🕒 " + expert.optString("availability", "Available"));
                        tvFee.setText(expert.optString("fee", "₹0/session"));
                        boolean isExpertUser = getActivity() instanceof MainActivity
                                && "expert".equals(((MainActivity) getActivity()).getUserRole());
                        if (isExpertUser) {
                            btnBookExpert.setVisibility(View.GONE);
                        } else {
                            btnBookExpert.setVisibility(View.VISIBLE);
                            btnBookExpert.setOnClickListener(v -> bookFirstSessionForExpert(expert, btnBookExpert));
                        }

                        llProfessionalSupport.addView(itemView);
                    }
                });
            }

            @Override
            public void onError(String message) {}
        });
    }

    private void bookFirstSessionForExpert(JSONObject expert, Button btnBookExpert) {
        long expertId = expert.optLong("user_id", -1);
        if (expertId <= 0) {
            Toast.makeText(getContext(), "No sessions available for this expert", Toast.LENGTH_SHORT).show();
            return;
        }

        JSONObject targetEvent = null;
        long now = System.currentTimeMillis();
        for (int i = 0; i < latestEvents.length(); i++) {
            JSONObject event = latestEvents.optJSONObject(i);
            if (event == null) continue;
            if (event.optLong("expert_id", -1) != expertId) continue;
            long ts = parseBackendDateMillis(event.optString("event_date", ""));
            if (ts < now) continue;
            targetEvent = event;
            break;
        }

        if (targetEvent == null) {
            Toast.makeText(getContext(), "No upcoming session by this expert", Toast.LENGTH_SHORT).show();
            return;
        }

        boolean alreadyBooked = targetEvent.optInt("is_booked", 0) == 1 || targetEvent.optBoolean("is_booked", false);
        if (alreadyBooked) {
            Toast.makeText(getContext(), "You already booked a session with this expert", Toast.LENGTH_SHORT).show();
            return;
        }

        long eventId = targetEvent.optLong("id", -1);
        if (eventId <= 0) return;
        BackendClient.bookSession(requireContext(), eventId, new BackendClient.SimpleCallback() {
            @Override
            public void onSuccess(String message) {
                if (getActivity() == null) return;
                getActivity().runOnUiThread(() -> {
                    btnBookExpert.setText("Booked");
                    btnBookExpert.setEnabled(false);
                    Toast.makeText(getContext(), "Session booked successfully", Toast.LENGTH_SHORT).show();
                    refreshData();
                });
            }

            @Override
            public void onError(String message) {
                if (getActivity() == null) return;
                getActivity().runOnUiThread(() -> Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show());
            }
        });
    }

    private String formatEventDate(String dateTime) {
        if (dateTime == null || dateTime.trim().isEmpty()) return "TBD";
        String value = dateTime.trim().replace("T", " ");
        if (value.length() >= 19) value = value.substring(0, 19);
        try {
            java.util.Date date = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).parse(value);
            if (date == null) return dateTime;
            SimpleDateFormat dayFmt  = new SimpleDateFormat("dd MMM", Locale.US);
            SimpleDateFormat timeFmt = new SimpleDateFormat("hh:mm a", Locale.US);
            return dayFmt.format(date) + "  \u2022  " + timeFmt.format(date);
        } catch (Exception e) {
            return dateTime;
        }
    }

    private long parseBackendDateMillis(String dateTime) {
        if (dateTime == null || dateTime.trim().isEmpty()) return Long.MIN_VALUE;
        String value = dateTime.trim().replace("T", " ");
        if (value.length() >= 19) value = value.substring(0, 19);
        try {
            return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).parse(value).getTime();
        } catch (Exception e) {
            return Long.MIN_VALUE;
        }
    }

    private void showAddSessionDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("Add Session");
        
        LinearLayout layout = new LinearLayout(getContext());
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(40, 20, 40, 20);

        EditText etTitle = new EditText(getContext());
        etTitle.setHint("Session Title");
        layout.addView(etTitle);

        EditText etDesc = new EditText(getContext());
        etDesc.setHint("Description");
        layout.addView(etDesc);

        EditText etDate = new EditText(getContext());
        etDate.setHint("Select date & time");
        etDate.setFocusable(false);
        etDate.setClickable(true);
        etDate.setLongClickable(false);
        layout.addView(etDate);

        etDate.setOnClickListener(v -> showDateTimePicker(etDate));

        EditText etLink = new EditText(getContext());
        etLink.setHint("Meet Link");
        layout.addView(etLink);

        builder.setView(layout);

        builder.setPositiveButton("Add", (dialog, which) -> {
            String title = etTitle.getText().toString();
            String desc = etDesc.getText().toString();
            String date = etDate.getText().toString();
            String link = etLink.getText().toString();
            if (!title.isEmpty() && !date.isEmpty()) {
                BackendClient.createSession(requireContext(), title, desc, date, link, new BackendClient.SimpleCallback() {
                    @Override
                    public void onSuccess(String message) {
                        if (getActivity() == null) return;
                        getActivity().runOnUiThread(() -> {
                            Toast.makeText(getContext(), "Session Added!", Toast.LENGTH_SHORT).show();
                            loadEvents();
                        });
                    }
                    @Override
                    public void onError(String message) {
                        if (getActivity() == null) return;
                        getActivity().runOnUiThread(() -> Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show());
                    }
                });
            }
        });
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private void showUpdateProfileDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("Update Expert Profile");
        
        LinearLayout layout = new LinearLayout(getContext());
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(40, 20, 40, 20);

        EditText etSpecialty = new EditText(getContext());
        etSpecialty.setHint("Specialty (e.g., Postpartum Depression)");
        layout.addView(etSpecialty);

        EditText etLocation = new EditText(getContext());
        etLocation.setHint("Location (e.g., Mumbai)");
        layout.addView(etLocation);

        EditText etFormat = new EditText(getContext());
        etFormat.setHint("Format (e.g., In-person & Teleconsult)");
        layout.addView(etFormat);

        EditText etAvailability = new EditText(getContext());
        etAvailability.setHint("Availability (e.g., Available this week)");
        layout.addView(etAvailability);

        EditText etFee = new EditText(getContext());
        etFee.setHint("Session Fee (e.g., ₹1,500/session)");
        layout.addView(etFee);

        builder.setView(layout);

        builder.setPositiveButton("Save", (dialog, which) -> {
            String specialty = etSpecialty.getText().toString();
            String location = etLocation.getText().toString();
            String format = etFormat.getText().toString();
            String availability = etAvailability.getText().toString();
            String fee = etFee.getText().toString();
            
            BackendClient.createExpertProfile(requireContext(), specialty, location, format, availability, fee, new BackendClient.SimpleCallback() {
                @Override
                public void onSuccess(String message) {
                    if (getActivity() == null) return;
                    getActivity().runOnUiThread(() -> {
                        Toast.makeText(getContext(), "Profile Updated!", Toast.LENGTH_SHORT).show();
                        loadExperts();
                    });
                }
                @Override
                public void onError(String message) {
                    if (getActivity() == null) return;
                    getActivity().runOnUiThread(() -> Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show());
                }
            });
        });
        builder.setNegativeButton("Cancel", null);
        builder.show();
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
}
