# betelgeuse-core
Core reliability domain service for managing services, incidents, deployments, dependencies, and operational workflows.

**Language:** ```Java```
**Stack:** ```Spring Boot, Spring Security, PostgreSQL, Redis, JWT.```

**Domain:**
```
Organization
Team
Service
Environment
ServiceDependency
HealthCheckConfig
Deployment
Incident
IncidentTimeline
Runbook
SLO
AlertRule
WebhookSubscription
AuditLog
```

**Main features:**
```
Create organization/team
Register backend service
Define environments: dev/staging/prod
Add service dependencies
Create deployment record
Create incident manually
Auto-open incident from health failure event
Attach runbook to incident
Resolve incident
Track incident timeline
Publish domain events
```

**Core service event should publish:**
```
service.registered
deployment.created
deployment.failed
incident.opened
incident.resolved
healthcheck.failed
runbook.attached
```

**Must Have Patterns:**
```
Specification pattern
Policy classes
State transition validator
Outbox pattern
Domain events
Audit logging
Idempotency for important commands
```
