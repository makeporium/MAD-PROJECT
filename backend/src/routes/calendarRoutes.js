const express = require("express");
const sequelize = require("../config/database");
const auth = require("../middleware/authMiddleware");

const router = express.Router();
router.use(auth);

router.get("/progress", async (req, res) => {
  try {
    const userId = req.user.sub;
    const [[therapyRow]] = await sequelize.query(
      `SELECT COUNT(*) AS total
       FROM event_bookings eb
       JOIN events e ON e.id = eb.event_id
       WHERE eb.user_id = ?
         AND MONTH(e.event_date) = MONTH(CURRENT_DATE())
         AND YEAR(e.event_date) = YEAR(CURRENT_DATE())`,
      { replacements: [userId] }
    );

    const [[moodRow]] = await sequelize.query(
      `SELECT COUNT(*) AS total
       FROM mood_entries
       WHERE user_id = ?
         AND MONTH(entry_date) = MONTH(CURRENT_DATE())
         AND YEAR(entry_date) = YEAR(CURRENT_DATE())`,
      { replacements: [userId] }
    );

    const [[communityRow]] = await sequelize.query(
      `SELECT COUNT(DISTINCT room_id) AS total
       FROM community_messages
       WHERE user_id = ?
         AND MONTH(created_at) = MONTH(CURRENT_DATE())
         AND YEAR(created_at) = YEAR(CURRENT_DATE())`,
      { replacements: [userId] }
    );

    const [[affirmationRow]] = await sequelize.query(
      `SELECT t.content, u.name AS user_name
       FROM testimonials t
       JOIN users u ON u.id = t.user_id
       ORDER BY t.created_at DESC
       LIMIT 1`
    );

    const [[moodTodayRow]] = await sequelize.query(
      `SELECT COUNT(*) AS total FROM mood_entries
       WHERE user_id = ? AND DATE(entry_date) = CURDATE()`,
      { replacements: [userId] }
    );

    return res.status(200).json({
      therapy_sessions_this_month: Number(therapyRow.total || 0),
      mood_checkins_this_month: Number(moodRow.total || 0),
      community_sessions_joined: Number(communityRow.total || 0),
      mood_done_today: Number(moodTodayRow.total || 0) > 0,
      affirmation: affirmationRow
        ? {
            content: affirmationRow.content,
            user_name: affirmationRow.user_name,
          }
        : null,
    });
  } catch (err) {
    return res.status(500).json({ error: err.message });
  }
});

module.exports = router;
