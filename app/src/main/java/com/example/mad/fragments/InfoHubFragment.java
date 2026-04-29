package com.example.mad.fragments;

import android.content.Intent;
import android.net.Uri;
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

public class InfoHubFragment extends Fragment {
    private android.widget.LinearLayout articlesContainer;
    private JSONArray allArticles = new JSONArray();
    private android.widget.EditText etSearch;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_info_hub, container, false);
    }

    private String currentTopic = "";
    private final java.util.List<TextView> chips = new java.util.ArrayList<>();

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        articlesContainer = view.findViewById(R.id.articlesContainer);

        chips.clear();
        addChip(view, R.id.chipPPD, "PPD", R.color.chip_ppd);
        addChip(view, R.id.chipCoping, "Coping", R.color.chip_coping);
        addChip(view, R.id.chipPartner, "Partner", R.color.chip_partner);
        addChip(view, R.id.chipSleep, "Sleep", R.color.chip_sleep);
        addChip(view, R.id.chipFeeding, "Feeding", R.color.chip_feeding);
        addChip(view, R.id.chipSelfCare, "Self-care", R.color.chip_selfcare);

        loadResources("");

        android.widget.ImageView ivAddArticle = view.findViewById(R.id.ivAddArticle);
        if (getActivity() instanceof com.example.mad.MainActivity) {
            String role = ((com.example.mad.MainActivity) getActivity()).getUserRole();
            if ("expert".equals(role)) {
                ivAddArticle.setVisibility(View.VISIBLE);
                ivAddArticle.setOnClickListener(v -> showAddArticleDialog());
            } else {
                ivAddArticle.setVisibility(View.GONE);
            }
        }

        etSearch = view.findViewById(R.id.etSearch);
        if (etSearch != null) {
            etSearch.addTextChangedListener(new android.text.TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    renderArticles(s.toString());
                }
                @Override
                public void afterTextChanged(android.text.Editable s) {}
            });
        }
    }

    // ── Expert: add article dialog ────────────────────────────────────────────

    private void showAddArticleDialog() {
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(getContext());
        builder.setTitle("Add Article");

        android.widget.LinearLayout layout = new android.widget.LinearLayout(getContext());
        layout.setOrientation(android.widget.LinearLayout.VERTICAL);
        layout.setPadding(48, 24, 48, 24);

        final android.widget.EditText titleInput   = addInput(layout, "Article Title");
        final android.widget.EditText topicInput   = addInput(layout, "Topic (e.g., Sleep)");
        final android.widget.EditText excerptInput = addInput(layout, "Short Excerpt");
        final android.widget.EditText contentInput = addInput(layout, "Full content text or URL");
        final android.widget.EditText imageInput   = addInput(layout, "Image URL (optional)");
        final android.widget.EditText tagsInput    = addInput(layout, "Tags (comma separated)");

        builder.setView(layout);
        builder.setPositiveButton("Create", (dialog, which) -> {
            try {
                JSONObject bodyJson = new JSONObject();
                bodyJson.put("title",    titleInput.getText().toString().trim());
                bodyJson.put("topic",    topicInput.getText().toString().trim());
                bodyJson.put("excerpt",  excerptInput.getText().toString().trim());
                bodyJson.put("content",  contentInput.getText().toString().trim());
                bodyJson.put("imageUrl", imageInput.getText().toString().trim());
                bodyJson.put("tags",     tagsInput.getText().toString().trim());

                okhttp3.Request request = BackendClient
                        .authorizedBuilder(requireContext(), "/api/resources")
                        .post(okhttp3.RequestBody.create(bodyJson.toString(), BackendClient.JSON))
                        .build();

                BackendClient.executeSimpleRequest(request, new BackendClient.SimpleCallback() {
                    @Override public void onSuccess(String message) {
                        if (getActivity() == null) return;
                        getActivity().runOnUiThread(() -> {
                            Toast.makeText(getContext(), "Article added", Toast.LENGTH_SHORT).show();
                            loadResources("");
                        });
                    }
                    @Override public void onError(String message) {
                        if (getActivity() == null) return;
                        getActivity().runOnUiThread(() ->
                                Toast.makeText(getContext(), message, Toast.LENGTH_LONG).show());
                    }
                });
            } catch (Exception e) {
                Toast.makeText(getContext(), "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private android.widget.EditText addInput(android.widget.LinearLayout parent, String hint) {
        android.widget.EditText et = new android.widget.EditText(getContext());
        et.setHint(hint);
        android.widget.LinearLayout.LayoutParams lp =
                new android.widget.LinearLayout.LayoutParams(
                        android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                        android.widget.LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, 12, 0, 0);
        et.setLayoutParams(lp);
        parent.addView(et);
        return et;
    }

    // ── Load articles ─────────────────────────────────────────────────────────

    private final java.util.Map<TextView, Integer> chipColors = new java.util.HashMap<>();

    private void addChip(View view, int id, String topicValue, int colorResId) {
        TextView chip = view.findViewById(id);
        if (chip != null) {
            chip.setTag(topicValue);
            chips.add(chip);
            chipColors.put(chip, colorResId);
            chip.setOnClickListener(v -> loadResources(topicValue));
        }
    }

    private void refreshChipVisuals(String selectedTopic) {
        for (TextView chip : chips) {
            String topicTag = (String) chip.getTag();
            boolean isSelected = topicTag != null && topicTag.equalsIgnoreCase(selectedTopic);
            Integer colorRes = chipColors.get(chip);
            int bgColor = getResources().getColor(colorRes != null ? colorRes : R.color.white);
            
            android.graphics.drawable.GradientDrawable gd = new android.graphics.drawable.GradientDrawable();
            gd.setColor(bgColor);
            gd.setCornerRadius(24f); // Matching bg_onboarding_card radius
            
            if (isSelected) {
                // Add a prominent border (stroke)
                gd.setStroke(6, getResources().getColor(R.color.primary_terracotta));
                chip.setTypeface(null, android.graphics.Typeface.BOLD);
            } else {
                gd.setStroke(0, android.graphics.Color.TRANSPARENT);
                chip.setTypeface(null, android.graphics.Typeface.NORMAL);
            }
            
            chip.setBackground(gd);
            chip.setTextColor(getResources().getColor(R.color.text_dark));
        }
    }

    private void renderArticles(String query) {
        if (getActivity() == null || articlesContainer == null) return;
        getActivity().runOnUiThread(() -> {
            articlesContainer.removeAllViews();
            
            String lowerQuery = query == null ? "" : query.toLowerCase().trim();
            int displayedCount = 0;

            for (int i = 0; i < allArticles.length(); i++) {
                JSONObject article = allArticles.optJSONObject(i);
                if (article == null) continue;

                String topicStr   = article.optString("topic", "");
                String titleStr   = article.optString("title", "");
                String excerptStr = article.optString("excerpt", "");
                String tagsStr    = article.optString("tags", "");

                // Filtering logic
                if (!lowerQuery.isEmpty()) {
                    boolean matches = titleStr.toLowerCase().contains(lowerQuery) ||
                                      topicStr.toLowerCase().contains(lowerQuery) ||
                                      tagsStr.toLowerCase().contains(lowerQuery) ||
                                      excerptStr.toLowerCase().contains(lowerQuery);
                    if (!matches) continue;
                }

                displayedCount++;
                View articleView = LayoutInflater.from(getContext())
                        .inflate(R.layout.item_article, articlesContainer, false);

                String contentStr = article.optString("content", "");
                String imageUrl   = article.optString("image_url", "");
                int views         = article.optInt("views", 0);
                long articleId    = article.optLong("id", -1);

                ((TextView) articleView.findViewById(R.id.tvResourceTopic)).setText(topicStr);
                ((TextView) articleView.findViewById(R.id.tvResourceTitle)).setText(titleStr);
                ((TextView) articleView.findViewById(R.id.tvResourceExcerpt)).setText(excerptStr);
                ((TextView) articleView.findViewById(R.id.tvResourceViews)).setText(views + " views");

                // Load thumbnail
                android.widget.ImageView ivThumb = articleView.findViewById(R.id.ivArticleImage);
                if (imageUrl != null && !imageUrl.isEmpty()) {
                    com.bumptech.glide.Glide.with(InfoHubFragment.this)
                            .load(imageUrl)
                            .centerCrop()
                            .into(ivThumb);
                }

                articleView.setOnClickListener(v ->
                        openArticle(articleId, titleStr, contentStr, topicStr, views, imageUrl));
                articlesContainer.addView(articleView);
            }

            if (displayedCount == 0) {
                TextView empty = new TextView(getContext());
                empty.setText("No articles found.");
                empty.setTextColor(android.graphics.Color.parseColor("#888888"));
                empty.setPadding(0, 24, 0, 24);
                articlesContainer.addView(empty);
            }
        });
    }

    private void loadResources(String topic) {
        currentTopic = topic;
        refreshChipVisuals(topic);

        BackendClient.getResources(requireContext(), topic, new BackendClient.JsonCallback() {
            @Override
            public void onSuccess(JSONArray data) {
                allArticles = data;
                String currentSearch = etSearch != null ? etSearch.getText().toString() : "";
                renderArticles(currentSearch);
            }

            @Override
            public void onError(String message) {
                if (getActivity() == null) return;
                getActivity().runOnUiThread(() ->
                        Toast.makeText(getContext(), message, Toast.LENGTH_LONG).show());
            }
        });
    }

    // ── Full-screen article reader ────────────────────────────────────────────

    private void openArticle(long articleId, String title, String content,
                              String topic, int views, String imageUrl) {
        if (content.startsWith("http")) {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(content)));
        } else {
            // Increment view count silently
            if (articleId > 0) {
                okhttp3.Request req = BackendClient
                        .authorizedBuilder(requireContext(), "/api/resources/" + articleId)
                        .get().build();
                BackendClient.executeSimpleRequest(req, new BackendClient.SimpleCallback() {
                    @Override public void onSuccess(String message) {}
                    @Override public void onError(String message) {}
                });
            }
            ArticleReaderFragment reader =
                    ArticleReaderFragment.newInstance(title, content, topic, views, imageUrl);
            if (getActivity() instanceof com.example.mad.MainActivity) {
                ((com.example.mad.MainActivity) getActivity()).loadFragment(reader);
            }
        }
    }
}
