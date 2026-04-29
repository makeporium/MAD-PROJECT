const sequelize = require('./src/config/database');

async function migrate() {
  try {
    console.log("Adding expert_id to events...");
    await sequelize.query("ALTER TABLE events ADD COLUMN expert_id BIGINT").catch(e => console.log(e.message));
    await sequelize.query("ALTER TABLE events ADD FOREIGN KEY (expert_id) REFERENCES users(id) ON DELETE CASCADE").catch(e => console.log(e.message));
    
    console.log("Creating event_bookings...");
    await sequelize.query(`
      CREATE TABLE IF NOT EXISTS event_bookings (
        id BIGINT PRIMARY KEY AUTO_INCREMENT,
        event_id BIGINT NOT NULL,
        user_id BIGINT NOT NULL,
        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
        FOREIGN KEY (event_id) REFERENCES events(id) ON DELETE CASCADE,
        FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
      )
    `);

    console.log("Creating expert_profiles...");
    await sequelize.query(`
      CREATE TABLE IF NOT EXISTS expert_profiles (
        id BIGINT PRIMARY KEY AUTO_INCREMENT,
        user_id BIGINT NOT NULL,
        specialty VARCHAR(150),
        location VARCHAR(100),
        format VARCHAR(50),
        availability VARCHAR(100),
        fee VARCHAR(50),
        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
        FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
      )
    `);

    console.log("Migrations applied successfully!");
  } catch (error) {
    console.error("Migration failed:", error);
  } finally {
    process.exit(0);
  }
}

migrate();
