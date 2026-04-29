const fs = require("fs");
const path = require("path");
const sequelize = require("../config/database");
const env = require("../config/env");

function buildSchemaSql(rawSql, databaseName) {
  return rawSql
    .replace(/CREATE DATABASE IF NOT EXISTS\s+`?[\w-]+`?;/i, `CREATE DATABASE IF NOT EXISTS \`${databaseName}\`;`)
    .replace(/USE\s+`?[\w-]+`?;/i, `USE \`${databaseName}\`;`);
}

async function ensureEventsSchema() {
  const [expertIdCol] = await sequelize.query(
    `SELECT 1 FROM information_schema.COLUMNS
     WHERE TABLE_SCHEMA = ? AND TABLE_NAME = 'events' AND COLUMN_NAME = 'expert_id' LIMIT 1`,
    { replacements: [env.mysqlDatabase] }
  );

  if (!expertIdCol.length) {
    await sequelize.query("ALTER TABLE events ADD COLUMN expert_id BIGINT NULL AFTER join_link");
  }

  await sequelize.query(
    `CREATE TABLE IF NOT EXISTS event_bookings (
      id BIGINT PRIMARY KEY AUTO_INCREMENT,
      event_id BIGINT NOT NULL,
      user_id BIGINT NOT NULL,
      created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
      UNIQUE KEY uq_event_user (event_id, user_id),
      FOREIGN KEY (event_id) REFERENCES events(id) ON DELETE CASCADE,
      FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
    )`
  );

  await sequelize.query(
    `CREATE TABLE IF NOT EXISTS expert_profiles (
      id BIGINT PRIMARY KEY AUTO_INCREMENT,
      user_id BIGINT NOT NULL UNIQUE,
      specialty VARCHAR(120),
      location VARCHAR(120),
      format VARCHAR(40),
      availability VARCHAR(255),
      fee DECIMAL(10,2),
      created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
      updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
      FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
    )`
  );
}

async function initializeDatabaseSchema() {
  const sqlPath = path.resolve(__dirname, "../../sql/001_init.sql");
  const rawSql = fs.readFileSync(sqlPath, "utf8");
  const sql = buildSchemaSql(rawSql, env.mysqlDatabase);
  await sequelize.query(sql);
  await ensureEventsSchema();
}

async function run() {
  await initializeDatabaseSchema();
  await sequelize.close();
  console.log("Database initialized successfully.");
}

if (require.main === module) {
  run().catch(async (error) => {
    console.error("Failed to initialize DB:", error.message);
    await sequelize.close();
    process.exit(1);
  });
}

module.exports = { initializeDatabaseSchema };
