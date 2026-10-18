# Cargar productos y ventas en Tienda MAYEN

Importar `Tienda_MAYEN_Datos.postman_collection.json`. Su nombre en Postman es **Tienda MAYEN · Catálogo y ventas**. Ejecutar con Collection Runner, una iteración, en este orden:

1. **01 · Catálogo**: carga 12 productos con nombres de tienda. Si un nombre ya existe, guarda su ID y omite la creación; no cambia sus datos ni su stock. Si hay nombres duplicados anteriores, reutiliza el de menor ID y deja los demás intactos.
2. **02 · Cinco ventas nuevas**: registra cinco ventas con varios artículos y consulta sus detalles. Los IDs y los totales se calculan con los productos realmente guardados, sin escribirlos a mano.
3. **03 · Verificar inventario e historial**: consulta productos, ventas, stock bajo y agotados.

Antes de cada solicitud, el script inicia sesión como administrador y utiliza el token recibido. No necesitas copiar JWT. Las variables de colección son `baseUrl=http://localhost:8080` y `adminPassword=admin123`; cambiar esta última si la contraseña local es distinta.

Los productos incluyen arroz, leche, aceite, azúcar, fideos, atún, galletas, agua, gaseosa, avena, café y detergente. Los datos de JSON están también en `catalogo_productos.json`; es una lista de referencia, no un endpoint de carga masiva.

**Cada ejecución de la carpeta de ventas crea cinco ventas adicionales y descuenta stock.** Repetir solo el catálogo no duplica productos. Si no hay stock para una venta, el script informa qué artículo requiere reposición. Para volver a vender, actualizar la existencia mediante PUT; no reiniciar ni borrar la base.

## Qué significan los códigos de respuesta

| Código | En la colección de datos | En la colección de pruebas APF2 |
| --- | --- | --- |
| 200 | Login o consulta correcta | Resultado normal |
| 201 | Producto o venta creados | Creación correcta |
| 204 | — | Eliminación correcta |
| 400 | Revisar el cuerpo enviado | Esperado en datos inválidos |
| 401 | Revisar login, token y contraseña | Esperado sin token, credenciales incorrectas o token inválido |
| 403 | Revisar el rol utilizado | Esperado cuando un rol no tiene permiso |
| 404 | Revisar ID | Esperado al consultar un producto eliminado |
| 409 | Revisar stock o relaciones | Esperado para stock insuficiente, rollback o producto vinculado a ventas |

La colección `Tienda_MAYEN_APF2.postman_collection.json` sirve para **probar** esos casos. Cada solicitud ahora indica `[esperado XXX]` en su nombre. Lo importante es que **Test Results** coincida con el código esperado. Importar un JSON solo configura las solicitudes; los datos se generan cuando se pulsa Send o se ejecuta el Runner.

Ejecutar las pruebas negativas en una base de pruebas. Para cargar datos en tu base de trabajo, usar la colección **Catálogo y ventas**. Las pruebas antiguas crearon los nombres “Demo APF2”; la versión nueva utiliza nombres de tienda.

## Retirar los registros Demo APF2 existentes

En H2 Console, pegar y ejecutar completo `database/limpiar_demo_apf2_h2.sql`.

El script guarda los registros retirados en `ARCHIVO_DEMO_PRODUCTOS`, `ARCHIVO_DEMO_VENTAS` y `ARCHIVO_DEMO_DETALLES`. Retira ventas compuestas exclusivamente por productos Demo y después los productos Demo que ya no tengan referencias. No toca arroz ni otros productos reales, sus ventas o usuarios. Conserva una venta mezclada entre productos reales y Demo para no perder una operación real. Las últimas consultas muestran el resultado y cualquier Demo que siga referenciado.

El script se prepara como archivo para ejecutarlo sobre tu base desde H2; generar o importar las colecciones no ejecuta esta limpieza automáticamente.

## Reproducir la comprobación aislada

```powershell
.\scripts\verificar_reinicio.ps1 -EjecutarPostman -VerificarCargaDatos
```

Prueba la colección APF2, carga el catálogo y cinco ventas, repite la carga para comprobar que no se dupliquen productos y verifica persistencia tras reiniciar. Se utiliza una base separada en `target/apf2-verificacion/`; no se agregan ventas a tu base normal durante esta comprobación.

Verificado el 8 de octubre de 2026: colección APF2, 40 aserciones correctas; primera carga, 51 aserciones correctas; segunda carga, 29 aserciones correctas y ningún producto duplicado. También se verificó la limpieza dos veces sobre otra base aislada: respaldó los Demo, conservó productos reales y ventas mezcladas, y no duplicó los respaldos.
