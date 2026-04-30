const express = require("express");
const sequelize = require("../config/database");
const auth = require("../middleware/authMiddleware");
const { GoogleGenAI } = require("@google/genai");

const router = express.Router();
router.use(auth);

// We keep a GET route just in case the old UI triggers it on load
router.get("/", async (_req, res) => {
  const [rows] = await sequelize.query(
    "SELECT id, title, category, description, match_score FROM recommendations ORDER BY match_score DESC, id DESC"
  );
  res.status(200).json(rows);
});

// New POST route for AI search
router.post("/ai-search", async (req, res) => {
  try {
    const { prompt } = req.body;
    if (!prompt) return res.status(400).json({ error: "Prompt is required" });

    if (!process.env.GEMINI_API_KEY) {
      return res.status(500).json({ error: "GEMINI_API_KEY is not configured on the server." });
    }

    const ai = new GoogleGenAI({ apiKey: process.env.GEMINI_API_KEY });

    // 1. Fetch Context from DB
    const [articles] = await sequelize.query("SELECT id, title, topic, excerpt, tags FROM info_resources LIMIT 50");
    const [events] = await sequelize.query(`
      SELECT e.id, e.title, e.description, e.event_date, u.name as expert_name 
      FROM events e LEFT JOIN users u ON e.expert_id = u.id 
      WHERE e.event_date >= NOW() LIMIT 20
    `);
    const [experts] = await sequelize.query(`
      SELECT ep.user_id, u.name, ep.specialty, ep.location 
      FROM expert_profiles ep JOIN users u ON ep.user_id = u.id LIMIT 20
    `);

    // 2. Build the AI prompt
    const systemInstruction = `
You are Mother's Nest AI, an assistant for a postpartum and motherhood support app.
The user is asking: "${prompt}"

Here is what is currently available in the app database:
---
ARTICLES (Info Hub):
${JSON.stringify(articles)}
---
UPCOMING SESSIONS (Events):
${JSON.stringify(events)}
---
EXPERT PROFILES:
${JSON.stringify(experts)}
---

Instructions:
1. Analyze the user's query and try to match it with the tags, topics, titles, or descriptions of the available app content.
2. If you find highly relevant items (e.g. they ask for depression specialist and you see an expert with that specialty, or an article about PPD), return them as recommendations.
3. If NOTHING in the database context matches the user's query reasonably well, you MUST output a fallback response. The fallback response must have the title exactly "no good content found on database" and the description should be a helpful, empathetic answer to the user's query using your own knowledge.

You must return your response as a valid JSON array of objects. Do not use Markdown formatting like \`\`\`json. Just raw JSON.
Example of success:
[
  { "title": "Article: Understanding PPD", "description": "This article discusses signs and symptoms of postpartum depression which matches your query." },
  { "title": "Expert: Dr. Sarah", "description": "She specializes in postpartum depression." }
]

Example of fallback (when no DB content matches):
[
  { "title": "no good content found on database", "description": "I couldn't find specific resources in the app, but here is some advice: ..." }
]
    `;

    const response = await ai.models.generateContent({
      model: 'gemini-2.5-flash',
      contents: systemInstruction,
      config: {
          temperature: 0.7,
      }
    });

    let aiText = response.text || "";
    // Clean up potential markdown formatting from the response
    aiText = aiText.replace(/```json/g, "").replace(/```/g, "").trim();
    
    let resultJson;
    try {
      resultJson = JSON.parse(aiText);
    } catch (e) {
      console.error("Failed to parse AI response:", aiText);
      return res.status(500).json({ error: "AI generated invalid JSON" });
    }

    res.status(200).json(resultJson);

  } catch (err) {
    console.error("AI Search Error:", err);
    res.status(500).json({ error: err.message });
  }
});

module.exports = router;
