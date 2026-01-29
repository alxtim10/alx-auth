package com.company.auth.dto;

public class UpdateUserRequest {
    public String email;
    public String password;
    public String role;     // ADMIN only
    public Boolean active;  // ADMIN only
}
