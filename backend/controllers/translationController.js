const axios = require('axios');
const Translation = require('../models/Translation');

const DEEPL_API_URL = 'https://api-free.deepl.com/v2/translate';

const translationController = {
  /**
   * POST /api/translate
   * Traduce un texto de español a inglés y lo guarda en la BD
   */
  async translate(req, res) {
    try {
      const { text, source_lang, target_lang } = req.body;

      // Validación de entrada
      if (!text || typeof text !== 'string' || text.trim() === '') {
        return res.status(400).json({
          success: false,
          message: 'El campo "text" es requerido y no puede estar vacío.',
        });
      }

      const cleanText   = text.trim();
      const sourceLang  = (source_lang || 'ES').toUpperCase();
      const targetLang  = (target_lang  || 'EN').toUpperCase();

      // Llamada a la API de DeepL
      const deeplResponse = await axios.post(
        DEEPL_API_URL,
        {
          text: [cleanText],
          source_lang: sourceLang,
          target_lang: targetLang,
        },
        {
          headers: {
            'Authorization': `DeepL-Auth-Key ${process.env.DEEPL_API_KEY}`,
            'Content-Type': 'application/json',
          },
        }
      );

      const translatedText = deeplResponse.data.translations[0].text;

      // Guardar en Supabase (Model). Si la BD falla, igual devolvemos la traducción.
      try {
        const savedRecord = await Translation.create(cleanText, translatedText, sourceLang, targetLang);
        return res.status(201).json({ success: true, saved: true, data: savedRecord });
      } catch (dbError) {
        console.error('No se pudo guardar en Supabase:', dbError.message);
        return res.status(200).json({
          success: true,
          saved: false,
          data: {
            id: `local-${Date.now()}`,
            original_text: cleanText,
            translated_text: translatedText,
            source_lang: sourceLang,
            target_lang: targetLang,
            created_at: new Date().toISOString(),
          },
        });
      }
    } catch (error) {
      console.error('Error en translate:', error.message);
      return res.status(500).json({
        success: false,
        message: 'Error interno del servidor al traducir.',
        error: error.message,
      });
    }
  },

  /**
   * GET /api/translations
   * Obtiene el historial completo de traducciones
   */
  async getAll(req, res) {
    try {
      const translations = await Translation.getAll();
      return res.status(200).json({
        success: true,
        count: translations.length,
        data: translations,
      });
    } catch (error) {
      console.error('Error en getAll:', error.message);
      return res.status(500).json({
        success: false,
        message: 'Error al obtener el historial de traducciones.',
        error: error.message,
      });
    }
  },

  /**
   * GET /api/translations/:id
   * Obtiene una traducción específica por ID
   */
  async getById(req, res) {
    try {
      const { id } = req.params;
      const translation = await Translation.getById(id);
      return res.status(200).json({
        success: true,
        data: translation,
      });
    } catch (error) {
      console.error('Error en getById:', error.message);
      return res.status(404).json({
        success: false,
        message: 'Traducción no encontrada.',
        error: error.message,
      });
    }
  },

  /**
   * DELETE /api/translations/:id
   * Elimina una traducción por ID
   */
  async deleteById(req, res) {
    try {
      const { id } = req.params;
      const deleted = await Translation.delete(id);
      return res.status(200).json({
        success: true,
        message: 'Traducción eliminada exitosamente.',
        data: deleted,
      });
    } catch (error) {
      console.error('Error en deleteById:', error.message);
      return res.status(500).json({
        success: false,
        message: 'Error al eliminar la traducción.',
        error: error.message,
      });
    }
  },
};

module.exports = translationController;
