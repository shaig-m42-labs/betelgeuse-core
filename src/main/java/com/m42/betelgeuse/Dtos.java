package com.m42.betelgeuse;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

record CreateOrganizationRequest(@NotBlank String name) {
}

record CreateTeamRequest(@NotNull UUID organizationId, @NotBlank String name) {
}

record CreateServiceRequest(@NotNull UUID organizationId, @NotBlank String name, String description) {
}

record CreateEnvironmentRequest(@NotBlank String name, String baseUrl) {
}

record CreateHealthCheckRequest(@NotNull UUID environmentId, @NotBlank String name, @NotBlank String url,
                                String method, int intervalSeconds, int timeoutSeconds, boolean enabled) {
}

record CreateIncidentRequest(@NotNull UUID serviceId, String healthCheckConfigId, @NotBlank String title, String severity) {
}

record AddTimelineRequest(@NotBlank String message) {
}

record HealthFailurePayload(@NotNull UUID healthCheckConfigId, UUID serviceId, String url, int statusCode, String error) {
}

record EventEnvelope(String eventId, String eventType, String source, Instant occurredAt, String correlationId, Map<String, Object> payload) {
}

record ActiveHealthCheckResponse(UUID id, UUID serviceId, String serviceName, UUID environmentId, String environmentName,
                                 String url, String method, int intervalSeconds, int timeoutSeconds) {
}
