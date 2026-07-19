import { getJson } from "./apiClient";
import type {
  AlarmHistory,
  CuttingRatio,
  DailyTrend,
  DashboardFilters,
  DashboardSummary,
  HourlyRollup,
  Machine,
  StatusDistribution,
  Utilization
} from "../types/dashboard";

type DateRange = Pick<DashboardFilters, "from" | "to">;

export function fetchMachines(): Promise<Machine[]> {
  return getJson<Machine[]>("/machines");
}

export function fetchSummary(filters: DateRange): Promise<DashboardSummary> {
  return getJson<DashboardSummary>("/dashboard/summary", filters);
}

export function fetchUtilization(filters: DateRange): Promise<Utilization[]> {
  return getJson<Utilization[]>("/dashboard/utilization", filters);
}

export function fetchCuttingRatio(filters: DateRange): Promise<CuttingRatio[]> {
  return getJson<CuttingRatio[]>("/dashboard/cutting-ratio", filters);
}

export function fetchStatusDistribution(filters: DateRange): Promise<StatusDistribution[]> {
  return getJson<StatusDistribution[]>("/dashboard/status-distribution", filters);
}

export function fetchDailyTrend(filters: DateRange): Promise<DailyTrend[]> {
  return getJson<DailyTrend[]>("/dashboard/daily-trend", filters);
}

export function fetchAlarms(filters: DashboardFilters): Promise<AlarmHistory[]> {
  return getJson<AlarmHistory[]>("/alarms", filters);
}

export function fetchHourlyRollup(date: string): Promise<HourlyRollup[]> {
  return getJson<HourlyRollup[]>("/rollup/hourly", { date });
}
