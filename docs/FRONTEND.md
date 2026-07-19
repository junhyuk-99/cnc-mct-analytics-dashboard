# Frontend

This React frontend is the public portfolio UI for the CNC/MCT dashboard API, running on synthetic data. It calls the local Spring Boot backend directly and does not include login, JWT handling, user administration, file upload/download, or mock fallback behavior.

## Stack

- Vite
- React
- TypeScript
- Recharts
- Plain CSS
- npm

## Configuration

The API base URL is read from `VITE_API_BASE_URL`.

```env
VITE_API_BASE_URL=http://localhost:8090/api
```

Create local environment files only if needed for local development. Do not commit real credentials, production endpoints, customer names, server IPs, tokens, or private environment values.

## Run

Start the backend first:

```powershell
cd backend
.\gradlew.bat bootRun
```

Then run the frontend in a new PowerShell session:

```powershell
cd frontend
npm install
npm run dev
```

Open:

```text
http://localhost:5173
```

## Build

```powershell
cd frontend
npm install
npm run build
```

## API Integration

The frontend calls:

- `GET /machines`
- `GET /dashboard/summary?from=YYYY-MM-DD&to=YYYY-MM-DD`
- `GET /dashboard/utilization?from=YYYY-MM-DD&to=YYYY-MM-DD`
- `GET /dashboard/cutting-ratio?from=YYYY-MM-DD&to=YYYY-MM-DD`
- `GET /dashboard/status-distribution?from=YYYY-MM-DD&to=YYYY-MM-DD`
- `GET /dashboard/daily-trend?from=YYYY-MM-DD&to=YYYY-MM-DD`
- `GET /alarms?from=YYYY-MM-DD&to=YYYY-MM-DD&machineId=&severity=`
- `GET /rollup/hourly?date=YYYY-MM-DD`
- `GET /rollup/daily?year=YYYY&month=M`
- `GET /rollup/monthly?year=YYYY`
- `GET /machines/history?machineId=&from=&to=&page=&size=`
- `GET /prealarm/summary?from=&to=` / `GET /prealarm/indicators?from=&to=`

The API client expects the common response wrapper:

```json
{
  "success": true,
  "data": {}
}
```

If `success` is not `true`, or if the HTTP request fails, the dashboard shows an API error banner. There is intentionally no mock data fallback.

## Initial Filters

- `from`: `2026-01-01`
- `to`: `2026-01-30`
- `machineId`: all machines
- `severity`: all severities

## Implemented UI

- Synthetic Precision command-center app shell with demo data notices
- Date, machine, and severity filters
- `REFRESH_ANALYTICS` action, measured frontend request latency, and loading state
- API error and empty states
- Cyber telemetry KPI cards for fleet size, average utilization, cutting ratio, derived active ratio, alarm events, and critical vectors
- Data-backed fleet overview and selected machine focus panel
- CSS/SVG-only `SYNTHETIC MACHINING ENVELOPE` visualization with `NO LIVE CONTROL LINK` and `READ_ONLY_ANALYTICS` labels
- Critical vectors panel derived from critical alarm records, with read-only demo labels instead of control actions
- Frontend-generated `ANALYTICS_EVENT_LOG` based on loaded API results and active filters
- Dark themed utilization, cutting ratio, status distribution, and daily trend Recharts visualizations
- `HOURLY_ROLLUP` chart reading the rollup engine's pre-aggregated hourly buckets, with its own date picker and the shared machine filter; failures on the rollup endpoint never block the rest of the dashboard
- `CUT_PERIOD` chart with a MONTHLY/DAILY granularity toggle and month picker, rolling the hourly buckets forward to per-day and per-month totals
- `PRE_ALARM` panel combining the aggregated alarm landscape (level totals, top codes) with the backend's threshold-based early-warning indicators
- `MACHINE_HISTORY` paged status-event timeline per machine (global machine filter takes precedence over the panel's own picker)
- Terminal-style alarm history table capped at 50 visible rows

## Data Mapping Notes

The reference command-center style includes live camera, G-code, OEE, and production control concepts that are not exposed by the demo API. The frontend intentionally maps or replaces those concepts with available synthetic analytics data:

- `OEE_INDEX` style KPI is represented by `AVG_UTIL`.
- `PERFORMANCE` style KPI is represented by `CUT_RATIO`.
- `FLEET_ACTIVE_RATIO` is derived from `runningMachineCount / machineCount`.
- Live camera or machining telemetry is represented by the CSS-only `SYNTHETIC MACHINING ENVELOPE`.
- G-code terminal output is replaced by `ANALYTICS_EVENT_LOG`.
- Emergency stop, halt, acknowledge, and other production-control actions are not included.
