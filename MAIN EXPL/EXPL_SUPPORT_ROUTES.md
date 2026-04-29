# Comprehensive File Analysis: `backend/src/routes/supportRoutes.js`

This file handles the conversational AI feature of the app and emergency SOS tracking.

## 1. POST `/ai` (AI Conversation Logging)

### The SQL Query
```javascript
    await sequelize.query(
      "INSERT INTO ai_support_messages (user_id, user_message, ai_reply) VALUES (?, ?, ?)",
      { replacements: [req.user.sub, payload.user_message, aiReply] }
    );
```
### Deep Dive: Concepts and Theory
- **SQL Category**: DML (Data Manipulation).
- **Audit Logging**: This table strictly exists as an audit/history log. It adheres to **3NF Normalization** because the message attributes depend exclusively on the auto-incrementing Primary Key, and the `user_id` Foreign Key properly links it to the exact user who initiated the chat.

## 2. GET `/ai/history` (Retrieving Chat History)

### The SQL Query
```javascript
    const [rows] = await sequelize.query(
      "SELECT user_message, ai_reply, created_at FROM ai_support_messages WHERE user_id = ? ORDER BY created_at ASC",
      { replacements: [req.user.sub] }
    );
```
### Deep Dive: Concepts and Theory
- **SQL Category**: DQL (Data Query Language).
- **Ordering**: `ORDER BY created_at ASC`. Because this powers a chat interface, we want the oldest messages at the top of the array (Index 0) and the newest messages at the bottom, so we sort in `ASC` (Ascending) order.
- **Tenant Isolation**: `WHERE user_id = ?` ensures a user cannot request the deeply personal therapy chats of another user.

---

## 3. POST `/sos` (Emergency Alerts)

### The SQL Query
```javascript
  await sequelize.query("INSERT INTO sos_alerts (user_id) VALUES (?)", {
    replacements: [req.user.sub],
  });
```
### Deep Dive: Concepts and Theory
- **Implicit Schema Defaults**: Note that we only insert `user_id`. The `001_init.sql` schema defined this table with `id AUTO_INCREMENT` and `created_at DEFAULT CURRENT_TIMESTAMP`. By omitting those columns in our `INSERT` statement, we allow the database engine to automatically fill them in.
