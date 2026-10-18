package com.tienda.gestion.config;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.LocalDateTime;

final class SecurityErrors {
    private SecurityErrors() {}
    static void write(HttpServletResponse response, int status) throws IOException {
        response.setStatus(status); response.setContentType("application/json"); response.setCharacterEncoding("UTF-8");
        if (status == 401) response.setHeader("WWW-Authenticate", "Bearer");
        String error = status == 401 ? "Unauthorized" : "Forbidden";
        String message = status == 401 ? "Autenticación requerida o token inválido" : "No tienes permiso para esta operación";
        response.getWriter().write("{\"fecha\":\"" + LocalDateTime.now() + "\",\"estado\":" + status
                + ",\"error\":\"" + error + "\",\"mensaje\":\"" + message + "\"}");
    }
}
