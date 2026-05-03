# EREMIS Software Engineering Documentation (2026 Model)

## Document Control

- Product: EREMIS (Enterprise Real Estate Management and Intelligence System)
- Version: 1.0.0 (Delivery baseline)
- Date: 2026-05-01
- Owners: Engineering, QA, Product, Operations
- Status: Approved for delivery handover

## 1. 2026 Documentation Model Used

This documentation follows a hybrid 2026 software engineering model that combines:

- ISO/IEC/IEEE 15288 lifecycle viewpoints (operations + maintenance focus)
- ISO/IEC/IEEE 42010 architecture description principles
- C4-style architecture communication (context, container, component)
- DevSecOps and supply-chain hardening practices (SBOM/SLSA-ready process)
- SRE operations readiness (SLOs, runbooks, incident workflow)
- Risk-first delivery governance with traceability from requirement to verification

## 2. Product Vision and Scope

EREMIS is a desktop platform for real estate operations, sales workflow, and management intelligence.

### 2.1 In Scope

- Identity and role-based access
- Property listing and search
- Inquiry workflow
- Payment-backed transaction workflow with admin decisions
- Notifications and audit logging
- Database compatibility migration at startup

### 2.2 Out of Scope (Current Release)

- Public web/mobile frontend
- Multi-tenant SaaS isolation
- Third-party payment gateway integration
- Distributed microservices deployment

## 3. Stakeholders and Responsibilities

- Product Owner: Business prioritization and acceptance
- Engineering Lead: Architecture and code quality
- QA Lead: Test strategy and sign-off
- Security Lead: Risk review and access controls
- Operations Lead: Runtime stability, backup, and incidents
- Support Team: User-facing troubleshooting and triage

## 4. System Context (C4 Level 1)

Actors:

- Admin
- Seller/Agent
- Buyer (User)
- Analyst

External Systems:

- MySQL database
- SMTP (optional for email service)

System Boundary:

- Desktop Java Swing application with local configuration and direct DB access

## 5. Container View (C4 Level 2)

Container A: Desktop Client (Java Swing)

- UI modules in `src/com/eremis/ui`
- Controllers in `src/com/eremis/controller`
- Services in `src/com/eremis/service`
- DAO in `src/com/eremis/dao`

Container B: MySQL Database

- Schema from `sql/schema.sql`
- Tables for users, properties, inquiries, transactions, notifications, logs, and search history

## 6. Component and Module View (C4 Level 3)

### 6.1 Presentation Layer

- Login, Dashboard, Search, Property, Inquiry, Notifications, Settings, User panels
- Transaction dialogs and admin transaction panel

### 6.2 Application Layer (Controllers)

- `AuthController`, `PropertyController`, `InquiryController`, `TransactionController`, etc.

### 6.3 Domain + Business Layer (Services)

- Authentication, property lifecycle, inquiry processing, transaction state transitions
- Logging and notification side effects

### 6.4 Data Layer (DAOs)

- JDBC DAOs with prepared statements and domain mapping

### 6.5 Cross-Cutting Utilities

- Session management
- Input validation
- Encryption utility
- Bank registry
- Theme management

## 7. Requirements Baseline

### 7.1 Functional Requirements

- FR-01: User authentication and session lifecycle
- FR-02: Role-aware navigation and action authorization
- FR-03: Create/update/delete property lifecycle
- FR-04: Advanced property search and filters
- FR-05: Inquiry creation and tracking
- FR-06: Buyer transaction initiation with payment metadata
- FR-07: Admin approve/reject transaction actions
- FR-08: Notifications and audit logs for key actions
- FR-09: Startup schema compatibility checks

### 7.2 Non-Functional Requirements

- NFR-01: Access control enforcement at service layer
- NFR-02: SQL injection resistance via prepared statements
- NFR-03: Startup reliability with DB compatibility migration
- NFR-04: Response usability for desktop interaction
- NFR-05: Maintainable layered architecture

## 8. Data Architecture and Governance

Schema source:

- `sql/schema.sql`

Key entities:

