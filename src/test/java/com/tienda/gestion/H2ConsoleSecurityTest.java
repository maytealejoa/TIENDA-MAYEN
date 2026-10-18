package com.tienda.gestion;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import org.springframework.mock.web.MockHttpSession;

@SpringBootTest @AutoConfigureMockMvc @ActiveProfiles({"demo", "consola"})
class H2ConsoleSecurityTest {
    @Autowired MockMvc mvc;
    @Test void consolaRequiereAutenticacionYRolAdmin() throws Exception {
        mvc.perform(get("/h2-console/")).andExpect(status().isUnauthorized())
                .andExpect(header().string("WWW-Authenticate", "Basic realm=\"Tienda MAYEN H2\""));
        mvc.perform(get("/h2-console/").with(httpBasic("lector", "lector123"))).andExpect(status().isForbidden());
        // MockMvc no ejecuta el servlet de H2: su respuesta se verifica en la prueba de arranque real.
        mvc.perform(get("/api/productos").with(httpBasic("admin", "admin123"))).andExpect(status().isUnauthorized());
    }
    @Test void navegadorPuedeEntrarMedianteFormularioSinAutenticarLaApi() throws Exception {
        mvc.perform(get("/h2-console/").header("Accept", "text/html"))
                .andExpect(status().isFound()).andExpect(header().string("Location", "/login"));
        mvc.perform(get("/login")).andExpect(status().isOk());
        var result = mvc.perform(post("/login").param("username", "admin").param("password", "admin123"))
                .andExpect(status().isFound()).andExpect(header().string("Location", "/h2-console/"))
                .andReturn();
        MockHttpSession session = (MockHttpSession) result.getRequest().getSession(false);
        mvc.perform(get("/api/productos").session(session)).andExpect(status().isUnauthorized());
    }
}
