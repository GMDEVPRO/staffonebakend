-- ════════════════════════════════════════════════════════════════
-- B1__h2_compat.sql — Compatibilidade H2 / PostgreSQL
-- Prefixo B (Baseline) executa ANTES de V1
-- Cria gen_random_uuid() que H2 não tem nativamente
-- ════════════════════════════════════════════════════════════════

CREATE ALIAS IF NOT EXISTS gen_random_uuid
    FOR "java.util.UUID.randomUUID";