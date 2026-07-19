import type { ReactNode } from "react";
import { apiBaseUrl } from "../../services/apiClient";

type AppShellProps = {
  children: ReactNode;
};

const demoNavigation = [
  { label: "Overview", href: "#overview" },
  { label: "Fleet", href: "#fleet" },
  { label: "Utilization", href: "#utilization" },
  { label: "Cutting Ratio", href: "#cutting-ratio" },
  { label: "Status", href: "#status-distribution" },
  { label: "Trend", href: "#daily-trend" },
  { label: "Hourly Rollup", href: "#hourly-rollup" },
  { label: "Cut Period", href: "#cut-period" },
  { label: "Pre-Alarm", href: "#prealarm" },
  { label: "Machine History", href: "#machine-history" },
  { label: "Critical Vectors", href: "#critical-vectors" }
];

export function AppShell({ children }: AppShellProps) {
  return (
    <div className="app-shell">
      <aside className="sidebar">
        <div className="sidebar-main">
          <div>
            <p className="eyebrow">PRECISION_CORE_DEMO</p>
            <h1>CNC/MCT COMMAND CENTER</h1>
            <p className="subtitle">Synthetic Precision analytics shell</p>
          </div>
          <nav className="demo-nav" aria-label="Dashboard sections">
            {demoNavigation.map((item) => (
              <a key={item.href} href={item.href}>
                {item.label}
              </a>
            ))}
          </nav>
        </div>
        <div className="sidebar-footer">
          <div className="notice-stack" aria-label="data safety notices">
            <span>SYNTHETIC DATA</span>
            <span>LOCAL API</span>
            <span>NO PRODUCTION DATA</span>
          </div>
          <div className="api-note">
            <span>API</span>
            <strong>{apiBaseUrl}</strong>
          </div>
        </div>
      </aside>
      <main className="main-content">{children}</main>
    </div>
  );
}
