package com.company.auth.security;

import io.smallrye.jwt.build.Jwt;
import jakarta.enterprise.context.ApplicationScoped;

import java.time.Duration;
import java.util.Set;

@ApplicationScoped
public class JwtService {

    public String generate(Long userId, String username, String role) {
        return Jwt.issuer("default-be")
                .subject(username)
                .claim("jti", java.util.UUID.randomUUID().toString())
                .groups(Set.of(role))
                .expiresIn(Duration.ofHours(2))
                .sign();
    }
}
