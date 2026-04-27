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
      "SELECT id, firebase_uid, email, name, avatar_url, role FROM users WHERE firebase_uid = ? LIMIT 1",
      { replacements: [decoded.uid] }
    );

    const user = userRows[0];
    const accessToken = signAccessToken({ sub: user.id, firebaseUid: user.firebase_uid, email: user.email, role: user.role });

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

router.get("/me", require("../middleware/authMiddleware"), async (req, res) => {
  const [userRows] = await sequelize.query(
    "SELECT id, firebase_uid, email, name, avatar_url, role FROM users WHERE id = ? LIMIT 1",
    { replacements: [req.user.sub] }
  );
  if (!userRows.length) return res.status(404).json({ message: "User not found" });
  res.status(200).json(userRows[0]);
});

router.put("/me", require("../middleware/authMiddleware"), async (req, res) => {
  const { name, avatar_url } = req.body;
  await sequelize.query(
    "UPDATE users SET name = ?, avatar_url = ? WHERE id = ?",
    { replacements: [name || null, avatar_url || null, req.user.sub] }
  );
  res.status(200).json({ message: "Profile updated" });
});

router.put("/users/:id/role", require("../middleware/authMiddleware"), async (req, res) => {
  // Only experts can change roles
  if (req.user.role !== 'expert') return res.status(403).json({ message: "Forbidden" });
  const { role } = req.body;
  if (!["user", "expert"].includes(role)) return res.status(400).json({ message: "Invalid role" });
  await sequelize.query(
    "UPDATE users SET role = ? WHERE id = ?",
    { replacements: [role, req.params.id] }
  );
  res.status(200).json({ message: "Role updated" });
});

module.exports = router;
