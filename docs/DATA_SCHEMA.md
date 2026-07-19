# Data Schema

This repository uses only synthetic data. The data files described here are not copied from any production system and must not contain real customer names, site names, equipment names, LOT numbers, alarm records, user information, credentials, server addresses, logs, or screenshots.

The backend API, rollup engine, and frontend charts are implemented against this schema.

## File Overview

| File | Purpose | Cardinality |
| --- | --- | --- |
| `sample-data/machines.json` | Demo CNC/MCT machine master data | 6 records |
| `sample-data/status-history.json` | Synthetic machine status intervals | 30 days x 6 machines x multiple intervals |
| `sample-data/runtime-cuttime.json` | Daily runtime and cuttime metrics | 30 days x 6 machines |
| `sample-data/alarm-history.json` | Synthetic alarm events | Variable |
| `sample-data/daily-summary.json` | Daily aggregate metrics derived from the other files | 30 records |
| `sample-data/machine-signal-pool.json` | Raw cumulative RunTime/CutTime signal readings (rollup engine input) | First 3 days x 6 machines, 2-min steps |
| `sample-data/runtime-daily.json` | Pre-aggregated hourly runtime buckets (rollup target seed) | Full range, KST hours |
| `sample-data/cuttime-daily.json` | Pre-aggregated hourly cutting buckets (rollup target seed) | Full range, KST hours |

## `machines.json`

Machine master data for the demo.

| Field | Type | Description |
| --- | --- | --- |
| `machineId` | string | Stable fake machine ID, such as `CNC-DEMO-01` |
| `machineName` | string | Display name for the fake machine |
| `machineType` | string | `CNC` or `MCT` |
| `line` | string | Fake demo production line |
| `enabled` | boolean | Whether the machine is active in the demo |
| `plannedDailySeconds` | integer | Planned operating seconds per day |

Example:

```json
{
  "machineId": "CNC-DEMO-01",
  "machineName": "Demo CNC 01",
  "machineType": "CNC",
  "line": "Demo Line A",
  "enabled": true,
  "plannedDailySeconds": 28800
}
```

## `status-history.json`

Interval-based synthetic machine state history.

Allowed statuses:

- `RUNNING`
- `IDLE`
- `ALARM`
- `OFFLINE`

| Field | Type | Description |
| --- | --- | --- |
| `eventId` | string | Stable synthetic event ID |
| `machineId` | string | Must exist in `machines.json` |
| `status` | string | One of the allowed status values |
| `startedAt` | string | ISO-8601 UTC timestamp |
| `endedAt` | string | ISO-8601 UTC timestamp |
| `durationSeconds` | integer | Exact difference between `startedAt` and `endedAt` |
| `workDate` | string | Work date in `YYYY-MM-DD` format |

Example:

```json
{
  "eventId": "STATUS-20260101-CNC-DEMO-01-001",
  "machineId": "CNC-DEMO-01",
  "status": "RUNNING",
  "startedAt": "2026-01-01T08:00:00Z",
  "endedAt": "2026-01-01T10:30:00Z",
  "durationSeconds": 9000,
  "workDate": "2026-01-01"
}
```

Calculation rule:

- `durationSeconds` must equal `endedAt - startedAt`.
- For this demo, each machine has generated status intervals inside the planned daily window.

## `runtime-cuttime.json`

Daily runtime and cuttime metrics for cutting ratio charts.

| Field | Type | Description |
| --- | --- | --- |
| `machineId` | string | Must exist in `machines.json` |
| `workDate` | string | Work date in `YYYY-MM-DD` format |
| `runtimeSeconds` | integer | Synthetic daily runtime seconds |
| `cuttimeSeconds` | integer | Synthetic daily cuttime seconds |
| `cuttingRatio` | number | `cuttimeSeconds / runtimeSeconds * 100` |

Example:

```json
{
  "machineId": "MCT-DEMO-01",
  "workDate": "2026-01-01",
  "runtimeSeconds": 21600,
  "cuttimeSeconds": 15120,
  "cuttingRatio": 70.0
}
```

Calculation rules:

- `cuttimeSeconds <= runtimeSeconds`
- `cuttingRatio = round(cuttimeSeconds / runtimeSeconds * 100, 2)`

## `alarm-history.json`

Synthetic demo alarm events.

Allowed alarm codes:

- `DEMO-W001`
- `DEMO-W002`
- `DEMO-C001`
- `DEMO-I001`

Allowed severities:

- `INFO`
- `WARNING`
- `CRITICAL`

