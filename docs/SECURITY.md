# Security Policy for the Public Demo

This repository is a public portfolio demo. It must remain independent from private production repositories and private Git history.

## Core Rules

- Do not connect this project to any production database.
- Do not commit real DB URIs, server IP addresses, usernames, passwords, tokens, API keys, access keys, JWT secrets, encryption keys, private keys, certificates, or keystores.
- Do not commit real customer names, site names, equipment names, LOT numbers, production order numbers, user accounts, phone numbers, emails, alarm history, production history, or equipment history.
- Do not commit production logs, error output, build output, dumps, backups, exports, or raw operational data.
- Do not commit screenshots from real production systems.
- Do not copy files directly from private repositories into this demo repo.
- Do not import private repository Git history.

## Allowed Data

Only synthetic data is allowed.

Allowed examples:

- Fake machine IDs created for the demo
- Fake status history
- Fake runtime and cuttime measurements
- Fake alarm events
- Fake utilization summaries

## Blocked File Types

The following files must not be committed:

- `.env` and `.env.*` files other than `.env.example`
- `*.pem`, `*.key`, `*.p12`, `*.jks`, `*.keystore`, `*.cert`, `*.crt`
- `*.log`, `*.err`, `*.out`, `bootrun*`
- `*.sql`, `*.dump`, `*.bak`, `*.db`, `*.sqlite`, `*.sqlite3`
- real exports such as production CSV/XLSX files

## Before Publishing Changes

Run a secret scan and review changed files manually. Any value that looks like a real credential, private network detail, customer identifier, or production dataset must be removed before commit.
