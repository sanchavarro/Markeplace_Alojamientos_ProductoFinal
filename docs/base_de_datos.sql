-- =====================================================================
-- Base de datos del Marketplace de Alojamientos - Inversiones LR
-- Ejecutar en la Terminal (con la aplicación detenida):
--     psql postgres -f docs/base_de_datos.sql
-- Borra y vuelve a crear las bases vacías. Las tablas las crea la aplicación
-- al iniciar (Hibernate) y las restricciones las agrega schema-extra.sql.
-- =====================================================================

DROP DATABASE IF EXISTS marketplace_db;
DROP DATABASE IF EXISTS marketplace_test;

DO $$
BEGIN
    IF NOT EXISTS (SELECT FROM pg_roles WHERE rolname = 'marketplace') THEN
        CREATE ROLE marketplace LOGIN PASSWORD 'MarketplaceLR2026';
    ELSE
        ALTER ROLE marketplace WITH LOGIN PASSWORD 'MarketplaceLR2026';
    END IF;
END $$;

CREATE DATABASE marketplace_db OWNER marketplace ENCODING 'UTF8';
CREATE DATABASE marketplace_test OWNER marketplace ENCODING 'UTF8';
