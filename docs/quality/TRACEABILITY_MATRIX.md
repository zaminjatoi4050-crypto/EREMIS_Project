# Requirements Traceability Matrix (RTM)

| Req ID | Requirement | Primary Implementation | Verification Evidence | Status |
|---|---|---|---|---|
| FR-01 | Authentication + session | `service/AuthService.java`, `utils/SessionManager.java` | Startup/login runtime checks | Implemented |
| FR-02 | Role-based authorization | `model/enums/UserRole.java`, service-layer guards | Regression checks for restricted actions | Implemented |
| FR-03 | Property lifecycle CRUD | `service/PropertyService.java`, `dao/PropertyDAO.java` | CRUD/service tests | Implemented |
| FR-04 | Advanced search | `dao/PropertyDAO.java` search method | Search flow validation | Implemented |
| FR-05 | Inquiry workflow | `dao/InquiryDAO.java`, `service/InquiryService.java`, inquiry UI | Inquiry create/delete checks | Implemented |
| FR-06 | Buyer transaction initiation | `service/TransactionService.java` initiatePurchase | Pending transaction checks | Implemented |
| FR-07 | Admin approve/reject | `service/TransactionService.java` approve/reject | Approval/rejection regression checks | Implemented |
| FR-08 | Notifications + audit | `service/NotificationService.java`, `service/LoggingService.java` | Runtime log evidence | Implemented |
| FR-09 | Schema compatibility | `config/DatabaseConfig.java` compatibility methods | Startup migration logs | Implemented |
| NFR-01 | Service-layer auth enforcement | `service/PropertyService.java` delete/update restrictions | Unauthorized delete blocked | Implemented |
| NFR-02 | SQL injection resistance | DAO prepared statements | Code review + runtime stability | Implemented |
| NFR-03 | Startup reliability | `Main.java` DB check + compatibility migration | Startup smoke pass | Implemented |
| NFR-04 | Secure transaction state | `TransactionService` state machine | Flow tests pass | Implemented |

## Notes

- Canonical transaction rejection field is `rejection_reason`.
- Legacy duplicate field migration is handled by startup compatibility checks.
