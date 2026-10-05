const express = require('express');
const router = express.Router();
const translationController = require('../controllers/translationController');

// POST /api/translate → traduce y guarda en BD
router.post('/translate', translationController.translate);

// GET /api/translations → historial completo
router.get('/translations', translationController.getAll);

// GET /api/translations/:id → una traducción por ID
router.get('/translations/:id', translationController.getById);

// DELETE /api/translations/:id → eliminar traducción
router.delete('/translations/:id', translationController.deleteById);

module.exports = router;
