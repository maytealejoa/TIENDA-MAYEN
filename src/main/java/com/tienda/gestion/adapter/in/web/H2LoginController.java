package com.tienda.gestion.adapter.in.web;

import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController @Profile("consola")
public class H2LoginController {
    @GetMapping(value = "/login", produces = "text/html;charset=UTF-8")
    public String login() {
        return """
            <!DOCTYPE html>
            <html lang="es"><head><meta charset="UTF-8">
            <meta name="viewport" content="width=device-width,initial-scale=1">
            <title>Acceso a H2 — Tienda MAYEN</title>
            <style>body{font:16px system-ui;background:#f1f5f9;color:#172033;margin:0;padding:32px}
            main{max-width:380px;margin:8vh auto;background:white;padding:28px;border-radius:16px}
            label{display:block;margin-top:18px}input,button{box-sizing:border-box;width:100%;padding:12px;margin-top:6px;border-radius:6px;border:1px solid #b5bfcc}
            button{background:#174b8a;color:white;margin-top:24px;cursor:pointer}.error{color:#b91c1c}</style>
            </head><body><main><h1>Consola de base de datos</h1><p>Tienda MAYEN · Acceso de administrador</p>
            <p id="error" class="error" hidden>Usuario o contraseña incorrectos.</p>
            <form method="post" action="/login">
            <label for="username">Usuario</label><input id="username" name="username" autocomplete="username" required autofocus>
            <label for="password">Contraseña</label><input id="password" name="password" type="password" autocomplete="current-password" required>
            <button type="submit">Entrar a H2</button></form>
            <script>document.getElementById('error').hidden=!new URLSearchParams(location.search).has('error');</script>
            </main></body></html>
            """;
    }
}
