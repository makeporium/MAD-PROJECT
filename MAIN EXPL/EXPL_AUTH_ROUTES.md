# Comprehensive File Analysis: `backend/src/routes/authRoutes.js`

This document provides a massive, deep-dive explanation of the SQL queries, Data Manipulation Language (DML), and Data Query Language (DQL) used exclusively inside `authRoutes.js`. 

## 1. POST `/google` (Authentication & Upsert)

### The SQL Query
```javascript
    await sequelize.query(
      `INSERT INTO users (firebase_uid, email, name, avatar_url)
       VALUES (?, ?, ?, ?)
       ON DUPLICATE KEY UPDATE email=VALUES(email), name=VALUES(name), avatar_url=VALUES(avatar_url), updated_at=CURRENT_TIMESTAMP;`,
      { replacements: [decoded.uid, decoded.email || null, decoded.name || null, decoded.picture || null] }
    );
```

### Deep Dive: Concepts and Theory
- **SQL Category**: DML (Data Manipulation Language).
- **Parameterized Query**: The `(?, ?, ?, ?)` syntax replaces raw strings with parameters. This forces the SQL engine to treat the inputs purely as string literals, completely nullifying **SQL Injection** attacks.
- **Candidate Key Constraint**: In our schema (`001_init.sql`), `firebase_uid` is marked as `UNIQUE`. This enforces a mathematical guarantee at the B-Tree index level that no two users can exist with the same Firebase ID.
- **The Upsert Operation**: `ON DUPLICATE KEY UPDATE`. 
  - **Why used?**: Without this, we would need to write two queries: `SELECT * FROM users WHERE firebase_uid = ?`. If found, `UPDATE`. If not found, `INSERT`. That introduces a **Race Condition** if two requests arrive simultaneously, and takes double the network trips.
  - The `UPSERT` command makes this action **Atomic** (from the ACID principles). The database attempts an `INSERT`. If the B-Tree index throws a collision error on the unique `firebase_uid`, it immediately pivots to an `UPDATE` command on that exact row.
  - `VALUES(email)` extracts the value that *would* have been inserted, and assigns it to the existing row.
- **Scalar Functions**: `CURRENT_TIMESTAMP`. We manually trigger the timestamp update here.

---

## 2. POST `/google` (Fetching the Synced User)

### The SQL Query
```javascript
    const [userRows] = await sequelize.query(
      "SELECT id, firebase_uid, email, name, avatar_url, role FROM users WHERE firebase_uid = ? LIMIT 1",
      { replacements: [decoded.uid] }
    );
```

### Deep Dive: Concepts and Theory
- **SQL Category**: DQL (Data Query Language).
- **Query Optimization**: We only `SELECT` the exact 6 columns needed to generate a JWT token. This prevents fetching unnecessary text payloads and saves memory over a `SELECT *` query.
- **Filtering & The LIMIT Clause**: 
  - `WHERE firebase_uid = ?`: Because `firebase_uid` has a `UNIQUE` constraint, MySQL automatically creates a Unique Index for it. Searching an indexed column operates in O(log N) time complexity.
  - `LIMIT 1`: Even though the DBMS knows it's unique, appending `LIMIT 1` is an explicit directive to the Query Optimizer to immediately halt the search cursor once the first match is found, saving CPU cycles.

---

## 3. GET `/me` (Profile Retrieval)

### The SQL Query
```javascript
  const [userRows] = await sequelize.query(
    "SELECT id, firebase_uid, email, name, avatar_url, role FROM users WHERE id = ? LIMIT 1",
    { replacements: [req.user.sub] }
  );
```

### Deep Dive: Concepts and Theory
- **Primary Key Lookups**: Here, the `WHERE id = ?` clause filters by the Primary Key. Primary Keys are always indexed as a **Clustered Index** in InnoDB (MySQL's default engine). This means the actual table data is physically stored on the disk in the order of the Primary Key, making this the fastest possible query in a relational database.

---

## 4. PUT `/me` (Profile Update)

### The SQL Query
```javascript
  await sequelize.query(
    "UPDATE users SET name = ?, avatar_url = ? WHERE id = ?",
    { replacements: [name || null, avatar_url || null, req.user.sub] }
  );
```

### Deep Dive: Concepts and Theory
- **SQL Category**: DML.
- **Referential Protection**: The query explicitly filters by `req.user.sub` (which is extracted securely from the JWT token via middleware). This prevents a malicious user from passing an arbitrary `id` in the JSON body and updating someone else's profile.
- **Handling Nulls**: `name || null`. In SQL, `NULL` represents the absolute absence of a value (it is not the same as an empty string `""`). We correctly translate JavaScript undefined/empty values into SQL `NULL`.

---

## 5. PUT `/users/:id/role` (Role Management)

### The SQL Query
```javascript
  await sequelize.query(
    "UPDATE users SET role = ? WHERE id = ?",
    { replacements: [role, req.params.id] }
  );
```

### Deep Dive: Concepts and Theory
- **Entity Attributes**: `role` is an attribute of the `users` entity. It has a default value of `'user'` per the schema.
- **Data Integrity**: Before executing this query, the Node.js code explicitly checks: `if (!["user", "expert"].includes(role))`. While this enforces validation on the backend server, a stricter Database Administrator might enforce this at the DBMS level using a `CHECK CONSTRAINT` or an `ENUM` type on the `role` column during table creation. By doing it in application code, we maintain schema flexibility if we ever want to add an "admin" role without altering the table structure.
