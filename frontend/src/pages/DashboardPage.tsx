import { useCallback, useEffect, useMemo, useState } from "react";
import { CutPeriodChart, type CutGranularity } from "../components/charts/CutPeriodChart";
import { CuttingRatioChart } from "../components/charts/CuttingRatioChart";
import { DailyTrendChart } from "../components/charts/DailyTrendChart";
import { HourlyRollupChart } from "../components/charts/HourlyRollupChart";
import { MachineHistoryTimeline } from "../components/panels/MachineHistoryTimeline";
import { PreAlarmPanel } from "../components/panels/PreAlarmPanel";
import { StatusDistributionChart } from "../components/charts/StatusDistributionChart";
import { UtilizationBarChart } from "../components/charts/UtilizationBarChart";
import { DateRangeFilter } from "../components/filters/DateRangeFilter";
import { MachineFilter } from "../components/filters/MachineFilter";
import { KpiCard } from "../components/kpi/KpiCard";
import { AlarmHistoryTable } from "../components/tables/AlarmHistoryTable";
import {
  fetchAlarms,
  fetchCuttingRatio,
  fetchDailyRollup,
  fetchDailyTrend,
  fetchHourlyRollup,
  fetchMachines,
  fetchMonthlyRollup,
  fetchPreAlarmIndicators,
  fetchPreAlarmSummary,
  fetchStatusDistribution,
  fetchSummary,
  fetchUtilization
} from "../services/dashboardApi";
import type {
  AlarmHistory,
  CuttingRatio,
  DailyRollup,
  DailyTrend,
  DashboardFilters,
  DashboardSummary,
  HourlyRollup,
  Machine,
  MonthlyRollup,
  PreAlarmIndicator,
  PreAlarmSummary,
  StatusDistribution,
  Utilization
} from "../types/dashboard";
import { formatDateTime, formatNumber, formatPercent } from "../utils/format";

const INITIAL_FILTERS: DashboardFilters = {
  from: "2026-01-01",
  to: "2026-01-30",
  machineId: "",
  severity: ""
};

// The synthetic sample range lives in this year (see scripts/generate_sample_data.py).
const SAMPLE_YEAR = 2026;

type FleetRow = {
  machineId: string;
  machineName: string;
  machineType: string;
  line: string;
  utilization: number;
  cuttingRatio: number;
  alarmCount: number;
  criticalAlarmCount: number;
  latestAlarmSeverity: string;
  riskLevel: string;
};

