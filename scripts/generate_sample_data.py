from __future__ import annotations

import json
import random
from collections import defaultdict
from datetime import date, datetime, time, timedelta, timezone
from pathlib import Path


START_DATE = date(2026, 1, 1)
DAYS = 30
PLANNED_DAILY_SECONDS = 8 * 60 * 60
OUTPUT_DIR = Path(__file__).resolve().parents[1] / "sample-data"

# Raw cumulative signal pool (rollup engine input). Generated for a shorter
# window than the summary collections to keep the sample file small, while still
# exercising the counter-to-delta correction rules.
SIGNAL_DAYS = 3
SIGNAL_STEP_SECONDS = 120  # emit a cumulative reading every 2 minutes while running
SIGNAL_CUT_RATIO = 0.7     # cutting-time counter advances at ~70% of runtime

MACHINE_IDS = [
    ("CNC-DEMO-01", "Demo CNC 01", "CNC", "Demo Line A"),
    ("CNC-DEMO-02", "Demo CNC 02", "CNC", "Demo Line A"),
    ("CNC-DEMO-03", "Demo CNC 03", "CNC", "Demo Line B"),
    ("MCT-DEMO-01", "Demo MCT 01", "MCT", "Demo Line C"),
    ("MCT-DEMO-02", "Demo MCT 02", "MCT", "Demo Line C"),
    ("MCT-DEMO-03", "Demo MCT 03", "MCT", "Demo Line D"),
]

ALARM_DEFINITIONS = [
    ("DEMO-W001", "WARNING", "Demo coolant warning"),
    ("DEMO-W002", "WARNING", "Demo tool wear warning"),
    ("DEMO-C001", "CRITICAL", "Demo spindle stop"),
    ("DEMO-I001", "INFO", "Demo operator note"),
]


def iso_z(value: datetime) -> str:
    return value.astimezone(timezone.utc).replace(microsecond=0).isoformat().replace("+00:00", "Z")


def work_dates() -> list[date]:
    return [START_DATE + timedelta(days=offset) for offset in range(DAYS)]


def build_machines() -> list[dict]:
    return [
        {
            "machineId": machine_id,
            "machineName": machine_name,
            "machineType": machine_type,
            "line": line,
            "enabled": True,
            "plannedDailySeconds": PLANNED_DAILY_SECONDS,
        }
        for machine_id, machine_name, machine_type, line in MACHINE_IDS
    ]


def status_plan(day_index: int, machine_index: int) -> dict[str, int]:
    if (day_index + machine_index) % 17 == 0:
        return {
            "RUNNING": 7200,
            "IDLE": 7200,
            "ALARM": 0,
            "OFFLINE": 14400,
        }
    if (day_index + machine_index) % 11 == 0:
        return {
            "RUNNING": 10800,
            "IDLE": 15000,
            "ALARM": 1200,
            "OFFLINE": 1800,
        }

    offline = random.choice([0, 0, 0, 900, 1800])
    alarm = random.choice([0, 0, 600, 900, 1200])
    idle = random.randint(1800, 5400)
    running = PLANNED_DAILY_SECONDS - offline - alarm - idle

    if running < 9000:
        idle = max(1200, idle - (9000 - running))
        running = PLANNED_DAILY_SECONDS - offline - alarm - idle

    return {
        "RUNNING": running,
        "IDLE": idle,
        "ALARM": alarm,
        "OFFLINE": offline,
    }


def split_running(running_seconds: int) -> tuple[int, int]:
    if running_seconds <= 7200:
        return running_seconds, 0
    first = random.randint(3600, running_seconds - 3600)
    return first, running_seconds - first


def build_status_history(machines: list[dict]) -> list[dict]:
    events: list[dict] = []

    for machine_index, machine in enumerate(machines):
        machine_id = machine["machineId"]
        for day_index, work_day in enumerate(work_dates()):
            plan = status_plan(day_index, machine_index)
            run_first, run_second = split_running(plan["RUNNING"])

            segments = [
                ("RUNNING", run_first),
                ("IDLE", plan["IDLE"]),
                ("RUNNING", run_second),
                ("ALARM", plan["ALARM"]),
                ("OFFLINE", plan["OFFLINE"]),
            ]

            cursor = datetime.combine(work_day, time(8, 0), timezone.utc)
            sequence = 1
            for status, duration in segments:
                if duration <= 0:
                    continue
                ended_at = cursor + timedelta(seconds=duration)
                events.append(
                    {
                        "eventId": f"STATUS-{work_day:%Y%m%d}-{machine_id}-{sequence:03d}",
                        "machineId": machine_id,
                        "status": status,
                        "startedAt": iso_z(cursor),
                        "endedAt": iso_z(ended_at),
                        "durationSeconds": duration,
                        "workDate": work_day.isoformat(),
                    }
                )
                cursor = ended_at
                sequence += 1

    return events