- `users`
- `properties`
- `transactions`
- `inquiries`
- `notifications`
- `logs`

### 8.1 Transaction Data Rules

- Property is set `LOCKED` for pending purchase
- Approval sets property to `SOLD` and transfers ownership
- Rejection returns property to `AVAILABLE` with rejection reason
- Canonical reason field is `rejection_reason`

### 8.2 Data Protection

- Account metadata stored in encrypted form by application utility
- Credentials are externalized in `resources/db.properties`

## 9. Security Architecture (Zero-Trust-Inspired App Controls)

- Enforce authorization in service layer (not UI-only)
- Restrict destructive operations to admin-like roles
- Prevent horizontal privilege abuse (ownership checks)
- Audit critical actions via logging service
- Prefer secure defaults in status transitions and validations

## 10. Quality Engineering Strategy (2026)

### 10.1 Test Layers

- Static/compile verification
- Service-level functional regression tests
- DB migration and startup smoke tests
- Manual UI acceptance for key role workflows

### 10.2 Risk-Based Testing Focus

- Authorization bypasses
- Transaction status consistency
- Data migration safety
- Rejection/approval side effects

### 10.3 Release Gates

- Gate 1: Compile clean
- Gate 2: Startup + DB compatibility clean
- Gate 3: Critical flows pass (search, inquiry, transaction)
- Gate 4: Security regressions pass (unauthorized delete blocked)

## 11. DevSecOps and Delivery Process

Current delivery process:

- Local compile and run verification
- Schema setup and compatibility migration
- Functional regression checks
- Documentation update and handover package

Recommended near-term upgrades:

- CI pipeline for compile + test + packaging
- SBOM generation on build artifact
- Automated DB migration smoke environment
- Signed release artifacts

## 12. SRE and Operations Readiness

### 12.1 Operational Checks

- DB service available before app launch
- Resource files present (`db.properties`, assets)
- Startup logs reviewed for schema compatibility outcomes

### 12.2 Monitoring Signals (Current)

- Application logs
- Audit logs in DB table `logs`

### 12.3 Incident Handling

- Detect issue from runtime/logs
- Triage severity (security/data-loss/functionality)
- Mitigate via rollback or hotfix
- Record root-cause analysis and corrective action

## 13. Release Notes (Current Baseline)

Included hardening updates:

- Unauthorized property deletion blocked at service layer
- Direct buy bypass removed from property controller/service path
- Transaction reason column normalized to canonical field
- Delivery readiness regression checks passed

## 14. Verification Summary

Delivery verification completed:

- Full compile: pass
- App startup + DB connection: pass
- Schema compatibility execution: pass
- Security regression for delete authorization: pass
- Transaction flow regression: pass

Release status:

- READY_FOR_DELIVERY = true

## 15. Risks and Mitigations

- Risk: Manual deployment mistakes
  - Mitigation: Use setup checklist and scripted commands
- Risk: Secret leakage in plain local config
  - Mitigation: environment variable overrides in controlled deployments
- Risk: Regression on authorization rules
  - Mitigation: preserve service-layer checks and add automated tests

## 16. Backlog and Evolution Plan (Post-Delivery)

- Introduce CI/CD quality gates
- Add formal unit/integration test suite
- Add automated test data management
- Add deployment profiles (dev/staging/prod)
- Add observability enhancements and alerting integration

## 17. Handover Checklist

- [x] Architecture documentation
- [x] Requirements baseline
- [x] Security and risk documentation
- [x] Testing and quality strategy
- [x] Operations run model
- [x] Release verification and status
- [x] Maintenance and roadmap notes

## 18. Linked Documents

- Existing system documentation: `docs/PROJECT_DOCUMENTATION.md`
- Quality traceability: `docs/quality/TRACEABILITY_MATRIX.md`
- NFR/SLO metrics: `docs/quality/NFR_SLOS_AND_METRICS.md`
- Architecture decisions: `docs/adr/ADR-0001-layered-architecture.md`, `docs/adr/ADR-0002-transaction-workflow-and-security-gates.md`
