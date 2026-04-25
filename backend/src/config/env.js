const dotenv = require("dotenv");

dotenv.config();

module.exports = {
  port: Number(process.env.PORT || 5000),
  mysqlHost: process.env.MYSQL_HOST || "127.0.0.1",
  mysqlPort: Number(process.env.MYSQL_PORT || 3306),
  mysqlDatabase: process.env.MYSQL_DATABASE || "mad_app",
  mysqlUser: process.env.MYSQL_USER || "root",
  mysqlPassword: process.env.MYSQL_PASSWORD || "",
  jwtSecret: process.env.JWT_SECRET || "dev_secret",
  jwtExpiresIn: process.env.JWT_EXPIRES_IN || "7d",
  corsOrigins: (process.env.CORS_ORIGINS || "*").split(",").map((x) => x.trim()),
  firebaseProjectId: process.env.FIREBASE_PROJECT_ID || "",
  firebaseServiceAccountPath: process.env.FIREBASE_SERVICE_ACCOUNT_PATH || "./firebase-service-account.json",
};
