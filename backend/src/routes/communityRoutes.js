const express = require("express");
const { z } = require("zod");
const sequelize = require("../config/database");
const auth = require("../middleware/authMiddleware");

const router = express.Router();
const roomSchema = z.object({ name: z.string().min(1), description: z.string().min(1) });
const messageSchema = z.object({ message: z.string().min(1).max(300), isAnonymous: z.boolean().optional() });
router.use(auth);

router.get("/rooms", async (_req, res) => {
  /*
  // OLD CODE: Grouped rooms by participants but returned all rooms.
  const [rows] = await sequelize.query(`
    SELECT r.id, r.name, r.description, r.created_by,
           COUNT(DISTINCT m.user_id) as participants_count,
           MAX(m.created_at) as last_message_at,
           (SELECT message FROM community_messages WHERE room_id = r.id ORDER BY created_at DESC LIMIT 1) as last_message
    FROM community_rooms r
    LEFT JOIN community_messages m ON r.id = m.room_id
    GROUP BY r.id, r.name, r.description, r.created_by
    ORDER BY participants_count DESC, r.id
  `);
  */

  // NEW CODE: Demonstrates a HAVING clause after GROUP BY. 
  // We use >= 0 so we don't accidentally hide newly created, empty rooms!
  const [rows] = await sequelize.query(`
    SELECT r.id, r.name, r.description, r.created_by,
           COUNT(DISTINCT m.user_id) as participants_count,
           MAX(m.created_at) as last_message_at,
           (SELECT message FROM community_messages WHERE room_id = r.id ORDER BY created_at DESC LIMIT 1) as last_message
    FROM community_rooms r
    LEFT JOIN community_messages m ON r.id = m.room_id
    GROUP BY r.id, r.name, r.description, r.created_by
    HAVING participants_count >= 0
    ORDER BY participants_count DESC, r.id
  `);
  res.status(200).json(rows);
});

router.post("/rooms", async (req, res) => {
  const { name, description } = roomSchema.parse(req.body);
  await sequelize.query(
    "INSERT INTO community_rooms (name, description, created_by) VALUES (?, ?, ?)",
    { replacements: [name, description, req.user.sub] }
  );
  res.status(201).json({ message: "Room created" });
});

router.delete("/rooms/:roomId", async (req, res) => {
  const [rooms] = await sequelize.query("SELECT created_by FROM community_rooms WHERE id = ?", { replacements: [req.params.roomId] });
  if (!rooms.length) return res.status(404).json({ message: "Room not found" });
  if (rooms[0].created_by !== req.user.sub && req.user.role !== 'expert') {
    return res.status(403).json({ message: "Forbidden" });
  }
  await sequelize.query("DELETE FROM community_rooms WHERE id = ?", { replacements: [req.params.roomId] });
  res.status(200).json({ message: "Room deleted" });
});

router.put("/rooms/:roomId", async (req, res) => {
  const { name, description } = roomSchema.parse(req.body);
  const [rooms] = await sequelize.query("SELECT created_by FROM community_rooms WHERE id = ?", { replacements: [req.params.roomId] });
  if (!rooms.length) return res.status(404).json({ message: "Room not found" });
  if (rooms[0].created_by !== req.user.sub && req.user.role !== 'expert') {
    return res.status(403).json({ message: "Forbidden" });
  }
  await sequelize.query(
    "UPDATE community_rooms SET name = ?, description = ? WHERE id = ?",
    { replacements: [name, description, req.params.roomId] }
  );
  res.status(200).json({ message: "Room updated" });
});

router.get("/rooms/:roomId/messages", async (req, res) => {
  const [rows] = await sequelize.query(
    "SELECT m.id, m.room_id, m.user_id, m.message, m.is_anonymous, m.created_at, u.name AS user_name, u.avatar_url FROM community_messages m JOIN users u ON u.id = m.user_id WHERE m.room_id = ? ORDER BY m.created_at DESC LIMIT 50",
    { replacements: [req.params.roomId] }
  );
  const mapped = rows.map(r => ({
    ...r,
    user_name: r.is_anonymous ? 'Anonymous' : r.user_name,
    avatar_url: r.is_anonymous ? null : r.avatar_url
  }));
  res.status(200).json(mapped.reverse());
});

router.post("/rooms/:roomId/messages", async (req, res) => {
  const { message, isAnonymous } = messageSchema.parse(req.body);
  await sequelize.query(
    "INSERT INTO community_messages (room_id, user_id, message, is_anonymous) VALUES (?, ?, ?, ?)",
    { replacements: [req.params.roomId, req.user.sub, message, isAnonymous || false] }
  );
  res.status(201).json({ message: "Message sent" });
});

module.exports = router;
