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

  // 1. Custom Indexing
  // Check if index exists before creating to prevent "Duplicate key" crashes on nodemon restarts.
  const [indexExists] = await sequelize.query(
    `SELECT 1 FROM information_schema.STATISTICS
     WHERE TABLE_SCHEMA = ? AND TABLE_NAME = 'mood_entries' AND INDEX_NAME = 'idx_entry_date' LIMIT 1`,
    { replacements: [env.mysqlDatabase] }
  );

  if (!indexExists.length) {
    await sequelize.query("CREATE INDEX idx_entry_date ON mood_entries(entry_date)");
  }

  // 2. Views
  // We create this view dynamically after expert_profiles table is guaranteed to exist.
  await sequelize.query(`
    CREATE OR REPLACE VIEW expert_directory AS
    SELECT ep.*, u.name as expert_name, u.avatar_url 
    FROM expert_profiles ep 
    JOIN users u ON ep.user_id = u.id
  `);

  // 3. Stored Procedures
  // we are doing this in single block of code to avoid ; or //dilimter issues -- delimiter is only allowed in mysql workbench not in node, so we didnt use begin and end
   // A single-statement procedure avoids the need for BEGIN/END and DELIMITER bugs in NodeJS SQL execution.
  await sequelize.query(`DROP PROCEDURE IF EXISTS upsert_expert_profile`);
  await sequelize.query(`
    CREATE PROCEDURE upsert_expert_profile(
        IN p_user_id BIGINT,
        IN p_specialty VARCHAR(150),
        IN p_location VARCHAR(100),
        IN p_format VARCHAR(50),
        IN p_availability VARCHAR(100),
        IN p_fee INT
    )
    INSERT INTO expert_profiles (user_id, specialty, location, format, availability, fee) 
    VALUES (p_user_id, p_specialty, p_location, p_format, p_availability, p_fee)
    ON DUPLICATE KEY UPDATE 
    specialty = p_specialty, location = p_location, format = p_format, availability = p_availability, fee = p_fee
  `);
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
