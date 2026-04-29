const fs = require("fs");
const path = require("path");
const sequelize = require("../config/database");
const env = require("../config/env");

function buildSeedSql(rawSql, databaseName) {
  return rawSql.replace(/USE\s+`?[\w-]+`?;/i, `USE \`${databaseName}\`;`);
}

async function run() {
  const sqlPath = path.resolve(__dirname, "../../sql/002_seed.sql");
  const rawSql = fs.readFileSync(sqlPath, "utf8");
  const sql = buildSeedSql(rawSql, env.mysqlDatabase);
  await sequelize.query(sql);
  await sequelize.close();
  console.log("Database seeded successfully.");
}

run().catch(async (error) => {
  console.error("Failed to seed DB:", error.message);
  if (error.parent?.sqlMessage) console.error("sqlMessage:", error.parent.sqlMessage);
  if (error.parent?.sql) console.error("sql:", error.parent.sql);
  await sequelize.close();
  process.exit(1);
});
