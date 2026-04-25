const fs = require("fs");
const path = require("path");
const sequelize = require("../config/database");

async function run() {
  const sqlPath = path.resolve(__dirname, "../../sql/001_init.sql");
  const sql = fs.readFileSync(sqlPath, "utf8");
  await sequelize.query(sql);
  await sequelize.close();
  console.log("Database initialized successfully.");
}

run().catch(async (error) => {
  console.error("Failed to initialize DB:", error.message);
  await sequelize.close();
  process.exit(1);
});
