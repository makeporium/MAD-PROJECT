const express = require("express");
const { z } = require("zod");
const sequelize = require("../config/database");
const auth = require("../middleware/authMiddleware");

const router = express.Router();

const saveMoodSchema = z.object({
  mood_level: z.number().int().min(1).max(5),
  note: z.string().max(500).optional(),
  entry_date: z.string().optional(),
});

router.use(auth);

router.get("/", async (req, res) => {
  const [rows] = await sequelize.query(
    "SELECT id, mood_level, note, entry_date, created_at FROM mood_entries WHERE user_id = ? ORDER BY entry_date DESC, created_at DESC LIMIT 30",
    { replacements: [req.user.sub] }
  );
  res.status(200).json(rows);
});

router.post("/", async (req, res) => {
  const payload = saveMoodSchema.parse(req.body);
  await sequelize.query(
    "INSERT INTO mood_entries (user_id, mood_level, note, entry_date) VALUES (?, ?, ?, COALESCE(?, CURDATE()))",
    { replacements: [req.user.sub, payload.mood_level, payload.note || null, payload.entry_date || null] }
  );
  res.status(201).json({ message: "Mood entry saved" });
});

module.exports = router;