def build_runtime_cuttime(status_history: list[dict]) -> list[dict]:
    running_by_day_machine: dict[tuple[str, str], int] = defaultdict(int)
    for event in status_history:
        if event["status"] == "RUNNING":
            key = (event["workDate"], event["machineId"])
            running_by_day_machine[key] += event["durationSeconds"]

    rows: list[dict] = []
    for work_day in work_dates():
        work_date = work_day.isoformat()
        for machine_id, *_ in MACHINE_IDS:
            runtime_seconds = running_by_day_machine[(work_date, machine_id)]
            cuttime_seconds = int(runtime_seconds * random.uniform(0.55, 0.88))
            cutting_ratio = round((cuttime_seconds / runtime_seconds) * 100, 2) if runtime_seconds else 0.0
            rows.append(
                {
                    "machineId": machine_id,
                    "workDate": work_date,
                    "runtimeSeconds": runtime_seconds,
                    "cuttimeSeconds": cuttime_seconds,
                    "cuttingRatio": cutting_ratio,
                }
            )
    return rows


def build_alarm_history() -> list[dict]:
    alarms: list[dict] = []
    sequence = 1

    for day_index, work_day in enumerate(work_dates()):
        for machine_index, (machine_id, *_rest) in enumerate(MACHINE_IDS):
            base_roll = random.random()
            alarm_total = 0
            if base_roll < 0.18:
                alarm_total = 2
            elif base_roll < 0.52:
                alarm_total = 1

            for _ in range(alarm_total):
                code, severity, message = random.choice(ALARM_DEFINITIONS)
                minute_offset = random.randint(15, (8 * 60) - 45)
                clear_minutes = random.randint(5, 45)
                occurred_at = datetime.combine(work_day, time(8, 0), timezone.utc) + timedelta(minutes=minute_offset)
                cleared_at = occurred_at + timedelta(minutes=clear_minutes)
                alarms.append(
                    {
                        "alarmId": f"ALARM-{work_day:%Y%m%d}-{machine_id}-{sequence:04d}",
                        "machineId": machine_id,
                        "severity": severity,
                        "alarmCode": code,
                        "message": message,
                        "occurredAt": iso_z(occurred_at),
                        "clearedAt": iso_z(cleared_at),
                        "workDate": work_day.isoformat(),
                    }
                )
                sequence += 1

            if (day_index + machine_index) % 19 == 0:
                code, severity, message = ("DEMO-C001", "CRITICAL", "Demo spindle stop")
                occurred_at = datetime.combine(work_day, time(13, 30), timezone.utc)
                alarms.append(
                    {
                        "alarmId": f"ALARM-{work_day:%Y%m%d}-{machine_id}-{sequence:04d}",
                        "machineId": machine_id,
                        "severity": severity,
                        "alarmCode": code,
                        "message": message,
                        "occurredAt": iso_z(occurred_at),
                        "clearedAt": iso_z(occurred_at + timedelta(minutes=25)),
                        "workDate": work_day.isoformat(),
                    }
                )
                sequence += 1

    return alarms


def parse_z(value: str) -> datetime:
    return datetime.fromisoformat(value.replace("Z", "+00:00"))


def build_machine_signal_pool(status_history: list[dict]) -> list[dict]:
    """Build the raw cumulative signal pool the rollup engine aggregates.

    For each machine, RunTime/CutTime counters increase monotonically only while
    the machine is RUNNING. Readings are emitted every ``SIGNAL_STEP_SECONDS``
    during running segments; idle/offline gaps produce no readings, so the rollup
    naturally drops those spans (gap-drop rule). A few anomalies are injected to
    exercise the correction rules:

    - a counter reset (value goes down -> delta dropped),
    - a large jump (delta capped to the per-step maximum).
    """
    signal_dates = {day.isoformat() for day in work_dates()[:SIGNAL_DAYS]}

    running_by_machine: dict[str, list[dict]] = defaultdict(list)
    for event in status_history:
        if event["status"] == "RUNNING" and event["workDate"] in signal_dates:
            running_by_machine[event["machineId"]].append(event)

    rows: list[dict] = []
    for machine_id, *_ in MACHINE_IDS:
        run_cum = 0.0
        cut_cum = 0.0
        step_index = 0
        segments = sorted(running_by_machine[machine_id], key=lambda e: e["startedAt"])
        for segment in segments:
            cursor = parse_z(segment["startedAt"])
            segment_end = parse_z(segment["endedAt"])
            while cursor < segment_end:
                run_cum += SIGNAL_STEP_SECONDS
                cut_cum += SIGNAL_STEP_SECONDS * SIGNAL_CUT_RATIO
                step_index += 1

                # Inject anomalies on the first machine to showcase delta rules.
                if machine_id == MACHINE_IDS[0][0]:
                    if step_index == 12:
                        run_cum -= 3000  # device counter reset -> negative delta -> dropped
                    elif step_index == 24:
                        run_cum += 5000  # spurious jump -> delta capped to per-step max

                rows.append(signal_reading(machine_id, "RunTime", round(run_cum, 1), cursor))
                rows.append(signal_reading(machine_id, "CutTime", round(cut_cum, 1), cursor))
                cursor += timedelta(seconds=SIGNAL_STEP_SECONDS)

    return rows


