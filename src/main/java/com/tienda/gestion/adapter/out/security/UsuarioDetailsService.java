package com.tienda.gestion.adapter.out.security;

import com.tienda.gestion.adapter.out.persistence.UsuarioJpaRepository;
import org.springframework.stereotype.Service;
import org.springframework.security.core.userdetails.*;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UsuarioDetailsService implements UserDetailsService {
    private final UsuarioJpaRepository usuarios;
    public UsuarioDetailsService(UsuarioJpaRepository usuarios) { this.usuarios = usuarios; }
    @Override @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) {
        var usuario = usuarios.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("Credenciales inválidas"));
        return User.withUsername(usuario.getUsername()).password(usuario.getPasswordHash())
                .roles(usuario.getRol().name()).disabled(!usuario.isEnabled()).build();
    }
}
