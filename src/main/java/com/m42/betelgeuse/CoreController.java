package com.m42.betelgeuse;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
class CoreController {
    private final CoreService core;

    CoreController(CoreService core) {
        this.core = core;
    }

    @PostMapping("/organizations")
    ApiResponse<Organization> createOrganization(@Valid @RequestBody CreateOrganizationRequest request, HttpServletRequest servlet) {
        return ok(core.createOrganization(request, servlet.getHeader("X-User-Email")), servlet);
    }

    @GetMapping("/organizations")
    ApiResponse<List<Organization>> organizations(HttpServletRequest servlet) {
        return ok(core.organizations(), servlet);
    }

    @PostMapping("/teams")
    ApiResponse<Team> createTeam(@Valid @RequestBody CreateTeamRequest request, HttpServletRequest servlet) {
        return ok(core.createTeam(request), servlet);
    }

    @GetMapping("/teams")
    ApiResponse<List<Team>> teams(HttpServletRequest servlet) {
        return ok(core.teams(), servlet);
    }

    @PostMapping("/services")
    ApiResponse<BackendService> createService(@Valid @RequestBody CreateServiceRequest request, HttpServletRequest servlet) {
        return ok(core.createService(request, correlation(servlet)), servlet);
    }

    @GetMapping("/services")
    ApiResponse<List<BackendService>> services(HttpServletRequest servlet) {
        return ok(core.services(), servlet);
    }

    @GetMapping("/services/{id}")
    ApiResponse<BackendService> service(@PathVariable UUID id, HttpServletRequest servlet) {
        return ok(core.service(id), servlet);
    }

    @PostMapping("/services/{id}/environments")
    ApiResponse<Environment> createEnvironment(@PathVariable UUID id, @Valid @RequestBody CreateEnvironmentRequest request, HttpServletRequest servlet) {
        return ok(core.createEnvironment(id, request), servlet);
    }

    @PostMapping("/services/{id}/health-checks")
    ApiResponse<HealthCheckConfig> createHealthCheck(@PathVariable UUID id, @Valid @RequestBody CreateHealthCheckRequest request, HttpServletRequest servlet) {
        return ok(core.createHealthCheck(id, request), servlet);
    }

    @GetMapping("/internal/health-checks/active")
    ApiResponse<List<ActiveHealthCheckResponse>> activeHealthChecks(HttpServletRequest servlet) {
        return ok(core.activeHealthChecks(), servlet);
    }

    @PostMapping("/internal/health-check-failures")
    ApiResponse<Map<String, String>> healthFailure(@Valid @RequestBody HealthFailurePayload request, HttpServletRequest servlet) {
        core.handleHealthCheckFailed(request.healthCheckConfigId(), correlation(servlet), Map.of(
                "healthCheckConfigId", request.healthCheckConfigId().toString(),
                "serviceId", request.serviceId().toString(),
                "url", request.url(),
                "statusCode", request.statusCode(),
                "error", request.error() == null ? "" : request.error()));
        return ok(Map.of("status", "accepted"), servlet);
    }

    @PostMapping("/incidents")
    ApiResponse<Incident> createIncident(@Valid @RequestBody CreateIncidentRequest request, HttpServletRequest servlet) {
        return ok(core.createIncident(request, correlation(servlet)), servlet);
    }

    @GetMapping("/incidents")
    ApiResponse<List<Incident>> incidents(HttpServletRequest servlet) {
        return ok(core.incidents(), servlet);
    }

    @GetMapping("/incidents/{id}")
    ApiResponse<Incident> incident(@PathVariable UUID id, HttpServletRequest servlet) {
        return ok(core.incident(id), servlet);
    }

    @PatchMapping("/incidents/{id}/resolve")
    ApiResponse<Incident> resolve(@PathVariable UUID id, HttpServletRequest servlet) {
        return ok(core.resolve(id, correlation(servlet)), servlet);
    }

    @PostMapping("/incidents/{id}/timeline")
    ApiResponse<IncidentTimeline> timeline(@PathVariable UUID id, @Valid @RequestBody AddTimelineRequest request, HttpServletRequest servlet) {
        return ok(core.addTimeline(id, request), servlet);
    }

    @GetMapping("/health")
    ApiResponse<Map<String, String>> health(HttpServletRequest servlet) {
        return ok(Map.of("service", "betelgeuse-core", "status", "UP"), servlet);
    }

    private <T> ApiResponse<T> ok(T data, HttpServletRequest servlet) {
        return ApiResponse.ok(data, correlation(servlet));
    }

    private String correlation(HttpServletRequest servlet) {
        return (String) servlet.getAttribute("correlationId");
    }
}
