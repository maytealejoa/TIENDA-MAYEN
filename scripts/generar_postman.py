"""Genera la colección reproducible de APF2. Solo requiere Python estándar."""
from pathlib import Path
import base64
import hashlib
import hmac
import json

ROOT = Path(__file__).resolve().parents[1]
items = []

def request(name, method, path, status, *, role=None, body=None, tests=(), headers=(), before=()):
    header = [{"key": k, "value": v} for k, v in headers]
    if role:
        header.append({"key": "Authorization", "value": "Bearer {{" + role + "Token}}"})
    req = {"method": method, "url": "{{baseUrl}}" + path, "header": header, "auth": {"type": "noauth"}}
    if body is not None:
        header.append({"key": "Content-Type", "value": "application/json"})
        req["body"] = {"mode": "raw", "raw": body if isinstance(body, str) else json.dumps(body), "options": {"raw": {"language": "json"}}}
    script = [f'pm.test("HTTP {status}", () => pm.response.to.have.status({status}));', *tests]
    event = [{"listen": "test", "script": {"type": "text/javascript", "exec": script}}]
    if before:
        event.insert(0, {"listen": "prerequest", "script": {"type": "text/javascript", "exec": list(before)}})
    items.append({"name": f"{len(items)+1:02d} · {name} [esperado {status}]", "request": req, "event": event})

def product(name, stock):
    return {"nombre": name, "precio": 3.50, "stock": stock, "stockMinimo": 2, "fechaVencimiento": "2027-12-15"}

request("Ping público", "GET", "/api/public/ping", 200)
request("Sin token", "GET", "/api/productos", 401)
request("Login incorrecto", "POST", "/api/auth/login", 401, body={"username": "admin", "password": "incorrecta"})
for role in ("lector", "editor", "admin"):
    request("Login " + role, "POST", "/api/auth/login", 200,
            body={"username": role, "password": "{{" + role + "Password}}"}, tests=(
                f'if (pm.response.code === 200) pm.collectionVariables.set("{role}Token", pm.response.json().accessToken);',
                'pm.test("Token Bearer con expiración", () => { pm.expect(pm.response.json().tokenType).to.eql("Bearer"); pm.expect(pm.response.json().expiresIn).to.eql(3600); });'))
request("LECTOR lee", "GET", "/api/productos", 200, role="lector")
request("LECTOR no crea", "POST", "/api/productos", 403, role="lector", body=product("Prohibido", 10))
request("EDITOR crea producto A", "POST", "/api/productos", 201, role="editor", body=product("Galletas Soda Field 6 unidades", 10), tests=(
    'if (pm.response.code === 201) pm.collectionVariables.set("productoA", pm.response.json().id);',
    'pm.test("Location", () => pm.expect(pm.response.headers.has("Location")).to.eql(true));'))
request("ADMIN crea producto B", "POST", "/api/productos", 201, role="admin", body=product("Atún Florida en lata", 1), tests=(
    'if (pm.response.code === 201) pm.collectionVariables.set("productoB", pm.response.json().id);',))
request("Leer A por ID", "GET", "/api/productos/{{productoA}}", 200, role="lector")
request("EDITOR actualiza A", "PUT", "/api/productos/{{productoA}}", 200, role="editor", body=product("Galletas Soda Field 6 unidades", 10))
request("JPQL stock bajo incluye B", "GET", "/api/productos/stock-bajo", 200, role="lector", tests=(
    'pm.test("B tiene stock bajo", () => pm.expect(pm.response.json().some(p => String(p.id) === String(pm.collectionVariables.get("productoB")))).to.eql(true));',))
request("JPQL sin stock", "GET", "/api/productos/sin-stock", 200, role="lector", tests=(
    'pm.test("Solo agotados", () => pm.expect(pm.response.json().every(p => p.stock === 0)).to.eql(true));',))
sale = '{"items":[{"productoId":{{productoA}},"cantidad":2}]}'
request("LECTOR no registra ventas", "POST", "/api/ventas", 403, role="lector", body=sale)
request("Rollback: segundo producto insuficiente", "POST", "/api/ventas", 409, role="editor",
    body='{"items":[{"productoId":{{productoA}},"cantidad":3},{"productoId":{{productoB}},"cantidad":2}]}')
request("Rollback conserva stock A", "GET", "/api/productos/{{productoA}}", 200, role="lector", tests=(
    'pm.test("Stock sigue en 10", () => pm.expect(pm.response.json().stock).to.eql(10));',))
