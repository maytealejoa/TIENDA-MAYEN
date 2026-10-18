package com.tienda.gestion;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.test.web.servlet.MockMvc;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest @AutoConfigureMockMvc
class APF2IntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;
    @Autowired JwtEncoder encoder;
    @Autowired JwtDecoder decoder;
    String admin;

    @BeforeEach void preparar() throws Exception {
        jdbc.update("delete from detalles_venta"); jdbc.update("delete from ventas"); jdbc.update("delete from productos");
        admin = login("admin");
    }
    String login(String username) throws Exception {
        String body = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"" + username + "\",\"password\":\"" + username + "123\"}"))
                .andExpect(status().isOk()).andExpect(header().string("Cache-Control", "no-store"))
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.accessToken");
    }
    String producto(String nombre, int stock, int minimo) {
        return "{\"nombre\":\"" + nombre + "\",\"precio\":3.50,\"stock\":" + stock
                + ",\"stockMinimo\":" + minimo + ",\"fechaVencimiento\":\"2027-12-15\"}";
    }
    long crear(String nombre, int stock, int minimo) throws Exception {
        String body = mvc.perform(post("/api/productos").header("Authorization", "Bearer " + admin)
                .contentType(MediaType.APPLICATION_JSON).content(producto(nombre, stock, minimo)))
                .andExpect(status().isCreated()).andExpect(header().exists("Location"))
                .andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(body, "$.id")).longValue();
    }
    String venta(long id, int cantidad) { return "{\"items\":[{\"productoId\":" + id + ",\"cantidad\":" + cantidad + "}]}"; }
    int stock(long id) { return jdbc.queryForObject("select stock from productos where id=?", Integer.class, id); }
    @Test void pingPublicoYSinToken401() throws Exception {
        mvc.perform(get("/api/public/ping")).andExpect(status().isOk());
        mvc.perform(get("/api/productos")).andExpect(status().isUnauthorized()).andExpect(jsonPath("$.estado").value(401));
        mvc.perform(post("/api/productos").contentType(MediaType.APPLICATION_JSON).content(producto("A", 1, 0)))
                .andExpect(status().isUnauthorized());
    }
    @Test void loginIncorrectoYUsuarioInexistente401() throws Exception {
        for (String username : List.of("admin", "no-existe")) {
            mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                    .content("{\"username\":\"" + username + "\",\"password\":\"incorrecto\"}"))
                    .andExpect(status().isUnauthorized());
        }
    }
    @Test void jwtContieneClaimsYUsuariosUsanBcrypt() {
        Jwt jwt = decoder.decode(admin);
        assertEquals("admin", jwt.getSubject()); assertEquals("tienda-mayen", jwt.getClaimAsString("iss"));
        assertEquals(List.of("ADMIN"), jwt.getClaimAsStringList("roles"));
        assertEquals(3600, jwt.getExpiresAt().getEpochSecond() - jwt.getIssuedAt().getEpochSecond());
        assertNull(jwt.getClaim("password"));
        jdbc.queryForList("select password_hash from usuarios", String.class).forEach(hash -> assertTrue(hash.startsWith("$2")));
    }
    @ParameterizedTest @CsvSource({"lector,403,403", "editor,201,403", "admin,201,204"})
    void permisosPorRol(String usuario, int creacion, int borrado) throws Exception {
        long id = crear("Original", 5, 1); String token = login(usuario);
        mvc.perform(get("/api/productos").header("Authorization", "Bearer " + token)).andExpect(status().isOk());
        mvc.perform(post("/api/productos").header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON).content(producto("Nuevo", 3, 1))).andExpect(status().is(creacion));
        mvc.perform(put("/api/productos/" + id).header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON).content(producto("Editado", 6, 2)))
                .andExpect(status().is(usuario.equals("lector") ? 403 : 200));
        mvc.perform(delete("/api/productos/" + id).header("Authorization", "Bearer " + token)).andExpect(status().is(borrado));
    }
    @Test void crudEscribeFilasYActualizaSinInsertarOtroId() throws Exception {
        long id = crear("Leche", 5, 2);
        assertEquals("Leche", jdbc.queryForObject("select nombre from productos where id=?", String.class, id));
        mvc.perform(get("/api/productos/" + id).header("Authorization", "Bearer " + admin)).andExpect(jsonPath("$.stock").value(5));
        mvc.perform(put("/api/productos/" + id).header("Authorization", "Bearer " + admin)
                .contentType(MediaType.APPLICATION_JSON).content(producto("Leche editada", 10, 2)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.id").value(id));
        assertEquals(10, stock(id)); assertEquals(1, jdbc.queryForObject("select count(*) from productos", Integer.class));
        mvc.perform(delete("/api/productos/" + id).header("Authorization", "Bearer " + admin)).andExpect(status().isNoContent());
        mvc.perform(get("/api/productos/" + id).header("Authorization", "Bearer " + admin)).andExpect(status().isNotFound());
    }
    @Test void jpqlEncuentraSoloStockBajoYAgotados() throws Exception {
        crear("Agotado", 0, 1); crear("Limite", 2, 2); crear("Normal", 10, 2);
        mvc.perform(get("/api/productos/stock-bajo").header("Authorization", "Bearer " + admin))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(2));
        mvc.perform(get("/api/productos/sin-stock").header("Authorization", "Bearer " + admin))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1)).andExpect(jsonPath("$[0].nombre").value("Agotado"));
    }
    @Test void ventaGuardaDetalleTotalYDescuentaStock() throws Exception {
        long id = crear("Arroz", 10, 2);
        mvc.perform(post("/api/ventas").header("Authorization", "Bearer " + login("editor"))
                .contentType(MediaType.APPLICATION_JSON).content(venta(id, 2)))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.total").value(7.0))
                .andExpect(jsonPath("$.usuario").value("editor")).andExpect(jsonPath("$.detalles[0].cantidad").value(2));
        assertEquals(8, stock(id)); assertEquals(1, jdbc.queryForObject("select count(*) from detalles_venta", Integer.class));
        mvc.perform(get("/api/ventas").header("Authorization", "Bearer " + admin)).andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1));
        mvc.perform(get("/api/ventas").header("Authorization", "Bearer " + login("lector"))).andExpect(status().isForbidden());
        mvc.perform(post("/api/ventas").header("Authorization", "Bearer " + login("lector"))
                .contentType(MediaType.APPLICATION_JSON).content(venta(id, 1))).andExpect(status().isForbidden());
        mvc.perform(delete("/api/productos/" + id).header("Authorization", "Bearer " + admin)).andExpect(status().isConflict());
        assertEquals(8, stock(id));
    }
    @Test void rollbackReviertePrimerDescuentoSiSegundoProductoNoTieneStock() throws Exception {
        long primero = crear("Disponible", 10, 1), segundo = crear("Insuficiente", 1, 1);
        String body = "{\"items\":[{\"productoId\":" + primero + ",\"cantidad\":3},{\"productoId\":" + segundo + ",\"cantidad\":2}]}";
        mvc.perform(post("/api/ventas").header("Authorization", "Bearer " + admin).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isConflict());
        assertEquals(10, stock(primero)); assertEquals(1, stock(segundo));
        assertEquals(0, jdbc.queryForObject("select count(*) from ventas", Integer.class));
        assertEquals(0, jdbc.queryForObject("select count(*) from detalles_venta", Integer.class));
    }
    @Test void rollbackConProductoInexistente() throws Exception {
        long id = crear("Disponible", 10, 1);
        String body = "{\"items\":[{\"productoId\":" + id + ",\"cantidad\":3},{\"productoId\":999999999,\"cantidad\":1}]}";
        mvc.perform(post("/api/ventas").header("Authorization", "Bearer " + admin).contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isNotFound());
        assertEquals(10, stock(id)); assertEquals(0, jdbc.queryForObject("select count(*) from ventas", Integer.class));
    }
    @Test void ventasConcurrentesNoProducenStockNegativo() throws Exception {
        long id = crear("Ultima unidad", 1, 0);
        CountDownLatch inicio = new CountDownLatch(1);
        try (ExecutorService pool = Executors.newFixedThreadPool(2)) {
            Callable<Integer> vender = () -> { inicio.await(); return mvc.perform(post("/api/ventas")
                    .header("Authorization", "Bearer " + admin).contentType(MediaType.APPLICATION_JSON).content(venta(id, 1)))
                    .andReturn().getResponse().getStatus(); };
            Future<Integer> a = pool.submit(vender), b = pool.submit(vender); inicio.countDown();
            List<Integer> estados = new ArrayList<>(List.of(a.get(15, TimeUnit.SECONDS), b.get(15, TimeUnit.SECONDS)));
            Collections.sort(estados); assertEquals(List.of(201, 409), estados);
        }
        assertEquals(0, stock(id)); assertEquals(1, jdbc.queryForObject("select count(*) from ventas", Integer.class));
    }
    @Test void tokensManipuladosExpiradosYOtroEmisor401() throws Exception {
        Instant now = Instant.now();
        String expirado = token("tienda-mayen", now.minusSeconds(7200), now.minusSeconds(3600));
        String emisorAjeno = token("otro-emisor", now, now.plusSeconds(3600));
        String alterado = admin.substring(0, admin.lastIndexOf('.') + 1) + "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA";
        for (String token : List.of("no-es-jwt", expirado, emisorAjeno, alterado)) {
            mvc.perform(get("/api/productos").header("Authorization", "Bearer " + token)).andExpect(status().isUnauthorized());
        }
    }
    String token(String issuer, Instant inicio, Instant fin) {
        return encoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), JwtClaimsSet.builder()
                .issuer(issuer).subject("admin").issuedAt(inicio).expiresAt(fin).claim("roles", List.of("ADMIN")).build())).getTokenValue();
    }
    @Test void corsPermiteAngularYRechazaOtroOrigen() throws Exception {
        mvc.perform(options("/api/productos").header("Origin", "http://localhost:4200")
                .header("Access-Control-Request-Method", "POST").header("Access-Control-Request-Headers", "Authorization,Content-Type"))
                .andExpect(status().isOk()).andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:4200"));
        mvc.perform(options("/api/productos").header("Origin", "https://otro.example")
                .header("Access-Control-Request-Method", "POST")).andExpect(status().isForbidden());
    }
    @Test void validacionJsonYCantidadNoDejanDatos() throws Exception {
        for (String body : List.of("{}", "{", producto("", 1, 0), producto("A", -1, 0), producto("A", 1, 0).replace("3.50", "3.555"))) {
            mvc.perform(post("/api/productos").header("Authorization", "Bearer " + admin)
                    .contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isBadRequest());
        }
        mvc.perform(post("/api/ventas").header("Authorization", "Bearer " + admin)
                .contentType(MediaType.APPLICATION_JSON).content("{\"items\":[]}")).andExpect(status().isBadRequest());
        assertEquals(0, jdbc.queryForObject("select count(*) from productos", Integer.class));
    }
    @Test void ventaNoPermiteDuplicadosOCantidadNula() throws Exception {
        long id = crear("A", 10, 0);
        String duplicado = "{\"items\":[{\"productoId\":" + id + ",\"cantidad\":1},{\"productoId\":" + id + ",\"cantidad\":1}]}";
        mvc.perform(post("/api/ventas").header("Authorization", "Bearer " + admin).contentType(MediaType.APPLICATION_JSON).content(duplicado)).andExpect(status().isBadRequest());
        mvc.perform(post("/api/ventas").header("Authorization", "Bearer " + admin).contentType(MediaType.APPLICATION_JSON).content(venta(id, 0))).andExpect(status().isBadRequest());
        assertEquals(10, stock(id));
    }
}
