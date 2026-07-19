import { useMemo } from "react";
import type { PreAlarmIndicator, PreAlarmSummary } from "../../types/dashboard";
import { formatNumber } from "../../utils/format";

type PreAlarmPanelProps = {
  summary: PreAlarmSummary | null;
  indicators: PreAlarmIndicator[];
  machineId: string;
};

/**
 * Pre-alarm view: the aggregated alarm landscape plus threshold-based
 * early-warning indicators computed by the backend rule engine.
 */
export function PreAlarmPanel({ summary, indicators, machineId }: PreAlarmPanelProps) {
  const filteredIndicators = useMemo(
    () => (machineId ? indicators.filter((item) => item.machineId === machineId) : indicators),
    [indicators, machineId]
  );

  return (
    <section className="panel wide">
      <div className="panel-header">
        <div>
          <h2>PRE_ALARM</h2>
          <span>EARLY_WARNING_INDICATORS · {machineId || "ALL_MACHINES"}</span>
        </div>
        <span>{filteredIndicators.length} ACTIVE</span>
      </div>

      <div className="prealarm-summary" aria-label="alarm level totals">
        <div className="prealarm-stat">
          <dt>TOTAL</dt>
          <dd>{formatNumber(summary?.total)}</dd>
        </div>
        <div className="prealarm-stat">
          <dt>INFO</dt>
          <dd>{formatNumber(summary?.infoCount)}</dd>
        </div>
        <div className="prealarm-stat warning">
          <dt>WARNING</dt>
          <dd>{formatNumber(summary?.warningCount)}</dd>
        </div>
        <div className="prealarm-stat critical">
          <dt>CRITICAL</dt>
          <dd>{formatNumber(summary?.criticalCount)}</dd>
        </div>
        <div className="prealarm-codes">
          <dt>TOP_CODES</dt>
          <dd>
            {(summary?.topCodes ?? []).map((code) => (
              <span key={code.alarmCode} className="code-chip">
                {code.alarmCode}×{code.count}
              </span>
            ))}
          </dd>
        </div>
      </div>

      {filteredIndicators.length === 0 ? (
        <p className="empty-state table-empty">
          No early-warning indicators for the selected window — all machines are inside thresholds.
        </p>
      ) : (
        <div className="prealarm-list">
          {filteredIndicators.map((indicator) => (
            <article
              key={`${indicator.machineId}-${indicator.rule}`}
              className={`prealarm-card ${indicator.severity.toLowerCase()}`}
            >
              <div className="prealarm-card-head">
                <strong>{indicator.machineId}</strong>
                <span className={`severity-badge ${indicator.severity === "WARNING" ? "warning" : ""}`}>
                  {indicator.severity}
                </span>
              </div>
              <h3>{indicator.rule}</h3>
              <p>{indicator.message}</p>
              <div className="prealarm-card-meta">
                <span>VALUE: {indicator.metricValue}</span>
                <span>THRESHOLD: {indicator.threshold}</span>
              </div>
            </article>
          ))}
        </div>
      )}
    </section>
  );
}
