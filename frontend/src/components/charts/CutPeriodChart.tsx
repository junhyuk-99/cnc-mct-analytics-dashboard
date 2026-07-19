import { useMemo } from "react";
import {
  Bar,
  BarChart,
  CartesianGrid,
  Legend,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis
} from "recharts";
import type { DailyRollup, MonthlyRollup } from "../../types/dashboard";

export type CutGranularity = "month" | "day";

type CutPeriodChartProps = {
  monthly: MonthlyRollup[];
  daily: DailyRollup[];
  granularity: CutGranularity;
  year: number;
  month: number;
  machineId: string;
  error: string | null;
  onGranularityChange: (value: CutGranularity) => void;
  onMonthChange: (value: number) => void;
};

type PeriodRow = {
  label: string;
  runHours: number;
  cutHours: number;
};

const MONTHS = Array.from({ length: 12 }, (_, index) => index + 1);

function daysInMonth(year: number, month: number): number {
  return new Date(year, month, 0).getDate();
}

function toHours(seconds: number): number {
  return Math.round((seconds / 3600) * 100) / 100;
}

function formatHours(value: number): string {
  return `${value.toFixed(1)}h`;
}

/**
 * Cutting period view: runtime/cutting totals per month of a year, or per day
 * of a month, rolled forward from the engine's hourly buckets. Series colors
 * follow the app-wide entity mapping (runtime = green, cutting = blue).
 */
export function CutPeriodChart({
  monthly,
  daily,
  granularity,
  year,
  month,
  machineId,
  error,
  onGranularityChange,
  onMonthChange
}: CutPeriodChartProps) {
  const rows = useMemo<PeriodRow[]>(() => {
    if (granularity === "month") {
      const filtered = machineId ? monthly.filter((item) => item.machineId === machineId) : monthly;
      if (filtered.length === 0) {
        return [];
      }
      const byMonth = new Map<number, { run: number; cut: number }>();
      filtered.forEach((item) => {
        const current = byMonth.get(item.month) ?? { run: 0, cut: 0 };
        current.run += item.runTimeSeconds;
        current.cut += item.cutTimeSeconds;
        byMonth.set(item.month, current);
      });
      return MONTHS.map((value) => {
        const bucket = byMonth.get(value) ?? { run: 0, cut: 0 };
        return {
          label: `M${String(value).padStart(2, "0")}`,
          runHours: toHours(bucket.run),
          cutHours: toHours(bucket.cut)
        };
      });
    }

    const filtered = machineId ? daily.filter((item) => item.machineId === machineId) : daily;
    if (filtered.length === 0) {
      return [];
    }
    const byDay = new Map<number, { run: number; cut: number }>();
    filtered.forEach((item) => {
      const current = byDay.get(item.day) ?? { run: 0, cut: 0 };
      current.run += item.runTimeSeconds;
      current.cut += item.cutTimeSeconds;
      byDay.set(item.day, current);
    });
    return Array.from({ length: daysInMonth(year, month) }, (_, index) => {
      const value = index + 1;
      const bucket = byDay.get(value) ?? { run: 0, cut: 0 };
      return {
        label: String(value).padStart(2, "0"),
        runHours: toHours(bucket.run),
        cutHours: toHours(bucket.cut)
      };
    });
  }, [daily, granularity, machineId, month, monthly, year]);

  return (
    <section className="panel wide">
      <div className="panel-header">
        <div>
          <h2>CUT_PERIOD</h2>
          <span>
            {granularity === "month" ? `MONTHLY ${year}` : `DAILY ${year}-${String(month).padStart(2, "0")}`}
            {" · "}
            {machineId || "ALL_MACHINES"}
          </span>
        </div>
        <div className="period-controls">
          <div className="toggle-group" role="group" aria-label="cut period granularity">
            <button
              type="button"
              className={`mini-btn ${granularity === "month" ? "active" : ""}`}
              onClick={() => onGranularityChange("month")}
            >
              MONTHLY
            </button>
            <button
              type="button"
              className={`mini-btn ${granularity === "day" ? "active" : ""}`}
              onClick={() => onGranularityChange("day")}
            >
              DAILY
            </button>
          </div>
          {granularity === "day" ? (
            <label className="period-month-picker">
              <span>MONTH</span>
              <select value={month} onChange={(event) => onMonthChange(Number(event.target.value))}>
                {MONTHS.map((value) => (
                  <option key={value} value={value}>
                    {String(value).padStart(2, "0")}
                  </option>
                ))}
              </select>
            </label>
          ) : null}
        </div>
      </div>
      <div className="chart-frame large">
        {error ? (
          <p className="empty-state">Cut period query failed: {error}</p>
        ) : rows.length === 0 ? (
          <p className="empty-state">
            No rollup buckets for this period. The sample range covers 2026-01 — see
            docs/ROLLUP_ARCHITECTURE.md.
          </p>
        ) : (
          <ResponsiveContainer width="100%" height="100%">
            <BarChart data={rows} margin={{ top: 12, right: 18, bottom: 0, left: 0 }} barGap={2}>
              <CartesianGrid strokeDasharray="3 3" vertical={false} stroke="rgba(152,203,255,0.14)" />
              <XAxis dataKey="label" tick={{ fontSize: 11, fill: "#bec7d4" }} axisLine={false} tickLine={false} />
              <YAxis
                tickFormatter={(value) => `${value}h`}
                tick={{ fill: "#bec7d4" }}
                axisLine={false}
                tickLine={false}
              />
              <Tooltip
                contentStyle={{ background: "#111316", border: "1px solid #3f4852", color: "#e2e2e6" }}
                cursor={{ fill: "rgba(152,203,255,0.08)" }}
                formatter={(value, name) => [formatHours(Number(value)), name]}
              />
              <Legend wrapperStyle={{ color: "#bec7d4", fontFamily: "Consolas, monospace" }} />
              <Bar dataKey="runHours" name="Runtime (h)" fill="#0be298" radius={[2, 2, 0, 0]} />
              <Bar dataKey="cutHours" name="Cutting (h)" fill="#98cbff" radius={[2, 2, 0, 0]} />
            </BarChart>
          </ResponsiveContainer>
        )}
      </div>
    </section>
  );
}
