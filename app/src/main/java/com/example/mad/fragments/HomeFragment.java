package com.example.mad.fragments;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.example.mad.MainActivity;
import com.example.mad.R;
import com.example.mad.network.BackendClient;
import org.json.JSONArray;
import org.json.JSONObject;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;

import java.util.ArrayList;
import java.util.List;

public class HomeFragment extends Fragment {

    // Testimonials data
    private final String[][] testimonials = {
            {"This app made me realize I'm not alone in this journey. The community here truly understands.", "— Ananya, Mother of 1"},
            {"The daily check-ins helped me track my emotions and see patterns I never noticed before.", "— Sneha, Mother of 2"},
            {"I was struggling silently until I found this app. The AI support at 3 AM was a lifesaver.", "— Priya, First-time Mom"},
            {"My partner and I use this together. It's brought us closer during the hardest time of our lives.", "— Meera, Mother of Twins"},
            {"The mood tracking feature helped my therapist understand my postpartum journey better.", "— Kavya, Mother of 1"}
    };
    private int currentTestimonialIndex = 0;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        MainActivity activity = (MainActivity) getActivity();
        if (activity == null) return;

        // Welcome name — read from SharedPrefs (loadProfile keeps it fresh)
        TextView tvWelcome = view.findViewById(R.id.tvWelcomeName);
        // Refresh from backend so it shows the real name, not a hardcoded placeholder
        BackendClient.getProfile(requireContext(), new BackendClient.ObjectCallback() {
            @Override
            public void onSuccess(JSONObject data) {
                if (getActivity() == null) return;
                String name = data.optString("name", "");
                String[] parts = name.split(" ", 2);
                String firstName = parts.length > 0 ? parts[0] : "";
                getActivity().runOnUiThread(() -> {
                    if (!firstName.isEmpty()) {
                        tvWelcome.setText(getString(R.string.welcome_back, firstName));
                    }
                });
            }
            @Override public void onError(String message) {
                // Fallback to cached name
                String saved = activity.getSavedFirstName();
                if (saved != null && !saved.isEmpty()) {
                    tvWelcome.setText(getString(R.string.welcome_back, saved));
                }
            }
        });

        // Card click listeners
        view.findViewById(R.id.cardRecommends).setOnClickListener(v -> 
                activity.loadFragment(new RecommendsFragment()));

        view.findViewById(R.id.cardMoodCheckin).setOnClickListener(v -> 
                activity.loadFragment(new MoodCheckinFragment()));

        view.findViewById(R.id.cardAiSupport).setOnClickListener(v -> 
                activity.loadFragment(new AiSupportFragment()));

        view.findViewById(R.id.cardLearn).setOnClickListener(v -> 
                activity.navigateToTab(R.id.nav_info_hub));

        // Featured article loading
        loadFeaturedArticle(view);

        // Setup Mood Line Chart
        setupMoodLineChart(view);