export function DashboardPage() {
  const [filters, setFilters] = useState<DashboardFilters>(INITIAL_FILTERS);
  const [machines, setMachines] = useState<Machine[]>([]);
  const [summary, setSummary] = useState<DashboardSummary | null>(null);
  const [utilization, setUtilization] = useState<Utilization[]>([]);
  const [cuttingRatio, setCuttingRatio] = useState<CuttingRatio[]>([]);
  const [statusDistribution, setStatusDistribution] = useState<StatusDistribution[]>([]);
  const [dailyTrend, setDailyTrend] = useState<DailyTrend[]>([]);
  const [alarms, setAlarms] = useState<AlarmHistory[]>([]);
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [requestMs, setRequestMs] = useState<number | null>(null);
  const [rollupDate, setRollupDate] = useState("2026-01-01");
  const [hourlyRollup, setHourlyRollup] = useState<HourlyRollup[]>([]);
  const [rollupError, setRollupError] = useState<string | null>(null);
  const [cutGranularity, setCutGranularity] = useState<CutGranularity>("day");
  const [cutMonth, setCutMonth] = useState(1);
  const [monthlyRollup, setMonthlyRollup] = useState<MonthlyRollup[]>([]);
  const [dailyRollup, setDailyRollup] = useState<DailyRollup[]>([]);
  const [cutError, setCutError] = useState<string | null>(null);
  const [preAlarmSummary, setPreAlarmSummary] = useState<PreAlarmSummary | null>(null);
  const [preAlarmIndicators, setPreAlarmIndicators] = useState<PreAlarmIndicator[]>([]);

  const updateFilter = (key: keyof DashboardFilters, value: string) => {
    setFilters((current) => ({ ...current, [key]: value }));
  };

  const loadDashboard = useCallback(async () => {
    setIsLoading(true);
    setError(null);
    const startedAt = performance.now();
    try {
      const dateRange = { from: filters.from, to: filters.to };
      const [
        machinesData,
        summaryData,
        utilizationData,
        cuttingRatioData,
        statusData,
        trendData,
        alarmsData,
        preAlarmSummaryData,
        preAlarmIndicatorData
      ] = await Promise.all([
        fetchMachines(),
        fetchSummary(dateRange),
        fetchUtilization(dateRange),
        fetchCuttingRatio(dateRange),
        fetchStatusDistribution(dateRange),
        fetchDailyTrend(dateRange),
        fetchAlarms(filters),
        fetchPreAlarmSummary(dateRange),
        fetchPreAlarmIndicators(dateRange)
      ]);
      setMachines(machinesData);
      setSummary(summaryData);
      setUtilization(utilizationData);
      setCuttingRatio(cuttingRatioData);
      setStatusDistribution(statusData);
      setDailyTrend(trendData);
      setAlarms(alarmsData);
      setPreAlarmSummary(preAlarmSummaryData);
      setPreAlarmIndicators(preAlarmIndicatorData);
      setRequestMs(Math.round(performance.now() - startedAt));
    } catch (caught) {
      setError(caught instanceof Error ? caught.message : "Unable to load dashboard data");
      setRequestMs(null);
    } finally {
      setIsLoading(false);
    }
  }, [filters]);

  useEffect(() => {
    void loadDashboard();
  }, [loadDashboard]);

  // Rollup buckets are fetched separately so a rollup-side failure (for example
  // before the backfill has run) never blocks the rest of the dashboard.
  useEffect(() => {
    let cancelled = false;
    setRollupError(null);
    fetchHourlyRollup(rollupDate)
      .then((rows) => {
        if (!cancelled) {
          setHourlyRollup(rows);
        }
      })
      .catch((caught) => {
        if (!cancelled) {
          setHourlyRollup([]);
          setRollupError(caught instanceof Error ? caught.message : "Unable to load rollup data");
        }
      });
    return () => {
      cancelled = true;
    };
  }, [rollupDate]);

  // Cut period data follows its own granularity/month controls, isolated from
  // the main dashboard load for the same reason as the hourly rollup.
  useEffect(() => {
    let cancelled = false;
    setCutError(null);
    const request =
      cutGranularity === "month"
        ? fetchMonthlyRollup(SAMPLE_YEAR).then((rows) => {
            if (!cancelled) {
              setMonthlyRollup(rows);
            }
          })
        : fetchDailyRollup(SAMPLE_YEAR, cutMonth).then((rows) => {
            if (!cancelled) {
              setDailyRollup(rows);
            }
          });
    request.catch((caught) => {
      if (!cancelled) {
        setMonthlyRollup([]);
        setDailyRollup([]);
        setCutError(caught instanceof Error ? caught.message : "Unable to load cut period data");
      }
    });
    return () => {
      cancelled = true;
    };
  }, [cutGranularity, cutMonth]);

  const filteredUtilization = useMemo(
    () =>
      filters.machineId
        ? utilization.filter((item) => item.machineId === filters.machineId)
        : utilization,
    [filters.machineId, utilization]
  );

  const filteredCuttingRatio = useMemo(
    () =>
      filters.machineId
        ? cuttingRatio.filter((item) => item.machineId === filters.machineId)
        : cuttingRatio,
    [cuttingRatio, filters.machineId]
  );

  const alarmCountsByMachine = useMemo(() => {
    const counts = new Map<string, { total: number; critical: number; latest?: AlarmHistory }>();
    alarms.forEach((alarm) => {
      const current = counts.get(alarm.machineId) ?? { total: 0, critical: 0 };
      current.total += 1;
      if (alarm.severity === "CRITICAL") {
        current.critical += 1;
      }
      if (!current.latest || new Date(alarm.occurredAt) > new Date(current.latest.occurredAt)) {
        current.latest = alarm;
      }
      counts.set(alarm.machineId, current);
    });
    return counts;
  }, [alarms]);

  const fleetRows = useMemo(() => {
    const utilizationByMachine = new Map(utilization.map((item) => [item.machineId, item]));
    const cuttingByMachine = new Map(cuttingRatio.map((item) => [item.machineId, item]));

    return machines.map((machine) => {
      const utilizationValue = utilizationByMachine.get(machine.machineId)?.utilization ?? 0;
      const alarmInfo = alarmCountsByMachine.get(machine.machineId) ?? { total: 0, critical: 0 };
      const riskLevel =
        alarmInfo.critical > 0
          ? "CRITICAL"
          : alarmInfo.total > 0
            ? "WARNING"
            : utilizationValue >= 75
              ? "ACTIVE"
              : "IDLE / LOW_LOAD";

      return {
        ...machine,
        utilization: utilizationValue,
        cuttingRatio: cuttingByMachine.get(machine.machineId)?.cuttingRatio ?? 0,
        alarmCount: alarmInfo.total,
        criticalAlarmCount: alarmInfo.critical,
        latestAlarmSeverity: alarmInfo.latest?.severity ?? "CLEAR",
        riskLevel
      };
    });
  }, [alarmCountsByMachine, cuttingRatio, machines, utilization]);

  const focusMachine = useMemo(() => {
    if (filters.machineId) {
      return fleetRows.find((machine) => machine.machineId === filters.machineId) ?? fleetRows[0];
    }
    return (
      [...fleetRows].sort((left, right) => {
        if (right.criticalAlarmCount !== left.criticalAlarmCount) {
          return right.criticalAlarmCount - left.criticalAlarmCount;
        }
        return right.alarmCount - left.alarmCount;
      })[0] ?? null
    );
  }, [filters.machineId, fleetRows]);

  const criticalAlarms = useMemo(
    () =>
      alarms
        .filter((alarm) => alarm.severity === "CRITICAL")
        .sort((left, right) => new Date(right.occurredAt).getTime() - new Date(left.occurredAt).getTime())
        .slice(0, 4),
    [alarms]
  );

  const fleetActiveRatio =
    summary && summary.machineCount > 0
      ? ((summary.runningMachineCount ?? 0) / summary.machineCount) * 100
      : null;

  const analyticsLog = [
    `[API] MACHINES_LOADED count=${machines.length}`,
    `[API] SUMMARY_WINDOW ${filters.from}..${filters.to}`,
    `[ANALYSIS] CRITICAL_EVENTS count=${summary?.criticalAlarmCount ?? criticalAlarms.length}`,
    `[ANALYSIS] ALARM_EVENTS count=${summary?.alarmCount ?? alarms.length}`,
    `[FILTER] MACHINE=${filters.machineId || "ALL"} SEVERITY=${filters.severity || "ALL"}`,
    `[STATE] READ_ONLY_SYNTHETIC_MODE`
  ];

  return (
    <div className="dashboard-page">
      <header className="top-header">
        <div>
          <p className="system-link">SYSTEM_LINK_ACTIVE: CNC_MCT_CORE</p>
          <h2>Synthetic Precision Command Center</h2>
        </div>
        <div className="header-telemetry" aria-label="runtime status">
          <span className={error ? "status-pill danger" : summary ? "status-pill online" : "status-pill"}>
            API {error ? "ERROR" : summary ? "ONLINE" : "PENDING"}
          </span>
          <span>LATENCY: {requestMs === null ? "--" : `${requestMs}ms`}</span>
          <span>OPERATOR: LOCAL_USER</span>
        </div>
      </header>

      <section className="filters-bar" aria-label="dashboard filters">
        <DateRangeFilter
          from={filters.from}
          to={filters.to}
          onFromChange={(value) => updateFilter("from", value)}
          onToChange={(value) => updateFilter("to", value)}
        />
        <MachineFilter
          machines={machines}
          value={filters.machineId}
          onChange={(value) => updateFilter("machineId", value)}
        />
        <label>
          <span>SEVERITY</span>
          <select
            value={filters.severity}
            onChange={(event) => updateFilter("severity", event.target.value)}
          >
            <option value="">ALL_SEVERITIES</option>
            <option value="INFO">INFO</option>
            <option value="WARNING">WARNING</option>
            <option value="CRITICAL">CRITICAL</option>
          </select>
        </label>
        <button type="button" onClick={() => void loadDashboard()} disabled={isLoading}>
          {isLoading ? "LOADING_ANALYTICS" : "REFRESH_ANALYTICS"}
        </button>
        <div className="filter-status">
          <span>WINDOW: {filters.from} TO {filters.to}</span>
          <span>MODE: READ_ONLY_SYNTHETIC</span>
        </div>
      </section>

      {error ? (
        <div className="error-banner" role="alert">
          API error: {error}
        </div>
      ) : null}

      <section id="overview" className="kpi-grid section-anchor" aria-label="dashboard KPI cards">
        <KpiCard label="FLEET_SIZE" value={formatNumber(summary?.machineCount)} meta="MACHINES" />
        <KpiCard
          label="AVG_UTIL"
          value={formatPercent(summary?.averageUtilization)}
          percent={summary?.averageUtilization}
          tone="primary"
        />
        <KpiCard
          label="CUT_RATIO"
          value={formatPercent(summary?.averageCuttingRatio)}
          percent={summary?.averageCuttingRatio}
          tone="secondary"
        />
        <KpiCard
          label="FLEET_ACTIVE_RATIO"
          value={formatPercent(fleetActiveRatio)}
          percent={fleetActiveRatio}
          tone="tertiary"
        />
        <KpiCard label="ALARM_EVENTS" value={formatNumber(summary?.alarmCount)} tone="secondary" />
        <KpiCard
          label="CRITICAL_VECTORS"
          value={formatNumber(summary?.criticalAlarmCount)}
          tone="critical"
        />
      </section>

      {isLoading ? <div className="loading-strip">Loading dashboard data...</div> : null}

      <section className="command-grid">
        <FocusPanel machine={focusMachine} />
        <FleetPanel rows={fleetRows} />
        <CriticalVectorsPanel alarms={criticalAlarms} />
        <AnalyticsLogPanel entries={analyticsLog} />
      </section>

      <section className="dashboard-grid">
        <div id="utilization" className="section-anchor">
          <UtilizationBarChart data={filteredUtilization} />
        </div>
        <div id="cutting-ratio" className="section-anchor">
          <CuttingRatioChart data={filteredCuttingRatio} />
        </div>
        <div id="status-distribution" className="section-anchor">
          <StatusDistributionChart data={statusDistribution} />
        </div>
        <div id="daily-trend" className="section-anchor grid-wide">
          <DailyTrendChart data={dailyTrend} />
        </div>
        <div id="hourly-rollup" className="section-anchor grid-wide">
          <HourlyRollupChart
            data={hourlyRollup}
            date={rollupDate}
            machineId={filters.machineId}
            error={rollupError}
            onDateChange={setRollupDate}
          />
        </div>
        <div id="cut-period" className="section-anchor grid-wide">
          <CutPeriodChart
            monthly={monthlyRollup}
            daily={dailyRollup}
            granularity={cutGranularity}
            year={SAMPLE_YEAR}
            month={cutMonth}
            machineId={filters.machineId}
            error={cutError}
            onGranularityChange={setCutGranularity}
            onMonthChange={setCutMonth}
          />
        </div>
        <div id="prealarm" className="section-anchor grid-wide">
          <PreAlarmPanel
            summary={preAlarmSummary}
            indicators={preAlarmIndicators}
            machineId={filters.machineId}
          />
        </div>
        <div id="machine-history" className="section-anchor grid-wide">
          <MachineHistoryTimeline
            machines={machines}
            machineId={filters.machineId}
            from={filters.from}
            to={filters.to}
          />
        </div>
        <div id="alarm-history" className="section-anchor grid-wide">
          <AlarmHistoryTable data={alarms} />
        </div>
      </section>
    </div>
  );
}

