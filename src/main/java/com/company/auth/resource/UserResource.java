package com.company.auth.resource;

import com.company.auth.dto.UpdateUserRequest;
import com.company.auth.dto.UserResponse;
import com.company.auth.entity.UserEntity;
import com.company.auth.service.UserService;

import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@Path("/users")
public class UserResource {

    @Inject
    UserService userService;

    @Inject
    org.eclipse.microprofile.jwt.JsonWebToken jwt;

    @GET
@RolesAllowed("ADMIN")
public Map<String, Object> list(
        @QueryParam("page") @DefaultValue("1") int page,
        @QueryParam("size") @DefaultValue("10") int size,
        @QueryParam("sort") @DefaultValue("id") String sort,
        @QueryParam("dir") @DefaultValue("desc") String dir,
        @QueryParam("q") String q,
        @QueryParam("role") String role,
        @QueryParam("active") Boolean active,
        @QueryParam("createdFrom") String createdFrom,
        @QueryParam("createdTo") String createdTo
) {
    Instant from = (createdFrom != null && !createdFrom.isBlank()) ? Instant.parse(createdFrom) : null;
    Instant to = (createdTo != null && !createdTo.isBlank()) ? Instant.parse(createdTo) : null;

    var result = userService.listUsers(page, size, sort, dir, q, role, active, from, to);

    int safeSize = Math.min(Math.max(size, 1), 100);
    int totalPages = (int) Math.ceil((double) result.total() / safeSize);

    Map<String, Object> filters = new LinkedHashMap<>();
    filters.put("q", q);
    filters.put("role", role);
    filters.put("active", active);
    filters.put("createdFrom", createdFrom);
    filters.put("createdTo", createdTo);

    Map<String, Object> resp = new LinkedHashMap<>();
    resp.put("page", page);
    resp.put("size", safeSize);
    resp.put("total", result.total());
    resp.put("totalPages", totalPages);
    resp.put("sort", sort);
    resp.put("dir", dir);
    resp.put("filters", filters);
    resp.put("data", result.data().stream().map(UserResponse::from).toList());

    return resp;
}

    @PUT
    @Path("/{id}")
    @RolesAllowed("ADMIN")
    public UserEntity update(@PathParam("id") Long id, UpdateUserRequest req) {
        return userService.update(id, req, true);
    }

    @DELETE
    @Path("/{id}")
    @RolesAllowed("ADMIN")
    public void delete(@PathParam("id") Long id) {
        String actorUsername = jwt.getSubject();
        UserEntity actor = UserEntity.find("username", actorUsername).firstResult();
        userService.softDelete(id, actor);
    }
}
