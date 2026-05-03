# ADR-0001: Layered Architecture with Service-Layer Authorization

- Status: Accepted
- Date: 2026-05-01

## Context

EREMIS is a desktop system with multiple role-sensitive workflows. UI-only restrictions are insufficient for security because controllers/services may be called from multiple entry points.

## Decision

Use a layered architecture with mandatory business and authorization checks in service layer:

- UI: rendering and user interaction
- Controller: request orchestration
- Service: authorization and business rules
- DAO: persistence

## Consequences

Positive:

- Centralized policy enforcement
- Reduced chance of UI bypass vulnerabilities
- Better testability of business rules

Trade-offs:

- Slightly more boilerplate between layers
- Requires discipline to keep checks out of UI-only scope

## Verification

- Unauthorized delete regression test now passes with service-layer security check.
