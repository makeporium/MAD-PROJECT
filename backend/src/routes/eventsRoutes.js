const express = require("express");
const sequelize = require("../config/database");
const auth = require("../middleware/authMiddleware");

const router = express.Router();
router.use(auth);

// GET all upcoming sessions
router.get("/", async (req, res) => {
  try {
    const userId = req.user.sub;
    const [rows] = await sequelize.query(
      `SELECT e.id, e.title, e.description, e.event_date, e.mode, e.join_link, e.expert_id, u.name as expert_name,
              CASE WHEN eb.id IS NULL THEN 0 ELSE 1 END AS is_booked,
              (SELECT COUNT(*) FROM event_bookings WHERE event_id = e.id) AS booking_count
       FROM events e 
       LEFT JOIN users u ON e.expert_id = u.id 
       LEFT JOIN event_bookings eb ON eb.event_id = e.id AND eb.user_id = ?
       ORDER BY e.event_date ASC`,
      { replacements: [userId] }
    );
    res.status(200).json(rows);
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

// POST to add a new session (Experts only)
router.post("/", async (req, res) => {
  try {
    const userId = req.user.sub;
    const { title, description, event_date, mode, join_link } = req.body;
    
    // Check if user is expert
    const [users] = await sequelize.query("SELECT role FROM users WHERE id = ?", {
      replacements: [userId],
    });
    if (users.length === 0 || users[0].role !== 'expert') {
      return res.status(403).json({ error: "Only experts can create sessions." });
    }

    const [result] = await sequelize.query(
      "INSERT INTO events (title, description, event_date, mode, join_link, expert_id) VALUES (?, ?, ?, ?, ?, ?)",
      { replacements: [title, description, event_date, mode || "online", join_link, userId] }
    );
    res.status(201).json({ id: result.insertId, message: "Session created successfully" });
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

// POST to book a session
router.post("/:id/book", async (req, res) => {
  try {
    const userId = req.user.sub;
    const eventId = req.params.id;

    // OLD CODE: Executed a SELECT and INSERT sequentially without locking, creating a race condition risk.
    // NEW CODE: Wraps the read-then-write sequence in a database transaction to ensure atomicity and prevent double-booking.
    /*
    // Check if already booked
    const [existing] = await sequelize.query(
      "SELECT id FROM event_bookings WHERE event_id = ? AND user_id = ?",
      { replacements: [eventId, userId] }
    );
    if (existing.length > 0) {
      return res.status(400).json({ error: "Already booked this session." });
    }

    await sequelize.query(
      "INSERT INTO event_bookings (event_id, user_id) VALUES (?, ?)",
      { replacements: [eventId, userId] }
    );
    */
    
    const t = await sequelize.transaction();
    try {
      const [existing] = await sequelize.query(
        "SELECT id FROM event_bookings WHERE event_id = ? AND user_id = ? FOR UPDATE",
        { replacements: [eventId, userId], transaction: t }
      );
      if (existing.length > 0) {
        await t.rollback();
        return res.status(400).json({ error: "Already booked this session." });
      }

      await sequelize.query(
        "INSERT INTO event_bookings (event_id, user_id) VALUES (?, ?)",
        { replacements: [eventId, userId], transaction: t }
      );
      await t.commit();
    } catch (err) {
      await t.rollback();
      throw err;
    }
    res.status(201).json({ message: "Session booked successfully" });
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

// GET expert's created sessions and who booked them
router.get("/my-sessions", async (req, res) => {
  try {
    const userId = req.user.sub;
    const [events] = await sequelize.query(
      "SELECT id, title, description, event_date, join_link FROM events WHERE expert_id = ? ORDER BY event_date ASC",
      { replacements: [userId] }
    );

    for (let event of events) {
      const [bookings] = await sequelize.query(
        `SELECT u.id, u.name, u.email 
         FROM event_bookings eb 
         JOIN users u ON eb.user_id = u.id 
         WHERE eb.event_id = ?`,
        { replacements: [event.id] }
      );
      event.bookings = bookings;
    }

    res.status(200).json(events);
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

// POST to create/update expert profile
router.post("/expert-profile", async (req, res) => {
  try {
    const userId = req.user.sub;
    const { specialty, location, format, availability, fee } = req.body;
    
    // Check if expert
    const [users] = await sequelize.query("SELECT role FROM users WHERE id = ?", {
      replacements: [userId],
    });
    if (users.length === 0 || users[0].role !== 'expert') {
      return res.status(403).json({ error: "Only experts can create expert profiles." });
    }

    // OLD CODE: Made separate queries to check for existing profile and then issue an INSERT or UPDATE from Node.js.
    // NEW CODE: Calls a Stored Procedure 'upsert_expert_profile' to handle all logic in a single trip to the database.
    /*
    // Upsert profile
    const [existing] = await sequelize.query("SELECT id FROM expert_profiles WHERE user_id = ?", {
      replacements: [userId],
    });
    if (existing.length > 0) {
      await sequelize.query(
        "UPDATE expert_profiles SET specialty=?, location=?, format=?, availability=?, fee=? WHERE user_id=?",
        { replacements: [specialty, location, format, availability, fee, userId] }
      );
    } else {
      await sequelize.query(
        "INSERT INTO expert_profiles (user_id, specialty, location, format, availability, fee) VALUES (?, ?, ?, ?, ?, ?)",
        { replacements: [userId, specialty, location, format, availability, fee] }
      );
    }
    */
    await sequelize.query(
      "CALL upsert_expert_profile(?, ?, ?, ?, ?, ?)",
      { replacements: [userId, specialty, location, format, availability, fee] }
    );
    res.status(200).json({ message: "Expert profile saved successfully" });
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

// GET all expert profiles
router.get("/expert-profiles", async (_req, res) => {
  try {
    // OLD CODE: Used an INNER JOIN directly inside the route to stitch the expert_profiles and users tables.
    // NEW CODE: Uses the pre-compiled 'expert_directory' View, abstracting the complexity away from the application code.
    /*
    const [rows] = await sequelize.query(
      `SELECT ep.*, u.name as expert_name, u.avatar_url 
       FROM expert_profiles ep 
       JOIN users u ON ep.user_id = u.id`
    );
    */
    const [rows] = await sequelize.query(
      `SELECT * FROM expert_directory`
    );
    res.status(200).json(rows);
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

module.exports = router;
