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

        // Dynamic welcome name
        TextView tvWelcome = view.findViewById(R.id.tvWelcomeName);
        String savedName = activity.getSavedFirstName();
        if (savedName != null && !savedName.isEmpty()) {
            tvWelcome.setText(getString(R.string.welcome_back, savedName));
        }

        // Card click listeners
        view.findViewById(R.id.cardRecommends).setOnClickListener(v -> 
                activity.loadFragment(new RecommendsFragment()));

        view.findViewById(R.id.cardMoodCheckin).setOnClickListener(v -> 
                activity.loadFragment(new MoodCheckinFragment()));

        view.findViewById(R.id.cardAiSupport).setOnClickListener(v -> 
                activity.loadFragment(new AiSupportFragment()));

        view.findViewById(R.id.cardLearn).setOnClickListener(v -> 
                activity.navigateToTab(R.id.nav_info_hub));

        view.findViewById(R.id.cardFeaturedArticle).setOnClickListener(v -> 
                activity.navigateToTab(R.id.nav_info_hub));

        // Setup Mood Line Chart
        setupMoodLineChart(view);

        // Setup Testimonials
        setupTestimonials(view);
    }

    private void setupMoodLineChart(View view) {
        LineChart chart = view.findViewById(R.id.moodLineChart);

        // Sample mood data for 7 days (scale 1-5)
        List<Entry> entries = new ArrayList<>();
        entries.add(new Entry(0, 3.5f));
        entries.add(new Entry(1, 2.8f));
        entries.add(new Entry(2, 4.2f));
        entries.add(new Entry(3, 3.0f));
        entries.add(new Entry(4, 4.5f));
        entries.add(new Entry(5, 3.8f));
        entries.add(new Entry(6, 4.0f));

        LineDataSet dataSet = new LineDataSet(entries, "Mood");

        // Styling the line
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

        // X-axis labels (days)
        String[] days = {"Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"};
        XAxis xAxis = chart.getXAxis();
        xAxis.setValueFormatter(new IndexAxisValueFormatter(days));
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setGranularity(1f);
        xAxis.setDrawGridLines(false);
        xAxis.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_muted));
        xAxis.setTextSize(11f);

        // Y-axis
        YAxis leftAxis = chart.getAxisLeft();
        leftAxis.setAxisMinimum(0f);
        leftAxis.setAxisMaximum(5f);
        leftAxis.setGranularity(1f);
        leftAxis.setDrawGridLines(true);
        leftAxis.setGridColor(Color.parseColor("#E8E0D8"));
        leftAxis.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_muted));
        leftAxis.setTextSize(11f);

        chart.getAxisRight().setEnabled(false);

        // Chart styling
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
    }

    private void setupTestimonials(View view) {
        TextView tvQuote = view.findViewById(R.id.tvTestimonialQuote);
        TextView tvAuthor = view.findViewById(R.id.tvTestimonialAuthor);

        // Set initial testimonial
        tvQuote.setText(testimonials[0][0]);
        tvAuthor.setText(testimonials[0][1]);

        // Tap to cycle through testimonials
        view.findViewById(R.id.cardTestimonials).setOnClickListener(v -> {
            currentTestimonialIndex = (currentTestimonialIndex + 1) % testimonials.length;
            tvQuote.setText(testimonials[currentTestimonialIndex][0]);
            tvAuthor.setText(testimonials[currentTestimonialIndex][1]);
        });
    }
}
