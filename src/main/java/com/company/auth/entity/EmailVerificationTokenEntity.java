package com.company.auth.entity;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.*;

@Entity @Table(name="email_verification_tokens")
public class EmailVerificationTokenEntity extends PanacheEntity {
    @ManyToOne(optional=false) @JoinColumn(name="user_id")
    public UserEntity user;

    @Column(nullable=false, unique=true, length=64)
    public String tokenHash;

    @Column(nullable=false)
    public java.time.Instant expiresAt;

    public java.time.Instant usedAt;
}
