# Comprehensive File Analysis: `backend/src/routes/eventsRoutes.js`

This file handles the most complex relational logic in the application. It heavily utilizes Joins, Correlated Subqueries, Aliasing, and Conditional Aggregations.

## 1. GET `/` (Fetch All Upcoming Sessions)

### The SQL Query
```javascript
    const [rows] = await sequelize.query(
      `SELECT e.id, e.title, e.description, e.event_date, e.mode, e.join_link, e.expert_id, u.name as expert_name,
              CASE WHEN eb.id IS NULL THEN 0 ELSE 1 END AS is_booked,
              (SELECT COUNT(*) FROM event_bookings WHERE event_id = e.id) AS booking_count
       FROM events e 
       LEFT JOIN users u ON e.expert_id = u.id 
       LEFT JOIN event_bookings eb ON eb.event_id = e.id AND eb.user_id = ?
       ORDER BY e.event_date ASC`,
      { replacements: [userId] }
    );
```

### Deep Dive: Concepts and Theory
- **Table Aliasing**: `events e`, `users u`, `event_bookings eb`. Aliasing isn't just about typing less; it forces exact column resolution. If `users` and `events` both had an `id` column, querying `SELECT id` would throw an "Ambiguous Column" error.
- **Column Aliasing**: `u.name as expert_name`. This dynamically renames the output column in the final result set, converting physical DB names to presentation-ready JSON keys.
- **The CASE Statement**: `CASE WHEN eb.id IS NULL THEN 0 ELSE 1 END AS is_booked`. 
  - This is conditional projection (like an if/else block inside SQL).
  - Because of the `LEFT JOIN event_bookings eb ... AND eb.user_id = ?`, if the user has NOT booked the event, the join finds no matching row, and `eb.id` will physically be `NULL` in the virtual row. The `CASE` statement catches this `NULL` and outputs `0` (false), allowing the frontend UI to toggle the "Book" vs "Already Booked" button state dynamically.
- **Correlated Subquery (Scalar Projection)**: `(SELECT COUNT(*) FROM event_bookings WHERE event_id = e.id) AS booking_count`.
  - An aggregate `COUNT(*)` wrapped in parentheses.
  - It's "Correlated" because it references `e.id` from the outer main query. 
  - **Performance Note**: The engine must execute this sub-select for *every single row* returned by the outer query. This is slightly inefficient but perfectly acceptable for small datasets. For millions of events, a `GROUP BY` aggregate join would be more optimized.
- **LEFT JOIN vs INNER JOIN**: We specifically use `LEFT JOIN` on `users`. If an event was created but hasn't been assigned an expert yet (`expert_id` is NULL), an `INNER JOIN` would drop the entire event from the result set. `LEFT JOIN` preserves the event and outputs `expert_name` as `NULL`.

---

## 2. POST `/` (Create an Event - Experts Only)

### The SQL Query
```javascript
    const [result] = await sequelize.query(
      "INSERT INTO events (title, description, event_date, mode, join_link, expert_id) VALUES (?, ?, ?, ?, ?, ?)",
      { replacements: [title, description, event_date, mode || "online", join_link, userId] }
    );
```

### Deep Dive: Concepts and Theory
- **SQL Category**: DML (Data Manipulation).
- **Foreign Key Allocation**: We map the requesting user's JWT ID (`userId`) into the `expert_id` column. This establishes a physical **1:N Relationship** where one expert creates many events. 
- **Defaults**: `mode || "online"`. While the database schema (`001_init.sql`) defines `DEFAULT 'online'`, supplying it here at the code level guarantees consistency even if the DB default is altered later.

---

## 3. POST `/:id/book` (Booking a Session)

### The SQL Queries
```javascript
    const [existing] = await sequelize.query(
      "SELECT id FROM event_bookings WHERE event_id = ? AND user_id = ?",
      { replacements: [eventId, userId] }
    );

    await sequelize.query(
      "INSERT INTO event_bookings (event_id, user_id) VALUES (?, ?)",
      { replacements: [eventId, userId] }
    );
```

### Deep Dive: Concepts and Theory
- **Bridge Entity**: `event_bookings` is a pure Junction Table breaking down the **Many-to-Many (M:N)** relationship between Users and Events.
- **Double-booking Prevention**: The first `SELECT` query checks if the row exists. If so, it returns an HTTP 400.
- **Normalization (3NF)**: We only insert the `event_id` and `user_id`. We *never* insert the event's title or the user's name into the bookings table, maintaining absolute adherence to 3rd Normal Form.

---

## 4. GET `/my-sessions` (Expert Dashboard)

### The SQL Queries
```javascript
    const [events] = await sequelize.query(
      "SELECT id, title, description, event_date, join_link FROM events WHERE expert_id = ? ORDER BY event_date ASC",
      { replacements: [userId] }
    );

    for (let event of events) {
      const [bookings] = await sequelize.query(
        `SELECT u.id, u.name, u.email 
         FROM event_bookings eb 
         JOIN users u ON eb.user_id = u.id 
         WHERE eb.event_id = ?`,
        { replacements: [event.id] }
      );
      event.bookings = bookings;
    }
```

### Deep Dive: Concepts and Theory
- **The "N+1" Query Problem**: This is a classic database anti-pattern in action. 
  - **Query 1**: Fetches `N` events.
  - **The Loop**: Runs a new `SELECT` query `N` times for every event found.
  - While it's easy to write and produces deeply nested JSON `[ { id: 1, bookings: [ {name: 'John'} ] } ]`, it generates excessive network overhead between Node.js and MySQL.
  - **Alternative (DBMS Optimization)**: Using an `INNER JOIN` in a single query and grouping the nested data in code using a `reduce` function would condense `N+1` roundtrips into exactly `1` roundtrip.
- **INNER JOIN (`JOIN`)**: Used explicitly in the sub-query (`JOIN users u ON eb.user_id = u.id`). This works perfectly because a booking *must* have a user. If there is no user, the booking is physically invalid due to the Foreign Key constraint.

---

## 5. POST `/expert-profile` (Upserting Profiles)

### The SQL Query
```javascript
    const [existing] = await sequelize.query("SELECT id FROM expert_profiles WHERE user_id = ?", {
      replacements: [userId],
    });
    if (existing.length > 0) {
      await sequelize.query(
        "UPDATE expert_profiles SET specialty=?, location=?, format=?, availability=?, fee=? WHERE user_id=?",
        { replacements: [specialty, location, format, availability, fee, userId] }
      );
    } else {
      await sequelize.query(
        "INSERT INTO expert_profiles (user_id, specialty, location, format, availability, fee) VALUES (?, ?, ?, ?, ?, ?)",
        { replacements: [userId, specialty, location, format, availability, fee] }
      );
    }
```

### Deep Dive: Concepts and Theory
- **Relationship Type**: **1:1 (One-to-One)**. By looking up by `user_id` and forcing an UPDATE if found, we guarantee that one User can only ever have exactly one `expert_profile`.
- **Application-Level Upsert**: Unlike `authRoutes.js` where we used `ON DUPLICATE KEY UPDATE` to let the DBMS handle it, here we do it procedurally in code. 
- **Transactions**: Since this requires two queries (Read, then Write), it is susceptible to race conditions. Wrapping this in `START TRANSACTION;` and `COMMIT;` would guarantee absolute integrity.