| Field | Type | Description |
| --- | --- | --- |
| `alarmId` | string | Stable synthetic alarm ID |
| `machineId` | string | Must exist in `machines.json` |
| `severity` | string | `INFO`, `WARNING`, or `CRITICAL` |
| `alarmCode` | string | One of the allowed demo alarm codes |
| `message` | string | Fake demo alarm message |
| `occurredAt` | string | ISO-8601 UTC timestamp |
| `clearedAt` | string | ISO-8601 UTC timestamp |
| `workDate` | string | Work date in `YYYY-MM-DD` format |

Example:

```json
{
  "alarmId": "ALARM-20260101-CNC-DEMO-01-001",
  "machineId": "CNC-DEMO-01",
  "severity": "WARNING",
  "alarmCode": "DEMO-W001",
  "message": "Demo coolant warning",
  "occurredAt": "2026-01-01T10:15:00Z",
  "clearedAt": "2026-01-01T10:35:00Z",
  "workDate": "2026-01-01"
}
```

## `daily-summary.json`

Daily aggregate metrics calculated from the generated status, runtime/cuttime, and alarm files.

| Field | Type | Description |
| --- | --- | --- |
| `workDate` | string | Work date in `YYYY-MM-DD` format |
| `machineCount` | integer | Number of enabled demo machines |
| `averageUtilization` | number | Average per-machine utilization percentage |
| `averageCuttingRatio` | number | Average per-machine cutting ratio |
| `alarmCount` | integer | Alarm count for the date |
| `criticalAlarmCount` | integer | Critical alarm count for the date |
| `runningMachineCount` | integer | Machines whose dominant status was `RUNNING` |
| `idleMachineCount` | integer | Machines whose dominant status was `IDLE` |
| `offlineMachineCount` | integer | Machines whose dominant status was `OFFLINE` |

Example:

```json
{
  "workDate": "2026-01-01",
  "machineCount": 6,
  "averageUtilization": 82.5,
  "averageCuttingRatio": 68.4,
  "alarmCount": 4,
  "criticalAlarmCount": 1,
  "runningMachineCount": 5,
  "idleMachineCount": 1,
  "offlineMachineCount": 0
}
```

Calculation rules:

- Per-machine utilization is `(RUNNING seconds + ALARM seconds) / plannedDailySeconds * 100`.
- `averageUtilization` is the average of per-machine utilization values for the date.
- `averageCuttingRatio` is the average of daily machine `cuttingRatio` values.
- `alarmCount` and `criticalAlarmCount` are calculated from `alarm-history.json`.
- Dominant status is the status with the largest total duration for a machine on a given date.

## `machine-signal-pool.json`

Raw cumulative counter readings that feed the rollup engine. Each document is a
monotonically increasing `RunTime` or `CutTime` counter reading for one machine
at one point in time. Readings are emitted every 2 minutes while a machine is
running; idle/offline spans produce gaps. A few anomalies (a counter reset and a
spurious jump on the first machine) are included so the delta-correction rules
are exercised.

| Field | Type | Description |
| --- | --- | --- |
| `machineId` | string | Must exist in `machines.json` |
| `signalName` | string | `RunTime` or `CutTime` |
| `value` | number | Cumulative counter value (seconds) |
| `endDate` | string | ISO-8601 UTC timestamp of the reading |
| `timespan` | integer | Nominal step between readings (seconds) |

Example:

```json
{
  "machineId": "CNC-DEMO-01",
  "signalName": "RunTime",
  "value": 3360.0,
  "endDate": "2026-01-01T08:56:00Z",
  "timespan": 120
}
```

## Rollup target collections

The rollup engine writes pre-aggregated hourly buckets into `runtime_daily` and
`cuttime_daily`. They are seeded from `runtime-daily.json` / `cuttime-daily.json`
for the full sample range so period charts have data immediately; the seed for
the signal-pool days is generated by replaying the same delta rules the engine
applies, so a live backfill overwrites those days idempotently with identical
values. One document per `(machineId, baseDate, hour)`:

| Field | Type | Description |
| --- | --- | --- |
| `machineId` | string | Machine identifier |
| `baseDate` | date | KST midnight of the bucket day (stored as a UTC instant) |
| `year` / `month` / `day` / `hour` | integer | KST-based bucket keys |
| `runTime` / `cutTime` | number | Corrected delta seconds accumulated in the hour |
| `createdAt` / `updatedAt` | date | Bookkeeping timestamps |

See [ROLLUP_ARCHITECTURE.md](ROLLUP_ARCHITECTURE.md) for the aggregation and
delta-correction rules.
