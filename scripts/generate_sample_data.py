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

    write_json("machines.json", machines)
    write_json("status-history.json", status_history)
    write_json("runtime-cuttime.json", runtime_cuttime)
    write_json("alarm-history.json", alarm_history)
    write_json("daily-summary.json", daily_summary)


if __name__ == "__main__":
    main()
