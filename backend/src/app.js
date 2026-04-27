const express = require("express");
const cors = require("cors");
const env = require("./config/env");
const authRoutes = require("./routes/authRoutes");
const moodRoutes = require("./routes/moodRoutes");
const recommendsRoutes = require("./routes/recommendsRoutes");
const communityRoutes = require("./routes/communityRoutes");
const eventsRoutes = require("./routes/eventsRoutes");
const resourcesRoutes = require("./routes/resourcesRoutes");
const remindersRoutes = require("./routes/remindersRoutes");
const supportRoutes = require("./routes/supportRoutes");

const app = express();

app.use(
  cors({
    origin(origin, callback) {
      if (!origin || env.corsOrigins.includes("*") || env.corsOrigins.includes(origin)) {
        callback(null, true);
        return;
      }
      callback(new Error("Not allowed by CORS"));
    },
  })
);
app.use(express.json());

app.get("/health", (_req, res) => res.status(200).json({ ok: true }));

app.use("/api/auth", authRoutes);
app.use("/api/moods", moodRoutes);
app.use("/api/recommends", recommendsRoutes);
app.use("/api/community", communityRoutes);
app.use("/api/events", eventsRoutes);
app.use("/api/resources", resourcesRoutes);
app.use("/api/reminders", remindersRoutes);
app.use("/api/support", supportRoutes);
app.use("/api/testimonials", require("./routes/testimonialsRoutes"));

app.use((err, _req, res, _next) => {
  console.error("Unhandled error:", err);
  if (err.name === "ZodError") {
    return res.status(400).json({ message: "Invalid request body", issues: err.issues });
  }
  return res.status(500).json({ message: err.message || "Internal server error" });
});

module.exports = app;
