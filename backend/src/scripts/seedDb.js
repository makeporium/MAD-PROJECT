const fs = require("fs");
const path = require("path");
const sequelize = require("../config/database");

async function run() {
  const sqlPath = path.resolve(__dirname, "../../sql/002_seed.sql");
  const sql = fs.readFileSync(sqlPath, "utf8");
  await sequelize.query(sql);
  await sequelize.close();
  console.log("Database seeded successfully.");
}

run().catch(async (error) => {
  console.error("Failed to seed DB:", error.message);
  await sequelize.close();
  process.exit(1);
});
