const express = require('express');
const cors = require('cors');
const translationRoutes = require('./routes/translationRoutes');

const app = express();

// ── Middlewares ──────────────────────────────────────────────
app.use(cors());
app.use(express.json());
app.use(express.urlencoded({ extended: true }));

// ── Rutas ────────────────────────────────────────────────────
app.use('/api', translationRoutes);

// ── Ruta raíz (health check) ─────────────────────────────────
app.get('/', (req, res) => {
  res.json({
    message: '🌐 Smartwatch Translator API funcionando',
    version: '1.0.0',
    endpoints: {
      translate: 'POST /api/translate',
      getAll: 'GET /api/translations',
      getById: 'GET /api/translations/:id',
      delete: 'DELETE /api/translations/:id',
    },
  });
});

// ── Manejo de rutas no encontradas ───────────────────────────
app.use((req, res) => {
  res.status(404).json({
    success: false,
    message: `Ruta ${req.method} ${req.path} no encontrada`,
  });
});

// ── Manejo global de errores ─────────────────────────────────
app.use((err, req, res, next) => {
  console.error('Error no manejado:', err.stack);
  res.status(500).json({
    success: false,
    message: 'Error interno del servidor',
  });
});

module.exports = app;
