const express = require("express");
const { z } = require("zod");
const sequelize = require("../config/database");
const auth = require("../middleware/authMiddleware");

const router = express.Router();
router.use(auth);

async function isExpertUser(userId) {
  const [rows] = await sequelize.query("SELECT role FROM users WHERE id = ? LIMIT 1", {
    replacements: [userId],
  });
  return rows.length > 0 && rows[0].role === "expert";
}

const testimonialSchema = z.object({
  content: z.string().min(1)
});

router.get("/", async (req, res) => {
  const [rows] = await sequelize.query(
    "SELECT t.id, t.content, t.created_at, u.name AS user_name, u.avatar_url, u.role FROM testimonials t JOIN users u ON u.id = t.user_id ORDER BY t.created_at DESC"
  );
  return res.status(200).json(rows);
});

router.post("/", async (req, res) => {
  if (!(await isExpertUser(req.user.sub))) return res.status(403).json({ message: "Forbidden" });
  const { content } = testimonialSchema.parse(req.body);
  await sequelize.query(
    "INSERT INTO testimonials (user_id, content) VALUES (?, ?)",
    { replacements: [req.user.sub, content] }
  );
  res.status(201).json({ message: "Created" });
});

router.put("/:id", async (req, res) => {
  if (!(await isExpertUser(req.user.sub))) return res.status(403).json({ message: "Forbidden" });
  const { content } = testimonialSchema.parse(req.body);
  await sequelize.query(
    "UPDATE testimonials SET content = ? WHERE id = ?",
    { replacements: [content, req.params.id] }
  );
  res.status(200).json({ message: "Updated" });
});

router.delete("/:id", async (req, res) => {
  if (!(await isExpertUser(req.user.sub))) return res.status(403).json({ message: "Forbidden" });
  await sequelize.query("DELETE FROM testimonials WHERE id = ?", { replacements: [req.params.id] });
  res.status(200).json({ message: "Deleted" });
});

module.exports = router;