function FocusPanel({ machine }: { machine: FleetRow | null }) {
  return (
    <section id="fleet" className="panel focus-panel section-anchor">
      <div className="panel-header">
        <div>
          <p className="panel-kicker">SELECTED_MACHINE_FOCUS</p>
          <h2>{machine?.machineId ?? "NO_MACHINE"} / {machine?.machineType ?? "SYNTHETIC"}</h2>
        </div>
        <span>READ_ONLY_ANALYTICS</span>
      </div>
      <div className="focus-layout">
        <div className="machining-envelope" aria-label="Synthetic machining envelope visualization">
          <div className="envelope-path" />
          <div className="envelope-core" />
          <span>SYNTHETIC MACHINING ENVELOPE</span>
        </div>
        <dl className="machine-metrics">
          <div>
            <dt>MACHINE_NAME</dt>
            <dd>{machine?.machineName ?? "-"}</dd>
          </div>
          <div>
            <dt>LINE</dt>
            <dd>{machine?.line ?? "-"}</dd>
          </div>
          <div>
            <dt>UTILIZATION CLASS</dt>
            <dd>{formatPercent(machine?.utilization)}</dd>
          </div>
          <div>
            <dt>CUTTING_RATIO</dt>
            <dd>{formatPercent(machine?.cuttingRatio)}</dd>
          </div>
          <div>
            <dt>ALARM_COUNT</dt>
            <dd>{formatNumber(machine?.alarmCount)}</dd>
          </div>
          <div>
            <dt>CRITICAL_COUNT</dt>
            <dd>{formatNumber(machine?.criticalAlarmCount)}</dd>
          </div>
        </dl>
      </div>
      <div className="read-only-strip">
        <span>NO LIVE CONTROL LINK</span>
        <span>READ_ONLY_ANALYTICS</span>
      </div>
    </section>
  );
}

