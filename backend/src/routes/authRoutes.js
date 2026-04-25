const express = require("express");
const { z } = require("zod");
const { getFirebaseAuth } = require("../config/firebase");
const { signAccessToken } = require("../services/jwtService");
const sequelize = require("../config/database");

const router = express.Router();

const schema = z.object({ idToken: z.string().min(20) });

router.post("/google", async (req, res) => {
  try {
    const { idToken } = schema.parse(req.body);
    const decoded = await getFirebaseAuth().verifyIdToken(idToken, true);

    await sequelize.query(
      `INSERT INTO users (firebase_uid, email, name, avatar_url)
       VALUES (?, ?, ?, ?)
       ON DUPLICATE KEY UPDATE email=VALUES(email), name=VALUES(name), avatar_url=VALUES(avatar_url), updated_at=CURRENT_TIMESTAMP;`,
      { replacements: [decoded.uid, decoded.email || null, decoded.name || null, decoded.picture || null] }
    );

    const [userRows] = await sequelize.query(
      "SELECT id, firebase_uid, email, name, avatar_url FROM users WHERE firebase_uid = ? LIMIT 1",
      { replacements: [decoded.uid] }
    );

    const user = userRows[0];
    const accessToken = signAccessToken({ sub: user.id, firebaseUid: user.firebase_uid, email: user.email });

    return res.status(200).json({ accessToken, user });
  } catch (error) {
    if (error.name === "ZodError") {
      return res.status(400).json({ message: "Invalid request body" });
    }
    console.error("Auth /google failed:");
    console.error("name:", error.name);
    console.error("message:", error.message);
    console.error("stack:", error.stack);
    return res.status(401).json({
      message: "Firebase authentication failed",
      errorDetail: error.message,
    });
  }
});

module.exports = router;