def signal_reading(machine_id: str, signal_name: str, value: float, at: datetime) -> dict:
    return {
        "machineId": machine_id,
        "signalName": signal_name,
        "value": value,
        "endDate": iso_z(at),
        "timespan": SIGNAL_STEP_SECONDS,
    }


KST = timezone(timedelta(hours=9))
ROLLUP_MAX_STEP_SEC = 120.0
ROLLUP_MAX_GAP_SEC = 600.0
ROLLUP_SEED_STAMP = "2026-01-31T00:00:00Z"


def rollup_resolve_delta(prev_value: float, prev_ts: datetime, cur_value: float, cur_ts: datetime) -> float:
    """Mirror of the backend DeltaRule so seeded buckets match engine output."""
    raw_delta = cur_value - prev_value
    if raw_delta <= 0:
        return 0.0
    gap_sec = (cur_ts - prev_ts).total_seconds()
    if gap_sec <= 0 or gap_sec > ROLLUP_MAX_GAP_SEC:
        return 0.0
    return min(raw_delta, min(gap_sec, ROLLUP_MAX_STEP_SEC))


def build_rollup_daily(
    status_history: list[dict],
    runtime_cuttime: list[dict],
    machine_signal_pool: list[dict],
) -> tuple[list[dict], list[dict]]:
    """Seed the rollup target collections for the full sample range.

    - Signal days (first ``SIGNAL_DAYS``): buckets are computed by running the
      same delta rules the backend rollup engine applies, so a live backfill
      over ``machine_signal_pool`` reproduces these exact values.
    - Remaining days: buckets are derived from RUNNING status segments split by
      KST hour, with cutting time scaled by that day's cutting ratio.
    """
    run_buckets: dict[tuple[str, str, int], float] = defaultdict(float)
    cut_buckets: dict[tuple[str, str, int], float] = defaultdict(float)

    # --- Signal days: replay the engine's delta walk over the signal pool.
    by_signal: dict[tuple[str, str], list[tuple[datetime, float]]] = defaultdict(list)
    for row in machine_signal_pool:
        ts = parse_z(row["endDate"])
        by_signal[(row["machineId"], row["signalName"])].append((ts, row["value"]))

    for (machine_id, signal_name), points in by_signal.items():
        points.sort(key=lambda item: item[0])
        prev: tuple[datetime, float] | None = None
        for ts, value in points:
            if prev is not None:
                delta = rollup_resolve_delta(prev[1], prev[0], value, ts)
                if delta > 0:
                    kst = ts.astimezone(KST)
                    key = (machine_id, kst.date().isoformat(), kst.hour)
                    if signal_name.lower().startswith("runtime"):
                        run_buckets[key] += delta
                    else:
                        cut_buckets[key] += delta
            prev = (ts, value)

    # --- Remaining days: split RUNNING segments into KST hour buckets.
    signal_dates = {day.isoformat() for day in work_dates()[:SIGNAL_DAYS]}
    ratio_by_day_machine = {
        (row["workDate"], row["machineId"]): (
            row["cuttimeSeconds"] / row["runtimeSeconds"] if row["runtimeSeconds"] else 0.0
        )
        for row in runtime_cuttime
    }

    for event in status_history:
        if event["status"] != "RUNNING" or event["workDate"] in signal_dates:
            continue
        ratio = ratio_by_day_machine.get((event["workDate"], event["machineId"]), 0.0)
        cursor = parse_z(event["startedAt"]).astimezone(KST)
        segment_end = parse_z(event["endedAt"]).astimezone(KST)
        while cursor < segment_end:
            hour_end = (cursor + timedelta(hours=1)).replace(minute=0, second=0, microsecond=0)
            slice_end = min(hour_end, segment_end)
            seconds = (slice_end - cursor).total_seconds()
            key = (event["machineId"], cursor.date().isoformat(), cursor.hour)
            run_buckets[key] += seconds
            cut_buckets[key] += seconds * ratio
            cursor = slice_end

    def to_rows(buckets: dict[tuple[str, str, int], float], value_field: str) -> list[dict]:
        rows: list[dict] = []
        for (machine_id, kst_date, hour), seconds in sorted(buckets.items()):
            day = date.fromisoformat(kst_date)
            base_date_utc = datetime.combine(day, time(0, 0), KST).astimezone(timezone.utc)
            rows.append(
                {
                    "machineId": machine_id,
                    "baseDate": iso_z(base_date_utc),
                    "year": day.year,
                    "month": day.month,
                    "day": day.day,
                    "hour": hour,
                    value_field: round(seconds, 1),
                    "createdAt": ROLLUP_SEED_STAMP,
                    "updatedAt": ROLLUP_SEED_STAMP,
                }
            )
        return rows

    return to_rows(run_buckets, "runTime"), to_rows(cut_buckets, "cutTime")


