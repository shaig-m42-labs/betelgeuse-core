package com.m42.betelgeuse;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

interface OrganizationRepository extends JpaRepository<Organization, UUID> {
}

interface TeamRepository extends JpaRepository<Team, UUID> {
}

interface BackendServiceRepository extends JpaRepository<BackendService, UUID> {
}

interface EnvironmentRepository extends JpaRepository<Environment, UUID> {
}

interface HealthCheckConfigRepository extends JpaRepository<HealthCheckConfig, UUID> {
    List<HealthCheckConfig> findByEnabledTrue();
}

interface IncidentRepository extends JpaRepository<Incident, UUID> {
    Optional<Incident> findByHealthCheckConfigIdAndStatus(UUID healthCheckConfigId, IncidentStatus status);
}

interface IncidentTimelineRepository extends JpaRepository<IncidentTimeline, UUID> {
    List<IncidentTimeline> findByIncidentIdOrderByCreatedAtAsc(UUID incidentId);
}
