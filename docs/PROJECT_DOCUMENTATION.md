# EREMIS Full Project Documentation

## 1. Project Overview

EREMIS is a desktop enterprise real estate platform built with Java Swing and MySQL. It provides property operations, advanced search, inquiries, transaction-based purchase approval, notifications, and auditing.

Primary goals:

- Manage real estate listings and lifecycle from listing to sold.
- Enforce role-based behavior across admin, seller, buyer, agent, and analyst users.
- Support secure purchase transactions with approval workflow.
- Maintain traceability through logs and notifications.

## 2. Technology Stack

- Language: Java 11
- UI: Java Swing
- Database: MySQL 8+
- Build: Ant (`build.xml`) and direct `javac` workflow
- JDBC Driver: MySQL Connector/J (from `lib/`)

## 3. High-Level Architecture

The codebase uses layered architecture:

- Config Layer: DB and app configuration (`config`)
- Model Layer: Domain entities and enums (`model`, `model/enums`)
- DAO Layer: JDBC persistence (`dao`)
- Service Layer: Business rules and workflows (`service`)
- Controller Layer: UI-facing orchestration (`controller`)
- UI Layer: Swing views and dialogs (`ui`)
- Utils Layer: validation, session, theming, encryption, banks (`utils`)

Data flow:

UI -> Controller -> Service -> DAO -> MySQL

## 4. Project Structure

Top-level:

- `src/`: Java source code
- `sql/`: schema and sample seed data
- `resources/`: runtime config (`db.properties`) and assets
- `lib/`: external jars
- `build.xml`: Ant build definition
- `build/`, `dist/`: generated outputs

Main package root:

- `src/com/eremis/`

Important package groups:

- `config`: `AppConfig`, `DatabaseConfig`
- `controller`: auth, user, property, inquiry, analytics, notification, transaction
- `service`: business logic modules
- `dao`: persistence modules
- `model`: entities (`User`, `Property`, `Inquiry`, `Transaction`, etc.)
- `ui`: dashboard, search, property, transactions, notifications, login, settings, inquiry, user management
- `utils`: security and helper utilities

## 5. Core Features

### 5.1 Authentication and Session

- Login with role-aware navigation.
- Lockout metadata support (`login_attempts`, `locked_until`).
- Session stored through singleton `SessionManager`.

### 5.2 User and Role Management

Roles:

- ADMIN
- SELLER
- USER (buyer)
- AGENT
- ANALYST

Role behavior helpers are centralized in `UserRole` methods:

- `isAdminLike()`
- `isSellerLike()`
- `isBuyerLike()`

### 5.3 Property Management

- Create, update, delete, search listings.
- Property status lifecycle includes:
  - AVAILABLE
  - LOCKED
  - RESERVED
  - SOLD
- Ownership-aware edit restrictions enforced in service layer.
- Admin-only deletion enforced in service layer.

### 5.4 Advanced Search

- Keyword, city, price range, type, and status filters.
- Safe sort whitelisting to prevent injection.

### 5.5 Inquiry Management

- Create inquiry against a property.
- Track inquiry status (`PENDING`, `CONTACTED`, `CLOSED`).
- Optional assignment to admin/operator.

### 5.6 Transaction and Payment Workflow

- Buyer submits purchase with bank/account and amount.
- Transaction states:
  - PENDING
  - APPROVED
  - REJECTED
- Seller and buyer linked in each transaction.
- Property is LOCKED while pending.
- Admin approval marks property SOLD and transfers ownership.
- Admin rejection returns property to AVAILABLE and stores reason.

### 5.7 Notifications and Logging

- User-targeted notifications by type (`INFO`, `SUCCESS`, `WARNING`, `ERROR`).
- Audit logs for critical actions (property create/update/delete, transaction decisions).

### 5.8 UI and UX

- Themed Swing interface via `UIThemeManager`.
- Reusable components:
  - `RoundedButton`
  - `ModernTextField`
  - `StyledTable`
  - `StatCard`

## 6. Database Schema

Schema file:

