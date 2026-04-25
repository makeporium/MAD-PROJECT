const express = require("express");
const sequelize = require("../config/database");
const auth = require("../middleware/authMiddleware");

const router = express.Router();
router.use(auth);

router.get("/", async (req, res) => {
  const topic = req.query.topic;
  if (topic) {
    const [rows] = await sequelize.query(
      "SELECT id, title, topic, excerpt, content_url FROM info_resources WHERE topic = ? ORDER BY id DESC",
      { replacements: [topic] }
    );
    return res.status(200).json(rows);
  }

  const [rows] = await sequelize.query(
    "SELECT id, title, topic, excerpt, content_url FROM info_resources ORDER BY id DESC"
  );
  return res.status(200).json(rows);
});

module.exports = router;
