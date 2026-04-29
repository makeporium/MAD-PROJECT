# Database Schema and Theory: The Mother's Nest Backend

This document serves as a massive, deep-dive explanation of the database structure, SQL theory, and DBMS concepts used in the project. If you are learning backend development, reading this will equip you with the knowledge to write your own queries and understand exactly why this schema is structured the way it is.

## 1. Relational Database Concepts & Normalization

Our database is built on Relational Database Management System (RDBMS) principles. The data is divided into **Entities** (tables) and **Attributes** (columns). 

### Entities and Keys
- **Entity**: An object or concept we want to store data about (e.g., `users`, `events`).
- **Primary Key (PK)**: A unique identifier for each record in a table. In our app, we use `id BIGINT PRIMARY KEY AUTO_INCREMENT` for almost all tables. This is called a *Surrogate Key* because it has no intrinsic meaning—it just counts up (1, 2, 3...) ensuring absolute uniqueness.
- **Candidate Key**: A column or set of columns that *could* uniquely identify a row. For example, in the `users` table, `firebase_uid` is marked as `UNIQUE`, making it a candidate key. We chose `id` as the PK for simplicity, but `firebase_uid` enforces uniqueness at the DB level.
- **Foreign Key (FK)**: A field in one table that links to the Primary Key of another table. This enforces **Referential Integrity**.
  - *Example*: `user_id` in the `mood_entries` table links to `id` in the `users` table. If a user is deleted, `ON DELETE CASCADE` automatically deletes all their mood entries to prevent "orphaned" records.

### Normalization (Up to 3NF)
Normalization is the process of organizing data to minimize redundancy (duplicate data) and dependency issues.
- **1NF (First Normal Form)**: Every column must contain atomic (indivisible) values, and each record must be unique. We don't store a comma-separated list of "booked_events" inside the user table. Instead, we use separate rows.
- **2NF (Second Normal Form)**: Must be in 1NF, and all non-key attributes must be fully dependent on the primary key. 
- **3NF (Third Normal Form)**: Must be in 2NF, and no transitive dependencies exist. (Non-key columns shouldn't depend on other non-key columns).
  - *Proof in App*: When a user books an event, we don't copy the user's `name` and `email` into the `event_bookings` table. We only store `user_id`. If the user changes their name, we only update it in the `users` table, eliminating update anomalies and data redundancy.

### Relationships and Cardinality
- **1:N (One-to-Many)**: A `user` can have many `reminders` or `mood_entries`. The "Many" side holds the Foreign Key.
- **M:N (Many-to-Many)**: A `user` can book many `events`, and an `event` can be booked by many `users`. Relational databases cannot handle M:N directly. We solve this using a **Bridge Entity** (also known as a junction table or associative entity).
  - *Example in our App*: The `event_bookings` table. It contains `user_id` and `event_id`. This correctly breaks the M:N relationship into two 1:N relationships.

---

## 2. Table-by-Table Schema Breakdown

Here is a breakdown of the DDL (Data Definition Language) used in `001_init.sql` and `002_seed.sql`. DDL includes commands like `CREATE`, `ALTER`, and `DROP` that define the structure of the database.

### The `users` Table
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
- **Why used?**: This is the core entity. All authentication is handled by Firebase, but we need local profiles to link data.
- **DBMS Concept**: `DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP` is a powerful DBMS trigger. Whenever an `UPDATE` statement is run on a row, the database automatically changes `updated_at` to the exact current time without us writing code in Node.js.

### The `events` and Bridge Entities
```sql
CREATE TABLE IF NOT EXISTS events (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  title VARCHAR(150) NOT NULL,
  description TEXT,
  event_date DATETIME NOT NULL,
  mode VARCHAR(30) DEFAULT 'online',
  join_link TEXT,
  expert_id BIGINT,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (expert_id) REFERENCES users(id) ON DELETE SET NULL
);
```
- **FK Constraint**: `ON DELETE SET NULL`. If an expert deletes their account, we don't necessarily want to delete the historical record of the event. Instead, the `expert_id` simply becomes `NULL`.

### The `ai_support_messages` Table
```sql
CREATE TABLE IF NOT EXISTS ai_support_messages (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  user_message VARCHAR(500) NOT NULL,
  ai_reply VARCHAR(500) NOT NULL,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);
```
- **Data Integrity**: By forcing `NOT NULL` on message columns, we guarantee via DDL that empty chat bubbles cannot be inserted into the database. 

---

## 3. SQL Core Concepts Used in the App

### DML (Data Manipulation Language) vs DQL (Data Query Language)
- **DML**: `INSERT`, `UPDATE`, `DELETE`. These modify the data.
- **DQL**: `SELECT`. This queries data without modifying it. We use parameterized queries `(?, ?)` in Node.js to prevent **SQL Injection** attacks.

### Joins
- **INNER JOIN**: Returns only rows that have matching values in both tables.
- **LEFT JOIN**: Returns all rows from the left table, and the matched rows from the right table. Used heavily in our `eventsRoutes.js` to get events *even if* no expert is assigned.

### Aggregations & Group By
- **Aggregate Functions**: `COUNT()`, `SUM()`, `MAX()`. These perform a calculation on a set of values and return a single value.
- **GROUP BY**: Groups rows that have the same values into summary rows.
- **HAVING**: Similar to `WHERE`, but `WHERE` filters rows *before* aggregation, while `HAVING` filters *after* aggregation.

### Subqueries
A query nested inside another query. We use Correlated Subqueries in our app to count how many bookings exist for a specific event while simultaneously selecting the event details.

### Transactions (ACID Properties)
Though explicitly writing `START TRANSACTION`, `COMMIT`, and `ROLLBACK` is sometimes handled implicitly by Sequelize, the concept remains:
- **Atomicity**: All operations succeed or fail together.
- **Consistency**: Database rules (like FKs) are never violated.
- **Isolation**: Concurrent transactions don't interfere.
- **Durability**: Once saved, it stays saved.
*(Note: Booking a session could utilize transactions to ensure double-booking doesn't happen).*

### Indexing
If a table gets massive (e.g., millions of mood entries), queries like `WHERE user_id = ?` become slow. By creating an **Index** (`CREATE INDEX idx_user_id ON mood_entries(user_id)`), the DBMS creates an optimized data structure (like a B-Tree) to find rows instantly without scanning the whole table.

*(Continue reading the Route Analysis markdown files for line-by-line breakdowns of exactly how these queries are executed in Node.js).*
