package com.company.auth.dto;

import com.company.auth.entity.UserEntity;

import java.time.Instant;

public class UserResponse {
    public Long id;
    public String username;
    public String email;
    public String role;
    public boolean active;
    public Instant createdAt;
    public Instant updatedAt;

    public static UserResponse from(UserEntity u) {
        UserResponse r = new UserResponse();
        r.id = u.id;
        r.username = u.username;
        r.email = u.email;
        r.role = u.role;
        r.active = u.active;
        r.createdAt = u.createdAt;
        r.updatedAt = u.updatedAt;
        return r;
    }
}
