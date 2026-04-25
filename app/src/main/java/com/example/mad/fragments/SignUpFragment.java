package com.example.mad.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.mad.MainActivity;
import com.example.mad.R;
import com.example.mad.network.BackendClient;
import com.google.firebase.auth.FirebaseAuth;

public class SignUpFragment extends Fragment {

    private FirebaseAuth firebaseAuth;
    private EditText etEmail, etPassword;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_signup, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        firebaseAuth = FirebaseAuth.getInstance();
        etEmail = view.findViewById(R.id.etEmail);
        etPassword = view.findViewById(R.id.etPassword);

        MainActivity activity = (MainActivity) getActivity();
        if (activity == null) return;

        // Normal Email Signup
        view.findViewById(R.id.btnSignup).setOnClickListener(v -> signupWithEmail());

        // Google Signup (Handled via SignInFragment's existing logic for simplicity)
        view.findViewById(R.id.btnGoogleSignup).setOnClickListener(v ->
                activity.loadFragment(new SignInFragment()));

        view.findViewById(R.id.tvGoToSignin).setOnClickListener(v ->
                activity.loadFragment(new SignInFragment()));
    }

    private void signupWithEmail() {
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(getContext(), R.string.fill_fields, Toast.LENGTH_SHORT).show();
            return;
        }

        if (password.length() < 6) {
            Toast.makeText(getContext(), R.string.password_too_short, Toast.LENGTH_SHORT).show();
            return;
        }

        firebaseAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        exchangeFirebaseTokenForBackendToken();
                    } else {
                        String error = task.getException() != null ? task.getException().getMessage() : "Unknown error";
                        Toast.makeText(getContext(), getString(R.string.registration_failed, error),
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void exchangeFirebaseTokenForBackendToken() {
        if (firebaseAuth.getCurrentUser() == null) return;

        firebaseAuth.getCurrentUser().getIdToken(true).addOnCompleteListener(tokenTask -> {
            if (!tokenTask.isSuccessful() || tokenTask.getResult() == null) {
                Toast.makeText(getContext(), R.string.token_error, Toast.LENGTH_SHORT).show();
                return;
            }

            String idToken = tokenTask.getResult().getToken();
            BackendClient.exchangeGoogleToken(idToken, new BackendClient.AuthCallback() {
                @Override
                public void onSuccess(String accessToken) {
                    if (getActivity() == null) return;
                    BackendClient.saveAccessToken(requireContext(), accessToken);
                    getActivity().runOnUiThread(() -> {
                        ((MainActivity) getActivity()).onAuthSuccess();
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
    }
}
