package com.example.mad.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.mad.R;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

public class ChatDialogFragment extends BottomSheetDialogFragment {

    private String title;
    private String description;

    public static ChatDialogFragment newInstance(String title, String description) {
        ChatDialogFragment fragment = new ChatDialogFragment();
        Bundle args = new Bundle();
        args.putString("title", title);
        args.putString("description", description);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
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
        
        ((TextView) view.findViewById(R.id.chatTitle)).setText(title);
        ((TextView) view.findViewById(R.id.chatDesc)).setText(description);
        
        view.findViewById(R.id.btnSendChat).setOnClickListener(v -> 
            Toast.makeText(getContext(), "Message sent to " + title, Toast.LENGTH_SHORT).show());
            
        return view;
    }
}
