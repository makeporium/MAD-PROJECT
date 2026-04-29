# Comprehensive File Analysis: `backend/src/routes/calendarRoutes.js`

This file is a fantastic example of utilizing **Aggregate Functions**, **Scalar Date Functions**, and **Inner Joins** to generate dashboard statistics for a user.

## GET `/progress` (Aggregating User Progress)

This route runs five distinct SQL queries to build a comprehensive dashboard object.

### Query 1: Counting Therapy Sessions
```javascript
    const [[therapyRow]] = await sequelize.query(
      `SELECT COUNT(*) AS total
       FROM event_bookings eb
       JOIN events e ON e.id = eb.event_id
       WHERE eb.user_id = ?
         AND MONTH(e.event_date) = MONTH(CURRENT_DATE())
         AND YEAR(e.event_date) = YEAR(CURRENT_DATE())`,
      { replacements: [userId] }
    );
```
- **Aggregate Function (`COUNT(*)`)**: Physically counts the number of rows that survive the `WHERE` filter. `AS total` aliases the output so we can access it via `therapyRow.total`.
- **INNER JOIN (`JOIN`)**: Joins the `event_bookings` bridge entity to the `events` table to access the `event_date`.
- **Scalar Date Functions**: `MONTH()` and `YEAR()` extract the month and year from a timestamp. `CURRENT_DATE()` returns today's date. This isolates the query to *only* count events happening in the current calendar month. 
- **Performance consideration**: Wrapping indexed columns in functions (like `MONTH(e.event_date)`) generally bypasses the index (causing a full table scan). A more optimal index-friendly query would use `e.event_date >= '2026-04-01' AND e.event_date < '2026-05-01'`.

### Query 2: Counting Mood Entries
```javascript
    const [[moodRow]] = await sequelize.query(
      `SELECT COUNT(*) AS total
       FROM mood_entries
       WHERE user_id = ?
         AND MONTH(entry_date) = MONTH(CURRENT_DATE())
         AND YEAR(entry_date) = YEAR(CURRENT_DATE())`,
      { replacements: [userId] }
    );
```
- **Concept**: Identical filtering to Query 1, applied to `mood_entries` to see how many times the user checked their mood this month.

### Query 3: Counting Unique Community Rooms Joined
```javascript
    const [[communityRow]] = await sequelize.query(
      `SELECT COUNT(DISTINCT room_id) AS total
       FROM community_messages
       WHERE user_id = ?
         AND MONTH(created_at) = MONTH(CURRENT_DATE())
         AND YEAR(created_at) = YEAR(CURRENT_DATE())`,
      { replacements: [userId] }
    );
```
- **DISTINCT Keyword**: This is crucial. If a user sends 50 messages in 1 room, `COUNT(room_id)` would return `50`. We want to know how many *unique* rooms they participated in. `COUNT(DISTINCT room_id)` mathematically collapses the duplicates and returns `1`.

### Query 4: Fetching the Daily Affirmation
```javascript
    const [[affirmationRow]] = await sequelize.query(
      `SELECT t.content, u.name AS user_name
       FROM testimonials t
       JOIN users u ON u.id = t.user_id
       ORDER BY t.created_at DESC
       LIMIT 1`
    );
```
- **Limiting (Pagination)**: We use `ORDER BY DESC` combined with `LIMIT 1` to grab only the single most recently submitted testimonial in the entire app. We use an `INNER JOIN` to attach the author's name.

### Query 5: Checking Today's Mood Status
```javascript
    const [[moodTodayRow]] = await sequelize.query(
      `SELECT COUNT(*) AS total FROM mood_entries
       WHERE user_id = ? AND DATE(entry_date) = CURDATE()`,
      { replacements: [userId] }
    );
```
- **Scalar Functions**: `DATE()` strips the time from a datetime column (if applicable). `CURDATE()` returns exactly today. 
- **Application Logic**: In Node.js, we do `mood_done_today: Number(moodTodayRow.total || 0) > 0`. If the count is 1 or more, we return boolean `true` to the Android frontend.