function FleetPanel({ rows }: { rows: FleetRow[] }) {
  return (
    <section className="panel fleet-panel">
      <div className="panel-header">
        <div>
          <p className="panel-kicker">SYNTHETIC WINDOW</p>
          <h2>Fleet Overview</h2>
        </div>
        <span>ANALYSIS RANGE</span>
      </div>
      <div className="fleet-list">
        {rows.map((row) => (
          <article key={row.machineId} className={`fleet-row ${row.riskLevel.toLowerCase().replace(/\s*\/\s*/g, "-")}`}>
            <div>
              <strong>{row.machineId}</strong>
              <span>{row.machineType} / {row.line}</span>
            </div>
            <div className="mini-bars">
              <span style={{ width: `${Math.max(2, row.utilization)}%` }} />
              <span style={{ width: `${Math.max(2, row.cuttingRatio)}%` }} />
            </div>
            <dl>
              <div>
                <dt>UTIL</dt>
                <dd>{formatPercent(row.utilization)}</dd>
              </div>
              <div>
                <dt>RISK</dt>
                <dd>{row.riskLevel}</dd>
              </div>
              <div>
                <dt>LATEST</dt>
                <dd>{row.latestAlarmSeverity}</dd>
              </div>
            </dl>
          </article>
        ))}
      </div>
    </section>
  );
}

