package com.company.auth.entity;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "refresh_tokens")
public class RefreshTokenEntity extends PanacheEntity {

    @ManyToOne(optional = false)
    @JoinColumn(name = "user_id")
    public UserEntity user;

    @Column(nullable = false, unique = true, length = 64)
    public String tokenHash; // hash(refreshToken)

    @Column(nullable = false)
    public Instant expiresAt;

    public Instant revokedAt;

    @Column(length = 120)
    public String device;

    @Column(length = 64)
    public String ip;

    public static RefreshTokenEntity findValidByHash(String hash) {
        return find("tokenHash = ?1 and revokedAt is null and expiresAt > ?2", hash, Instant.now())
                .firstResult();
    }
}
