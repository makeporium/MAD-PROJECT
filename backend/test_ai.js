require('dotenv').config();
const { GoogleGenAI } = require("@google/genai");
const sequelize = require("./src/config/database");

async function testAI() {
  try {
    console.log("Testing Gemini AI...");
    const ai = new GoogleGenAI({ apiKey: process.env.GEMINI_API_KEY });
    
    const [articles] = await sequelize.query("SELECT id, title, topic, excerpt, tags FROM info_resources LIMIT 5");
    const prompt = "tell me top depression specialist";

    const systemInstruction = `
You are Mother's Nest AI, an assistant for a postpartum and motherhood support app.
The user is asking: "${prompt}"

Here is what is currently available in the app database:
---
ARTICLES:
${JSON.stringify(articles)}
---

Instructions:
If NOTHING matches, MUST output fallback. You must return valid JSON array.
    `;

    const response = await ai.models.generateContent({
      model: 'gemini-2.5-flash',
      contents: systemInstruction,
      config: { temperature: 0.7 }
    });

    console.log("AI Response:", response.text);
    process.exit(0);
  } catch (err) {
    console.error("AI Error:", err.message);
    process.exit(1);
  }
}
testAI();