        // Setup Testimonials
        setupTestimonials(view);
    }

    private void loadFeaturedArticle(View view) {
        BackendClient.getResources(requireContext(), null, new BackendClient.JsonCallback() {
            @Override
            public void onSuccess(JSONArray data) {
                if (getActivity() == null || data.length() == 0) return;
                
                // Find most viewed article (or just the first one since it's sorted by id/views on backend)
                // Let's explicitly sort by views on the backend path if possible
                // Wait, BackendClient.getResources uses /api/resources, I'll update it to support sort=views
                
                getActivity().runOnUiThread(() -> {
                    try {
                        // The backend /api/resources already supports ?sort=views.
                        // I'll re-fetch with sort=views specifically for the featured article.
                        fetchTopArticle(view);
                    } catch (Exception e) {}
                });
            }
            @Override public void onError(String message) {}
        });
    }

    private void fetchTopArticle(View view) {
        // We'll use a direct request since BackendClient.getResources doesn't expose sort param yet
        String path = "/api/resources?sort=views";
        BackendClient.makeAuthorizedGetArray(requireContext(), path, new BackendClient.JsonCallback() {
            @Override
            public void onSuccess(JSONArray data) {
                if (getActivity() == null || data.length() == 0) return;
                JSONObject top = data.optJSONObject(0);
                if (top == null) return;

                String title    = top.optString("title", "");
                String excerpt  = top.optString("excerpt", "");
                String content  = top.optString("content", "");
                String topic    = top.optString("topic", "");
                String imageUrl = top.optString("image_url", "");
                int views       = top.optInt("views", 0);
                long id         = top.optLong("id", -1);

                getActivity().runOnUiThread(() -> {
                    ((TextView) view.findViewById(R.id.tvFeaturedTitle)).setText(title);
                    ((TextView) view.findViewById(R.id.tvFeaturedExcerpt)).setText(excerpt);
                    ((TextView) view.findViewById(R.id.tvFeaturedBadge)).setText(topic.toUpperCase());
                    
                    android.widget.ImageView iv = view.findViewById(R.id.ivFeaturedImage);
                    if (!imageUrl.isEmpty()) {
                        com.bumptech.glide.Glide.with(HomeFragment.this)
                                .load(imageUrl)
                                .centerCrop()
                                .into(iv);
                    }

                    View.OnClickListener openReader = v -> {
                        ArticleReaderFragment reader = ArticleReaderFragment.newInstance(title, content, topic, views, imageUrl);
                        ((MainActivity) getActivity()).loadFragment(reader);
                    };

                    view.findViewById(R.id.cardFeaturedArticle).setOnClickListener(openReader);
                    view.findViewById(R.id.btnFeaturedReadMore).setOnClickListener(openReader);
                });
            }
            @Override public void onError(String message) {}
        });
    }

    private void setupMoodLineChart(View view) {
        LineChart chart = view.findViewById(R.id.moodLineChart);

        BackendClient.getMoodEntries(requireContext(), new BackendClient.JsonCallback() {
            @Override
            public void onSuccess(JSONArray data) {
                if (getActivity() == null) return;

                getActivity().runOnUiThread(() -> {
                    List<Entry> entries = new ArrayList<>();
                    List<String> daysList = new ArrayList<>();

                    try {
                        if (data.length() == 0) {
                            chart.setVisibility(View.INVISIBLE);
                            return;
                        }
                        chart.setVisibility(View.VISIBLE);

                        java.text.SimpleDateFormat inFormat = new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US);
                        java.text.SimpleDateFormat outFormat = new java.text.SimpleDateFormat("MMM dd", java.util.Locale.US);

                        // Filter to get the latest entry per day, up to 7 days
                        List<JSONObject> dailyEntries = new ArrayList<>();
                        java.util.Set<String> seenDates = new java.util.HashSet<>();

                        for (int i = 0; i < data.length(); i++) {
                            JSONObject obj = data.getJSONObject(i);
                            String rawDate = obj.optString("entry_date", "");
                            String dateKey = rawDate.length() >= 10 ? rawDate.substring(0, 10) : rawDate;
                            if (!dateKey.isEmpty() && !seenDates.contains(dateKey)) {
                                seenDates.add(dateKey);
                                dailyEntries.add(obj);
                                if (dailyEntries.size() == 7) break;
                            }
                        }

                        // Reverse to plot chronologically (oldest on left)
                        java.util.Collections.reverse(dailyEntries);

                        for (int i = 0; i < dailyEntries.size(); i++) {
                            JSONObject obj = dailyEntries.get(i);
                            float mood = (float) obj.optDouble("mood_level", 0);
                            entries.add(new Entry(i, mood));

                            String rawDate = obj.optString("entry_date", "");
                            String label = "";
                            if (!rawDate.isEmpty() && rawDate.length() >= 10) {
                                try {
                                    java.util.Date date = inFormat.parse(rawDate.substring(0, 10));
                                    if (date != null) {
                                        label = outFormat.format(date);
                                    }
                                } catch (Exception e) {}
                            }
                            daysList.add(label);
                        }

                        LineDataSet dataSet = new LineDataSet(entries, "Mood");
                        int terracotta = ContextCompat.getColor(requireContext(), R.color.primary_terracotta);
                        dataSet.setColor(terracotta);
                        dataSet.setLineWidth(3f);
                        dataSet.setCircleColor(terracotta);
                        dataSet.setCircleRadius(5f);
                        dataSet.setCircleHoleColor(Color.WHITE);
                        dataSet.setCircleHoleRadius(3f);
                        dataSet.setDrawValues(false);
                        dataSet.setMode(LineDataSet.Mode.CUBIC_BEZIER);
                        dataSet.setDrawFilled(true);
                        dataSet.setFillColor(terracotta);
                        dataSet.setFillAlpha(40);

                        LineData lineData = new LineData(dataSet);
                        chart.setData(lineData);

                        XAxis xAxis = chart.getXAxis();
                        xAxis.setValueFormatter(new IndexAxisValueFormatter(daysList));
                        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
                        xAxis.setGranularity(1f);
                        xAxis.setDrawGridLines(false);
                        xAxis.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_muted));
                        xAxis.setTextSize(11f);

                        YAxis leftAxis = chart.getAxisLeft();
                        leftAxis.setAxisMinimum(0f);
                        leftAxis.setAxisMaximum(5f);
                        leftAxis.setGranularity(1f);
                        leftAxis.setDrawGridLines(true);
                        leftAxis.setGridColor(Color.parseColor("#E8E0D8"));
                        leftAxis.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_muted));
                        leftAxis.setTextSize(11f);

                        chart.getAxisRight().setEnabled(false);
                        chart.getDescription().setEnabled(false);
                        chart.getLegend().setEnabled(false);
                        chart.setTouchEnabled(true);
                        chart.setDragEnabled(false);
                        chart.setScaleEnabled(false);
                        chart.setPinchZoom(false);
                        chart.setDrawBorders(false);
                        chart.setExtraBottomOffset(8f);
                        chart.setBackgroundColor(Color.TRANSPARENT);
                        chart.animateX(1000);
                        chart.invalidate();

                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });
            }

            @Override
            public void onError(String message) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> chart.setVisibility(View.INVISIBLE));
                }
            }
        });
    }

    private List<String[]> realTestimonials = new ArrayList<>();

    private void setupTestimonials(View view) {
        TextView tvQuote = view.findViewById(R.id.tvTestimonialQuote);
        TextView tvAuthor = view.findViewById(R.id.tvTestimonialAuthor);

        // Fallback or loading state
        tvQuote.setText("Loading testimonials...");
        tvAuthor.setText("");

        BackendClient.getTestimonials(requireContext(), new BackendClient.JsonCallback() {
            @Override
            public void onSuccess(org.json.JSONArray data) {
                if (getActivity() == null) return;
                getActivity().runOnUiThread(() -> {
                    realTestimonials.clear();
                    for (int i = 0; i < data.length(); i++) {
                        try {
                            org.json.JSONObject obj = data.getJSONObject(i);
                            String quote = obj.optString("content", "");
                            String author = obj.optString("user_name", "Anonymous");
                            String role = obj.optString("role", "user");
                            if ("expert".equals(role)) {
                                author += " (Expert)";
                            }
                            realTestimonials.add(new String[]{quote, "— " + author});
                        } catch (Exception e) {}
                    }
                    if (!realTestimonials.isEmpty()) {
                        tvQuote.setText(realTestimonials.get(0)[0]);
                        tvAuthor.setText(realTestimonials.get(0)[1]);
                        currentTestimonialIndex = 0;
                    } else {
                        tvQuote.setText("No testimonials yet.");
                        tvAuthor.setText("");
                    }
                });
            }

            @Override
            public void onError(String message) {
                if (getActivity() == null) return;
                getActivity().runOnUiThread(() -> {
                    tvQuote.setText("Could not load testimonials.");
                    tvAuthor.setText("");
                });
            }
        });

        // Tap to cycle through testimonials
        view.findViewById(R.id.cardTestimonials).setOnClickListener(v -> {
            if (realTestimonials.isEmpty()) return;
            currentTestimonialIndex = (currentTestimonialIndex + 1) % realTestimonials.size();
            tvQuote.setText(realTestimonials.get(currentTestimonialIndex)[0]);
            tvAuthor.setText(realTestimonials.get(currentTestimonialIndex)[1]);
        });
    }
}
