# Comprehensive File Analysis: `backend/src/routes/communityRoutes.js`

This file manages the Community Chat Rooms. It uses advanced Aggregation (`GROUP BY`, `COUNT`, `MAX`), Subqueries, and complex Data Query Language (DQL) structures to build a complete view of active rooms.

## 1. GET `/rooms` (Fetching Rooms and Metadata)

### The SQL Query
```javascript
    SELECT r.id, r.name, r.description, r.created_by,
           COUNT(DISTINCT m.user_id) as participants_count,
           MAX(m.created_at) as last_message_at,
           (SELECT message FROM community_messages WHERE room_id = r.id ORDER BY created_at DESC LIMIT 1) as last_message
    FROM community_rooms r
    LEFT JOIN community_messages m ON r.id = m.room_id
    GROUP BY r.id, r.name, r.description, r.created_by
    ORDER BY participants_count DESC, r.id
```

### Deep Dive: Concepts and Theory
- **Aggregate Functions**: 
  - `COUNT(DISTINCT m.user_id)`: Counts unique users who have posted in a room. Without `DISTINCT`, one user posting 10 times would inflate the `participants_count` to 10.
  - `MAX(m.created_at)`: Finds the absolute newest timestamp out of all messages linked to this room.
- **GROUP BY**: 
  - Because we used Aggregate Functions (`COUNT` and `MAX`), we *must* list all non-aggregated columns in the `GROUP BY` clause. This tells the database exactly how to "collapse" the many message rows into single room rows.
- **Correlated Subquery**: 
  - `(SELECT message FROM ... ORDER BY created_at DESC LIMIT 1) as last_message`
  - For every room it processes, the database executes this subquery to fetch the actual text of the most recent message.
- **LEFT JOIN**: 
  - If a room was just created and has zero messages, `community_messages` will have no matching rows. A standard `INNER JOIN` would cause the entire room to disappear from the result set! `LEFT JOIN` preserves the room and returns `participants_count` as 0.

---

## 2. DELETE `/rooms/:roomId` and PUT `/rooms/:roomId` (Authorization & DML)

### The SQL Query (Authorization Check)
```javascript
  const [rooms] = await sequelize.query("SELECT created_by FROM community_rooms WHERE id = ?", { replacements: [req.params.roomId] });
```
### Deep Dive: Concepts and Theory
- **Role-Based Access Control (RBAC)**: Before issuing a `DELETE` or `UPDATE`, we must read the data to ensure the `req.user.sub` matches the `created_by` field. If they don't match, we check if `req.user.role === 'expert'`. This logic implements secure ownership verification.

### The SQL Query (Deletion)
```javascript
  await sequelize.query("DELETE FROM community_rooms WHERE id = ?", { replacements: [req.params.roomId] });
```
- **Referential Integrity (ON DELETE CASCADE)**: When this `DELETE` runs, it destroys the room. What happens to the thousands of messages in `community_messages` tied to `room_id`? 
  - Because `001_init.sql` defined the Foreign Key with `ON DELETE CASCADE`, the database engine intercepts the deletion of the room and atomically deletes every single child message. This prevents "Orphaned Records" and requires 0 extra lines of code in Node.js.

---

## 3. GET `/rooms/:roomId/messages` (Fetching Chat History)

### The SQL Query
```javascript
    SELECT m.id, m.room_id, m.user_id, m.message, m.is_anonymous, m.created_at, u.name AS user_name, u.avatar_url 
    FROM community_messages m 
    JOIN users u ON u.id = m.user_id 
    WHERE m.room_id = ? 
    ORDER BY m.created_at DESC 
    LIMIT 50
```

### Deep Dive: Concepts and Theory
- **INNER JOIN (`JOIN`)**: We join `users u` on `m.user_id = u.id` to retrieve the author's avatar and name. Because a message *cannot* exist without a user (enforced by the Foreign Key), an `INNER JOIN` is perfectly safe here.
- **Pagination Strategy**: `ORDER BY m.created_at DESC LIMIT 50`. It fetches the 50 most recent messages. In the Node.js code, we run `mapped.reverse()` before sending it to the client. This is a common chat app pattern: fetch newest first (to limit data), then flip the array so they render top-to-bottom chronologically in the UI.
- **Data Obfuscation**: In Node.js, we check `r.is_anonymous`. If true, we manually overwrite the `user_name` with `'Anonymous'` and `avatar_url` with `null`. While the database stores who actually sent it, the API guarantees anonymity to other clients.
