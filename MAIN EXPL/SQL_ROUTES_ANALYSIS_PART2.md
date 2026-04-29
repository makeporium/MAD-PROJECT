# SQL Route Analysis: Part 2

This document continues the deep-dive explanation of the SQL queries (DML/DQL) embedded inside the Node.js Express routes. It covers `moodRoutes.js`, `recommendsRoutes.js`, `remindersRoutes.js`, `resourcesRoutes.js`, `supportRoutes.js`, and `testimonialsRoutes.js`.

## 5. `moodRoutes.js`

### Query: Fetching Recent Moods
```javascript
  const [rows] = await sequelize.query(
    "SELECT id, mood_level, note, entry_date, created_at FROM mood_entries WHERE user_id = ? ORDER BY entry_date DESC, created_at DESC LIMIT 30",
    { replacements: [req.user.sub] }
  );
```
- **Line-by-line Explanation**:
  - `WHERE user_id = ?`: Secures the route (DQL). The backend enforces that users can only see their own moods.
  - `ORDER BY entry_date DESC, created_at DESC`: Multi-column sorting. It sorts primarily by the date they entered. If two entries have the exact same date, it breaks the tie by sorting based on the exact timestamp they were created (`created_at`).
  - `LIMIT 30`: Pagination/Optimization. Only fetches the last 30 entries to save bandwidth and memory.

### Query: Inserting Moods (with COALESCE)
```javascript
  await sequelize.query(
    "INSERT INTO mood_entries (user_id, mood_level, note, entry_date) VALUES (?, ?, ?, COALESCE(?, CURDATE()))",
    { replacements: [req.user.sub, payload.mood_level, payload.note, payload.entry_date] }
  );
```
- **Line-by-line Explanation**:
  - `COALESCE(?, CURDATE())`: This is a very powerful SQL scalar function. `COALESCE` takes a list of arguments and returns the first one that is NOT NULL. If the frontend passes an `entry_date` (the first `?`), it uses it. If the frontend passes `NULL` or undefined, it falls back to `CURDATE()` (today's date). This pushes default-value logic down to the DBMS engine level.

---

## 6. `resourcesRoutes.js` (The Info Hub)

### Query: Dynamic Filtering and Searching (LIKE Operator)
```javascript
    const [rows] = await sequelize.query(
      "SELECT id, title, topic, excerpt, content, image_url, tags, views, author_id, created_at FROM info_resources WHERE topic = ? OR tags LIKE ? ORDER BY id DESC",
      { replacements: [topic, `%${topic}%`] }
    );
```
- **Line-by-line Explanation**:
  - `tags LIKE ?`: The `LIKE` operator is used for pattern matching in SQL.
  - `%${topic}%`: The `%` is a wildcard. If the topic is "sleep", the query looks for `%sleep%`, meaning "sleep" can appear anywhere in the string.
  - **DBMS Concept - Indexing Impact**: Using `LIKE '%something'` prevents the database from using a standard B-Tree index effectively (a "Full Table Scan" is required). For small tables, this is fine. For millions of rows, Full-Text Search indexing would be required.

### Query: Atomic Updates
```javascript
  await sequelize.query("UPDATE info_resources SET views = views + 1 WHERE id = ?", { replacements: [req.params.id] });
```
- **Line-by-line Explanation**:
  - `views = views + 1`: Atomic increment. Instead of fetching the views into Node.js (e.g., getting `5`), adding 1 in code, and sending `6` back, we let the database do the math. 
  - **Why used?**: This solves **Race Conditions**. If two users click an article at the exact same millisecond, doing the math in Node.js might result in both updating the views to `6`. Letting the database handle it `views = views + 1` locks the row momentarily, ensuring the views properly reach `7`.

---

## 7. `recommendsRoutes.js` (AI Context Gathering)

This file fetches massive amounts of context to feed into the Gemini LLM.

```javascript
    const [events] = await sequelize.query(`
      SELECT e.id, e.title, e.description, e.event_date, u.name as expert_name 
      FROM events e LEFT JOIN users u ON e.expert_id = u.id 
      WHERE e.event_date >= NOW() LIMIT 20
    `);
```
- **Line-by-line Explanation**:
  - We use `LEFT JOIN` to fetch the `expert_name`. We extract this data as raw JSON and stringify it directly into the AI's system prompt. Because the schema is normalized, the AI is able to see exactly who is hosting the event without having redundant text scattered in the database.

---

## 8. Theoretical Extensions

While not currently used in this project, here is how other SQL features would apply to this exact schema:

### Views
A View is a virtual table based on the result of a SQL statement.
If we constantly needed to query Expert Profiles along with their user data, we could create a view:
```sql
CREATE VIEW expert_directory AS
SELECT u.id, u.name, u.avatar_url, ep.specialty, ep.location
FROM users u
JOIN expert_profiles ep ON u.id = ep.user_id;
```
Then, instead of writing a massive `JOIN` in Node.js, we could simply run:
`SELECT * FROM expert_directory;`

### Stored Procedures
A Stored Procedure is prepared SQL code that you can save and reuse.
If booking an event required checking balance, checking capacity, and inserting a row, we could write a Stored Procedure to handle it entirely on the Database Server, reducing network trips between Node.js and MySQL.

### Advanced Transactions
If we added a feature for "Paid Sessions," we would use Explicit Transactions:
```sql
START TRANSACTION;
UPDATE users SET balance = balance - 50 WHERE id = 1;
INSERT INTO event_bookings (user_id, event_id) VALUES (1, 5);
COMMIT; -- Or ROLLBACK if any step fails
```
This ensures that if the server crashes after charging the user but before booking the event, the money is returned (Atomicity).
