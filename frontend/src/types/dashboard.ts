export type ApiResponse<T> = {
  success: boolean;
  data?: T;
  error?: string;
};

export type Machine = {
  machineId: string;
  machineName: string;
  machineType: string;
  line: string;
  enabled: boolean;
  plannedDailySeconds: number;
};

export type DashboardSummary = {
  machineCount: number;
  averageUtilization: number;
  averageCuttingRatio: number;
  alarmCount: number;
  criticalAlarmCount: number;
  runningMachineCount: number;
  idleMachineCount: number;
  offlineMachineCount: number;
};

export type Utilization = {
  machineId: string;
  machineName: string;
  machineType: string;
  operatingSeconds: number;
  plannedSeconds: number;
  utilization: number;
};

export type CuttingRatio = {
  machineId: string;
  runtimeSeconds: number;
  cuttimeSeconds: number;
  cuttingRatio: number;
};

export type StatusDistribution = {
  status: "RUNNING" | "IDLE" | "ALARM" | "OFFLINE" | string;
  durationSeconds: number;
  ratio: number;
};

export type DailyTrend = {
  workDate: string;
  averageUtilization: number;
  averageCuttingRatio: number;
  alarmCount: number;
  criticalAlarmCount: number;
};

export type AlarmHistory = {
  alarmId: string;
  machineId: string;
  severity: "INFO" | "WARNING" | "CRITICAL" | string;
  alarmCode: string;
  message: string;
  occurredAt: string;
  clearedAt: string | null;
  workDate: string;
};

export type HourlyRollup = {
  machineId: string;
  hour: number;
  runTimeSeconds: number;
  cutTimeSeconds: number;
};

export type DailyRollup = {
  machineId: string;
  day: number;
  runTimeSeconds: number;
  cutTimeSeconds: number;
};

export type MonthlyRollup = {
  machineId: string;
  month: number;
  runTimeSeconds: number;
  cutTimeSeconds: number;
};

export type MachineHistoryItem = {
  eventId: string;
  machineId: string;
  status: string;
  startedAt: string;
  endedAt: string;
  durationSeconds: number;
  workDate: string;
};

export type MachineHistoryPage = {
  items: MachineHistoryItem[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
};

export type PreAlarmSummary = {
  total: number;
  infoCount: number;
  warningCount: number;
  criticalCount: number;
  topCodes: { alarmCode: string; severity: string; count: number }[];
  byMachine: { machineId: string; count: number; criticalCount: number }[];
};

export type PreAlarmIndicator = {
  machineId: string;
  rule: string;
  metricValue: number;
  threshold: number;
  severity: "WATCH" | "WARNING" | string;
  message: string;
};

export type DashboardFilters = {
  from: string;
  to: string;
  machineId: string;
  severity: string;
};
