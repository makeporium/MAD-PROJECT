# Comprehensive File Analysis: `backend/src/routes/resourcesRoutes.js`

This file is responsible for the "Info Hub". It features Pattern Matching, Conditional Ordering, and Atomic Updates.

## 1. GET `/` (Search and Fetch Resources)

### The SQL Query
```javascript
    const [rows] = await sequelize.query(
      "SELECT id, title, topic, excerpt, content, image_url, tags, views, author_id, created_at FROM info_resources WHERE topic = ? OR tags LIKE ? ORDER BY id DESC",
      { replacements: [topic, `%${topic}%`] }
    );
```
### Deep Dive: Concepts and Theory
- **The `LIKE` Operator**: SQL provides `LIKE` for simple pattern matching (it is not Regex).
- **Wildcards (`%`)**: The `%` character represents zero, one, or multiple characters. By doing `LIKE '%sleep%'`, the query will find "sleep", "sleepless", "deep sleep", etc., located anywhere inside the `tags` column.
- **Why this breaks Normalization**: The `tags` column stores comma-separated strings (e.g., "ppd, nutrition"). This violates **First Normal Form (1NF)** which demands atomic values. To be fully normalized up to 3NF, we would need:
  - Table 1: `info_resources`
  - Table 2: `tags` (id, tag_name)
  - Table 3: `resource_tags` (resource_id, tag_id)
  - We would then use a `JOIN` to search. However, storing it as a CSV string and using `LIKE` is a deliberate, simplified design choice for this specific application.

---

## 2. GET `/:id` (Tracking Views)

### The SQL Query
```javascript
  await sequelize.query("UPDATE info_resources SET views = views + 1 WHERE id = ?", { replacements: [req.params.id] });
```
### Deep Dive: Concepts and Theory
- **Atomic Operations**: `views = views + 1`. This is executed purely on the database engine.
- **Race Condition Prevention**: If this was done in application memory (e.g., Node.js runs `SELECT views`, gets 50, adds 1 in JS, then runs `UPDATE views = 51`), two users hitting the route at the same time would both read 50 and both write 51, losing a view. By running `SET views = views + 1`, the DBMS applies an implicit **Row-Level Lock** for a microsecond, ensuring the views perfectly increment to 52 regardless of concurrency.

---

## 3. POST / PUT / DELETE (Expert-Only Authorization)

### The SQL Query
```javascript
    const [users] = await sequelize.query("SELECT role FROM users WHERE id = ?", { replacements: [req.user.sub] });
    if (!users.length || users[0].role !== 'expert') return res.status(403).json({ message: "Forbidden" });
```
### Deep Dive: Concepts and Theory
- **Application-Layer RBAC (Role-Based Access Control)**: Just like `communityRoutes.js`, we run a preliminary `SELECT` query. If the `role` attribute in the returned tuple isn't exactly `'expert'`, we block the subsequent DML commands (`INSERT`, `UPDATE`, `DELETE`).
