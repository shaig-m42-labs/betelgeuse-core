create table organizations (
    id uuid primary key,
    name varchar(255) not null,
    created_by varchar(255),
    created_at timestamptz not null default now()
);

create table teams (
    id uuid primary key,
    organization_id uuid not null references organizations(id),
    name varchar(255) not null,
    created_at timestamptz not null default now()
);

create table services (
    id uuid primary key,
    organization_id uuid not null references organizations(id),
    name varchar(255) not null,
    description text,
    created_at timestamptz not null default now()
);

create table environments (
    id uuid primary key,
    service_id uuid not null references services(id),
    name varchar(255) not null,
    base_url text,
    created_at timestamptz not null default now()
);

create table health_check_configs (
    id uuid primary key,
    service_id uuid not null references services(id),
    environment_id uuid not null references environments(id),
    name varchar(255) not null,
    url text not null,
    method varchar(16) not null default 'GET',
    interval_seconds integer not null default 30,
    timeout_seconds integer not null default 3,
    enabled boolean not null default true,
    created_at timestamptz not null default now()
);

create table incidents (
    id uuid primary key,
    service_id uuid not null references services(id),
    health_check_config_id uuid references health_check_configs(id),
    title varchar(255) not null,
    severity varchar(32) not null,
    status varchar(32) not null,
    resolved_at timestamptz,
    created_at timestamptz not null default now()
);

create unique index uq_active_incident_per_health_check
    on incidents(health_check_config_id)
    where status = 'OPEN' and health_check_config_id is not null;

create table incident_timeline (
    id uuid primary key,
    incident_id uuid not null references incidents(id),
    message text not null,
    created_at timestamptz not null default now()
);
