# EREMIS

Enterprise Real Estate Management and Intelligence System (Java Swing + MySQL).

## What This Project Provides

- Role-based real estate management for admin, seller, buyer, agent, and analyst users.
- Property listing, search, status lifecycle, and ownership controls.
- Inquiry workflow for property communication.
- Transaction-based purchase workflow with admin approval/rejection.
- Notifications and audit logging.
- Built-in schema compatibility checks and migration logic during startup.

## Quick Start

1. Ensure MySQL is running.
2. Create schema:
   - Run `sql/schema.sql`
3. Seed optional sample data:
   - Run `sql/sample_data.sql`
4. Configure database credentials:
   - Update `resources/db.properties`
   - Keep `db.password` empty if you do not want it persisted; the app prompts for it at startup.
   - Set `app.payment.encryptionKey` to a strong random secret before using payment features.

## Email OTP Setup

The login screen supports two OTP email flows:

- Create Account: verifies the user's email before the account is created.
- Forgot Password: sends a password reset OTP before changing the password.

Configure SMTP safely using environment variables (recommended):

```powershell
setx EREMIS_MAIL_ENABLED true
setx EREMIS_MAIL_HOST smtp.gmail.com
setx EREMIS_MAIL_PORT 465
setx EREMIS_MAIL_SSL true
setx EREMIS_MAIL_STARTTLS false
setx EREMIS_MAIL_USERNAME your.email@gmail.com
setx EREMIS_MAIL_PASSWORD YOUR_16_CHAR_GMAIL_APP_PASSWORD
setx EREMIS_MAIL_FROM_NAME EREMIS
setx EREMIS_REQUIRE_REGISTRATION_OTP true
```

Important notes:
- Close and reopen VS Code/terminal after `setx` so variables are reloaded.
- Do not commit real secrets to source control.
- Keep `mail.username` and `mail.password` empty in `resources/db.properties` when using env vars.
- For port `587`, set `EREMIS_MAIL_SSL=false` and `EREMIS_MAIL_STARTTLS=true`.
- Registration email OTP is required by default. For an offline demo only, set `app.registration.requireEmailOtp=false` or `EREMIS_REQUIRE_REGISTRATION_OTP=false`.

## Build And Run

```powershell
cd "f:\Education\Software Engineering\OOP Practical\Project\EREMIS_FIXED"
javac -cp "lib/*" -d build/classes @(Get-ChildItem -Path src -Recurse -Filter *.java | ForEach-Object { $_.FullName })
java -cp "build/classes;lib/*" com.eremis.Main
```

## Build With Ant

```powershell
cd "f:\Education\Software Engineering\OOP Practical\Project\EREMIS_FIXED"
ant compile
ant run
ant jar
```

## Entry Point

- Main class: `com.eremis.Main`
- Source: `src/com/eremis/Main.java`

## Documentation

Full project documentation is available in:

- `docs/PROJECT_DOCUMENTATION.md`
- `docs/SE_2026_MASTER_DOCUMENTATION.md` (2026 software engineering model)
- `docs/quality/TRACEABILITY_MATRIX.md`
- `docs/quality/NFR_SLOS_AND_METRICS.md`
- `docs/adr/ADR-0001-layered-architecture.md`
- `docs/adr/ADR-0002-transaction-workflow-and-security-gates.md`

## Delivery Status

As of 2026-05-01, release-hardening checks were completed:

- Clean compile
- Startup and DB connectivity verified
- Security fix verified for unauthorized property deletion
- Transaction flow and schema compatibility verified

## License

Internal project delivery artifact. Add organizational license if needed.