request("EDITOR registra venta", "POST", "/api/ventas", 201, role="editor", body=sale, tests=(
    'if (pm.response.code === 201) pm.collectionVariables.set("ventaId", pm.response.json().id);',
    'pm.test("Total y usuario", () => { pm.expect(pm.response.json().total).to.eql(7); pm.expect(pm.response.json().usuario).to.eql("editor"); });'))
request("Consultar venta persistida", "GET", "/api/ventas/{{ventaId}}", 200, role="admin", tests=(
    'pm.test("Detalle persistido", () => pm.expect(pm.response.json().detalles[0].cantidad).to.eql(2));',))
request("Venta descuenta stock", "GET", "/api/productos/{{productoA}}", 200, role="lector", tests=(
    'pm.test("Stock 8", () => pm.expect(pm.response.json().stock).to.eql(8));',))
request("EDITOR no elimina", "DELETE", "/api/productos/{{productoA}}", 403, role="editor")
request("No eliminar producto vendido", "DELETE", "/api/productos/{{productoA}}", 409, role="admin")
request("JWT manipulado", "GET", "/api/productos", 401, headers=(("Authorization", "Bearer {{tamperedToken}}"),), before=(
    'const token = pm.collectionVariables.get("adminToken"); pm.collectionVariables.set("tamperedToken", token.substring(0, token.lastIndexOf(".") + 1) + "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA");',))
request("JWT expirado de laboratorio", "GET", "/api/productos", 401, headers=(("Authorization", "Bearer {{expiredToken}}"),))
request("Preflight Angular", "OPTIONS", "/api/productos", 200, headers=(
    ("Origin", "http://localhost:4200"), ("Access-Control-Request-Method", "POST"), ("Access-Control-Request-Headers", "Authorization,Content-Type")), tests=(
    'pm.test("Origen permitido", () => pm.expect(pm.response.headers.get("Access-Control-Allow-Origin")).to.eql("http://localhost:4200"));',))
request("Datos inválidos", "POST", "/api/productos", 400, role="editor", body=product("", -1))
request("Crear producto para eliminar", "POST", "/api/productos", 201, role="admin", body=product("Agua San Luis 625 ml", 2), tests=(
    'if (pm.response.code === 201) pm.collectionVariables.set("productoEliminar", pm.response.json().id);',))
request("ADMIN elimina producto", "DELETE", "/api/productos/{{productoEliminar}}", 204, role="admin")
request("Producto eliminado da 404", "GET", "/api/productos/{{productoEliminar}}", 404, role="admin")

def b64(value):
    return base64.urlsafe_b64encode(value).decode().rstrip("=")
head = b64(json.dumps({"alg": "HS256", "typ": "JWT"}, separators=(",", ":")).encode())
payload = b64(json.dumps({"iss": "tienda-mayen", "sub": "admin", "roles": ["ADMIN"], "iat": 946684800, "exp": 946688400}, separators=(",", ":")).encode())
message = head + "." + payload
expired = message + "." + b64(hmac.new(b"tienda-mayen-demo-clave-local-de-32-bytes-minimo-2026", message.encode(), hashlib.sha256).digest())
variables = {"baseUrl": "http://localhost:8080", "adminPassword": "admin123", "editorPassword": "editor123", "lectorPassword": "lector123", "expiredToken": expired}
variables.update({key: "" for key in ("adminToken", "editorToken", "lectorToken", "productoA", "productoB", "productoEliminar", "ventaId", "tamperedToken")})
collection = {"info": {"name": "Tienda MAYEN · APF2", "schema": "https://schema.getpostman.com/json/collection/v2.1.0/collection.json",
    "description": "PRUEBAS DE APF2. Ejecutar en orden sobre una base de pruebas. El código esperado está en el nombre de cada solicitud: 400/401/403/404/409 son correctos en las pruebas negativas. Para cargar productos y ventas, usar Tienda_MAYEN_Datos.postman_collection.json. Esta colección verifica la API y puede crear registros de prueba."},
    "variable": [{"key": k, "value": v, "type": "string"} for k, v in variables.items()], "item": items}
out = ROOT / "postman"
out.mkdir(exist_ok=True)
(out / "Tienda_MAYEN_APF2.postman_collection.json").write_text(json.dumps(collection, ensure_ascii=False, indent=2), encoding="utf-8")
print(f"Colección generada: {len(items)} solicitudes")

