# Data Notice

All data in this repository must be synthetic sample data.

This demo must not include real production data, equipment history, alarm history, LOT numbers, production order numbers, user information, customer information, logs, screenshots, or operational exports.

## Planned Synthetic Sample Files

The `sample-data/` directory may later contain files such as:

- `machines.json`
- `status-history.json`
- `runtime-cuttime.json`
- `alarm-history.json`
- `utilization-summary.json`

These files must be generated for the demo and must not be copied from real systems.

## Data Generation Rules

- Use fake machine IDs such as `CNC-DEMO-01` or `MCT-DEMO-01`.
- Use fake timestamps and metric values.
- Use fake alarm codes and descriptions.
- Avoid real customer names, real site names, real operator names, real phone numbers, and real emails.
- Keep sample volume small enough for public review and local testing.