function CriticalVectorsPanel({ alarms }: { alarms: AlarmHistory[] }) {
  return (
    <section id="critical-vectors" className="panel critical-panel section-anchor">
      <div className="panel-header">
        <div>
          <p className="panel-kicker">ERROR_ACCENT_STREAM</p>
          <h2>CRITICAL_VECTORS</h2>
        </div>
        <span>{alarms.length} shown</span>
      </div>
      {alarms.length === 0 ? (
        <p className="empty-state table-empty">No critical vectors in the selected alarm result.</p>
      ) : (
        <div className="critical-list">
          {alarms.map((alarm) => (
            <article key={alarm.alarmId} className="critical-card">
              <div className="critical-meta">
                <span>{formatDateTime(alarm.occurredAt)}</span>
                <strong>{alarm.machineId}</strong>
              </div>
              <h3>{alarm.alarmCode}</h3>
              <p>{alarm.message}</p>
              <div className="critical-actions">
                <span className="severity-badge critical">{alarm.severity}</span>
                <span>READ_ONLY</span>
                <span>SYNTHETIC_EVENT</span>
                <span>CLEARED: {formatDateTime(alarm.clearedAt)}</span>
              </div>
            </article>
          ))}
        </div>
      )}
    </section>
  );
}

function AnalyticsLogPanel({ entries }: { entries: string[] }) {
  return (
    <section className="panel log-panel">
      <div className="panel-header">
        <h2>ANALYTICS_EVENT_LOG</h2>
        <span>LOCAL</span>
      </div>
      <div className="log-lines" aria-label="synthetic analytics event log">
        {entries.map((entry) => (
          <code key={entry}>{entry}</code>
        ))}
      </div>
    </section>
  );
}
