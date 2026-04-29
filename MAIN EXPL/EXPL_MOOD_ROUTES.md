# Comprehensive File Analysis: `backend/src/routes/moodRoutes.js`

This file handles the storage and retrieval of daily user mood check-ins. It showcases proper API security bounding and the use of the `COALESCE` function to safely handle default dates directly in SQL.

## 1. GET `/` (Fetching Mood History)

### The SQL Query
```javascript
  const [rows] = await sequelize.query(
    "SELECT id, mood_level, note, entry_date, created_at FROM mood_entries WHERE user_id = ? ORDER BY entry_date DESC, created_at DESC LIMIT 30",
    { replacements: [req.user.sub] }
  );
```

### Deep Dive: Concepts and Theory
- **SQL Category**: DQL (Data Query Language).
- **Data Security boundary**: `WHERE user_id = ?`. This guarantees **Tenant Isolation**. A user can NEVER query `moodRoutes.js` to see another user's mood. The `user_id` is supplied by `req.user.sub` which comes from the cryptographically verified JWT token, not from the request body.
- **Multi-column Sorting**: `ORDER BY entry_date DESC, created_at DESC`. 
  - Why both? If a user submits two moods on the exact same `entry_date` (e.g. today), the database needs a tie-breaker to know which one to list first. `created_at` provides exact millisecond precision.
- **Pagination Strategy**: `LIMIT 30`. Fetches only the last month of data to ensure the API response is lightning-fast, even if the user has logged their mood every day for 5 years.

---

## 2. POST `/` (Saving a Mood)

### The SQL Query
```javascript
  await sequelize.query(
    "INSERT INTO mood_entries (user_id, mood_level, note, entry_date) VALUES (?, ?, ?, COALESCE(?, CURDATE()))",
    { replacements: [req.user.sub, payload.mood_level, payload.note || null, payload.entry_date || null] }
  );
```

### Deep Dive: Concepts and Theory
- **SQL Category**: DML (Data Manipulation).
- **The `COALESCE` Scalar Function**: `COALESCE(?, CURDATE())`
  - This function evaluates the arguments in order and returns the first one that is **NOT NULL**.
  - If the Android client sends an exact date (e.g., retroactively logging yesterday's mood), it binds to the first `?`. 
  - If the Android client doesn't send a date (the typical flow), `payload.entry_date || null` outputs `NULL`. `COALESCE` sees the NULL, moves to the next parameter, and executes `CURDATE()`.
  - **Why is this brilliant?** It delegates date generation to the Database Server. If the Node.js server is in UTC time but the MySQL server is configured for the local timezone, running `CURDATE()` in the DB ensures the date is physically accurate for the geographic location the database was configured for.
- **Normalization Proof**: The `mood_level` integer (1 to 5) directly maps to the user without redundant data. We don't store "Happy" or "Sad", we store the atomic integer and let the frontend translate that integer into text/emojis.
