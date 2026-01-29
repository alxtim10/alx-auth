package com.company.auth.service;

import com.company.auth.dto.RegisterRequest;
import com.company.auth.dto.UpdateUserRequest;
import com.company.auth.entity.RefreshTokenEntity;
import com.company.auth.entity.UserEntity;
import io.quarkus.elytron.security.common.BcryptUtil;
import io.quarkus.panache.common.Page;
import io.quarkus.panache.common.Sort;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.WebApplicationException;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@ApplicationScoped
public class UserService {

    @Inject
    AuditService auditService;

    public boolean verify(String raw, String hash) {
        return BcryptUtil.matches(raw, hash);
    }

    @Transactional
    public UserEntity register(RegisterRequest req) {
        if (UserEntity.find("username", req.username).firstResult() != null)
            throw new WebApplicationException("Username exists", 409);
        if (req.email == null || req.email.isBlank())
            throw new WebApplicationException("Email is required", 400);

        UserEntity u = new UserEntity();
        u.username = req.username;
        u.email = req.email;
        u.passwordHash = BcryptUtil.bcryptHash(req.password);
        u.persist();
        return u;
    }

    public record PagedResult<T>(List<T> data, long total) {
    }

    public PagedResult<UserEntity> listUsers(
            int page, int size,
            String sortField, String dir,
            String q, String role, Boolean active,
            Instant createdFrom, Instant createdTo) {
        if (page < 1)
            page = 1;
        if (size < 1)
            size = 10;
        if (size > 100)
            size = 100;

        if (!isSortableField(sortField))
            sortField = "id";
        Sort sort = "asc".equalsIgnoreCase(dir)
                ? Sort.by(sortField).ascending()
                : Sort.by(sortField).descending();

        StringBuilder where = new StringBuilder("deletedAt is null");
        List<Object> params = new ArrayList<>();

        if (q != null && !q.isBlank()) {
            where.append(" and (LOWER(username) like ?").append(params.size() + 1)
                    .append(" or LOWER(email) like ?").append(params.size() + 2).append(")");
            String like = "%" + q.toLowerCase() + "%";
            params.add(like);
            params.add(like);
        }

        if (role != null && !role.isBlank()) {
            where.append(" and role = ?").append(params.size() + 1);
            params.add(role.trim().toUpperCase());
        }

        if (active != null) {
            where.append(" and active = ?").append(params.size() + 1);
            params.add(active);
        }

        if (createdFrom != null) {
            where.append(" and createdAt >= ?").append(params.size() + 1);
            params.add(createdFrom);
        }

        if (createdTo != null) {
            where.append(" and createdAt <= ?").append(params.size() + 1);
            params.add(createdTo);
        }

        var query = UserEntity.find(where.toString(), sort, params.toArray());
        long total = query.count();

        List<UserEntity> data = query.page(Page.of(page - 1, size)).list();
        return new PagedResult<>(data, total);
    }

    public long countUsers() {
        return UserEntity.count();
    }

    private boolean isSortableField(String field) {
        return field != null && switch (field) {
            case "id", "username", "email", "role", "active", "createdAt", "updatedAt" -> true;
            default -> false;
        };
    }

    public UserEntity find(Long id) {
        UserEntity u = UserEntity.findById(id);
        if (u == null)
            throw new WebApplicationException("Not found", 404);
        return u;
    }

    public UserEntity findByEmail(String email) {
        UserEntity u = UserEntity.find("email", email).firstResult();
        if (u == null)
            throw new WebApplicationException("Not found", 404);
        return u;
    }

    @Transactional
    public UserEntity update(Long id, UpdateUserRequest req, boolean admin) {
        UserEntity u = find(id);

        if (req.email != null)
            u.email = req.email;
        if (req.password != null)
            u.passwordHash = BcryptUtil.bcryptHash(req.password);

        if (admin) {
            if (req.role != null)
                u.role = req.role;
            if (req.active != null)
                u.active = req.active;
        }
        return u;
    }

    @Transactional
    public void delete(Long id) {
        find(id).delete();
    }

    @Transactional
    public void softDelete(Long id, UserEntity actor) {
        UserEntity u = find(id);
        u.deletedAt = java.time.Instant.now();
        u.active = false;
        u.updatedAt = java.time.Instant.now();

        // revoke refresh tokens too
        RefreshTokenEntity.update("revokedAt = ?1 where user = ?2 and revokedAt is null",
                java.time.Instant.now(), u);

        auditService.log(actor, "USER_DELETE", "USER", u.id,
                "{\"deletedAt\":\"" + u.deletedAt + "\"}");
    }

}
