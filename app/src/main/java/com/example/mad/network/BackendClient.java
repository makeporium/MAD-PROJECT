package com.example.mad.network;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

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
    public static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");

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

    public interface JsonCallback {
        void onSuccess(JSONArray data);
        void onError(String message);
    }

    public interface ObjectCallback {
        void onSuccess(JSONObject data);
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

    public static Request.Builder authorizedBuilder(Context context, String path) {
        String token = getAccessToken(context);
        return new Request.Builder()
                .url(BASE_URL + path)
                .addHeader("Authorization", "Bearer " + token);
    }

    public static void getProfile(Context context, ObjectCallback callback) {
        try {
            Request request = authorizedBuilder(context, "/api/auth/me").get().build();
            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) { callback.onError("Network error: " + e.getMessage()); }
                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    String raw = response.body() != null ? response.body().string() : "";
                    response.close();
                    if (!response.isSuccessful()) { callback.onError(extractErrorMessage(raw)); return; }
                    try { callback.onSuccess(new JSONObject(raw)); } catch (Exception e) { callback.onError("Invalid backend response"); }
                }
            });
        } catch (Exception e) { callback.onError("Client error: " + e.getMessage()); }
    }

    public static void getTestimonials(Context context, JsonCallback callback) {
        makeAuthorizedGetArray(context, "/api/testimonials", callback);
    }

    public static void getRecommendations(Context context, JsonCallback callback) {
        makeAuthorizedGetArray(context, "/api/recommends", callback);
    }

    public static void getEvents(Context context, JsonCallback callback) {
        makeAuthorizedGetArray(context, "/api/events", callback);
    }

    public static void bookSession(Context context, long eventId, SimpleCallback callback) {
        Request request = authorizedBuilder(context, "/api/events/" + eventId + "/book")
                .post(RequestBody.create("{}", JSON))
                .build();
        executeSimpleRequest(request, callback);
    }

    public static void createSession(Context context, String title, String description, String date, String joinLink, SimpleCallback callback) {
        try {
            JSONObject bodyJson = new JSONObject();
            bodyJson.put("title", title);
            bodyJson.put("description", description);
            bodyJson.put("event_date", date);
            bodyJson.put("join_link", joinLink);
            Request request = authorizedBuilder(context, "/api/events")
                    .post(RequestBody.create(bodyJson.toString(), JSON))
                    .build();
            executeSimpleRequest(request, callback);
        } catch (Exception e) {
            callback.onError(e.getMessage());
        }
    }

    public static void getMySessions(Context context, JsonCallback callback) {
        makeAuthorizedGetArray(context, "/api/events/my-sessions", callback);
    }

    public static void createExpertProfile(Context context, String specialty, String location, String format, String availability, String fee, SimpleCallback callback) {
        try {
            JSONObject bodyJson = new JSONObject();
            bodyJson.put("specialty", specialty);
            bodyJson.put("location", location);
            bodyJson.put("format", format);
            bodyJson.put("availability", availability);
            bodyJson.put("fee", fee);
            Request request = authorizedBuilder(context, "/api/events/expert-profile")
                    .post(RequestBody.create(bodyJson.toString(), JSON))
                    .build();
            executeSimpleRequest(request, callback);
        } catch (Exception e) {
            callback.onError(e.getMessage());
        }
    }

    public static void getExpertProfiles(Context context, JsonCallback callback) {
        makeAuthorizedGetArray(context, "/api/events/expert-profiles", callback);
    }

    public static void getResources(Context context, String topic, JsonCallback callback) {
        String path = "/api/resources";
        if (topic != null && !topic.isEmpty()) {
            path += "?topic=" + topic;
        }
        makeAuthorizedGetArray(context, path, callback);
    }

    public static void getCommunityRooms(Context context, JsonCallback callback) {
        makeAuthorizedGetArray(context, "/api/community/rooms", callback);
    }

    public static void createCommunityRoom(Context context, String name, String description, SimpleCallback callback) {
        try {
            JSONObject bodyJson = new JSONObject();
            bodyJson.put("name", name);
            bodyJson.put("description", description);
            Request request = authorizedBuilder(context, "/api/community/rooms")
                    .post(RequestBody.create(bodyJson.toString(), JSON))
                    .build();
            executeSimpleRequest(request, callback);
        } catch (Exception e) {
            callback.onError(e.getMessage());
        }
    }

    public static void deleteCommunityRoom(Context context, long roomId, SimpleCallback callback) {
        Request request = authorizedBuilder(context, "/api/community/rooms/" + roomId)
                .delete()
                .build();
        executeSimpleRequest(request, callback);
    }

    public static void getRoomMessages(Context context, long roomId, JsonCallback callback) {
        makeAuthorizedGetArray(context, "/api/community/rooms/" + roomId + "/messages", callback);
    }

    public static void sendRoomMessage(Context context, long roomId, String message, boolean isAnonymous, SimpleCallback callback) {
        try {
            JSONObject bodyJson = new JSONObject();
            bodyJson.put("message", message);
            bodyJson.put("isAnonymous", isAnonymous);
            Request request = authorizedBuilder(context, "/api/community/rooms/" + roomId + "/messages")
                    .post(RequestBody.create(bodyJson.toString(), JSON))
                    .build();
            executeSimpleRequest(request, callback);
        } catch (Exception e) {
            callback.onError("Client error: " + e.getMessage());
        }
    }

    public static void sendAiMessage(Context context, String message, ObjectCallback callback) {
        try {
            JSONObject bodyJson = new JSONObject();
            bodyJson.put("user_message", message);
            Request request = authorizedBuilder(context, "/api/support/ai")
                    .post(RequestBody.create(bodyJson.toString(), JSON))
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
                    if (!response.isSuccessful()) {
                        callback.onError(extractErrorMessage(raw));
                        return;
                    }
                    try {
                        callback.onSuccess(new JSONObject(raw));
                    } catch (Exception e) {
                        callback.onError("Invalid backend response");
                    }
                }
            });
        } catch (Exception e) {
            callback.onError("Client error: " + e.getMessage());
        }
    }

    public static void sendSos(Context context, SimpleCallback callback) {
        Request request = authorizedBuilder(context, "/api/support/sos")
                .post(RequestBody.create("{}", JSON))
                .build();
        executeSimpleRequest(request, callback);
    }

    public static void createQuickReminder(Context context, String title, SimpleCallback callback) {
        try {
            Calendar calendar = Calendar.getInstance();
            calendar.add(Calendar.HOUR_OF_DAY, 1);
            String remindAt = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(calendar.getTime());
            JSONObject bodyJson = new JSONObject();
            bodyJson.put("title", title);
            bodyJson.put("remind_at", remindAt);
            bodyJson.put("notes", "Added from app quick action");
            Request request = authorizedBuilder(context, "/api/reminders")
                    .post(RequestBody.create(bodyJson.toString(), JSON))
                    .build();
            executeSimpleRequest(request, callback);
        } catch (Exception e) {
            callback.onError("Client error: " + e.getMessage());
        }
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

    public static void makeAuthorizedGetArray(Context context, String path, JsonCallback callback) {
        try {
            String token = getAccessToken(context);
            if (token == null) {
                callback.onError("Please sign in first");
                return;
            }

            Request request = new Request.Builder()
                    .url(BASE_URL + path)
                    .addHeader("Authorization", "Bearer " + token)
                    .get()
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
                    if (!response.isSuccessful()) {
                        callback.onError(extractErrorMessage(raw));
                        return;
                    }
                    try {
                        callback.onSuccess(new JSONArray(raw));
                    } catch (Exception e) {
                        callback.onError("Invalid backend response");
                    }
                }
            });
        } catch (Exception e) {
            callback.onError("Client error: " + e.getMessage());
        }
    }

    public static void executeSimpleRequest(Request request, SimpleCallback callback) {
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
                    callback.onSuccess("Success");
                } else {
                    callback.onError(extractErrorMessage(raw));
                }
            }
        });
    }
}
