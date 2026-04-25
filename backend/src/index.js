const app = require("./app");
const sequelize = require("./config/database");
const env = require("./config/env");

async function startServer() {
  try {
    await sequelize.authenticate();
    console.log("MySQL connection established.");
    app.listen(env.port, () => {
      console.log(`Backend running on http://localhost:${env.port}`);
    });
  } catch (error) {
    console.error("Failed to start server:", error.message);
    process.exit(1);
  }
}

startServer();
