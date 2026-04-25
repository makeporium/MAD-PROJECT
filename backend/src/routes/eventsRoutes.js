const express = require("express");
const sequelize = require("../config/database");
const auth = require("../middleware/authMiddleware");

const router = express.Router();
router.use(auth);

router.get("/", async (_req, res) => {
  const [rows] = await sequelize.query(
    "SELECT id, title, description, event_date, mode, join_link FROM events ORDER BY event_date ASC"
  );
  res.status(200).json(rows);
});

module.exports = router;
