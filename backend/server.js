require('dotenv').config();
const app = require('./app');

const PORT = process.env.PORT || 3000;

app.listen(PORT, () => {
  console.log(`\n🚀 Servidor corriendo en http://localhost:${PORT}`);
  console.log(`📡 Endpoints disponibles:`);
  console.log(`   POST   http://localhost:${PORT}/api/translate`);
  console.log(`   GET    http://localhost:${PORT}/api/translations`);
  console.log(`   GET    http://localhost:${PORT}/api/translations/:id`);
  console.log(`   DELETE http://localhost:${PORT}/api/translations/:id\n`);
});
