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

### GET `/api/rollup/daily`

Per-day runtime/cutting totals for one month, rolled forward from the hourly
buckets. Query parameters: `year` (required), `month` (required, 1-12).
Ordered by `machineId`, then `day`.

```json
{
  "success": true,
  "data": [
    { "machineId": "CNC-DEMO-01", "day": 1, "runTimeSeconds": 6960.0, "cutTimeSeconds": 4956.0 }
  ]
}
```

### GET `/api/rollup/monthly`

Per-month runtime/cutting totals for one year, rolled forward from the hourly
buckets. Query parameter: `year` (required). Ordered by `machineId`, then
`month`.

```json
{
  "success": true,
  "data": [
    { "machineId": "CNC-DEMO-01", "month": 1, "runTimeSeconds": 561240.0, "cutTimeSeconds": 392868.0 }
  ]
}
```

### GET `/api/machines/history`

Paged status-event timeline for one machine, newest first.

| Name | Required | Description |
| --- | --- | --- |
| `machineId` | Yes | Machine ID. |
| `from` / `to` | No | Work date range, `yyyy-MM-dd`. |
| `page` | No | Zero-based page index (default 0). |
| `size` | No | Page size (default 20, max 100). |

```json
{
  "success": true,
  "data": {
    "items": [
      {
        "eventId": "STATUS-20260128-CNC-DEMO-01-003",
        "machineId": "CNC-DEMO-01",
        "status": "RUNNING",
        "startedAt": "2026-01-28T10:43:00Z",
        "endedAt": "2026-01-28T16:00:00Z",
        "durationSeconds": 19020,
        "workDate": "2026-01-28"
      }
    ],
    "page": 0,
    "size": 20,
    "totalElements": 117,
    "totalPages": 6
  }
}
```

### GET `/api/prealarm/summary`

Aggregated alarm landscape for the pre-alarm view: totals by severity, the most
frequent alarm codes, and per-machine counts. Query parameters: `from`, `to`.

```json
{
  "success": true,
  "data": {
    "total": 125,
    "infoCount": 29,
    "warningCount": 51,
    "criticalCount": 45,
    "topCodes": [{ "alarmCode": "DEMO-C001", "severity": "CRITICAL", "count": 45 }],
    "byMachine": [{ "machineId": "CNC-DEMO-03", "count": 28, "criticalCount": 11 }]
  }
}
```

### GET `/api/prealarm/indicators`

Threshold-based early-warning indicators computed over the window: utilization
decay, cutting-ratio decay, and alarm frequency. Thresholds are configured via
`prealarm.*` properties. Severity is `WARNING` or `WATCH`, warnings first.
Query parameters: `from`, `to`.

```json
{
  "success": true,
  "data": [
    {
      "machineId": "CNC-DEMO-03",
      "rule": "CRITICAL_ALARM_FREQUENCY",
      "metricValue": 11,
      "threshold": 6,
      "severity": "WARNING",
      "message": "11 critical alarms in the window reached the warning threshold 6"
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
