const sequelize = require('./src/config/database');

async function makeExpert() {
  try {
    const email = 'coverpillow24@gmail.com';
    const [result] = await sequelize.query(
      "UPDATE users SET role = 'expert' WHERE email = ?",
      { replacements: [email] }
    );
    console.log(`Update result:`, result);
    console.log(`Successfully made ${email} an expert (admin).`);
  } catch (error) {
    console.error("Failed to update role:", error);
  } finally {
    process.exit(0);
  }
}

makeExpert();
