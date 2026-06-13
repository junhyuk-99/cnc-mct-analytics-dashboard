# CNC/MCT Analytics Dashboard Demo

Portfolio demo for a CNC/MCT equipment analytics dashboard.

This repository is a public, rebuilt demo project. It is not a copy of production source code and does not include real database connections, customer data, equipment history, server IP addresses, credentials, logs, certificates, or private environment values.

All future demo data will be synthetic sample data.

## Planned Demo Scope

The planned demo will show analytics workflows for CNC/MCT equipment using a local-only stack:

- Equipment utilization
- RunTime / CutTime cutting ratio
- Alarm history
- Machine status distribution
- Daily trend charts
- KPI cards and chart-based dashboard views

## Planned Tech Stack

- Spring Boot API
- React dashboard
- MongoDB
- Python seed script
- Docker Compose

## Planned Local Demo Flow

1. Start local MongoDB.
2. Seed synthetic sample data.
3. Run the Spring Boot backend API.
4. Run the React frontend.
5. Open the dashboard and review synthetic CNC/MCT analytics.

## Sample Data

Generate local synthetic sample data with:

```bash
python scripts/generate_sample_data.py
```

The generated files under `sample-data/` are fake demo records only. They are not copied from real production systems.

## Security Notice

- Do not add production `.env` files.
- Do not add real DB URIs, server IPs, credentials, keys, certificates, logs, dumps, or customer screenshots.
- Do not import private repository history.
- Use only synthetic data in `sample-data/`.

See [docs/SECURITY.md](docs/SECURITY.md) and [docs/DATA_NOTICE.md](docs/DATA_NOTICE.md).
