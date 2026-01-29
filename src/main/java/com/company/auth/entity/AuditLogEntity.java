package com.company.auth.entity;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.*;

@Entity
@Table(name="audit_logs")
public class AuditLogEntity extends PanacheEntity {

    @ManyToOne @JoinColumn(name="actor_user_id")
    public UserEntity actor; // who did it (nullable for system)

    @Column(nullable=false, length=50)
    public String action; // USER_UPDATE, USER_DELETE, PASSWORD_RESET, LOGIN...

    @Column(nullable=false, length=50)
    public String resource; // USER, AUTH, etc

    public Long resourceId;

    @Column(columnDefinition="text")
    public String metaJson;

    @Column(nullable=false)
    public java.time.Instant createdAt = java.time.Instant.now();
}
