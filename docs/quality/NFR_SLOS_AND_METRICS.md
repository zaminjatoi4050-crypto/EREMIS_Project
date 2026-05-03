# NFR, SLOs, and Engineering Metrics (2026)

## 1. Quality Targets

### Security

- Target: 0 known critical authorization bypasses in release build
- Current: Pass (unauthorized delete regression fixed)

### Reliability

- Target: Application startup succeeds with compatible DB schema
- Current: Pass (startup migration + connection checks verified)

### Data Integrity

- Target: Transaction state transitions remain valid and auditable
- Current: Pass (pending/approve/reject verified)

## 2. Service Level Objectives (Desktop + DB)

- SLO-01: App startup to login screen under normal environment <= 10 seconds
- SLO-02: DB connectivity check result deterministic at startup (success/fail prompt path)
- SLO-03: Critical workflows (search, inquiry, transaction decision) complete without unhandled exception

## 3. DORA-Inspired Delivery Metrics (Recommended)

Track from next release onward:

- Deployment frequency
- Lead time for change
- Change failure rate
- Mean time to recovery (MTTR)

## 4. Security and Supply Chain Metrics (Recommended)

- Vulnerable dependency count (critical/high)
- Time-to-patch for critical dependency issues
- Build provenance completeness (artifact/source mapping)
- SBOM availability for release artifact

## 5. Verification Checklist for Each Release

- [ ] Compile successful
- [ ] Startup smoke successful
- [ ] DB migration/compatibility logs clean
- [ ] Authorization regression checks pass
- [ ] Transaction workflow regression checks pass
- [ ] Documentation updated and versioned

## 6. Current Baseline Snapshot

- Compile: pass
- Startup + DB compatibility: pass
- Security regression: pass
- Transaction workflow: pass
- Delivery status: ready
