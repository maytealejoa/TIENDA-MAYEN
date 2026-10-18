"""Genera una colección de carga con catálogo reutilizable y ventas adicionales."""
from pathlib import Path
import json

ROOT = Path(__file__).resolve().parents[1]
catalogo = [
    ("arroz", "Arroz Costeño 1 kg", 5.50, 60, 10),
    ("leche", "Leche Gloria 400 g", 4.80, 3, 5),
    ("aceite", "Aceite Primor 1 litro", 10.90, 0, 4),
    ("azucar", "Azúcar Rubia Cartavio 1 kg", 4.50, 50, 8),
    ("fideos", "Fideos Don Vittorio 500 g", 3.90, 45, 8),
    ("atun", "Atún Florida 170 g", 6.50, 35, 6),
    ("galletas", "Galletas Soda Field 6 unidades", 3.50, 40, 8),
    ("agua", "Agua San Luis 2.5 litros", 3.80, 50, 10),
    ("gaseosa", "Inca Kola 1.5 litros", 7.90, 30, 6),
    ("avena", "Avena Quaker 500 g", 6.20, 25, 5),
    ("cafe", "Café Altomayo 50 g", 8.50, 20, 4),
    ("detergente", "Detergente Bolívar 800 g", 9.90, 30, 5),
]
ventas = [
    [("arroz", 2), ("azucar", 1)],
    [("fideos", 3), ("atun", 2)],
    [("galletas", 2), ("agua", 2)],
    [("gaseosa", 1), ("avena", 1), ("cafe", 1)],
    [("detergente", 2), ("arroz", 1), ("azucar", 2)],
]
login_script = """
const baseUrl = pm.variables.replaceIn('{{baseUrl}}');
pm.sendRequest({url: baseUrl + '/api/auth/login', method: 'POST',
  header: {'Content-Type': 'application/json'},
  body: {mode: 'raw', raw: JSON.stringify({username: 'admin', password: pm.variables.get('adminPassword')})}},
  (error, response) => {
    if (error || response.code !== 200) {
      throw new Error('No se pudo iniciar sesión como admin. Verifica servidor y adminPassword.');
    }
    pm.collectionVariables.set('adminToken', response.json().accessToken);
    pm.variables.set('adminToken', response.json().accessToken);
  });
""".strip()

def event(kind, code):
    return {"listen": kind, "script": {"type": "text/javascript", "exec": code.strip().splitlines()}}

def request(name, method, path, expected, body=None, before="", tests=""):
    req = {"method": method, "url": "{{baseUrl}}" + path, "header": []}
    if body is not None:
        req["header"].append({"key": "Content-Type", "value": "application/json"})
        req["body"] = {"mode": "raw", "raw": body if isinstance(body, str) else json.dumps(body, ensure_ascii=False), "options": {"raw": {"language": "json"}}}
    events = [event("test", f'pm.test("HTTP {expected}", () => pm.response.to.have.status({expected}));\n' + tests)]
    if before:
        events.insert(0, event("prerequest", before))
    return {"name": name + f" [esperado {expected}]", "request": req, "event": events}

productos_items = []
for clave, nombre, precio, stock, minimo in catalogo:
    nombre_js = json.dumps(nombre, ensure_ascii=False)
    antes = f"""
pm.sendRequest({{url: pm.variables.replaceIn('{{{{baseUrl}}}}/api/productos'), method: 'GET',
  header: {{Authorization: 'Bearer ' + pm.variables.get('adminToken')}}}}, (error, response) => {{
    if (error || response.code !== 200) throw new Error('No se pudo consultar el catálogo');
    const coincidencias = response.json().filter(p => p.nombre === {nombre_js}).sort((a,b) => a.id-b.id);
    if (coincidencias.length) {{
      pm.collectionVariables.set('id_{clave}', coincidencias[0].id);
      console.log('Producto existente: ' + {nombre_js} + '. ID ' + coincidencias[0].id + '. No se cambia su stock.');
      pm.execution.skipRequest();
    }}
}});
"""
    tests = f"""
if (pm.response.code === 201) pm.collectionVariables.set('id_{clave}', pm.response.json().id);
pm.test('Producto guardado', () => pm.expect(pm.response.json().nombre).to.eql({nombre_js}));
"""
    productos_items.append(request("Crear o reutilizar " + nombre, "POST", "/api/productos", 201,
        {"nombre": nombre, "precio": precio, "stock": stock, "stockMinimo": minimo, "fechaVencimiento": "2028-06-30"}, antes, tests))

