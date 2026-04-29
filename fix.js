const sequelize = require('./backend/src/config/database');

async function fixEvents() {
  try {
    const [experts] = await sequelize.query("SELECT id FROM users WHERE role = 'expert' LIMIT 1");
    if (experts.length > 0) {
      const expertId = experts[0].id;
      await sequelize.query("UPDATE events SET expert_id = ? WHERE expert_id IS NULL", { replacements: [expertId] });
      console.log("Fixed events, set expert_id to " + expertId);
    } else {
      console.log("No experts found in DB.");
    }
  } catch (err) {
    console.error(err);
  } finally {
    process.exit();
  }
}
fixEvents();
