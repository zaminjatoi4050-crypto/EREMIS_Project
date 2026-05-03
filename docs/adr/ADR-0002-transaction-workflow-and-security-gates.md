# ADR-0002: Transaction-Gated Purchase Workflow

- Status: Accepted
- Date: 2026-05-01

## Context

Direct status flipping of properties to SOLD can bypass payment and approval controls.

## Decision

All purchase operations must flow through transaction lifecycle:

- Buyer initiates transaction -> status PENDING
- Property status set LOCKED during pending state
- Admin decides APPROVED or REJECTED
- APPROVED -> property SOLD + ownership transfer
- REJECTED -> property AVAILABLE + reason persisted

Direct property buy endpoint was removed to avoid bypass risk.

## Consequences

Positive:

- Uniform business process and auditability
- Better fraud/error control
- Deterministic state transitions

Trade-offs:

- Slightly longer user flow due to explicit approval process

## Verification

- Pending/approve/reject flow tested successfully after hardening.
