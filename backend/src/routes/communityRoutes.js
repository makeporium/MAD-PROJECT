const express = require("express");
const { z } = require("zod");
const sequelize = require("../config/database");
const auth = require("../middleware/authMiddleware");

const router = express.Router();
const messageSchema = z.object({ message: z.string().min(1).max(300) });
router.use(auth);

router.get("/rooms", async (_req, res) => {
  const [rows] = await sequelize.query("SELECT id, name, description FROM community_rooms ORDER BY id");
  res.status(200).json(rows);
});

router.get("/rooms/:roomId/messages", async (req, res) => {
  const [rows] = await sequelize.query(
    "SELECT m.id, m.room_id, m.message, m.created_at, u.name AS user_name FROM community_messages m JOIN users u ON u.id = m.user_id WHERE m.room_id = ? ORDER BY m.created_at DESC LIMIT 50",
    { replacements: [req.params.roomId] }
  );
  res.status(200).json(rows.reverse());
});

router.post("/rooms/:roomId/messages", async (req, res) => {
  const { message } = messageSchema.parse(req.body);
  await sequelize.query(
    "INSERT INTO community_messages (room_id, user_id, message) VALUES (?, ?, ?)",
    { replacements: [req.params.roomId, req.user.sub, message] }
  );
  res.status(201).json({ message: "Message sent" });
});

module.exports = router;
