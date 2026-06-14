# CNC/MCT Analytics Dashboard Demo

Synthetic CNC/MCT manufacturing dashboard demo built with a Spring Boot backend, MongoDB sample dataset, and React frontend.

This repository is a public demo rebuilt from the architecture and workflows of a real deployed CNC/MCT manufacturing dashboard project.

It is not a copy of production source code and does not include production data, customer data, real database connections, equipment history, server IP addresses, private credentials, logs, certificates, or private environment values.

All demo data is synthetic sample data.

## Production Background

This public demo was rebuilt from experience gained during a deployed CNC/MCT manufacturing dashboard project.

The original production system handled manufacturing equipment data, dashboard APIs, machine utilization metrics, RunTime/CutTime analysis, alarm history, status distribution, and operational trend views.

This repository is not a one-to-one copy of the production system. It preserves the main engineering concepts while replacing production-specific implementation details with synthetic data, simplified local runtime components, and read-only demo screens.

Production source code, production screenshots, customer-specific information, server addresses, credentials, private Git history, and real equipment data are intentionally excluded.

See the anonymized case study:

* [CNC/MCT Manufacturing Dashboard Case Study](docs/CASE_STUDY_CNC_MCT_DASHBOARD.md)

## Demo Scope

The demo shows analytics workflows for CNC/MCT equipment using a local-only stack:

* Equipment utilization
* RunTime / CutTime cutting ratio
* Alarm history
* Machine status distribution
* Daily trend charts
* KPI cards and chart-based dashboard views

The frontend uses a dark `Synthetic Precision` command-center interface. It is read-only and replaces unavailable live camera, G-code, and production-control concepts with synthetic analytics panels derived from the local demo API.

## My Role / Contribution

Role: Sole developer

Contribution:

- Designed and implemented the Spring Boot backend API.
- Designed and implemented the React + TypeScript dashboard frontend.
- Designed the MongoDB demo schema and synthetic sample dataset.
- Implemented equipment utilization, Runtime / CutTime ratio, alarm history, machine status distribution, and KPI dashboard views.
- Rebuilt the public demo repository from the architecture and workflows of a real deployed manufacturing dashboard project.
- Removed production source code, production data, customer information, credentials, infrastructure details, and private environment values.

## Tech Stack

* Spring Boot API
* Vite + React + TypeScript dashboard
* MongoDB
* Python seed script
* Docker Compose
* Recharts

## Screenshots

### Command Center Overview

![Command Center Overview](screenshots/dashboard-command-overview.png)

### Analytics Panels

![Analytics Panels](screenshots/dashboard-analytics-panels.png)

### Alarm History

![Alarm History](screenshots/dashboard-alarm-history.png)

## Architecture

```text
Synthetic Sample Data
        ??
MongoDB Docker Container
        ??
Spring Boot Demo API
        ??
React Command Center Dashboard
```

The production project followed the same general dashboard concept, but used operational manufacturing data and deployment-specific infrastructure that is not included in this repository.

## Local Demo Flow

1. Start local MongoDB.
2. Run the Spring Boot backend API.
3. Run the React frontend.
4. Open the dashboard and review synthetic CNC/MCT analytics.

## Sample Data

Generate local synthetic sample data with:

```bash
python scripts/generate_sample_data.py
```

The generated files under `sample-data/` are fake demo records only. They are not copied from real production systems.

## Backend

The Spring Boot backend lives in `backend/` and exposes read-only demo APIs over synthetic MongoDB collections:

* `machines`
* `status_history`
* `runtime_cuttime`
* `alarm_history`
* `daily_summary`

Configuration defaults:

* Java 17
* Spring Boot 3.x
* Server port `8090`
* MongoDB URI `${MONGODB_URI:mongodb://localhost:27017/cnc_mct_demo}`
* CORS origins `http://localhost:3000` and `http://localhost:5173`

Run with the included Gradle wrapper:

```powershell
cd backend
.\gradlew.bat bootRun
```

Build:

```powershell
cd backend
.\gradlew.bat build
```

On startup, the backend imports `sample-data/*.json` into MongoDB only when the target collections are empty. The loader works from either the repository root or the `backend/` directory.

See [docs/API.md](docs/API.md) for endpoint details and response examples.

## Frontend

The React frontend lives in `frontend/` and calls the backend API through `VITE_API_BASE_URL`, defaulting to:

```env
VITE_API_BASE_URL=http://localhost:8090/api
```

Run:

```powershell
cd frontend
npm install
npm run dev
```

Open:

```text
http://localhost:5173
```

Build:

```powershell
cd frontend
npm run build
```

The frontend has no authentication, JWT handling, user administration, file upload/download, or mock fallback. API errors are shown on screen.

See [docs/FRONTEND.md](docs/FRONTEND.md) for frontend details.

## Local Runtime

Run the full local demo:

```powershell
docker compose up -d mongo
cd backend
.\gradlew.bat bootRun
```

In a new PowerShell session:

```powershell
cd frontend
npm install
npm run dev
```

Open:

```text
http://localhost:5173
```

To smoke test the backend API from the repository root:

```powershell
.\scripts\test_backend_api.ps1
```

See [docs/RUNTIME_TEST.md](docs/RUNTIME_TEST.md) for the full runtime test flow and troubleshooting notes.

## Security Notice

* Do not add production `.env` files.
* Do not add real DB URIs, server IPs, credentials, keys, certificates, logs, dumps, or customer screenshots.
* Do not import private repository history.
* Do not include production source code or production Git history.
* Use only synthetic data in `sample-data/`.

See [docs/SECURITY.md](docs/SECURITY.md) and [docs/DATA_NOTICE.md](docs/DATA_NOTICE.md).

## Documentation

| Document | Description |
|---|---|
| [Architecture](docs/ARCHITECTURE.md) | System architecture and data flow overview |
| [API Reference](docs/API.md) | Backend API endpoints and response format |
| [Data Schema](docs/DATA_SCHEMA.md) | MongoDB demo schema and collection structure |
| [Security Notice](docs/SECURITY.md) | Security, anonymization, and disclosure policy |
| [Data Notice](docs/DATA_NOTICE.md) | Synthetic data and data handling notice |
| [Case Study](docs/CASE_STUDY_CNC_MCT_DASHBOARD.md) | Anonymized CNC/MCT dashboard case study |
| [Reuse Candidates](docs/REUSE_CANDIDATES.md) | Reusable modules and extension candidates |
