package com.tienda.gestion.config;

import org.springframework.context.annotation.*;
import org.springframework.core.annotation.Order;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration @Profile("consola")
public class H2ConsoleSecurityConfig {
    @Bean @Order(1)
    SecurityFilterChain consolaSecurity(HttpSecurity http) throws Exception {
        AuthenticationEntryPoint accesoConsola = (request, response, exception) -> {
            String accept = request.getHeader("Accept");
            if (accept != null && accept.contains("text/html")) {
                response.sendRedirect(request.getContextPath() + "/login");
            } else {
                // Evitar sendError: el dispatch a /error entraría en la cadena JWT.
                response.setStatus(401);
                response.setHeader("WWW-Authenticate", "Basic realm=\"Tienda MAYEN H2\"");
                response.setContentType("text/plain;charset=UTF-8");
                response.getWriter().write("Inicia sesión en /login para acceder a la consola H2");
            }
        };
        http.securityMatcher("/h2-console", "/h2-console/**", "/login")
            .authorizeHttpRequests(a -> a.requestMatchers("/login").permitAll().anyRequest().hasRole("ADMIN"))
            .formLogin(f -> f.loginPage("/login").defaultSuccessUrl("/h2-console/", true).permitAll())
            .httpBasic(b -> b.authenticationEntryPoint(accesoConsola))
            .exceptionHandling(e -> e.authenticationEntryPoint(accesoConsola))
            .csrf(csrf -> csrf.disable())
            .headers(h -> h.frameOptions(f -> f.sameOrigin()));
        return http.build();
    }
}
