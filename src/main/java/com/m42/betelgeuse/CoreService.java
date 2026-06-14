package com.m42.betelgeuse;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
class CoreService {
    private final OrganizationRepository organizations;
    private final TeamRepository teams;
    private final BackendServiceRepository services;
    private final EnvironmentRepository environments;
    private final HealthCheckConfigRepository healthChecks;
    private final IncidentRepository incidents;
    private final IncidentTimelineRepository timelines;
    private final EventPublisher events;

    CoreService(OrganizationRepository organizations, TeamRepository teams, BackendServiceRepository services,
                EnvironmentRepository environments, HealthCheckConfigRepository healthChecks,
                IncidentRepository incidents, IncidentTimelineRepository timelines, EventPublisher events) {
        this.organizations = organizations;
        this.teams = teams;
        this.services = services;
        this.environments = environments;
        this.healthChecks = healthChecks;
        this.incidents = incidents;
        this.timelines = timelines;
        this.events = events;
    }

    @Transactional
    Organization createOrganization(CreateOrganizationRequest request, String userEmail) {
        return organizations.save(new Organization(request.name(), userEmail));
    }

    List<Organization> organizations() {
        return organizations.findAll();
    }

    @Transactional
    Team createTeam(CreateTeamRequest request) {
        Organization org = organizations.findById(request.organizationId()).orElseThrow(() -> new NotFoundException("Organization not found"));
        return teams.save(new Team(org, request.name()));
    }

    List<Team> teams() {
        return teams.findAll();
    }

    @Transactional
    BackendService createService(CreateServiceRequest request, String correlationId) {
        Organization org = organizations.findById(request.organizationId()).orElseThrow(() -> new NotFoundException("Organization not found"));
        BackendService service = services.save(new BackendService(org, request.name(), request.description()));
        events.publish("orion.service.registered", "service.registered", correlationId, Map.of(
                "serviceId", service.id.toString(),
                "organizationId", org.id.toString(),
                "name", service.name));
        return service;
    }

    List<BackendService> services() {
        return services.findAll();
    }

    BackendService service(UUID id) {
        return services.findById(id).orElseThrow(() -> new NotFoundException("Service not found"));
    }

    @Transactional
    Environment createEnvironment(UUID serviceId, CreateEnvironmentRequest request) {
        BackendService service = service(serviceId);
        return environments.save(new Environment(service, request.name(), request.baseUrl()));
    }

    @Transactional
    HealthCheckConfig createHealthCheck(UUID serviceId, CreateHealthCheckRequest request) {
        BackendService service = service(serviceId);
        Environment environment = environments.findById(request.environmentId()).orElseThrow(() -> new NotFoundException("Environment not found"));
        if (!environment.service.id.equals(service.id)) {
            throw new IllegalArgumentException("Environment does not belong to service");
        }
        return healthChecks.save(new HealthCheckConfig(service, environment, request.name(), request.url(), request.method(),
                request.intervalSeconds(), request.timeoutSeconds(), request.enabled()));
    }

    List<ActiveHealthCheckResponse> activeHealthChecks() {
        return healthChecks.findByEnabledTrue().stream()
                .map(h -> new ActiveHealthCheckResponse(h.id, h.service.id, h.service.name, h.environment.id, h.environment.name,
                        h.url, h.method, h.intervalSeconds, h.timeoutSeconds))
                .toList();
    }

    @Transactional
    Incident createIncident(CreateIncidentRequest request, String correlationId) {
        BackendService service = service(request.serviceId());
        HealthCheckConfig check = request.healthCheckConfigId() == null || request.healthCheckConfigId().isBlank()
                ? null
                : healthChecks.findById(UUID.fromString(request.healthCheckConfigId())).orElseThrow(() -> new NotFoundException("Health check not found"));
        Incident incident = incidents.save(new Incident(service, check, request.title(), request.severity()));
        timelines.save(new IncidentTimeline(incident, "Incident opened"));
        publishIncident("orion.incident.opened", "incident.opened", correlationId, incident);
        return incident;
    }

    List<Incident> incidents() {
        return incidents.findAll();
    }

    Incident incident(UUID id) {
        return incidents.findById(id).orElseThrow(() -> new NotFoundException("Incident not found"));
    }

    @Transactional
    Incident resolve(UUID id, String correlationId) {
        Incident incident = incident(id);
        if (incident.status != IncidentStatus.RESOLVED) {
            incident.resolve();
            timelines.save(new IncidentTimeline(incident, "Incident resolved"));
            publishIncident("orion.incident.resolved", "incident.resolved", correlationId, incident);
        }
        return incident;
    }

    @Transactional
    IncidentTimeline addTimeline(UUID incidentId, AddTimelineRequest request) {
        return timelines.save(new IncidentTimeline(incident(incidentId), request.message()));
    }

    @Transactional
    void handleHealthCheckFailed(UUID healthCheckConfigId, String correlationId, Map<String, Object> failurePayload) {
        HealthCheckConfig check = healthChecks.findById(healthCheckConfigId).orElseThrow(() -> new NotFoundException("Health check not found"));
        incidents.findByHealthCheckConfigIdAndStatus(healthCheckConfigId, IncidentStatus.OPEN)
                .orElseGet(() -> {
                    Incident incident = incidents.save(new Incident(check.service, check, "Health check failed: " + check.name, "SEV3"));
                    timelines.save(new IncidentTimeline(incident, "Auto-opened from failed health check"));
                    publishIncident("orion.incident.opened", "incident.opened", correlationId, incident);
                    return incident;
                });
    }

    private void publishIncident(String subject, String eventType, String correlationId, Incident incident) {
        events.publish(subject, eventType, correlationId, Map.of(
                "incidentId", incident.id.toString(),
                "serviceId", incident.service.id.toString(),
                "status", incident.status.name(),
                "title", incident.title));
    }
}
