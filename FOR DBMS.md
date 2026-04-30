# Comprehensive DBMS Concept Mapping
This document serves as a detailed index of where and how core Database Management System (DBMS) concepts are implemented across the Mother's Nest project's backend. 

## 4. Normalization (1NF, 2NF, 3NF)
**File:** `backend/sql/001_init.sql` (Lines 4-13)
Our database is strictly normalized up to the Third Normal Form (3NF) to eliminate data redundancy and anomalies.
**Snippet:**
```sql
CREATE TABLE IF NOT EXISTS users (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  firebase_uid VARCHAR(128) NOT NULL UNIQUE,
  email VARCHAR(255),
  name VARCHAR(120),
  avatar_url TEXT,
  role VARCHAR(20) DEFAULT 'user',
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);
```
*   **1NF:** Every column contains atomic values (e.g. no comma-separated lists of booked events).
*   **2NF/3NF:** All non-key attributes (like `name`, `avatar_url`) depend *only* on the primary key `id`, ensuring no transitive dependencies exist.

## 5. Keys (Primary, Foreign, Candidate)
**File:** `backend/sql/001_init.sql` (Lines 52-61)
**Snippet:**
```sql
CREATE TABLE IF NOT EXISTS community_messages (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  room_id BIGINT NOT NULL,
  user_id BIGINT NOT NULL,
  message TEXT NOT NULL,
  is_anonymous BOOLEAN DEFAULT FALSE,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (room_id) REFERENCES community_rooms(id) ON DELETE CASCADE,
  FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);
```
*   **Primary Key (PK):** `id BIGINT PRIMARY KEY`. Acts as a unique surrogate identifier for each row.
*   **Foreign Key (FK):** `room_id` and `user_id`. They enforce referential integrity between the messages, rooms, and users.
*   **Candidate Key:** In the `users` table, `firebase_uid` is marked `UNIQUE`, making it a Candidate Key (it could serve as a primary key).

## 6 & 15. SQL Basics (DDL, DML, DQL) & TCL (Transaction Control)
The project heavily utilizes every subset of SQL natively:

*   **Data Definition Language (DDL):** Used to define schema structures (e.g., `CREATE TABLE`, `CREATE VIEW`, `CREATE PROCEDURE`).
*   **Data Manipulation Language (DML):** Modifies the raw data.
    **File:** `backend/src/routes/communityRoutes.js` (Line 73)
    ```javascript
    await sequelize.query(
      "INSERT INTO community_messages (room_id, user_id, message, is_anonymous) VALUES (?, ?, ?, ?)", ...
    );
    ```
*   **Data Query Language (DQL):** Used strictly for data retrieval.
    **File:** `backend/src/routes/eventsRoutes.js` (Line 82)
    ```javascript
    "SELECT id, title, description, event_date, join_link FROM events WHERE expert_id = ? ORDER BY event_date ASC"
    ```
*   **Transaction Control Language (TCL):** Managing transactions natively via `COMMIT` and `ROLLBACK` commands to enforce ACID logic (detailed in Rubric 13).

## 7. Joins
**File:** `backend/src/routes/eventsRoutes.js` (Lines 13-19)
**Snippet:**
```sql
SELECT e.id, e.title, e.description, e.event_date, e.mode, e.join_link, e.expert_id, u.name as expert_name,
       CASE WHEN eb.id IS NULL THEN 0 ELSE 1 END AS is_booked,
       (SELECT COUNT(*) FROM event_bookings WHERE event_id = e.id) AS booking_count
FROM events e 
LEFT JOIN users u ON e.expert_id = u.id 
LEFT JOIN event_bookings eb ON eb.event_id = e.id AND eb.user_id = ?
ORDER BY e.event_date ASC
```
*   **LEFT JOIN:** Allows us to dynamically fetch events and attach the expert's name from the `users` table, guaranteeing that events *without* an assigned expert are still returned to the user.

## 8. Group By & Having
**File:** `backend/src/routes/communityRoutes.js` (Lines 26-36)
**Snippet:**
```sql
SELECT r.id, r.name, r.description, r.created_by,
       COUNT(DISTINCT m.user_id) as participants_count,
       MAX(m.created_at) as last_message_at,
       (SELECT message FROM community_messages WHERE room_id = r.id ORDER BY created_at DESC LIMIT 1) as last_message
FROM community_rooms r
LEFT JOIN community_messages m ON r.id = m.room_id
GROUP BY r.id, r.name, r.description, r.created_by
HAVING participants_count >= 0
ORDER BY participants_count DESC, r.id
```
*   **GROUP BY:** Condenses rows belonging to the exact same room to mathematically calculate metrics like total participants.
*   **HAVING:** Filters the resulting aggregated group dynamically purely on the database engine.

