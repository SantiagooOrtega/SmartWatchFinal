const supabase = require('../config/supabase');

const TABLE = 'translations';

const Translation = {
  /**
   * Guarda una nueva traducción en la base de datos
   * @param {string} originalText - Texto original en español
   * @param {string} translatedText - Texto traducido al inglés
   * @returns {object} - Registro guardado
   */
  async create(originalText, translatedText, sourceLang = 'ES', targetLang = 'EN') {
    const { data, error } = await supabase
      .from(TABLE)
      .insert([
        {
          original_text: originalText,
          translated_text: translatedText,
          source_lang: sourceLang,
          target_lang: targetLang,
        },
      ])
      .select()
      .single();

    if (error) throw new Error(`Error al guardar traducción: ${error.message}`);
    return data;
  },

  /**
   * Obtiene todas las traducciones ordenadas por fecha descendente
   * @returns {Array} - Lista de traducciones
   */
  async getAll() {
    const { data, error } = await supabase
      .from(TABLE)
      .select('*')
      .order('created_at', { ascending: false });

    if (error) throw new Error(`Error al obtener traducciones: ${error.message}`);
    return data;
  },

  /**
   * Obtiene una traducción por su ID
   * @param {string} id - UUID de la traducción
   * @returns {object} - Registro encontrado
   */
  async getById(id) {
    const { data, error } = await supabase
      .from(TABLE)
      .select('*')
      .eq('id', id)
      .single();

    if (error) throw new Error(`Traducción no encontrada: ${error.message}`);
    return data;
  },

  /**
   * Elimina todas las traducciones
   * @returns {number} - Cantidad eliminada
   */
  async deleteAll() {
    const { data, error } = await supabase
      .from(TABLE)
      .delete()
      .not('id', 'is', null)
      .select('id');

    if (error) throw new Error(`Error al borrar historial: ${error.message}`);
    return data.length;
  },

  /**
   * Elimina una traducción por su ID
   * @param {string} id - UUID de la traducción
   * @returns {object} - Registro eliminado
   */
  async delete(id) {
    const { data, error } = await supabase
      .from(TABLE)
      .delete()
      .eq('id', id)
      .select()
      .single();

    if (error) throw new Error(`Error al eliminar traducción: ${error.message}`);
    return data;
  },
};

module.exports = Translation;
