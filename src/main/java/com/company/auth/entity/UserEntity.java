package com.company.auth.entity;

import java.time.Instant;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.*;

@Entity
@Table(name = "users")
public class UserEntity extends PanacheEntity {

    @Column(unique = true, nullable = false)
    public String username;

    @Column(unique = true, nullable = false)
    public String email;

    @Column(nullable = false)
    public String passwordHash;

    @Column(nullable = false)
    public String role = "USER"; // USER / ADMIN

    @Column(nullable = false)
    public boolean active = true;

    @Column(nullable = false)
    public Instant createdAt = Instant.now();

    @Column(nullable = false)
    public Instant updatedAt = Instant.now();

    public Instant deletedAt; // null = active, not null = soft deleted
    public Instant emailVerifiedAt; // null until verified
}
