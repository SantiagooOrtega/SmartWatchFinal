-- ============================================================
-- Smartwatch Translator — Schema Supabase (PostgreSQL)
-- Ejecutar en: supabase.com → tu proyecto → SQL Editor
-- ============================================================

CREATE TABLE translations (
  id           UUID        DEFAULT gen_random_uuid() PRIMARY KEY,
  original_text  TEXT      NOT NULL,
  translated_text TEXT     NOT NULL,
  source_lang  VARCHAR(10) DEFAULT 'ES',
  target_lang  VARCHAR(10) DEFAULT 'EN',
  created_at   TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- Índice para ordenar por fecha rápidamente
CREATE INDEX idx_translations_created_at ON translations(created_at DESC);
