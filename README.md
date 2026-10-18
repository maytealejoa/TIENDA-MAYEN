# Tienda MAYEN - API de Gestión de Productos

> **Entrega vigente: APF2.** Consultar [README_APF2.md](README_APF2.md) para ejecución, base de datos, JWT, roles, ventas, pruebas y Postman. El contenido que sigue conserva la documentación histórica del APF1; el almacenamiento actual utiliza JPA/Hibernate.

> Proyecto académico del curso **Desarrollo Web Integrado** - APF1  
> Backend con **Spring Boot + API REST + Arquitectura Hexagonal + TDD**

## 1. Descripción

Este repositorio contiene la primera versión del sistema de gestión desarrollado para **Tienda MAYEN**, establecimiento tomado como caso de estudio por el equipo.

En **APF1** se implementa el recurso **Producto** mediante una API REST. La aplicación permite registrar y administrar productos, validar datos y consultar productos con stock bajo o agotados.

> **Importante:** en esta etapa los datos se almacenan únicamente en memoria. Al reiniciar la aplicación, los productos registrados se pierden. JPA/Hibernate, seguridad JWT y Angular se incorporarán en avances posteriores.

---

## 2. Funcionalidades implementadas

- ✅ Crear productos con ID generado automáticamente.
- ✅ Listar todos los productos registrados.
- ✅ Buscar un producto por ID.
- ✅ Actualizar un producto existente.
- ✅ Eliminar un producto existente.
- ✅ Consultar productos con stock bajo (`stock <= stockMinimo`).
- ✅ Consultar productos agotados (`stock == 0`).
- ✅ Validar nombre, precio, stock, stock mínimo y fecha de vencimiento.
- ✅ Responder `400 Bad Request` ante datos inválidos.
- ✅ Responder `404 Not Found` ante productos inexistentes.
- ✅ Pruebas automatizadas con JUnit, Mockito y MockMvc.
- ✅ Pruebas manuales mediante Postman.

### Funcionalidades planificadas

- ⏳ Persistencia con JPA/Hibernate y base de datos relacional.
- ⏳ Gestión de ventas y compras.
- ⏳ Actualización automática de inventario por movimientos.
- ⏳ Alertas de vencimiento y reportes.
- ⏳ Spring Security, roles y JWT.
- ⏳ Frontend Angular.
- ⏳ Despliegue.

---

## 3. Arquitectura Hexagonal

El proyecto utiliza **Arquitectura Hexagonal (Ports and Adapters)** para reducir el acoplamiento entre la lógica de negocio y los detalles externos.

```text
Postman / Cliente HTTP
        ↓
ProductoController              ← Adaptador de entrada
        ↓
ProductoUseCase                 ← Puerto de entrada
        ↓
ProductoService                 ← Lógica de aplicación
        ↓
ProductoRepository              ← Puerto de salida
        ↓
ProductoInMemoryRepository      ← Adaptador de salida
        ↓
Lista de productos en memoria
```

Esta organización permite que en APF2 el repositorio en memoria pueda reemplazarse por un adaptador JPA/Hibernate sin reescribir la lógica principal del servicio.

---

## 4. Estructura del proyecto

```text
src/
├── main/
│   └── java/
│       └── com/tienda/gestion/
│           ├── GestionApplication.java
│           ├── domain/
│           │   └── model/
│           │       └── Producto.java
│           ├── application/
│           │   ├── exception/
│           │   │   └── ProductoNoEncontradoException.java
│           │   ├── port/
│           │   │   ├── in/
│           │   │   │   └── ProductoUseCase.java
│           │   │   └── out/
│           │   │       └── ProductoRepository.java
│           │   └── service/
│           │       └── ProductoService.java
│           └── adapter/
│               ├── in/
│               │   └── web/
│               │       ├── ProductoController.java
│               │       ├── ErrorResponse.java
│               │       └── GlobalExceptionHandler.java
│               └── out/
│                   └── persistence/
│                       └── ProductoInMemoryRepository.java
└── test/
    └── java/
        └── com/tienda/gestion/
            ├── GestionApplicationTests.java
            ├── application/
            │   └── ProductoServiceTest.java
            └── adapter/
                └── in/
                    └── web/
                        └── ProductoControllerTest.java
```

---

## 5. Modelo `Producto`

| Campo | Tipo | Descripción |
|---|---|---|
| `id` | `Long` | Identificador generado automáticamente |
| `nombre` | `String` | Nombre del producto |
| `precio` | `double` | Precio de venta |
| `stock` | `int` | Cantidad disponible |
| `stockMinimo` | `int` | Nivel mínimo para reposición |
| `fechaVencimiento` | `LocalDate` | Fecha de vencimiento |

### Ejemplo JSON

```json
{
  "nombre": "Leche Gloria 400g",
  "precio": 5.50,
  "stock": 3,
  "stockMinimo": 5,
  "fechaVencimiento": "2026-12-20"
}
```

El cliente **no envía el `id`** al crear un producto; el repositorio en memoria lo genera automáticamente.

---

## 6. Endpoints

Base URL:

```text
http://localhost:8080/api/productos
```

