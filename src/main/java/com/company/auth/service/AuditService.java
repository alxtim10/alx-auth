package com.company.auth.service;

import com.company.auth.entity.AuditLogEntity;
import com.company.auth.entity.UserEntity;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

@ApplicationScoped
public class AuditService {
    @Transactional
    public void log(UserEntity actor, String action, String resource, Long resourceId, String metaJson) {
        AuditLogEntity l = new AuditLogEntity();
        l.actor = actor;
        l.action = action;
        l.resource = resource;
        l.resourceId = resourceId;
        l.metaJson = metaJson;
        l.persist();
    }
}
