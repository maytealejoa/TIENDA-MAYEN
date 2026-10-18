-- Ejecutar en la consola H2 después de probar la colección Postman.
SELECT id, nombre, precio, stock, stock_minimo, fecha_vencimiento FROM productos ORDER BY id;
SELECT id, fecha, usuario, total FROM ventas ORDER BY id;
SELECT id, venta_id, producto_id, nombre_producto, cantidad, precio_unitario, subtotal FROM detalles_venta ORDER BY id;
-- No mostrar el hash de contraseñas en evidencias académicas.
SELECT id, username, rol, enabled FROM usuarios ORDER BY id;
SELECT v.id, v.total, SUM(d.subtotal) AS total_detalles
FROM ventas v JOIN detalles_venta d ON d.venta_id = v.id GROUP BY v.id, v.total;
