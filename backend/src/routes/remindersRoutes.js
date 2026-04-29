const express = require("express");
const { z } = require("zod");
const sequelize = require("../config/database");
const auth = require("../middleware/authMiddleware");

const router = express.Router();
router.use(auth);

const schema = z.object({
  title: z.string().min(1).max(120),
  remind_at: z.string().min(8),
  notes: z.string().max(250).optional(),
});

router.get("/", async (req, res) => {
  const [rows] = await sequelize.query(
    "SELECT id, title, remind_at, notes, is_done FROM reminders WHERE user_id = ? ORDER BY remind_at ASC",
    { replacements: [req.user.sub] }
  );
  res.status(200).json(rows);
});

router.post("/", async (req, res) => {
  const payload = schema.parse(req.body);
  await sequelize.query(
    "INSERT INTO reminders (user_id, title, remind_at, notes) VALUES (?, ?, ?, ?)",
    { replacements: [req.user.sub, payload.title, payload.remind_at, payload.notes || null] }
  );
  res.status(201).json({ message: "Reminder created" });
});

router.put("/:id", async (req, res) => {
  try {
    const payload = schema.parse(req.body);
    const [result] = await sequelize.query(
      "UPDATE reminders SET title=?, remind_at=?, notes=? WHERE id=? AND user_id=?",
      { replacements: [payload.title, payload.remind_at, payload.notes || null, req.params.id, req.user.sub] }
    );
    if (result.affectedRows === 0) return res.status(404).json({ error: "Reminder not found or not yours." });
    res.status(200).json({ message: "Reminder updated" });
  } catch (err) {
    res.status(400).json({ error: err.message });
  }
});

router.delete("/:id", async (req, res) => {
  const [result] = await sequelize.query(
    "DELETE FROM reminders WHERE id=? AND user_id=?",
    { replacements: [req.params.id, req.user.sub] }
  );
  if (result.affectedRows === 0) return res.status(404).json({ error: "Reminder not found or not yours." });
  res.status(200).json({ message: "Reminder deleted" });
});

module.exports = router;