ventas_items = []
for numero, lineas in enumerate(ventas, 1):
    especificacion = [{"clave": k, "nombre": next(p[1] for p in catalogo if p[0] == k), "cantidad": q} for k, q in lineas]
    antes = """
const lineas = ESPECIFICACION;
pm.sendRequest({url: pm.variables.replaceIn('{{baseUrl}}/api/productos'), method: 'GET',
  header: {Authorization: 'Bearer ' + pm.variables.get('adminToken')}}, (error, response) => {
    if (error || response.code !== 200) throw new Error('No se pudo consultar el inventario');
    let total = 0;
    const datos = lineas.map(linea => {
      const producto = response.json().filter(p => p.nombre === linea.nombre).sort((a,b) => a.id-b.id)[0];
      if (!producto) throw new Error('Primero ejecutar la carpeta de catálogo: falta ' + linea.nombre);
      if (producto.stock < linea.cantidad) throw new Error('Stock insuficiente: ' + linea.nombre + '. Reponer antes de repetir esta venta.');
      total += producto.precio * linea.cantidad;
      return {productoId: producto.id, cantidad: linea.cantidad};
    });
    pm.variables.set('ventaBody', JSON.stringify({items: datos}));
    pm.variables.set('totalEsperado', Math.round(total * 100) / 100);
});
""".replace("ESPECIFICACION", json.dumps(especificacion, ensure_ascii=False))
    tests = f"""
if (pm.response.code === 201) pm.collectionVariables.set('venta_{numero}', pm.response.json().id);
pm.test('Total calculado con precios vigentes', () => pm.expect(pm.response.json().total).to.eql(Number(pm.variables.get('totalEsperado'))));
pm.test('Cantidad de detalles', () => pm.expect(pm.response.json().detalles.length).to.eql({len(lineas)}));
pm.test('Usuario autenticado', () => pm.expect(pm.response.json().usuario).to.eql('admin'));
"""
    ventas_items.append(request(f"Venta {numero}: " + ", ".join(f"{k} × {q}" for k, q in lineas), "POST", "/api/ventas", 201, "{{ventaBody}}", antes, tests))
    ventas_items.append(request(f"Consultar venta {numero}", "GET", f"/api/ventas/{{{{venta_{numero}}}}}", 200))

consulta_items = [request("Productos guardados", "GET", "/api/productos", 200),
    request("Ventas y detalles", "GET", "/api/ventas", 200),
    request("Productos con stock bajo", "GET", "/api/productos/stock-bajo", 200),
    request("Productos agotados", "GET", "/api/productos/sin-stock", 200)]
collection = {
    "info": {"name": "Tienda MAYEN · Catálogo y ventas", "schema": "https://schema.getpostman.com/json/collection/v2.1.0/collection.json",
        "description": "CARGA DE DATOS. Ejecutar una iteración en orden: catálogo, ventas, consultas. Login automático antes de cada solicitud. Catálogo reutiliza productos existentes por nombre, sin duplicarlos ni modificar stock. Cada ejecución de ventas registra cinco ventas NUEVAS y descuenta existencias. No ejecutar continuamente. Los errores intencionales están en la colección separada APF2."},
    "auth": {"type": "bearer", "bearer": [{"key": "token", "value": "{{adminToken}}", "type": "string"}]},
    "variable": [{"key": "baseUrl", "value": "http://localhost:8080"}, {"key": "adminPassword", "value": "admin123"}, {"key": "adminToken", "value": ""}],
    "event": [event("prerequest", login_script)],
    "item": [{"name": "01 · Catálogo: 12 productos (no duplica existentes)", "item": productos_items},
             {"name": "02 · Cinco ventas nuevas y sus consultas", "item": ventas_items},
             {"name": "03 · Verificar inventario e historial", "item": consulta_items}]
}
out = ROOT / "postman"
out.mkdir(exist_ok=True)
(out / "Tienda_MAYEN_Datos.postman_collection.json").write_text(json.dumps(collection, ensure_ascii=False, indent=2), encoding="utf-8")
(out / "catalogo_productos.json").write_text(json.dumps([
    {"nombre": n, "precio": p, "stock": s, "stockMinimo": m, "fechaVencimiento": "2028-06-30"}
    for _, n, p, s, m in catalogo], ensure_ascii=False, indent=2), encoding="utf-8")
print("Colección de datos generada: 12 productos, 5 ventas, 26 solicitudes")
