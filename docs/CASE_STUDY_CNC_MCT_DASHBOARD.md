# Case Study: CNC/MCT Manufacturing Dashboard

## 1. Overview

This case study summarizes a deployed manufacturing dashboard project for CNC/MCT equipment monitoring and analytics.

The production system itself is not included in this public repository. This document describes the technical problem, development challenges, architecture, implementation approach, and lessons learned using anonymized and sanitized information only.

The public repository is a rebuilt portfolio demo that uses synthetic sample data.

## 2. Background

The original project was built for a manufacturing environment where CNC/MCT equipment data was collected and stored in MongoDB.

Operators and managers needed a dashboard to review machine utilization, RunTime/CutTime cutting ratio, alarm history, machine status distribution, and daily operation trends.

Because the original system was deployed in an operational environment, production source code, production data, server addresses, credentials, logs, customer-specific names, and screenshots are excluded from this public repository.

## 3. Problem

The project addressed the following issues:

* Equipment status was difficult to review across multiple machines.
* RunTime and CutTime records needed to be transformed into meaningful utilization and cutting-ratio metrics.
* Alarm history needed to be searchable by machine, date range, alarm code, and severity.
* Large equipment history datasets caused performance and rendering concerns when queried directly.
* Field stakeholders needed practical indicators for daily operation review, not just raw machine data.
* The system required a maintainable backend API and frontend dashboard structure.

## 4. Development Challenges

### 4.1 Defining meaningful equipment data with field stakeholders

One of the most difficult parts of the project was not simply displaying collected machine data, but deciding which equipment data was actually meaningful for the field.

The raw data contained many signals, status values, timestamps, and machine-specific records. However, not every collected value was useful for operators or managers. During the project, we had to review the available equipment data with field stakeholders and clarify which values should be treated as key dashboard indicators.

This required repeated discussion around questions such as:

* Which machine status values should be considered operating, stopped, alarm, or disconnected?
* Which signals should be used for utilization analysis?
* How should RunTime and CutTime be interpreted?
* Which alarm records should be prioritized?
* Which indicators were useful for daily operation review rather than only for engineering analysis?

Through this process, the dashboard scope was refined around practical operational indicators such as equipment utilization, RunTime/CutTime cutting ratio, alarm history, machine status distribution, and daily trend analysis.

### 4.2 Handling large volumes of equipment history data

Another major challenge was the volume of collected equipment history data. Querying raw history collections directly from the dashboard caused performance concerns because the frontend had to render charts and tables based on large time-range datasets.

If every dashboard screen queried raw event-level data directly, the result would have been slow API responses, heavy frontend rendering, and inconsistent user experience.

To solve this, the system separated raw operational data from dashboard-ready summary data. Instead of calculating every metric at page load time, separate aggregation collections were created for frequently used dashboard metrics.

Scheduled jobs were then used to pre-calculate and store summary data such as daily runtime, cutting time, utilization, and trend records. The dashboard API could then read from these pre-aggregated collections instead of scanning large raw history datasets every time.

This improved dashboard responsiveness and made the frontend rendering more stable.

### 4.3 Interpreting manufacturing data semantics

Raw equipment records could not be used directly as dashboard metrics. For example, RunTime and CutTime records had to be interpreted based on operational time intervals rather than simply summing raw snapshot values.

The aggregation logic was designed around explicit time-window rules. Runtime and cutting time were calculated from valid machine event ranges, and dashboard APIs returned pre-structured metrics for frontend charts.

This made the dashboard more reliable because the displayed utilization and cutting-ratio values reflected the operational meaning of the data rather than raw record values.

### 4.4 Resolving equipment-code mapping issues

Another issue was the mismatch between internal equipment master codes and machine identifiers used in collected equipment data. In some cases, dashboard rollups could not find the correct equipment records because the master data and collected data used different identifiers.

The solution was to define a clear mapping rule between the equipment master collection and the collected machine data. This allowed daily summaries and machine-level dashboard APIs to aggregate the correct records consistently.

### 4.5 Working within MongoDB version constraints

The production MongoDB environment had version limitations, which meant some newer aggregation operators could not be used. Query logic had to be written in a way that remained compatible with the deployed MongoDB version.

This required adjusting aggregation and filtering logic, especially for pattern matching and date-range queries, while still keeping the API responses suitable for dashboard visualization.

### 4.6 Balancing field requirements and system performance

The field users wanted dashboard screens that were easy to understand and fast to load. At the same time, the available data was large, detailed, and not always directly aligned with dashboard requirements.

The implementation had to balance both sides:

* Preserve the meaning of the original manufacturing data.
* Avoid overloading the dashboard with unnecessary signals.
* Pre-calculate frequently used metrics.
* Keep API responses small enough for chart rendering.
* Provide filters by date range, machine, and alarm severity.
* Keep the dashboard usable for daily operational review.

