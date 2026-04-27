const express = require("express");
const { z } = require("zod");
const sequelize = require("../config/database");
const auth = require("../middleware/authMiddleware");

const router = express.Router();
router.use(auth);

const resourceSchema = z.object({
  title: z.string().min(1),
  topic: z.string().min(1),
  excerpt: z.string().min(1),
  content: z.string().min(1),
  imageUrl: z.string().url().optional().or(z.literal('')),
  tags: z.string().optional()
});

router.get("/", async (req, res) => {
  const topic = req.query.topic;
  if (topic) {
    const [rows] = await sequelize.query(
      "SELECT id, title, topic, excerpt, content, image_url, tags, views, author_id, created_at FROM info_resources WHERE topic = ? OR tags LIKE ? ORDER BY id DESC",
      { replacements: [topic, `%${topic}%`] }
    );
    return res.status(200).json(rows);
  }

  const sortByViews = req.query.sort === 'views';
  const query = sortByViews 
    ? "SELECT id, title, topic, excerpt, content, image_url, tags, views, author_id, created_at FROM info_resources ORDER BY views DESC"
    : "SELECT id, title, topic, excerpt, content, image_url, tags, views, author_id, created_at FROM info_resources ORDER BY id DESC";

  const [rows] = await sequelize.query(query);
  return res.status(200).json(rows);
});

router.get("/:id", async (req, res) => {
  await sequelize.query("UPDATE info_resources SET views = views + 1 WHERE id = ?", { replacements: [req.params.id] });
  const [rows] = await sequelize.query(
    "SELECT id, title, topic, excerpt, content, image_url, tags, views, author_id, created_at FROM info_resources WHERE id = ?",
    { replacements: [req.params.id] }
  );
  if (!rows.length) return res.status(404).json({ message: "Not found" });
  return res.status(200).json(rows[0]);
});

router.post("/", async (req, res) => {
  if (req.user.role !== 'expert') return res.status(403).json({ message: "Forbidden" });
  const { title, topic, excerpt, content, imageUrl, tags } = resourceSchema.parse(req.body);
  await sequelize.query(
    "INSERT INTO info_resources (title, topic, excerpt, content, image_url, tags, author_id) VALUES (?, ?, ?, ?, ?, ?, ?)",
    { replacements: [title, topic, excerpt, content, imageUrl || null, tags || '', req.user.sub] }
  );
  res.status(201).json({ message: "Created" });
});

router.put("/:id", async (req, res) => {
  if (req.user.role !== 'expert') return res.status(403).json({ message: "Forbidden" });
  const { title, topic, excerpt, content, imageUrl, tags } = resourceSchema.parse(req.body);
  await sequelize.query(
    "UPDATE info_resources SET title=?, topic=?, excerpt=?, content=?, image_url=?, tags=? WHERE id=?",
    { replacements: [title, topic, excerpt, content, imageUrl || null, tags || '', req.params.id] }
  );
  res.status(200).json({ message: "Updated" });
});

router.delete("/:id", async (req, res) => {
  if (req.user.role !== 'expert') return res.status(403).json({ message: "Forbidden" });
  await sequelize.query("DELETE FROM info_resources WHERE id=?", { replacements: [req.params.id] });
  res.status(200).json({ message: "Deleted" });
});

module.exports = router;
