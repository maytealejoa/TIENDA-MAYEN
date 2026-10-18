package com.tienda.gestion.adapter.in.web;

import com.tienda.gestion.adapter.out.security.TokenService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.security.authentication.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;

@RestController @RequestMapping("/api/auth")
public class AuthController {
    private final AuthenticationManager authenticationManager;
    private final TokenService tokens;
    public AuthController(AuthenticationManager authenticationManager, TokenService tokens) {
        this.authenticationManager = authenticationManager; this.tokens = tokens;
    }
    public record LoginRequest(@NotBlank @Size(max = 60) String username, @NotBlank @Size(max = 72) String password) {}
    @PostMapping("/login")
    public ResponseEntity<TokenService.TokenResponse> login(@Valid @RequestBody LoginRequest request) {
        if (request.password().getBytes(java.nio.charset.StandardCharsets.UTF_8).length > 72) {
            throw new IllegalArgumentException("La contraseña excede 72 bytes");
        }
        var auth = authenticationManager.authenticate(
                UsernamePasswordAuthenticationToken.unauthenticated(request.username(), request.password()));
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(tokens.generar(auth));
    }
}
