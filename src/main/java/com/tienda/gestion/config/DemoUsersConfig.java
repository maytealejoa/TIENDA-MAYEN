package com.tienda.gestion.config;

import com.tienda.gestion.adapter.out.persistence.*;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.*;
import org.springframework.core.env.Environment;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class DemoUsersConfig {
    @Bean @ConditionalOnProperty(name = "app.bootstrap.enabled", havingValue = "true")
    ApplicationRunner crearUsuariosIniciales(UsuarioJpaRepository usuarios, PasswordEncoder encoder, Environment env) {
        return args -> {
            for (UsuarioEntity.Rol rol : UsuarioEntity.Rol.values()) {
                String username = rol.name().toLowerCase(java.util.Locale.ROOT);
                if (usuarios.findByUsername(username).isEmpty()) {
                    String password = env.getRequiredProperty("app.bootstrap." + username + "-password");
                    if (password.length() < 8 || password.getBytes(java.nio.charset.StandardCharsets.UTF_8).length > 72) {
                        throw new IllegalArgumentException("La clave inicial debe tener 8 caracteres como mínimo y hasta 72 bytes");
                    }
                    usuarios.save(new UsuarioEntity(username, encoder.encode(password), rol));
                }
            }
        };
    }
}
