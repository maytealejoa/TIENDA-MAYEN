-- H2 Console: ejecutar este archivo completo para retirar solo datos Demo APF2.
-- Conserva los registros retirados en ARCHIVO_DEMO_* y no toca productos reales.
-- Una venta que mezcle productos demo y reales se conserva; sus productos demo
-- no se eliminan mientras estén vinculados a esa venta.
CREATE TABLE IF NOT EXISTS archivo_demo_productos AS SELECT * FROM productos WHERE 1 = 0;
CREATE TABLE IF NOT EXISTS archivo_demo_ventas AS SELECT * FROM ventas WHERE 1 = 0;
CREATE TABLE IF NOT EXISTS archivo_demo_detalles AS SELECT * FROM detalles_venta WHERE 1 = 0;
CREATE LOCAL TEMPORARY TABLE IF NOT EXISTS ids_demo_productos (id BIGINT PRIMARY KEY);
CREATE LOCAL TEMPORARY TABLE IF NOT EXISTS ids_demo_ventas (id BIGINT PRIMARY KEY);

SET AUTOCOMMIT FALSE;
DELETE FROM ids_demo_productos;
DELETE FROM ids_demo_ventas;
INSERT INTO ids_demo_productos
SELECT id FROM productos WHERE nombre IN (
  'Demo APF2 A', 'Demo APF2 A actualizado', 'Demo APF2 B', 'Demo descartable');
INSERT INTO ids_demo_ventas
SELECT v.id FROM ventas v
WHERE EXISTS (SELECT 1 FROM detalles_venta d WHERE d.venta_id = v.id
              AND d.producto_id IN (SELECT id FROM ids_demo_productos))
  AND NOT EXISTS (SELECT 1 FROM detalles_venta d WHERE d.venta_id = v.id
                  AND d.producto_id NOT IN (SELECT id FROM ids_demo_productos));

INSERT INTO archivo_demo_productos
SELECT p.* FROM productos p WHERE p.id IN (SELECT id FROM ids_demo_productos)
AND NOT EXISTS (SELECT 1 FROM detalles_venta d WHERE d.producto_id = p.id
                AND d.venta_id NOT IN (SELECT id FROM ids_demo_ventas))
AND NOT EXISTS (SELECT 1 FROM archivo_demo_productos a WHERE a.id = p.id);
INSERT INTO archivo_demo_ventas
SELECT v.* FROM ventas v WHERE v.id IN (SELECT id FROM ids_demo_ventas)
AND NOT EXISTS (SELECT 1 FROM archivo_demo_ventas a WHERE a.id = v.id);
INSERT INTO archivo_demo_detalles
SELECT d.* FROM detalles_venta d WHERE d.venta_id IN (SELECT id FROM ids_demo_ventas)
AND NOT EXISTS (SELECT 1 FROM archivo_demo_detalles a WHERE a.id = d.id);

DELETE FROM detalles_venta WHERE venta_id IN (SELECT id FROM ids_demo_ventas);
DELETE FROM ventas WHERE id IN (SELECT id FROM ids_demo_ventas);
DELETE FROM productos WHERE id IN (SELECT id FROM ids_demo_productos)
AND NOT EXISTS (SELECT 1 FROM detalles_venta d WHERE d.producto_id = productos.id);
COMMIT;
SET AUTOCOMMIT TRUE;
DROP TABLE ids_demo_ventas;
DROP TABLE ids_demo_productos;

SELECT id, nombre, precio, stock FROM productos ORDER BY id;
SELECT id, fecha, usuario, total FROM ventas ORDER BY id;
-- Solo aparecerán filas aquí si había ventas mezcladas que se conservaron.
SELECT id, nombre FROM productos WHERE nombre IN (
  'Demo APF2 A', 'Demo APF2 A actualizado', 'Demo APF2 B', 'Demo descartable');
