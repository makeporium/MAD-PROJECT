const jwt = require("jsonwebtoken");
const env = require("../config/env");

function signAccessToken(payload) {
  return jwt.sign(payload, env.jwtSecret, { expiresIn: env.jwtExpiresIn });
}

module.exports = { signAccessToken };
