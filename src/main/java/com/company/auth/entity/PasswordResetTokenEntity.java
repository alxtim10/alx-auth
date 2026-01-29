package com.company.auth.entity;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.*;

@Entity @Table(name="password_reset_tokens")
public class PasswordResetTokenEntity extends PanacheEntity {
    @ManyToOne(optional=false) @JoinColumn(name="user_id")
    public UserEntity user;

    @Column(nullable=false, unique=true, length=64)
    public String tokenHash;

    @Column(nullable=false)
    public java.time.Instant expiresAt;

    public java.time.Instant usedAt;
}
