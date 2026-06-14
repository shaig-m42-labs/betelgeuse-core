# betelgeuse-core

Core reliability domain service for Orion Platform V1.

## V1 Domain

- Organizations
- Teams
- Services
- Environments
- Health check configs
- Incidents
- Incident timeline

## Endpoints

- `POST /organizations`, `GET /organizations`
- `POST /teams`, `GET /teams`
- `POST /services`, `GET /services`, `GET /services/{id}`
- `POST /services/{id}/environments`
- `POST /services/{id}/health-checks`
- `GET /internal/health-checks/active`
- `POST /incidents`, `GET /incidents`, `GET /incidents/{id}`
- `PATCH /incidents/{id}/resolve`
- `POST /incidents/{id}/timeline`

Core owns incident creation and duplicate prevention.
