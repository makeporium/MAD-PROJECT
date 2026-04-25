const express = require("express");
const { z } = require("zod");
const sequelize = require("../config/database");
const auth = require("../middleware/authMiddleware");

const router = express.Router();
router.use(auth);

const aiSchema = z.object({ user_message: z.string().min(1).max(500) });

router.post("/ai", async (req, res) => {
  const payload = aiSchema.parse(req.body);
  const cannedReply = "Thank you for sharing. You are doing your best, and support is available.";

  await sequelize.query(
    "INSERT INTO ai_support_messages (user_id, user_message, ai_reply) VALUES (?, ?, ?)",
    { replacements: [req.user.sub, payload.user_message, cannedReply] }
  );

  res.status(200).json({ reply: cannedReply });
});

router.post("/sos", async (req, res) => {
  await sequelize.query("INSERT INTO sos_alerts (user_id) VALUES (?)", {
    replacements: [req.user.sub],
  });
  res.status(201).json({ message: "SOS alert logged" });
});

module.exports = router;