| Método | Endpoint | Descripción | Respuesta esperada |
|---|---|---|---|
| `POST` | `/api/productos` | Crear producto | `201 Created` |
| `GET` | `/api/productos` | Listar productos | `200 OK` |
| `GET` | `/api/productos/{id}` | Buscar por ID | `200 OK / 404 Not Found` |
| `PUT` | `/api/productos/{id}` | Actualizar producto | `200 OK / 400 / 404` |
| `DELETE` | `/api/productos/{id}` | Eliminar producto | `204 No Content / 404` |
| `GET` | `/api/productos/stock-bajo` | Productos con `stock <= stockMinimo` | `200 OK` |
| `GET` | `/api/productos/sin-stock` | Productos con `stock == 0` | `200 OK` |

---

## 7. Manejo de errores

### `400 Bad Request`

Se devuelve cuando los datos del producto no cumplen las reglas de validación.

Ejemplo inválido:

```json
{
  "nombre": "",
  "precio": -10,
  "stock": -2,
  "stockMinimo": 5,
  "fechaVencimiento": "2026-12-20"
}
```

Ejemplo de respuesta:

```json
{
  "estado": 400,
  "error": "Bad Request",
  "mensaje": "El nombre del producto es obligatorio"
}
```

### `404 Not Found`

Ejemplo:

```http
GET /api/productos/999
```

Respuesta esperada:

```json
{
  "estado": 404,
  "error": "Not Found",
  "mensaje": "Producto no encontrado con ID: 999"
}
```

---

## 8. Requisitos

- Java JDK 21 o versión compatible con el proyecto.
- Eclipse, Spring Tool Suite, IntelliJ IDEA o IDE compatible con Java/Maven.
- Maven Wrapper incluido (`mvnw` / `mvnw.cmd`).
- Postman.
- Git.

---

## 9. Ejecutar el proyecto

### Desde Eclipse

1. Importar el proyecto Maven.
2. Ubicar `GestionApplication.java`.
3. Ejecutar **Run As → Spring Boot App**.
4. Mantener la aplicación activa durante las pruebas con Postman.

La API se ejecuta normalmente en:

```text
http://localhost:8080
```

### Desde terminal en Windows

```powershell
.\mvnw.cmd spring-boot:run
```

---

## 10. Pruebas automatizadas

Tecnologías:

- JUnit 5
- Mockito
- MockMvc

Ejecutar desde terminal:

```powershell
.\mvnw.cmd test
```

O desde Eclipse:

```text
Run As → JUnit Test
```

Casos cubiertos incluyen:

- creación de producto;
- búsqueda de producto existente;
- producto inexistente;
- validaciones de datos;
- stock bajo;
- productos sin stock;
- respuestas HTTP de Controller.

> El warning de Mockito sobre `self-attaching` en versiones recientes del JDK no implica que los tests hayan fallado. La evidencia final debe mostrar `Failures: 0` y `Errors: 0`.

---

## 11. TDD: RED → GREEN → REFACTOR

Caso utilizado para la demostración:

1. **RED:** se escribe una prueba para buscar un producto inexistente y esperar `ProductoNoEncontradoException`.
2. **GREEN:** se implementa `buscarPorId(id).orElseThrow(...)` hasta que la prueba pasa.
3. **REFACTOR:** la comprobación se reutiliza en `actualizarProducto()` y `eliminarProducto()` para evitar código duplicado.

---

## 12. Postman

La colección debe guardar al menos estos casos:

```text
Productos
├── POST - Crear producto (201)
├── GET  - Listar productos (200)
├── GET  - Buscar producto (200)
├── PUT  - Actualizar producto (200)
├── DELETE - Eliminar producto (204)
├── GET  - Stock bajo (200)
├── GET  - Sin stock (200)
├── GET  - Producto inexistente (404)
└── POST - Producto inválido (400)
```

Exportar la colección y almacenarla en:

```text
postman/
└── Tienda_MAYEN_API.postman_collection.json
```

---

## 13. Equipo

| Integrante | Enfoque de trabajo inicial |
|---|---|
| Marcelo Alejandro Gaona Aguilar | Dominio y modelo |
| Roy Dewy Aqquepucho Peña | Documentación, integración, Postman y GitHub |
| Enrique Gerardo Tejada Chirinos | Puertos y casos de uso |
| Fernando José Concha Jiménez | Adaptador web y manejo de errores |
| Yamilet Mayte Alejo Álvarez | Persistencia en memoria y pruebas |

> La distribución es organizativa. Todos los integrantes deben comprender la arquitectura y el flujo completo para la defensa técnica.

---

## 14. Estado del APF1

- [x] Proyecto Spring Boot ejecutable
- [x] Arquitectura hexagonal
- [x] Inyección por constructor
- [x] CRUD de Producto en memoria
- [x] ID automático
- [x] Validaciones y error 400
- [x] Error 404
- [x] Consulta de stock bajo
- [x] Consulta de productos agotados
- [x] Tests de Service
- [x] Tests HTTP/MockMvc
- [x] Caso RED → GREEN → REFACTOR
- [x] Postman
- [x] README
- [x] Git / GitHub

---

## 15. Próxima evolución

```text
APF1
API REST + memoria + TDD
        ↓
APF2
JPA/Hibernate + BD + JPQL + transacciones + JWT
        ↓
APF3
Angular + consumo REST + autenticación
        ↓
Proyecto Final
Integración + pruebas + despliegue
```

---

**Curso:** Desarrollo Web Integrado  
**Proyecto:** Sistema de Gestión - Tienda MAYEN  
**Año:** 2026