The dashboard was designed not as a raw data viewer, but as an operational analytics layer built on top of manufacturing data.

## 5. Solution

The solution used a MongoDB-backed Spring Boot API and a React dashboard.

The backend aggregated equipment operation records, runtime/cuttime data, machine status records, and alarm history into dashboard-ready API responses.

The frontend visualized those responses through KPI cards, charts, machine-level summaries, filters, and alarm history tables.

The most important design decision was separating raw data from dashboard-ready summary data:

```text
Raw machine history data
        ↓
Scheduled aggregation / summary collections
        ↓
Dashboard API responses
        ↓
React chart and table UI
```

This separation made the system easier to maintain. Raw data remained available for detailed analysis, while the dashboard used summary collections optimized for fast visualization.

## 6. Architecture

The production architecture was conceptually structured as follows:

```text
CNC/MCT Equipment Data
        ↓
Data Collection / Integration Layer
        ↓
MongoDB
        ↓
Scheduled Aggregation Collections
        ↓
Spring Boot API
        ↓
React Dashboard
        ↓
Operator / Manager Monitoring
```

The public demo rebuilds the same general concept with local-only synthetic data:

```text
Synthetic Sample Data
        ↓
MongoDB Docker Container
        ↓
Spring Boot Demo API
        ↓
React Command Center Dashboard
```

## 7. Key Features

### Equipment Utilization

The dashboard aggregates operation data and presents machine-level utilization indicators by date range.

### RunTime / CutTime Cutting Ratio

RunTime and CutTime records are converted into cutting-ratio metrics to compare actual cutting time against machine runtime.

### Machine Status Distribution

Machine status records are summarized into distribution charts to review running, idle, alarm, and offline patterns.

### Alarm History

Alarm records are displayed by machine, severity, code, and time range. Critical alarms are highlighted separately for faster review.

### Daily Trend Analysis

Daily utilization, cutting ratio, and alarm count trends are visualized to support operational review.

### Dashboard UI

The dashboard provides KPI cards, chart panels, machine lists, alarm tables, and filtering controls.

## 8. My Role

My responsibilities included:

* Analyzing manufacturing data structures and dashboard requirements.
* Discussing field requirements and selecting meaningful equipment indicators.
* Designing MongoDB query and aggregation logic.
* Creating dashboard-ready summary data structures.
* Implementing scheduled aggregation logic for frequently used metrics.
* Implementing Spring Boot APIs for dashboard data access.
* Building React dashboard screens and chart-based visualizations.
* Implementing filters for date range, equipment, and alarm severity.
* Supporting local runtime testing and deployment workflow.
* Documenting architecture, data flow, and security boundaries.
* Rebuilding a sanitized public demo version using synthetic data.

## 9. Results

The project delivered a deployed dashboard for CNC/MCT equipment monitoring and analytics.

The system provided:

* Machine-level utilization monitoring.
* RunTime/CutTime cutting-ratio analysis.
* Alarm history review by machine, period, and severity.
* Machine status distribution views.
* Daily trend analysis for operational review.
* A maintainable Spring Boot API and React dashboard structure.
* Improved dashboard performance through summary collections and scheduled aggregation.

## 10. Public Demo Relationship

This public repository is not the production project.

It is a sanitized rebuild designed to demonstrate the same engineering concepts:

* Manufacturing dashboard architecture
* Spring Boot API design
* MongoDB-backed analytics
* React dashboard implementation
* Synthetic data generation
* Docker-based local runtime
* Read-only command-center style dashboard UI

The following are excluded:

* Production source code
* Production database connections
* Customer information
* Real equipment data
* Real alarm logs
* Server IP addresses
* Credentials, keys, tokens, certificates, and environment files
* Private Git history
* Production screenshots

## 11. Evidence of Work

The original work was developed across separate frontend and backend repositories with commit history covering dashboard UI, API implementation, data aggregation, menu/role structure, scheduled processing, and runtime testing.

Those repositories and commit histories are intentionally excluded from this public repository because they may contain private implementation details, internal naming, operational paths, and customer-specific context.

The public repository instead contains a rebuilt implementation with synthetic data and documentation.

## 12. Lessons Learned

Key lessons from the project:

* Manufacturing dashboards require field-level discussion to define which data is actually useful.
* Raw equipment data should be separated from dashboard-ready analytics data.
* Time-based aggregation rules must be explicit and consistently applied.
* Scheduled summary collections can reduce rendering and query performance issues.
* Alarm and status data should be modeled for filtering by equipment, severity, and period.
* Public portfolio versions of production systems should be rebuilt with synthetic data rather than copied from private source code.
* Runtime smoke tests and documentation improve the credibility of a technical portfolio demo.
