-- =====================================================================
-- Programa 4 - Script de base de datos (PostgreSQL 12 o superior)
-- Ejecutar con:  psql -U postgres -f schema.sql
-- =====================================================================

-- 1) Crear la base de datos (ejecutar conectado a la BD "postgres")
CREATE DATABASE inventario;

-- 2) Conectarse a la nueva base de datos
\c inventario

-- 3) Crear la tabla producto
CREATE TABLE IF NOT EXISTS producto (
    codigo   VARCHAR(20)   PRIMARY KEY
             CHECK (codigo ~ '^\S+$'),               -- sin espacios
    nombre   VARCHAR(100)  NOT NULL
             CHECK (btrim(nombre) <> ''),            -- no vacío
    cantidad INTEGER       NOT NULL DEFAULT 0
             CHECK (cantidad >= 0),                  -- entero >= 0
    precio   NUMERIC(12,2) NOT NULL DEFAULT 0.00
             CHECK (precio >= 0),                    -- >= 0.00
    total    NUMERIC(14,2) GENERATED ALWAYS AS (cantidad * precio) STORED
);

-- 4) Datos de prueba (opcional)
INSERT INTO producto (codigo, nombre, cantidad, precio) VALUES
    ('P001', 'Arroz extra superior', 10, 4.50),
    ('P002', 'Aceite vegetal 1 L',    5, 9.90)
ON CONFLICT (codigo) DO NOTHING;

-- 5) Consulta de verificación
SELECT codigo, nombre, cantidad, precio, total FROM producto ORDER BY codigo;
