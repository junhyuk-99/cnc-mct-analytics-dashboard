import { useEffect, useMemo, useState } from "react";
import { fetchMachineHistory } from "../../services/dashboardApi";
import type { Machine, MachineHistoryPage } from "../../types/dashboard";
import { formatDateTime } from "../../utils/format";

const PAGE_SIZE = 8;

const STATUS_COLORS: Record<string, string> = {
  RUNNING: "#0be298",
  IDLE: "#ffb77f",
  ALARM: "#ffb4ab",
  OFFLINE: "#88919d"
};

type MachineHistoryTimelineProps = {
  machines: Machine[];
  machineId: string;
  from: string;
  to: string;
};

function formatDuration(seconds: number): string {
  const hours = Math.floor(seconds / 3600);
  const minutes = Math.round((seconds % 3600) / 60);
  if (hours === 0) {
    return `${minutes}m`;
  }
  return `${hours}h ${String(minutes).padStart(2, "0")}m`;
}

/**
 * Paged status-event timeline for a single machine. The global machine filter
 * takes precedence; otherwise the panel keeps its own selection.
 */
export function MachineHistoryTimeline({ machines, machineId, from, to }: MachineHistoryTimelineProps) {
  const [localMachine, setLocalMachine] = useState("");
  const [page, setPage] = useState(0);
  const [data, setData] = useState<MachineHistoryPage | null>(null);
  const [error, setError] = useState<string | null>(null);

  const selectedMachine = machineId || localMachine || machines[0]?.machineId || "";

  // Reset paging whenever the machine or window changes.
  useEffect(() => {
    setPage(0);
  }, [selectedMachine, from, to]);

  useEffect(() => {
    if (!selectedMachine) {
      setData(null);
      return;
    }
    let cancelled = false;
    setError(null);
    fetchMachineHistory(selectedMachine, { from, to }, page, PAGE_SIZE)
      .then((result) => {
        if (!cancelled) {
          setData(result);
        }
      })
      .catch((caught) => {
        if (!cancelled) {
          setData(null);
          setError(caught instanceof Error ? caught.message : "Unable to load machine history");
        }
      });
    return () => {
      cancelled = true;
    };
  }, [selectedMachine, from, to, page]);

  const totalPages = data?.totalPages ?? 0;
  const items = useMemo(() => data?.items ?? [], [data]);

  return (
    <section className="panel wide">
      <div className="panel-header">
        <div>
          <h2>MACHINE_HISTORY</h2>
          <span>STATUS_EVENT_TIMELINE · {selectedMachine || "NO_MACHINE"}</span>
        </div>
        <label className="history-machine-picker">
          <span>MACHINE</span>
          <select
            value={selectedMachine}
            onChange={(event) => setLocalMachine(event.target.value)}
            disabled={Boolean(machineId)}
            title={machineId ? "Machine is fixed by the global filter" : undefined}
          >
            {machines.map((machine) => (
              <option key={machine.machineId} value={machine.machineId}>
                {machine.machineId}
              </option>
            ))}
          </select>
        </label>
      </div>

      {error ? (
        <p className="empty-state table-empty">Machine history query failed: {error}</p>
      ) : items.length === 0 ? (
        <p className="empty-state table-empty">No status events for the selected machine and range.</p>
      ) : (
        <div className="timeline-list">
          {items.map((item) => (
            <article key={item.eventId} className="timeline-row">
              <span
                className="timeline-dot"
                style={{ backgroundColor: STATUS_COLORS[item.status] ?? "#476582" }}
                aria-hidden="true"
              />
              <div className="timeline-main">
                <strong>{item.status}</strong>
                <span>
                  {formatDateTime(item.startedAt)} → {formatDateTime(item.endedAt)}
                </span>
              </div>
              <div className="timeline-meta">
                <span>{formatDuration(item.durationSeconds)}</span>
                <span>{item.workDate}</span>
              </div>
            </article>
          ))}
        </div>
      )}

      <div className="timeline-footer">
        <span>
          PAGE {totalPages === 0 ? 0 : (data?.page ?? 0) + 1}/{totalPages} · {data?.totalElements ?? 0} EVENTS
        </span>
        <div className="toggle-group">
          <button
            type="button"
            className="mini-btn"
            onClick={() => setPage((current) => Math.max(0, current - 1))}
            disabled={(data?.page ?? 0) <= 0}
          >
            PREV
          </button>
          <button
            type="button"
            className="mini-btn"
            onClick={() => setPage((current) => current + 1)}
            disabled={totalPages === 0 || (data?.page ?? 0) >= totalPages - 1}
          >
            NEXT
          </button>
        </div>
      </div>
    </section>
  );
}
