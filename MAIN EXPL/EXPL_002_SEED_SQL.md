# Comprehensive File Analysis: `backend/sql/002_seed.sql`

This file is a Data Manipulation Language (DML) script used to populate the database with initial state data. It demonstrates advanced multi-row insertions, temporal (date/time) functions, and nested Subqueries during `INSERT` statements.

## 1. Bulk Inserts (M:N and 1:N population)

### The SQL Query
```sql
INSERT INTO community_rooms (name, description) VALUES
('Night Feeding Support', 'Share tips and support for late-night feeding'),
('First Week Postpartum', 'For mothers in their first week after delivery'),
('Feeling Lonely', 'A safe space to share when you feel isolated');
```
### Deep Dive: Concepts and Theory
- **Bulk Insert**: Rather than writing 5 separate `INSERT INTO` statements, we pass multiple tuples `(val1, val2), (val3, val4)`. This is infinitely faster because it parses the query only once and builds the B-Tree index structure in bulk rather than re-balancing the tree on every single row.

---

## 2. Temporal Scalar Functions

### The SQL Query
```sql
INSERT INTO events (title, description, event_date, mode, join_link) VALUES
('Live postpartum support session', 'Weekly live support with a counselor', DATE_ADD(NOW(), INTERVAL 2 DAY), 'online', 'https://meet.example.com/postpartum');
```
### Deep Dive: Concepts and Theory
- **Scalar Function `NOW()`**: Fetches the exact current timestamp of the server.
- **Scalar Function `DATE_ADD()`**: Because we want our seed data to always have "Upcoming" events no matter what year you install this project, we cannot hardcode `'2026-05-01'`. `DATE_ADD(NOW(), INTERVAL 2 DAY)` dynamically calculates exactly 48 hours from the exact moment the script is run. This guarantees the `eventsRoutes.js` query `WHERE event_date >= NOW()` will always find this row immediately after setup.

---

## 3. The Dummy Expert Upsert

### The SQL Query
```sql
INSERT INTO users (firebase_uid, email, name, role) VALUES
('dummy-expert-uid-123', 'expert@example.com', 'Dr. Sarah (Expert)', 'expert')
ON DUPLICATE KEY UPDATE
email = VALUES(email),
name = VALUES(name),
role = VALUES(role);
```
### Deep Dive: Concepts and Theory
- **Idempotency**: Running `002_seed.sql` twice will normally crash if it tries to insert a row with a `UNIQUE` constraint. By using `ON DUPLICATE KEY UPDATE`, this specific query becomes *idempotent*—meaning you can run it 100 times safely. The first time it creates the expert, the next 99 times it just silently updates their name and role to the exact same values.

---

## 4. Subqueries in INSERT Statements (Foreign Key Resolution)

### The SQL Query
```sql
INSERT INTO testimonials (user_id, content) VALUES
((SELECT id FROM users WHERE firebase_uid = 'dummy-expert-uid-123' LIMIT 1), 'This app changed my life. I finally feel understood and supported during my postpartum journey.');
```
### Deep Dive: Concepts and Theory
- **The Problem**: We need to assign `user_id` (a Foreign Key) to the testimonial. But because `id` is an `AUTO_INCREMENT` primary key, we have no idea if Dr. Sarah's ID is `1`, `5`, or `50`. 
- **The Solution**: We replace the integer with a **Scalar Subquery**: `(SELECT id FROM users WHERE ...)`.
- The database engine executes the inner subquery first, retrieves the integer (e.g., `5`), and then proceeds to execute the outer query: `INSERT INTO testimonials (user_id, content) VALUES (5, '...')`. This guarantees absolute referential integrity without needing to hardcode IDs.