def dominant_status(events: list[dict]) -> str:
    totals: dict[str, int] = defaultdict(int)
    for event in events:
        totals[event["status"]] += event["durationSeconds"]
    return max(totals.items(), key=lambda item: item[1])[0]


def build_daily_summary(
    machines: list[dict],
    status_history: list[dict],
    runtime_cuttime: list[dict],
    alarm_history: list[dict],
) -> list[dict]:
    machine_by_id = {machine["machineId"]: machine for machine in machines}
    status_by_day_machine: dict[tuple[str, str], list[dict]] = defaultdict(list)
    for event in status_history:
        status_by_day_machine[(event["workDate"], event["machineId"])].append(event)

    cutting_by_date: dict[str, list[float]] = defaultdict(list)
    for row in runtime_cuttime:
        cutting_by_date[row["workDate"]].append(row["cuttingRatio"])

    alarms_by_date: dict[str, list[dict]] = defaultdict(list)
    for alarm in alarm_history:
        alarms_by_date[alarm["workDate"]].append(alarm)

    summaries: list[dict] = []
    for work_day in work_dates():
        work_date = work_day.isoformat()
        utilization_values: list[float] = []
        running_count = 0
        idle_count = 0
        offline_count = 0

        for machine in machines:
            machine_id = machine["machineId"]
            events = status_by_day_machine[(work_date, machine_id)]
            productive_seconds = sum(
                event["durationSeconds"] for event in events if event["status"] in {"RUNNING", "ALARM"}
            )
            planned_seconds = machine_by_id[machine_id]["plannedDailySeconds"]
            utilization_values.append((productive_seconds / planned_seconds) * 100)

            dominant = dominant_status(events)
            if dominant == "RUNNING":
                running_count += 1
            elif dominant == "IDLE":
                idle_count += 1
            elif dominant == "OFFLINE":
                offline_count += 1

        day_alarms = alarms_by_date[work_date]
        cutting_values = cutting_by_date[work_date]
        summaries.append(
            {
                "workDate": work_date,
                "machineCount": len(machines),
                "averageUtilization": round(sum(utilization_values) / len(utilization_values), 2),
                "averageCuttingRatio": round(sum(cutting_values) / len(cutting_values), 2),
                "alarmCount": len(day_alarms),
                "criticalAlarmCount": sum(1 for alarm in day_alarms if alarm["severity"] == "CRITICAL"),
                "runningMachineCount": running_count,
                "idleMachineCount": idle_count,
                "offlineMachineCount": offline_count,
            }
        )

    return summaries


def write_json(filename: str, rows: list[dict]) -> None:
    path = OUTPUT_DIR / filename
    path.write_text(json.dumps(rows, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    print(f"{filename}: {len(rows)} records")


def main() -> None:
    random.seed(42)
    OUTPUT_DIR.mkdir(parents=True, exist_ok=True)

    machines = build_machines()
    status_history = build_status_history(machines)
    runtime_cuttime = build_runtime_cuttime(status_history)
    alarm_history = build_alarm_history()
    daily_summary = build_daily_summary(machines, status_history, runtime_cuttime, alarm_history)
    machine_signal_pool = build_machine_signal_pool(status_history)
    runtime_daily, cuttime_daily = build_rollup_daily(status_history, runtime_cuttime, machine_signal_pool)

    write_json("machines.json", machines)
    write_json("status-history.json", status_history)
    write_json("runtime-cuttime.json", runtime_cuttime)
    write_json("alarm-history.json", alarm_history)
    write_json("daily-summary.json", daily_summary)
    write_json("machine-signal-pool.json", machine_signal_pool)
    write_json("runtime-daily.json", runtime_daily)
    write_json("cuttime-daily.json", cuttime_daily)


if __name__ == "__main__":
    main()
