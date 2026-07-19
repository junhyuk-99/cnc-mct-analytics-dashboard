# Runtime Test

This guide runs the public backend locally with MongoDB in Docker Compose. The backend imports only synthetic records from `sample-data/*.json` when MongoDB collections are empty.

## 1. Start MongoDB

From the repository root:

```powershell
docker compose up -d mongo
```

Verify the Compose file:

```powershell
docker compose config
```

## 2. Start The Backend

In a new PowerShell session:

```powershell
cd backend
.\gradlew.bat bootRun
```

The backend runs on:

```text
http://localhost:8090
```

MongoDB uses the local URI:

```text
mongodb://localhost:27017/cnc_mct_demo
```

## 3. Test The API

Run individual requests:

```powershell
Invoke-RestMethod "http://localhost:8090/api/machines"
Invoke-RestMethod "http://localhost:8090/api/dashboard/summary"
Invoke-RestMethod "http://localhost:8090/api/dashboard/utilization"
Invoke-RestMethod "http://localhost:8090/api/dashboard/cutting-ratio"
Invoke-RestMethod "http://localhost:8090/api/dashboard/status-distribution"
Invoke-RestMethod "http://localhost:8090/api/dashboard/daily-trend"
Invoke-RestMethod "http://localhost:8090/api/alarms?severity=CRITICAL"
```

Or run the smoke-test script from the repository root:

```powershell
.\scripts\test_backend_api.ps1
```

Use another base URL if needed:

```powershell
.\scripts\test_backend_api.ps1 -BaseUrl "http://localhost:8090/api"
```

Each endpoint should return the common wrapper:

```json
{
  "success": true,
  "data": {}
}
```

## 4. Run The Rollup Engine

The rollup engine aggregates the raw `machine_signal_pool` signals into the
`runtime_daily` / `cuttime_daily` summary collections. Because the sample signal
data lives in the `2026-01` range, use the one-shot backfill to populate buckets:

```powershell
cd backend
.\gradlew.bat bootRun --args="--rollup.backfill.enabled=true --rollup.backfill.from=2026-01-01 --rollup.backfill.to=2026-01-04"
```

Then read the aggregated hourly buckets:

```powershell
Invoke-RestMethod "http://localhost:8090/api/rollup/hourly?date=2026-01-01"
```

To run it as a recurring schedule instead (most recent window each tick):

```powershell
.\gradlew.bat bootRun --args="--rollup.enabled=true --rollup.lookback-days=2"
```

See [ROLLUP_ARCHITECTURE.md](ROLLUP_ARCHITECTURE.md) for details.

## 5. Reset MongoDB Data

To remove the local volume and reseed from `sample-data/*.json` on the next backend startup:

```powershell
docker compose down -v
docker compose up -d mongo
```

Then restart the backend:

```powershell
cd backend
.\gradlew.bat bootRun
```

## 6. Troubleshooting

- If port `27017` is already in use, stop the other local MongoDB instance or change the Compose port mapping for local testing.
- If port `8090` is already in use, stop the other process or set `SERVER_PORT` before running the backend.
- If API calls fail before the backend starts, wait for Spring Boot to finish startup and try again.
- If the API returns empty data after a reset, confirm the backend was started from either the repository root or `backend/` so the loader can find `sample-data`.
- If Docker is not running, start Docker Desktop and rerun `docker compose up -d mongo`.
