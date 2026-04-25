const jwt = require("jsonwebtoken");
const env = require("../config/env");

module.exports = function authMiddleware(req, res, next) {
  const auth = req.headers.authorization || "";
  const [scheme, token] = auth.split(" ");

  if (scheme !== "Bearer" || !token) {
    return res.status(401).json({ message: "Missing or invalid authorization header." });
  }

  try {
    req.user = jwt.verify(token, env.jwtSecret);
    return next();
  } catch (_error) {
    return res.status(401).json({ message: "Invalid or expired access token." });
  }
};