- `sql/schema.sql`

Tables:

- `users`
- `properties`
- `property_images`
- `inquiries`
- `transactions`
- `search_history`
- `logs`
- `notifications`

Critical relations:

- `properties.listed_by -> users.id`
- `inquiries.property_id -> properties.id`
- `inquiries.user_id -> users.id`
- `transactions.buyer_id -> users.id`
- `transactions.seller_id -> users.id`
- `transactions.property_id -> properties.id`

Indexes are defined for common filters and workflows (status, user, city, property references, timeline fields).

## 7. Schema Compatibility and Migration Behavior

`DatabaseConfig` applies compatibility checks at startup:

- Ensures required user columns exist and are compatible.
- Ensures property status supports `LOCKED`.
- Ensures transaction table and indexes exist.
- Normalizes transaction rejection reason columns by standardizing to `rejection_reason`.

This enables smooth startup even when older databases are in use.

## 8. Security Notes

Implemented safeguards:

- Prepared statements in DAO layer.
- Role checks in service layer.
- Ownership checks for property updates.
- Admin-only checks for deletion.
- Encrypted account storage (`EncryptionUtil`).

Operational advice:

- Keep `resources/db.properties` restricted.
- Rotate DB credentials in production.
- Keep encryption key management outside source control in production setup.

## 9. Build and Run

### 9.1 Direct Java Build

```powershell
cd f:\Project\EREMIS_FIXED
javac -cp "lib/*" -d build/classes @(Get-ChildItem -Path src -Recurse -Filter *.java | ForEach-Object { $_.FullName })
java -cp "build/classes;lib/*" com.eremis.Main
```

### 9.2 Ant Build

```powershell
cd f:\Project\EREMIS_FIXED
ant compile
ant run
ant jar
```

JAR output:

- `dist/EREMIS.jar`

## 10. Configuration

Main DB config file:

- `resources/db.properties`

Key properties:

- `db.url`
- `db.username`
- `db.password`
- `db.driver`
- `db.pool.maxConnections`
- `db.pool.timeout`

Runtime behavior:

- App tests DB connection on startup.
- If DB auth fails, UI prompts for credentials and can persist them.

## 11. End-to-End Workflows

### 11.1 Property Purchase

1. Buyer selects available property.
2. Buyer enters payment data.
3. Transaction created as PENDING.
4. Property status becomes LOCKED.
5. Admin approves or rejects:
   - Approve: property SOLD, ownership transferred.
   - Reject: property AVAILABLE, reason recorded.

### 11.2 Inquiry

1. User opens inquiry dialog on property.
2. Subject/message stored in `inquiries`.
3. Inquiry tracked by status and optionally assignment.

## 12. Testing and Delivery Verification

Delivery verification performed on 2026-05-01 included:

- Full compile success.
- Startup smoke check success.
- DB pool and schema compatibility success.
- Security regression pass for unauthorized property deletion.
- Transaction flow regression pass.
- Schema normalization regression pass.

Release decision:

- Delivery-ready after release-hardening fixes and regression checks.

## 13. Known Non-Blocking Items to Improve Later

- Add automated JUnit regression suite integrated with CI.
- Add explicit release notes/versioning document per milestone.
- Add environment-based secret handling for encryption key and DB secrets.

## 14. Troubleshooting

### App cannot connect to database

- Ensure MySQL service is running.
- Verify `resources/db.properties` credentials.
- Confirm `eremis_db` exists and `sql/schema.sql` has been applied.

### Build errors due to missing jars

- Confirm MySQL driver jar exists in `lib/`.
- Re-run compile with classpath `lib/*`.

### Transaction workflow errors

- Verify `transactions` table has `seller_id` and `rejection_reason`.
- Start app once to allow compatibility migration to run.

## 15. Handover Checklist

- [x] Source code available
- [x] SQL schema available
- [x] Build instructions available
- [x] Runtime config documented
- [x] Feature and workflow documentation completed
- [x] Delivery verification completed

---

Prepared for delivery on 2026-05-01.
