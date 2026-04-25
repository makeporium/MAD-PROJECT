package com.example.mad.network;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONObject;

import java.io.IOException;

import okhttp3.MediaType;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.RequestBody;
import okhttp3.Request;
import okhttp3.Response;

public class BackendClient {

    // Emulator URL: http://10.0.2.2:5000
    // Physical phone URL: use your laptop's local Wi-Fi IP, e.g. http://192.168.1.7:5000
    private static final String BASE_URL = "http://10.7.24.61:5000";
    private static final OkHttpClient client = new OkHttpClient();
    private static final String PREFS_NAME = "mad_prefs";
    private static final String KEY_ACCESS_TOKEN = "access_token";
    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");

    public interface HealthCallback {
        void onSuccess();
        void onError();
    }

    public interface AuthCallback {
        void onSuccess(String accessToken);
        void onError(String message);
    }

    public interface SimpleCallback {
        void onSuccess(String message);
        void onError(String message);
    }

    public static void checkHealth(HealthCallback callback) {
        Request request = new Request.Builder()
                .url(BASE_URL + "/health")
                .get()
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                callback.onError();
            }

            @Override
            public void onResponse(Call call, Response response) {
                if (response.isSuccessful()) {
                    callback.onSuccess();
                } else {
                    callback.onError();
                }
                response.close();
            }
        });
    }

    public static void exchangeGoogleToken(String firebaseIdToken, AuthCallback callback) {
        try {
            JSONObject json = new JSONObject();
            json.put("idToken", firebaseIdToken);

            RequestBody body = RequestBody.create(json.toString(), JSON);
            Request request = new Request.Builder()
                    .url(BASE_URL + "/api/auth/google")
                    .post(body)
                    .build();

            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    callback.onError("Network error: " + e.getMessage());
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    String raw = response.body() != null ? response.body().string() : "";
                    if (!response.isSuccessful()) {
                        response.close();
                        callback.onError(extractErrorMessage(raw));
                        return;
                    }

                    response.close();

                    try {
                        JSONObject obj = new JSONObject(raw);
                        String accessToken = obj.getString("accessToken");
                        callback.onSuccess(accessToken);
                    } catch (Exception e) {
                        callback.onError("Invalid backend response");
                    }
                }
            });
        } catch (Exception e) {
            callback.onError("Client error: " + e.getMessage());
        }
    }

    private static String extractErrorMessage(String rawJson) {
        try {
            JSONObject obj = new JSONObject(rawJson);
            if (obj.has("errorDetail")) {
                return obj.getString("errorDetail");
            }
            if (obj.has("message")) {
                return obj.getString("message");
            }
        } catch (Exception ignored) {
            // Keep fallback below.
        }
        return "Backend auth failed";
    }

    public static void saveAccessToken(Context context, String accessToken) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().putString(KEY_ACCESS_TOKEN, accessToken).apply();
    }

    public static String getAccessToken(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getString(KEY_ACCESS_TOKEN, null);
    }

    public static boolean hasAccessToken(Context context) {
        return getAccessToken(context) != null;
    }

    public static void clearAccessToken(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().remove(KEY_ACCESS_TOKEN).apply();
    }

    public static void saveMoodEntry(Context context, int moodLevel, String note, SimpleCallback callback) {
        try {
            String token = getAccessToken(context);
            if (token == null) {
                callback.onError("Please sign in first");
                return;
            }

            JSONObject json = new JSONObject();
            json.put("mood_level", moodLevel);
            json.put("note", note == null ? "" : note);

            RequestBody body = RequestBody.create(json.toString(), JSON);
            Request request = new Request.Builder()
                    .url(BASE_URL + "/api/moods")
                    .addHeader("Authorization", "Bearer " + token)
                    .post(body)
                    .build();

            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    callback.onError("Network error: " + e.getMessage());
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    String raw = response.body() != null ? response.body().string() : "";
                    response.close();
                    if (response.isSuccessful()) {
                        callback.onSuccess("Mood saved successfully");
                    } else {
                        callback.onError(extractErrorMessage(raw));
                    }
                }
            });
        } catch (Exception e) {
            callback.onError("Client error: " + e.getMessage());
        }
    }
}
