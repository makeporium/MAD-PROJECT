package com.example.mad.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.mad.R;

/**
 * Full-screen, beautifully rendered in-app article reader.
 * Launched by InfoHubFragment when an article's content is plain text (not a URL).
 */
public class ArticleReaderFragment extends Fragment {

    private static final String ARG_TITLE     = "title";
    private static final String ARG_CONTENT   = "content";
    private static final String ARG_TOPIC     = "topic";
    private static final String ARG_VIEWS     = "views";
    private static final String ARG_IMAGE_URL = "image_url";

    public static ArticleReaderFragment newInstance(String title, String content,
                                                    String topic, int views, String imageUrl) {
        ArticleReaderFragment f = new ArticleReaderFragment();
        Bundle args = new Bundle();
        args.putString(ARG_TITLE, title);
        args.putString(ARG_CONTENT, content);
        args.putString(ARG_TOPIC, topic);
        args.putInt(ARG_VIEWS, views);
        args.putString(ARG_IMAGE_URL, imageUrl);
        f.setArguments(args);
        return f;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_article_reader, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        String title    = arg(ARG_TITLE, "");
        String content  = arg(ARG_CONTENT, "");
        String topic    = arg(ARG_TOPIC, "Article");
        int views       = getArguments() != null ? getArguments().getInt(ARG_VIEWS, 0) : 0;
        String imageUrl = arg(ARG_IMAGE_URL, "");

        // Hero image loading
        android.widget.ImageView ivHero = view.findViewById(R.id.ivArticleHeroImage);
        ivHero.setVisibility(View.VISIBLE); // Always visible now
        String displayImage = (imageUrl != null && !imageUrl.isEmpty()) ? imageUrl 
                : "https://images.unsplash.com/photo-1519681393784-d120267933ba?q=80&w=1000"; // Fallback mountain/nature

        com.bumptech.glide.Glide.with(this)
                .load(displayImage)
                .centerCrop()
                .into(ivHero);

        // Hero title + topic badge
        ((TextView) view.findViewById(R.id.tvArticleTitle)).setText(title);
        ((TextView) view.findViewById(R.id.tvArticleTopicBadge)).setText(
                topic.isEmpty() ? "Article" : topic.toUpperCase());

        // Estimate reading time (~200 words per minute)
        int wordCount = content.split("\\s+").length;
        int minutes   = Math.max(1, wordCount / 200);
        ((TextView) view.findViewById(R.id.tvArticleReadTime))
                .setText("📖  " + minutes + " min read");

        ((TextView) view.findViewById(R.id.tvArticleViewCount))
                .setText("👁  " + views + " views");

        // Content
        ((TextView) view.findViewById(R.id.tvArticleContent)).setText(content);

        // Back button
        view.findViewById(R.id.btnArticleBack).setOnClickListener(v -> {
            if (getActivity() != null)
                getActivity().getSupportFragmentManager().popBackStack();
        });
    }

    private String arg(String key, String fallback) {
        return getArguments() != null ? getArguments().getString(key, fallback) : fallback;
    }
}
