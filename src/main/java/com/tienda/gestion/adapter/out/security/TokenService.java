package com.tienda.gestion.adapter.out.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.stereotype.Service;
import java.time.Instant;
import java.util.List;

@Service
public class TokenService {
    private final JwtEncoder encoder;
    private final String issuer;
    private final long ttl;
    public TokenService(JwtEncoder encoder, @Value("${app.jwt.issuer}") String issuer, @Value("${app.jwt.ttl-seconds}") long ttl) {
        if (ttl <= 0 || ttl > 86400) throw new IllegalArgumentException("Duración JWT fuera de rango");
        this.encoder = encoder; this.issuer = issuer; this.ttl = ttl;
    }
    public record TokenResponse(String accessToken, String tokenType, long expiresIn, String username, List<String> roles) {}
    public TokenResponse generar(Authentication authentication) {
        Instant now = Instant.now();
        List<String> roles = authentication.getAuthorities().stream().map(a -> a.getAuthority())
                .filter(a -> a.startsWith("ROLE_")).map(a -> a.substring(5)).toList();
        JwtClaimsSet claims = JwtClaimsSet.builder().issuer(issuer).subject(authentication.getName())
                .issuedAt(now).expiresAt(now.plusSeconds(ttl)).claim("roles", roles).build();
        String token = encoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims)).getTokenValue();
        return new TokenResponse(token, "Bearer", ttl, authentication.getName(), roles);
    }
}
