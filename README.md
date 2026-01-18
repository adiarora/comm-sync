# Comm-Sync

Comm-Sync is a backend service that synchronizes software packages across client environments using version checks and SHA-256 integrity verification. It was originally built to run inside a restricted corporate network where the authoritative database and package store were only reachable from internal infrastructure.

This repository contains the core service logic and a lightweight mock package store for local testing and demonstration.

## What it does

During each sync cycle, Comm-Sync:
- Queries for packages that are pending synchronization
- Fetches the authoritative package catalog from a store service
- Determines whether a client is behind the latest version
- Downloads the updated package when needed
- Verifies package integrity via SHA-256 checksum
- Uploads the package to a client-specific target location
- Updates status for observability and retries (SUCCESS / FAILED / PENDING)

## Architecture

- `MainController`  
  Orchestrates periodic sync cycles.

- `DbAccess`  
  Database access layer (SQL Server in production; not publicly reachable).

- `PackageService`  
  Implements the synchronization workflow: version gating, transfer, verification, status updates.

- `StoreClient`  
  Client for the package store API.

- `ChecksumUtil`  
  SHA-256 hashing and integrity checks.

- `mock-store/`  
  A minimal Flask-based store that serves a catalog and downloadable package artifacts for testing.
