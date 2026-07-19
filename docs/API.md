# Backend API

This is the public portfolio backend for the CNC/MCT dashboard, serving synthetic data. All records are loaded from `sample-data/*.json`; no production database, equipment, customer, credential, or private repository data is used.

Base URL:

```text
http://localhost:8090/api
```

Successful responses use a common wrapper:

```json
{
  "success": true,
  "data": {}
}
```

Validation errors return HTTP 400:

```json
{
  "success": false,
  "error": "from must be before or equal to to"
}
```

Default date range is `from=2026-01-01` and `to=2026-01-30` when omitted.

## Endpoints

### GET `/api/machines`

Returns enabled machines sorted by `machineType`, then `machineId`.

Example response:

```json
{
  "success": true,
  "data": [
    {
      "machineId": "CNC-DEMO-01",
      "machineName": "Demo CNC 01",
      "machineType": "CNC",
      "line": "Demo Line A",
      "enabled": true,
      "plannedDailySeconds": 28800
    }
  ]
}
```

### GET `/api/dashboard/summary`

Query parameters:

| Name | Required | Description |
| --- | --- | --- |
| `from` | No | Start work date, `yyyy-MM-dd`. |
| `to` | No | End work date, `yyyy-MM-dd`. |

Returns a period summary from `daily_summary`. Utilization and cutting ratio are averaged over matching daily summaries. Alarm counts are summed. Machine status counts use the latest day in the period.

Example:

```json
{
  "success": true,
  "data": {
    "machineCount": 6,
    "averageUtilization": 83.22,
    "averageCuttingRatio": 72.54,
    "alarmCount": 27,
    "criticalAlarmCount": 7,
    "runningMachineCount": 6,
    "idleMachineCount": 0,
    "offlineMachineCount": 0
  }
}
```

### GET `/api/dashboard/utilization`

Query parameters: `from`, `to`.

Calculation:

- `operatingSeconds = RUNNING + ALARM durationSeconds`
- `plannedSeconds = plannedDailySeconds * dateCount`
- `utilization = operatingSeconds / plannedSeconds * 100`

Example:

```json
{
  "success": true,
  "data": [
    {
      "machineId": "CNC-DEMO-01",
      "machineName": "Demo CNC 01",
      "machineType": "CNC",
      "operatingSeconds": 65321,
      "plannedSeconds": 86400,
      "utilization": 75.6
    }
  ]
}
```

### GET `/api/dashboard/cutting-ratio`

Query parameters: `from`, `to`.

Calculation:

- `runtimeSeconds = sum(runtimeSeconds)`
- `cuttimeSeconds = sum(cuttimeSeconds)`
- `cuttingRatio = cuttimeSeconds / runtimeSeconds * 100`

Example:

```json
{
  "success": true,
  "data": [
    {
      "machineId": "CNC-DEMO-01",
      "runtimeSeconds": 55123,
      "cuttimeSeconds": 40111,
      "cuttingRatio": 72.77
    }
  ]
}
```

### GET `/api/dashboard/status-distribution`

Query parameters: `from`, `to`.

Returns total duration and percentage by status:

- `RUNNING`
- `IDLE`
- `ALARM`
- `OFFLINE`

Example:

```json
{
  "success": true,
  "data": [
    {
      "status": "RUNNING",
      "durationSeconds": 400000,
      "ratio": 72.34
    }
  ]
}
```

### GET `/api/dashboard/daily-trend`

Query parameters: `from`, `to`.

Example:

```json
{
  "success": true,
  "data": [
    {
      "workDate": "2026-01-01",
      "averageUtilization": 73.52,
      "averageCuttingRatio": 78.02,
      "alarmCount": 5,
      "criticalAlarmCount": 2
    }
  ]
}
```

### GET `/api/rollup/hourly`

Reads the hourly runtime/cutting buckets produced by the rollup engine straight
from the pre-aggregated `runtime_daily` / `cuttime_daily` summary collections, so
this endpoint never scans the raw signal pool.

Query parameters:

| Name | Required | Description |
| --- | --- | --- |
| `date` | Yes | KST work date, `yyyy-MM-dd`. |

Results are ordered by `machineId`, then `hour`. Requires the rollup to have run
for the requested date (schedule or backfill — see
[ROLLUP_ARCHITECTURE.md](ROLLUP_ARCHITECTURE.md)).

Example:

```json
{
  "success": true,
  "data": [
    {
      "machineId": "CNC-DEMO-01",
      "hour": 17,
      "runTimeSeconds": 3360.0,
      "cutTimeSeconds": 2352.0
    }
  ]
}
```

### GET `/api/alarms`

Query parameters:

| Name | Required | Description |
| --- | --- | --- |
| `from` | No | Start work date, `yyyy-MM-dd`. |
| `to` | No | End work date, `yyyy-MM-dd`. |
| `severity` | No | One of `INFO`, `WARNING`, `CRITICAL`. Empty means all severities. |
| `machineId` | No | Machine ID filter. Empty means all machines. |

Results are sorted by `occurredAt` descending.

Example:

```json
{
  "success": true,
  "data": [
    {
      "alarmId": "ALARM-20260101-CNC-DEMO-01-0001",
      "machineId": "CNC-DEMO-01",
      "severity": "WARNING",
      "alarmCode": "DEMO-W001",
      "message": "Demo coolant warning",
      "occurredAt": "2026-01-01T13:10:00Z",
      "clearedAt": "2026-01-01T13:55:00Z",
      "workDate": "2026-01-01"
    }
  ]
}
```
