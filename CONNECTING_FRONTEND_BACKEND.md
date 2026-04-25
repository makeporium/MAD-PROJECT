# Connecting Android Frontend to Node.js Backend

This guide explains how your Android app (Java) talks to your Node.js backend (Express) to fetch data from the database instead of using hardcoded text.

---

### 1. The High-Level Flow
1.  **Android App** requests data from a URL (e.g., `http://10.0.2.2:3000/api/community/rooms`).
2.  **Backend** receives the request, queries **MySQL**, and gets the data.
3.  **Backend** sends the data back as **JSON** (a text format that's easy for computers to read).
4.  **Android App** parses the JSON and updates the UI (TextViews, RecyclerViews, etc.).

---

### 2. Changes in Android (Java)

To connect, you need a "Network Client." The industry standard is a library called **Retrofit**.

#### A. Define the Data Model
Instead of a hardcoded string, you create a class that matches your database row.
```java
// Room.java
public class Room {
    public int id;
    public String name;
    public String description;
}
```

#### B. Create an API Interface
This tells Android what URLs exist on your backend.
```java
// ApiService.java
public interface ApiService {
    @GET("api/community/rooms")
    Call<List<Room>> getRooms();
}
```

#### C. Replacing Hardcoded Logic in Fragment
In your `CommunityFragment.java`, instead of just setting text, you "Call" the API.

**Before (Hardcoded):**
```java
textView.setText("Night Feeding Support");
```

**After (Dynamic):**
```java
apiService.getRooms().enqueue(new Callback<List<Room>>() {
    @Override
    public void onResponse(Call<List<Room>> call, Response<List<Room>> response) {
        if (response.isSuccessful() && response.body() != null) {
            // Get the first room name from the database!
            String roomName = response.body().get(0).name;
            textView.setText(roomName); 
        }
    }

    @Override
    public void onFailure(Call<List<Room>> call, Throwable t) {
        // Handle error (e.g., No Internet)
    }
});
```

---

### 3. Changes in Android (XML)
Actually, **XML changes the least**. 
- You don't "hardcode" the text in the XML anymore (or you just leave a placeholder).
- The most important thing is that your XML elements have `android:id` so that the Java code can find them and "inject" the database data into them.

---

### 4. How the Backend Responds
Your backend is already set up to handle this! Here is how the `communityRoutes.js` handles that request:

```javascript
// backend/src/routes/communityRoutes.js
router.get("/rooms", async (_req, res) => {
  // 1. Fetch from MySQL
  const [rows] = await sequelize.query("SELECT id, name, description FROM community_rooms ORDER BY id");
  
  // 2. Send as JSON to the Android App
  res.status(200).json(rows); 
});
```

---

### 5. Important "Gotchas" for Beginners

#### A. The IP Address
If you are using the **Android Emulator**, you cannot use `localhost` because the emulator thinks "localhost" is itself. 
- Use `http://10.0.2.2:3000` to refer to the computer the emulator is running on.

#### B. Internet Permission
You must tell Android that your app is allowed to use the internet. Add this to your `AndroidManifest.xml`:
```xml
<uses-permission android:name="android.permission.INTERNET" />
```

#### C. Plaintext Traffic
By default, Android blocks `http` (it wants `https`). Since your local backend is `http`, you need to add this line to the `<application>` tag in `AndroidManifest.xml`:
```xml
android:usesCleartextTraffic="true"
```

### Summary
To see changes:
1.  **Modify MySQL:** Change a room name in your Workbench.
2.  **Start Backend:** Run your Node.js server.
3.  **Run Android:** The app makes the API call, and the new name from Workbench appears on the screen!
