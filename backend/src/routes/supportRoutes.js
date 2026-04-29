const express = require("express");
const { z } = require("zod");
const sequelize = require("../config/database");
const auth = require("../middleware/authMiddleware");
const { GoogleGenAI } = require("@google/genai");

const router = express.Router();
router.use(auth);

const aiSchema = z.object({ user_message: z.string().min(1).max(500) });

router.post("/ai", async (req, res) => {
  try {
    const payload = aiSchema.parse(req.body);

    if (!process.env.GEMINI_API_KEY) {
      return res.status(500).json({ reply: "I'm currently unavailable (Missing API Key)." });
    }

    const ai = new GoogleGenAI({ apiKey: process.env.GEMINI_API_KEY });
    
    const systemInstruction = `
You are Mother's Nest AI, an empathetic, supportive, and knowledgeable assistant for a maternal health and postpartum support app. 
The user is a mother (or partner) who might be experiencing postpartum depression, anxiety, exhaustion, or simply needs someone to talk to.
Your goal is to provide a warm, encouraging, and helpful response. You do NOT have access to a database to recommend specific doctors or local events, so rely entirely on your own knowledge about motherhood, coping strategies, self-care, and gentle encouragement.
Do not give professional medical diagnosis, but do encourage seeking professional help if they sound in crisis. Keep your responses concise (under 100 words if possible) and highly empathetic.

User message: "${payload.user_message}"
    `;

    const response = await ai.models.generateContent({
      model: 'gemini-2.5-flash',
      contents: systemInstruction,
      config: { temperature: 0.7 }
    });

    const aiReply = response.text ? response.text.trim() : "I'm here for you. Take a deep breath, you are doing great.";

    await sequelize.query(
      "INSERT INTO ai_support_messages (user_id, user_message, ai_reply) VALUES (?, ?, ?)",
      { replacements: [req.user.sub, payload.user_message, aiReply] }
    );

    res.status(200).json({ reply: aiReply });
  } catch (err) {
    console.error("AI Support Error:", err);
    res.status(500).json({ reply: "I'm having trouble connecting right now, but please know you are not alone." });
  }
});

router.post("/sos", async (req, res) => {
  await sequelize.query("INSERT INTO sos_alerts (user_id) VALUES (?)", {
    replacements: [req.user.sub],
  });
  res.status(201).json({ message: "SOS alert logged" });
});

router.get("/ai/history", async (req, res) => {
  try {
    const [rows] = await sequelize.query(
      "SELECT user_message, ai_reply, created_at FROM ai_support_messages WHERE user_id = ? ORDER BY created_at ASC",
      { replacements: [req.user.sub] }
    );
    res.status(200).json(rows);
  } catch (err) {
    console.error("AI History Error:", err);
    res.status(500).json({ error: "Failed to fetch AI history" });
  }
});

module.exports = router;
