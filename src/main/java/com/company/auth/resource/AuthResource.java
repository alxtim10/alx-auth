package com.company.auth.resource;

import java.time.Instant;

import org.eclipse.microprofile.jwt.JsonWebToken;

import com.company.auth.dto.ChangePasswordRequest;
import com.company.auth.dto.EmailRequest;
import com.company.auth.dto.LoginRequest;
import com.company.auth.dto.RefreshRequest;
import com.company.auth.dto.RegisterRequest;
import com.company.auth.dto.ResetPasswordRequest;
import com.company.auth.dto.VerifyRequest;
import com.company.auth.entity.EmailVerificationTokenEntity;
import com.company.auth.entity.PasswordResetTokenEntity;
import com.company.auth.entity.RefreshTokenEntity;
import com.company.auth.entity.UserEntity;
import com.company.auth.security.JwtService;
import com.company.auth.service.UserService;

import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/auth")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class AuthResource {

    @Inject
    UserService userService;
    @Inject
    JwtService jwtService;
    @Inject
    JsonWebToken jwt;

    @POST
    @Path("/register")
    public UserEntity register(RegisterRequest req) {
        return userService.register(req);
    }

    @POST
    @Path("/login")
    @Transactional
    public Response login(LoginRequest req, @HeaderParam("User-Agent") String ua,
            @HeaderParam("X-Forwarded-For") String xff) {
        UserEntity u = UserEntity.find("username = ?1 and deletedAt is null", req.username).firstResult();
        if (u == null || !u.active || !userService.verify(req.password, u.passwordHash))
            return Response.status(401).entity("{\"message\":\"Invalid credentials\"}").build();

        String accessToken = jwtService.generate(u.id, u.username, u.role);

        // generate refresh token (random)
        String refreshToken = java.util.UUID.randomUUID() + "." + java.util.UUID.randomUUID();
        String refreshHash = com.company.auth.security.TokenHash.sha256Hex(refreshToken);

        RefreshTokenEntity rt = new RefreshTokenEntity();
        rt.user = u;
        rt.tokenHash = refreshHash;
        rt.expiresAt = java.time.Instant.now().plus(java.time.Duration.ofDays(14));
        rt.device = ua;
        rt.ip = xff;
        rt.persist();

        return Response.ok()
                .entity("{\"access_token\":\"" + accessToken + "\",\"refresh_token\":\"" + refreshToken + "\"}")
                .build();
    }

    @POST
    @Path("/refresh")
    @Transactional
    public Response refresh(RefreshRequest req, @HeaderParam("User-Agent") String ua,
            @HeaderParam("X-Forwarded-For") String xff) {
        if (req.refresh_token == null || req.refresh_token.isBlank())
            throw new WebApplicationException("refresh_token required", 400);

        String hash = com.company.auth.security.TokenHash.sha256Hex(req.refresh_token);
        RefreshTokenEntity existing = RefreshTokenEntity.findValidByHash(hash);
        if (existing == null)
            return Response.status(401).entity("{\"message\":\"Invalid refresh\"}").build();

        // ROTATION: revoke old token, issue new one
        existing.revokedAt = java.time.Instant.now();

        UserEntity u = existing.user;
        if (u.deletedAt != null || !u.active)
            return Response.status(401).build();

        String newAccess = jwtService.generate(u.id, u.username, u.role);

        String newRefresh = java.util.UUID.randomUUID() + "." + java.util.UUID.randomUUID();
        String newHash = com.company.auth.security.TokenHash.sha256Hex(newRefresh);

        RefreshTokenEntity rt = new RefreshTokenEntity();
        rt.user = u;
        rt.tokenHash = newHash;
        rt.expiresAt = java.time.Instant.now().plus(java.time.Duration.ofDays(14));
        rt.device = ua;
        rt.ip = xff;
        rt.persist();

        return Response.ok()
                .entity("{\"access_token\":\"" + newAccess + "\",\"refresh_token\":\"" + newRefresh + "\"}")
                .build();
    }

    @POST
    @Path("/logout")
    @Transactional
    public Response logout(RefreshRequest req) {
        // revoke refresh token (so logout is real)
        if (req.refresh_token != null && !req.refresh_token.isBlank()) {
            String hash = com.company.auth.security.TokenHash.sha256Hex(req.refresh_token);
            RefreshTokenEntity token = RefreshTokenEntity.find("tokenHash", hash).firstResult();
            if (token != null && token.revokedAt == null)
                token.revokedAt = java.time.Instant.now();
        }
        return Response.ok("{\"message\":\"logout success\"}").build();
    }

    @POST
    @Path("/request-verify")
    @Transactional
    public Response requestVerify(EmailRequest req) {
        UserEntity u = UserEntity.find("email = ?1 and deletedAt is null", req.email).firstResult();
        if (u == null)
            return Response.ok("{\"message\":\"if exists, sent\"}").build(); // avoid user enumeration
        if (u.emailVerifiedAt != null)
            return Response.ok("{\"message\":\"already verified\"}").build();

        String raw = java.util.UUID.randomUUID().toString();
        String hash = com.company.auth.security.TokenHash.sha256Hex(raw);

        EmailVerificationTokenEntity t = new EmailVerificationTokenEntity();
        t.user = u;
        t.tokenHash = hash;
        t.expiresAt = java.time.Instant.now().plus(java.time.Duration.ofHours(24));
        t.persist();

        // TODO: send email with raw token link
        return Response.ok("{\"verify_token\":\"" + raw + "\"}").build(); // dev only
    }

    @POST
    @Path("/verify-email")
    @Transactional
    public Response verifyEmail(VerifyRequest req) {
        String hash = com.company.auth.security.TokenHash.sha256Hex(req.token);
        EmailVerificationTokenEntity t = EmailVerificationTokenEntity.find(
                "tokenHash = ?1 and usedAt is null and expiresAt > ?2",
                hash, java.time.Instant.now()).firstResult();

        if (t == null)
            return Response.status(400).entity("{\"message\":\"invalid/expired\"}").build();

        t.usedAt = java.time.Instant.now();
        t.user.emailVerifiedAt = java.time.Instant.now();
        t.user.updatedAt = java.time.Instant.now();

        return Response.ok("{\"message\":\"verified\"}").build();
    }

    @POST
    @Path("/forgot-password")
    @Transactional
    public Response forgotPassword(EmailRequest req) {
        UserEntity u = UserEntity.find("email = ?1 and deletedAt is null", req.email).firstResult();
        if (u == null)
            return Response.ok("{\"message\":\"if exists, sent\"}").build();

        String raw = java.util.UUID.randomUUID().toString();
        String hash = com.company.auth.security.TokenHash.sha256Hex(raw);

        PasswordResetTokenEntity t = new PasswordResetTokenEntity();
        t.user = u;
        t.tokenHash = hash;
        t.expiresAt = java.time.Instant.now().plus(java.time.Duration.ofMinutes(30));
        t.persist();

        // TODO: send email with raw reset token
        return Response.ok("{\"reset_token\":\"" + raw + "\"}").build(); // dev only
    }

    @POST
    @Path("/reset-password")
    @Transactional
    public Response resetPassword(ResetPasswordRequest req) {
        if (req.new_password == null || req.new_password.length() < 6)
            throw new WebApplicationException("password min 6", 400);

        String hash = com.company.auth.security.TokenHash.sha256Hex(req.token);
        PasswordResetTokenEntity t = PasswordResetTokenEntity.find(
                "tokenHash = ?1 and usedAt is null and expiresAt > ?2",
                hash, java.time.Instant.now()).firstResult();

        if (t == null)
            return Response.status(400).entity("{\"message\":\"invalid/expired\"}").build();

        t.usedAt = java.time.Instant.now();
        t.user.passwordHash = io.quarkus.elytron.security.common.BcryptUtil.bcryptHash(req.new_password);
        t.user.updatedAt = java.time.Instant.now();

        // optional: revoke all refresh tokens for this user
        RefreshTokenEntity.update("revokedAt = ?1 where user = ?2 and revokedAt is null",
                java.time.Instant.now(), t.user);

        return Response.ok("{\"message\":\"password updated\"}").build();
    }

    @POST
    @Path("/change-password")
    @RolesAllowed({ "USER", "ADMIN" })
    @Transactional
    public Response changePassword(ChangePasswordRequest req) {
        if (req.old_password == null || req.old_password.isBlank())
            throw new WebApplicationException("old_password is required", 400);
        if (req.new_password == null || req.new_password.length() < 6)
            throw new WebApplicationException("new_password min 6 chars", 400);

        String username = jwt.getSubject();
        UserEntity u = UserEntity.find("username = ?1 and deletedAt is null", username).firstResult();
        if (u == null)
            throw new WebApplicationException("User not found", 404);

        if (!userService.verify(req.old_password, u.passwordHash))
            throw new WebApplicationException("Old password is incorrect", 400);

        // update password
        u.passwordHash = io.quarkus.elytron.security.common.BcryptUtil.bcryptHash(req.new_password);
        u.updatedAt = Instant.now();

        // revoke all refresh tokens so other sessions get logged out
        RefreshTokenEntity.update("revokedAt = ?1 where user = ?2 and revokedAt is null", Instant.now(), u);

        return Response.ok().entity("{\"message\":\"password changed\"}").build();
    }
}