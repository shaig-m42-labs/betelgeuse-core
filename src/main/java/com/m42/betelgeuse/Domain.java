package com.m42.betelgeuse;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@MappedSuperclass
abstract class BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    UUID id;

    @Column(name = "created_at", nullable = false)
    Instant createdAt = Instant.now();

    public UUID getId() {
        return id;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}

@Entity
@Table(name = "organizations")
class Organization extends BaseEntity {
    @Column(nullable = false)
    String name;

    @Column(name = "created_by")
    String createdBy;

    protected Organization() {
    }

    Organization(String name, String createdBy) {
        this.name = name;
        this.createdBy = createdBy;
    }

    public String getName() {
        return name;
    }

    public String getCreatedBy() {
        return createdBy;
    }
}

@Entity
@Table(name = "teams")
class Team extends BaseEntity {
    @ManyToOne(optional = false)
    Organization organization;

    @Column(nullable = false)
    String name;

    protected Team() {
    }

    Team(Organization organization, String name) {
        this.organization = organization;
        this.name = name;
    }

    public Organization getOrganization() {
        return organization;
    }

    public String getName() {
        return name;
    }
}

@Entity
@Table(name = "services")
class BackendService extends BaseEntity {
    @ManyToOne(optional = false)
    Organization organization;

    @Column(nullable = false)
    String name;

    String description;

    protected BackendService() {
    }

    BackendService(Organization organization, String name, String description) {
        this.organization = organization;
        this.name = name;
        this.description = description;
    }

    public Organization getOrganization() {
        return organization;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }
}

@Entity
@Table(name = "environments")
class Environment extends BaseEntity {
    @ManyToOne(optional = false)
    BackendService service;

    @Column(nullable = false)
    String name;

    @Column(name = "base_url")
    String baseUrl;

    protected Environment() {
    }

    Environment(BackendService service, String name, String baseUrl) {
        this.service = service;
        this.name = name;
        this.baseUrl = baseUrl;
    }

    public BackendService getService() {
        return service;
    }

    public String getName() {
        return name;
    }

    public String getBaseUrl() {
        return baseUrl;
    }
}

@Entity
@Table(name = "health_check_configs")
class HealthCheckConfig extends BaseEntity {
    @ManyToOne(optional = false)
    BackendService service;

    @ManyToOne(optional = false)
    Environment environment;

    @Column(nullable = false)
    String name;

    @Column(nullable = false)
    String url;

    @Column(nullable = false)
    String method = "GET";

    @Column(name = "interval_seconds", nullable = false)
    int intervalSeconds = 30;

    @Column(name = "timeout_seconds", nullable = false)
    int timeoutSeconds = 3;

    @Column(nullable = false)
    boolean enabled = true;

    protected HealthCheckConfig() {
    }

    HealthCheckConfig(BackendService service, Environment environment, String name, String url, String method,
                      int intervalSeconds, int timeoutSeconds, boolean enabled) {
        this.service = service;
        this.environment = environment;
        this.name = name;
        this.url = url;
        this.method = method == null || method.isBlank() ? "GET" : method;
        this.intervalSeconds = intervalSeconds <= 0 ? 30 : intervalSeconds;
        this.timeoutSeconds = timeoutSeconds <= 0 ? 3 : timeoutSeconds;
        this.enabled = enabled;
    }

    public BackendService getService() {
        return service;
    }

    public Environment getEnvironment() {
        return environment;
    }

    public String getName() {
        return name;
    }

    public String getUrl() {
        return url;
    }

    public String getMethod() {
        return method;
    }

    public int getIntervalSeconds() {
        return intervalSeconds;
    }

    public int getTimeoutSeconds() {
        return timeoutSeconds;
    }

    public boolean isEnabled() {
        return enabled;
    }
}

enum IncidentStatus {
    OPEN,
    RESOLVED
}

@Entity
@Table(name = "incidents")
class Incident extends BaseEntity {
    @ManyToOne(optional = false)
    BackendService service;

    @ManyToOne
    HealthCheckConfig healthCheckConfig;

    @Column(nullable = false)
    String title;

    @Column(nullable = false)
    String severity;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    IncidentStatus status = IncidentStatus.OPEN;

    @Column(name = "resolved_at")
    Instant resolvedAt;

    protected Incident() {
    }

    Incident(BackendService service, HealthCheckConfig healthCheckConfig, String title, String severity) {
        this.service = service;
        this.healthCheckConfig = healthCheckConfig;
        this.title = title;
        this.severity = severity == null || severity.isBlank() ? "SEV3" : severity;
    }

    void resolve() {
        this.status = IncidentStatus.RESOLVED;
        this.resolvedAt = Instant.now();
    }

    public BackendService getService() {
        return service;
    }

    public HealthCheckConfig getHealthCheckConfig() {
        return healthCheckConfig;
    }

    public String getTitle() {
        return title;
    }

    public String getSeverity() {
        return severity;
    }

    public IncidentStatus getStatus() {
        return status;
    }

    public Instant getResolvedAt() {
        return resolvedAt;
    }
}

@Entity
@Table(name = "incident_timeline")
class IncidentTimeline extends BaseEntity {
    @ManyToOne(optional = false)
    Incident incident;

    @Column(nullable = false)
    String message;

    protected IncidentTimeline() {
    }

    IncidentTimeline(Incident incident, String message) {
        this.incident = incident;
        this.message = message;
    }

    public Incident getIncident() {
        return incident;
    }

    public String getMessage() {
        return message;
    }
}