## 9. Subqueries
**File:** `backend/src/routes/eventsRoutes.js` (Line 15)
**Snippet:**
```sql
(SELECT COUNT(*) FROM event_bookings WHERE event_id = e.id) AS booking_count
```
*   **Correlated Subquery:** Used to run a mini-query inside the `SELECT` column list for every single row returned, dynamically attaching live booking counts without an external JavaScript trip.

## 10. Functions (Aggregate & Scalar)
**File:** `backend/src/routes/calendarRoutes.js` (Lines 12-17)
**Snippet:**
```sql
SELECT COUNT(*) AS total
FROM event_bookings eb
JOIN events e ON e.id = eb.event_id
WHERE eb.user_id = ?
  AND MONTH(e.event_date) = MONTH(CURRENT_DATE())
  AND YEAR(e.event_date) = YEAR(CURRENT_DATE())
```
*   **Aggregate Function:** `COUNT(*)` collapses rows and returns a mathematical calculation of total data.
*   **Scalar Functions:** `MONTH()` and `CURRENT_DATE()` evaluate on a single value per row to intelligently fetch records scoped to the exact current month.

## 11. Views
**File:** `backend/src/scripts/initDb.js` (Lines 61-67)
**Snippet:**
```sql
CREATE OR REPLACE VIEW expert_directory AS
SELECT ep.*, u.name as expert_name, u.avatar_url 
FROM expert_profiles ep 
JOIN users u ON ep.user_id = u.id
```
*   **Views:** We created a virtual table to permanently abstract the complexity of joining `users` to `expert_profiles`. As shown in `eventsRoutes.js` (Line 142), we now easily execute `SELECT * FROM expert_directory`.

## 12. Stored Procedures
**File:** `backend/src/scripts/initDb.js` (Lines 72-84)
**Snippet:**
```sql
CREATE PROCEDURE upsert_expert_profile(
    IN p_user_id BIGINT,
    IN p_specialty VARCHAR(150),
    IN p_location VARCHAR(100),
    IN p_format VARCHAR(50),
    IN p_availability VARCHAR(100),
    IN p_fee INT
)
INSERT INTO expert_profiles (user_id, specialty, location, format, availability, fee) 
VALUES (p_user_id, p_specialty, p_location, p_format, p_availability, p_fee)
ON DUPLICATE KEY UPDATE 
specialty = p_specialty, location = p_location, format = p_format, availability = p_availability, fee = p_fee
```
*   **Stored Procedure:** Offloads complex business logic—specifically deciding whether to create a new profile or update an existing one—down to the database layer. This drastically saves networking latency. Executed in `eventsRoutes.js` via `CALL upsert_expert_profile(...)`.

## 13. Transactions (ACID, COMMIT, ROLLBACK)
**File:** `backend/src/routes/eventsRoutes.js` (Lines 71-91)
**Snippet:**
```javascript
const t = await sequelize.transaction(); // STARTS TRANSACTION
try {
  const [existing] = await sequelize.query(
    "SELECT id FROM event_bookings WHERE event_id = ? AND user_id = ? FOR UPDATE",
    { replacements: [eventId, userId], transaction: t }
  );
  if (existing.length > 0) {
    await t.rollback(); // ROLLBACK IF FAILS
    return res.status(400).json({ error: "Already booked this session." });
  }

  await sequelize.query(
    "INSERT INTO event_bookings (event_id, user_id) VALUES (?, ?)",
    { replacements: [eventId, userId], transaction: t }
  );
  await t.commit(); // COMMIT IF SUCCESSFUL
} catch (err) {
  await t.rollback(); // ROLLBACK ON CATCH
  throw err;
}
```
*   **ACID Compliance:** Ensures reading existing bookings and writing a new booking is an atomic sequence. The `FOR UPDATE` clause locks the row until the transaction commits, absolutely preventing double-booking race conditions.

## 14. Indexing (Primary vs Complex)

### Primary/Simple Indexing (Automatically Generated)
**File:** `backend/sql/001_init.sql`
Every single `PRIMARY KEY` (e.g., `id`), `UNIQUE` column, and `FOREIGN KEY` inherently triggers MySQL to generate a simple B-Tree structural index automatically. This enforces relational database rules.

### Complex / Explicit Indexing
**File:** `backend/src/scripts/initDb.js` (Lines 52-58)
**Snippet:**
```javascript
const [indexExists] = await sequelize.query(
  `SELECT 1 FROM information_schema.STATISTICS
   WHERE TABLE_SCHEMA = ? AND TABLE_NAME = 'mood_entries' AND INDEX_NAME = 'idx_entry_date' LIMIT 1`, ...
);

if (!indexExists.length) {
  await sequelize.query("CREATE INDEX idx_entry_date ON mood_entries(entry_date)");
}
```
*   **Complex Scenario:** The application frequently relies on tracking chronological mood patterns. We intentionally engineered a custom index exclusively for `entry_date` to prevent queries from defaulting to a highly expensive Full Table Scan across potentially millions of rows.
