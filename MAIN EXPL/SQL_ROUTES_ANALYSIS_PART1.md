# SQL Route Analysis: Part 1

This document provides a massive, line-by-line explanation of the SQL queries (DML/DQL) embedded inside the Node.js Express routes. It covers `authRoutes.js`, `calendarRoutes.js`, `communityRoutes.js`, and `eventsRoutes.js`.

## 1. `authRoutes.js`

### Query: Upserting Users
```javascript
  await sequelize.query(
    "INSERT INTO users (firebase_uid, email, name, avatar_url) VALUES (?, ?, ?, ?) " +
    "ON DUPLICATE KEY UPDATE email=VALUES(email), name=VALUES(name), avatar_url=VALUES(avatar_url)",
    { replacements: [uid, email, name, picture] }
  );
```
- **Line-by-line Explanation**:
  - `INSERT INTO users (...) VALUES (?, ?, ?, ?)`: This is basic DML. The `?` are placeholders for parameterized queries, preventing SQL injection.
  - `ON DUPLICATE KEY UPDATE`: This is a MySQL-specific "Upsert" mechanism.
  - **Why used?**: Our schema defines `firebase_uid` as `UNIQUE`. If the user logs in for the first time, it inserts. If they log in again and the database detects a collision on that unique key, it automatically shifts to an `UPDATE` command, refreshing their avatar and email in one atomic operation. This saves us from doing a separate `SELECT` to check existence.

### Query: Fetching Current User
```javascript
  const [userRows] = await sequelize.query(
    "SELECT id, firebase_uid, email, name, avatar_url, role FROM users WHERE id = ? LIMIT 1",
    { replacements: [req.user.sub] }
  );
```
- **Line-by-line Explanation**:
  - `SELECT id, ...`: (DQL) Only requesting specific columns reduces memory usage compared to `SELECT *`.
  - `WHERE id = ?`: Filters the result using the Primary Key.
  - `LIMIT 1`: Even though `id` is a Primary Key (and therefore unique), adding `LIMIT 1` signals the SQL optimizer to stop searching the B-Tree index immediately after finding the first match.

---

## 2. `eventsRoutes.js`

This file contains the most complex SQL in the app, demonstrating Joins, Subqueries, and Aliasing.

### Query: Fetching All Events
```javascript
  const [rows] = await sequelize.query(
    `SELECT e.id, e.title, e.description, e.event_date, e.mode, e.join_link, e.expert_id, u.name as expert_name,
            (SELECT COUNT(*) FROM event_bookings WHERE event_id = e.id) AS booking_count
     FROM events e 
     LEFT JOIN users u ON e.expert_id = u.id 
     LEFT JOIN event_bookings eb ON eb.event_id = e.id AND eb.user_id = ?
     WHERE e.event_date >= NOW()
     GROUP BY e.id
     ORDER BY e.event_date ASC`,
    { replacements: [req.user.sub] }
  );
```
- **Line-by-line Explanation**:
  - `SELECT e.id...`: `e` and `u` are **table aliases**. They make the query readable.
  - `u.name as expert_name`: This aliases a column. Since `events` and `users` might both have a `name` column theoretically, aliasing it prevents naming collisions in JSON output.
  - **The Subquery**: `(SELECT COUNT(*) FROM event_bookings WHERE event_id = e.id) AS booking_count`. This is a **Correlated Subquery** (a Scalar Function). For every row the main query processes, it runs this subquery to count the exact number of attendees. 
  - `FROM events e`: The primary entity.
  - `LEFT JOIN users u ON e.expert_id = u.id`: We use a `LEFT JOIN` (Outer Join) instead of an `INNER JOIN`. Why? If an event is created but hasn't been assigned an expert yet (`expert_id` is NULL), an `INNER JOIN` would cause the event to completely disappear from the results. `LEFT JOIN` guarantees the event is returned, with `expert_name` as NULL.
  - `WHERE e.event_date >= NOW()`: Filters out past events using the `NOW()` scalar function.
  - `GROUP BY e.id`: Groups the results. Necessary because the second `LEFT JOIN` to `event_bookings` could potentially multiply rows if we weren't careful.
  - `ORDER BY e.event_date ASC`: Sorts chronologically.

### Query: Booking an Event
```javascript
    await sequelize.query(
      "INSERT IGNORE INTO event_bookings (user_id, event_id) VALUES (?, ?)",
      { replacements: [req.user.sub, req.params.id] }
    );
```
- **Line-by-line Explanation**:
  - `INSERT IGNORE`: This is a DML command. The `event_bookings` table uses a Composite Primary Key `(user_id, event_id)`. If the same user clicks "Book" twice, it attempts to insert a duplicate key. `IGNORE` tells the database to silently swallow the error rather than crashing the backend.

---

## 3. `calendarRoutes.js`

### Query: Complex Progress Tracking
```javascript
  const [moodRows] = await sequelize.query(
    "SELECT COUNT(*) as mood_done FROM mood_entries WHERE user_id = ? AND entry_date = CURDATE()",
    { replacements: [userId] }
  );
```
- **Line-by-line Explanation**:
  - `COUNT(*)`: An aggregate function. It physically counts the rows matching the condition.
  - `entry_date = CURDATE()`: Uses the MySQL `CURDATE()` function (returns 'YYYY-MM-DD' without time) to check if a mood was logged exactly today.

---

## 4. `communityRoutes.js`

### Query: Fetching Room Messages with Joins
```javascript
  const [rows] = await sequelize.query(
    `SELECT cm.id, cm.message, cm.is_anonymous, cm.created_at, 
            u.name as author_name, u.avatar_url 
     FROM community_messages cm
     LEFT JOIN users u ON cm.user_id = u.id
     WHERE cm.room_id = ?
     ORDER BY cm.created_at ASC`,
    { replacements: [req.params.id] }
  );
```
- **Line-by-line Explanation**:
  - Similar to events, it joins `users` to fetch the `author_name`.
  - **Normalization Benefit**: We don't store the user's avatar directly in `community_messages`. If the user updates their avatar, the new avatar automatically propagates to every message they've ever sent simply because this query `JOIN`s to the single source of truth in the `users` table.

*(Continue to Part 2 for the rest of the routes, including AI Search mapping!)*
