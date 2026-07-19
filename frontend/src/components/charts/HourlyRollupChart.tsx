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
import type { HourlyRollup } from "../../types/dashboard";

type HourlyRollupChartProps = {
  data: HourlyRollup[];
  date: string;
  machineId: string;
  error: string | null;
  onDateChange: (value: string) => void;
};

type HourRow = {
  hour: string;
  runMinutes: number;
  cutMinutes: number;
};

function formatMinutes(value: number): string {
  return `${value.toFixed(1)}m`;
}

/**
 * Hourly runtime/cutting buckets read from the rollup engine's pre-aggregated
 * summary collections. Series colors follow the app-wide entity mapping
 * (runtime = green, cutting = blue).
 */
export function HourlyRollupChart({ data, date, machineId, error, onDateChange }: HourlyRollupChartProps) {
  const rows = useMemo<HourRow[]>(() => {
    const filtered = machineId ? data.filter((item) => item.machineId === machineId) : data;
    if (filtered.length === 0) {
      return [];
    }
    const byHour = new Map<number, { run: number; cut: number }>();
    filtered.forEach((item) => {
      const current = byHour.get(item.hour) ?? { run: 0, cut: 0 };
      current.run += item.runTimeSeconds;
      current.cut += item.cutTimeSeconds;
      byHour.set(item.hour, current);
    });
    return Array.from({ length: 24 }, (_, hour) => {
      const bucket = byHour.get(hour) ?? { run: 0, cut: 0 };
      return {
        hour: `${String(hour).padStart(2, "0")}h`,
        runMinutes: Math.round((bucket.run / 60) * 10) / 10,
        cutMinutes: Math.round((bucket.cut / 60) * 10) / 10
      };
    });
  }, [data, machineId]);

  return (
    <section className="panel wide">
      <div className="panel-header">
        <div>
          <h2>HOURLY_ROLLUP</h2>
          <span>RUNTIME / CUTTIME PER HOUR (KST) · {machineId || "ALL_MACHINES"}</span>
        </div>
        <label className="rollup-date-picker">
          <span>DATE</span>
          <input type="date" value={date} onChange={(event) => onDateChange(event.target.value)} />
        </label>
      </div>
      <div className="chart-frame large">
        {error ? (
          <p className="empty-state">Rollup query failed: {error}</p>
        ) : rows.length === 0 ? (
          <p className="empty-state">
            No rollup buckets for {date}. Run the rollup backfill for the sample signal range
            (2026-01-01 to 2026-01-03) — see docs/ROLLUP_ARCHITECTURE.md.
          </p>
        ) : (
          <ResponsiveContainer width="100%" height="100%">
            <BarChart data={rows} margin={{ top: 12, right: 18, bottom: 0, left: 0 }} barGap={2}>
              <CartesianGrid strokeDasharray="3 3" vertical={false} stroke="rgba(152,203,255,0.14)" />
              <XAxis dataKey="hour" tick={{ fontSize: 11, fill: "#bec7d4" }} axisLine={false} tickLine={false} />
              <YAxis
                tickFormatter={(value) => `${value}m`}
                tick={{ fill: "#bec7d4" }}
                axisLine={false}
                tickLine={false}
              />
              <Tooltip
                contentStyle={{ background: "#111316", border: "1px solid #3f4852", color: "#e2e2e6" }}
                cursor={{ fill: "rgba(152,203,255,0.08)" }}
                formatter={(value, name) => [formatMinutes(Number(value)), name]}
              />
              <Legend wrapperStyle={{ color: "#bec7d4", fontFamily: "Consolas, monospace" }} />
              <Bar dataKey="runMinutes" name="Runtime (min)" fill="#0be298" radius={[2, 2, 0, 0]} />
              <Bar dataKey="cutMinutes" name="Cutting (min)" fill="#98cbff" radius={[2, 2, 0, 0]} />
            </BarChart>
          </ResponsiveContainer>
        )}
      </div>
    </section>
  );
}
