const express = require("express");
const sequelize = require("../config/database");
const auth = require("../middleware/authMiddleware");

const router = express.Router();
router.use(auth);

router.get("/", async (_req, res) => {
  const [rows] = await sequelize.query(
    "SELECT id, title, category, description, match_score FROM recommendations ORDER BY match_score DESC, id DESC"
  );
  res.status(200).json(rows);
});

module.exports = router;
