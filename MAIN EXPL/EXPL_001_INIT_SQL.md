# Comprehensive File Analysis: `backend/sql/001_init.sql`

This file is the bedrock of the entire application. It uses Data Definition Language (DDL) to establish the schema, enforce normalization, build relationships, and protect data integrity. We will analyze it line by line and concept by concept.

## The Database Creation
```sql
CREATE DATABASE IF NOT EXISTS mad_app;
USE mad_app;
```
- **Concept**: Schema Initialization. The `IF NOT EXISTS` clause prevents destructive overrides. `USE` switches the operational context of the DBMS to our specific schema.

## Entity 1: `users`
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
- **Entity & Attributes**: This defines the core `User` entity.
- **Keys**: 
  - **Primary Key (PK)**: `id BIGINT`. It's a surrogate key. We use `BIGINT` to ensure we never run out of IDs. `AUTO_INCREMENT` delegates the generation of this unique identifier directly to the SQL engine (MySQL).
  - **Candidate Key**: `firebase_uid`. It is marked `NOT NULL UNIQUE`. While `id` is the PK we use for foreign key relationships, `firebase_uid` could theoretically serve as the PK. We enforce `UNIQUE` so no two users can share an authentication identity.
- **Normalization (1NF/2NF/3NF)**: This table is in 3NF. All attributes depend entirely on the primary key `id`.
- **Functions (Scalar)**: `CURRENT_TIMESTAMP` is a scalar function executed during row insertion. `ON UPDATE CURRENT_TIMESTAMP` is a native trigger that fires whenever a DML `UPDATE` is run against the row.

## Entity 2: `testimonials`
```sql
CREATE TABLE IF NOT EXISTS testimonials (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  content TEXT NOT NULL,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);
```
- **Relationship (1:N)**: A user can write many testimonials.
- **Keys**: `user_id` is the **Foreign Key (FK)**.
- **DBMS Concept - Referential Integrity**: `ON DELETE CASCADE`. If the parent row in `users` is deleted, the DBMS automatically deletes all dependent rows in `testimonials`. This physically guarantees that "orphaned" records cannot exist.

## Entity 3: `mood_entries`
```sql
CREATE TABLE IF NOT EXISTS mood_entries (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  mood_level TINYINT NOT NULL,
  note VARCHAR(500),
  entry_date DATE NOT NULL,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);
```
- **Optimization**: `mood_level` uses `TINYINT` instead of `INT`. `TINYINT` uses 1 byte (range 0-255), perfectly sizing it for a 1-5 mood scale, saving massive memory across millions of rows.
- **Indexing Opportunity**: Because users often look up their moods by date, adding `CREATE INDEX idx_entry_date ON mood_entries(entry_date);` would drastically speed up `SELECT` statements for the calendar.

## Entity 4: `recommendations`
```sql
CREATE TABLE IF NOT EXISTS recommendations (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  title VARCHAR(200) NOT NULL,
  category VARCHAR(80) NOT NULL,
  description TEXT,
  match_score INT DEFAULT 0,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```
- **Static Entity**: No foreign keys. This acts as a global lookup table.

## Entity 5 & 6: `community_rooms` and `community_messages`
```sql
CREATE TABLE IF NOT EXISTS community_rooms (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  name VARCHAR(120) NOT NULL,
  description TEXT,
  created_by BIGINT,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (created_by) REFERENCES users(id) ON DELETE SET NULL
);

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
- **Complex Relationships**: `community_messages` has two Foreign Keys. It is essentially a transactional entity connecting a `User` to a `Room` over time.
- **ON DELETE SET NULL**: If the user who created the room (`created_by`) deletes their account, we don't want the entire room to be deleted (which `CASCADE` would do). `SET NULL` just removes their name as the owner but keeps the room alive for others.

## Entity 7: `events`
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

*(Note: In the full app logic, `event_bookings` is the bridge entity resolving the M:N relationship between users and events. It is defined in our database logic and dynamically created if missing, or defined inside `002_seed.sql` additions.)*

## Entity 8: `info_resources`
```sql
CREATE TABLE IF NOT EXISTS info_resources (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  title VARCHAR(200) NOT NULL,
  topic VARCHAR(60) NOT NULL,
  excerpt TEXT,
  content TEXT,
  image_url TEXT,
  tags VARCHAR(255),
  views INT DEFAULT 0,
  author_id BIGINT,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  FOREIGN KEY (author_id) REFERENCES users(id) ON DELETE SET NULL
);
```
- **Redundancy & Normalization Analysis**: The `tags` column stores comma-separated strings (e.g., "sleep, anxiety"). Technically, this violates First Normal Form (1NF) because the column contains non-atomic values. A strictly normalized schema (up to 3NF) would create a `tags` table and a `resource_tags` bridge table. However, storing it as a CSV string is a deliberate denormalization tradeoff to simplify the AI vector/text searching capabilities.

## Entity 9 & 10: `ai_support_messages` and `sos_alerts`
```sql
CREATE TABLE IF NOT EXISTS ai_support_messages (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  user_message VARCHAR(500) NOT NULL,
  ai_reply VARCHAR(500) NOT NULL,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS sos_alerts (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);
```
- **Context**: These store historical logs. `ai_support_messages` stores the exact input and output of the LLM for persistent history. `sos_alerts` acts as an audit log.
