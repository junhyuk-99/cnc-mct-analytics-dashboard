# CNC/MCT Analytics Dashboard Demo

Synthetic CNC/MCT manufacturing dashboard demo with a Spring Boot backend, MongoDB sample dataset, and React frontend.

This repository is a public, rebuilt demo project. It is not a copy of production source code and does not include production data, customer data, real database connections, equipment history, server IP addresses, private credentials, logs, certificates, or private environment values.

All future demo data will be synthetic sample data.

## Demo Scope

The demo shows analytics workflows for CNC/MCT equipment using a local-only stack:

- Equipment utilization
- RunTime / CutTime cutting ratio
- Alarm history
- Machine status distribution
- Daily trend charts
- KPI cards and chart-based dashboard views

## Tech Stack

- Spring Boot API
- Vite + React + TypeScript dashboard
- MongoDB
- Python seed script
- Docker Compose
- Recharts

## Local Demo Flow

1. Start local MongoDB.
2. Run the Spring Boot backend API.
3. Run the React frontend.
4. Open the dashboard and review synthetic CNC/MCT analytics.

## Screenshots

### Command Center Overview

![Command Center Overview](screenshots/dashboard-command-overview.png)

### Analytics Panels

![Analytics Panels](screenshots/dashboard-analytics-panels.png)

### Alarm History

![Alarm History](screenshots/dashboard-alarm-history.png)

## Sample Data

Generate local synthetic sample data with:

```bash
python scripts/generate_sample_data.py
```

The generated files under `sample-data/` are fake demo records only. They are not copied from real production systems.

## Backend

The Spring Boot backend lives in `backend/` and exposes read-only demo APIs over synthetic MongoDB collections:

- `machines`
- `status_history`
- `runtime_cuttime`
- `alarm_history`
- `daily_summary`

Configuration defaults:

- Java 17
- Spring Boot 3.x
- Server port `8090`
- MongoDB URI `${MONGODB_URI:mongodb://localhost:27017/cnc_mct_demo}`
- CORS origins `http://localhost:3000` and `http://localhost:5173`

Run with the included Gradle wrapper:

```powershell
cd backend
.\gradlew.bat bootRun
```

Or run with an installed Gradle:

```powershell
cd backend
gradle bootRun
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

Open `http://localhost:5173` in a browser.

To smoke test the backend API from the repository root:

```powershell
.\scripts\test_backend_api.ps1
```

See [docs/RUNTIME_TEST.md](docs/RUNTIME_TEST.md) for the full runtime test flow and troubleshooting notes.

## Security Notice

- Do not add production `.env` files.
- Do not add real DB URIs, server IPs, credentials, keys, certificates, logs, dumps, or customer screenshots.
- Do not import private repository history.
- Use only synthetic data in `sample-data/`.

See [docs/SECURITY.md](docs/SECURITY.md) and [docs/DATA_NOTICE.md](docs/DATA_NOTICE.md).
