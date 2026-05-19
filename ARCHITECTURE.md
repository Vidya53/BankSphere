# BankSphere — Architecture & Project Guide

A production-shaped banking platform built on **Spring Boot 3.4.1 / Java 21**
(backend microservices) and **React 18 / Vite / Tailwind** (customer + staff
web app). Every service registers with **Eureka**, fetches its configuration
from a central **Config Server**, and is reached by clients exclusively through
the **API Gateway**. Async fan-out (audit log + non-OTP notifications) runs on
**Kafka**.

This document is the single entry point for someone joining the project. It
describes what each piece does, where the code lives, how a request travels
end-to-end, and how to run / test / extend everything.

---

## 1. Service inventory

| Service                  | Port | Role |
|--------------------------|------|------|
| `config-server`          | 8888 | Centralised configuration (filesystem-backed in dev, git-backed in prod), hot-reloadable via `/actuator/refresh` |
| `eureka-server`          | 8761 | Service-discovery registry. Open the dashboard at `http://localhost:8761` |
| `api-gateway`            | 8090 | Public entry point. JWT validation, rate limiting, RBAC, circuit-breaker routing, header injection |
| `identity-services`      | 8084 | Signup / login / JWT issuance / refresh-token rotation / forgot-password OTP (direct SMTP) / admin staff CRUD |
| `customer-services`      | 8081 | Customer profiles, KYC, CSR dashboard, support tickets, loan-eligibility evaluation |
| `Account-service`        | 8083 | Account applications, account lifecycle, transfers (with high-value approval queue), cash-counter ops, transaction PIN |
| `Transaction-service`    | 8082 | The immutable transaction ledger — initiate / cancel / reverse / mark-success / mark-failed |
| `loan-service`           | 8085 | Loan applications, decision (approve / reject), disbursement, EMI schedule, prepayment, foreclosure |
| `branch-service`         | 8086 | Branches, employees, operating hours, branch-manager dashboard |
| `notification-service`   | 8088 | Kafka consumer that renders email / SMS / push notifications. Retries with exponential backoff to a DLT |
| `audit-compliance-service` | 8087 | Kafka consumer that persists every action to an append-only audit log; powers staff analytics |

The React app runs on **`http://localhost:5173`** (Vite dev server).

---

## 2. Repository layout

```
BankSphere/
├── ARCHITECTURE.md                  ← this file
├── docker-compose-kafka.yml         ← Kafka + Zookeeper + Kafka-UI + topic seeding
├── .gitignore                       ← excludes target/, _logs/, logs/, .idea/, node_modules/, dist/
│
├── eureka-server/                   ← Spring Cloud Netflix Eureka
├── config-server/                   ← Spring Cloud Config Server
│   └── src/main/resources/config-repo/   ← the actual YAML configs each service pulls
│
├── api-gateway/                     ← Spring Cloud Gateway (reactive / WebFlux)
├── identity-services/               ← Auth, users, tokens
├── customer-services/               ← Customer + KYC
├── Account-service/                 ← Accounts + transfers + PIN
├── Transaction-service/             ← Transaction ledger
├── loan-service/                    ← Loans + EMIs
├── branch-service/                  ← Branches + employees
├── notification-service/            ← Email / SMS / push dispatcher
├── audit-compliance-service/        ← Audit log + analytics
│
└── frontend/                        ← React 18 + Vite + Tailwind
    ├── index.html
    ├── package.json
    ├── vite.config.js
    ├── tailwind.config.js
    └── src/
        ├── main.jsx                 ← entry point
        ├── App.jsx                  ← route table
        ├── index.css
        ├── api/                     ← axios client + per-domain API objects
        ├── components/              ← reusable UI (common / layout / dashboard / public / staff)
        ├── context/                 ← AuthContext, ThemeContext
        ├── data/                    ← static marketing content
        ├── hooks/                   ← useAsync
        ├── pages/                   ← route components (auth / public / customer / staff / shared)
        └── utils/                   ← format, jwt decoder, role routing
```

Each backend service follows the same Maven module layout:

```
<service>/
├── pom.xml
└── src/
    ├── main/
    │   ├── java/com/cts/<service-pkg>/
    │   │   ├── <Service>Application.java       ← @SpringBootApplication entry point
    │   │   ├── config/                         ← Spring config (Feign, OpenAPI, Jackson, Kafka)
    │   │   ├── controller/                     ← REST endpoints (annotated with @Operation, @PreAuthorize)
    │   │   ├── client/                         ← Feign clients + fallbacks for inter-service calls
    │   │   ├── dto/                            ← Request / response DTOs with Jakarta validation
    │   │   ├── entity/                         ← JPA entities
    │   │   ├── enums/                          ← Domain enums (statuses, types)
    │   │   ├── exception/                      ← Custom exceptions + GlobalExceptionHandler
    │   │   ├── mapper/                         ← Entity ↔ DTO mappers
    │   │   ├── repository/                     ← Spring Data JPA repositories
    │   │   ├── security/                       ← CorrelationIdFilter, HeaderAuthenticationFilter, SecurityConfig
    │   │   └── service/                        ← Business logic (interfaces + impl/)
    │   └── resources/
    │       └── application.yaml                ← bootstrap config (config-server import)
    └── test/
        └── java/com/cts/<service-pkg>/
            ├── <Service>ApplicationTests.java  ← @Disabled — needs MySQL/Kafka/Eureka/Config
            ├── controller/                     ← MockMvc tests
            ├── exception/                      ← GlobalExceptionHandler tests
            └── service/impl/                   ← Pure Mockito unit tests
```

---

## 3. Request lifecycle

```
Client (React app, Postman, curl)
  │
  ▼
API Gateway (:8090)
  │  1. CorsWebFilter ............. CORS preflight bypasses everything else
  │  2. RateLimitingFilter ........ 100 req/min per client IP, in-memory token bucket
  │  3. JwtAuthenticationFilter ... order=-1
  │      a) Block /api/v1/internal/** outright (403)
  │      b) Public paths (/auth/signup, /auth/login, /auth/refresh, /auth/logout,
  │         /auth/forgot-password, /auth/verify-otp, /auth/reset-password,
  │         /actuator/health, /actuator/info) → pass through with X-Correlation-ID
  │      c) Read Authorization: Bearer <jwt>, validate signature + expiry
  │         - EXPIRED → 401 TOKEN_EXPIRED (client calls POST /auth/refresh)
  │         - INVALID → 401 TOKEN_INVALID (client re-logs in)
  │      d) Coarse-grained RBAC via RouteAuthorizationConfig (path × role)
  │      e) Strip Authorization header, inject:
  │            X-User-Id, X-Role, X-Branch-Code, X-Email,
  │            X-Customer-Name, X-Correlation-ID
  │  4. Spring Cloud Gateway routes via lb://<service-name> with circuit breaker
  │      Fallback (Resilience4j open / time-limit) → forward:/fallback/<service>
  │
  ▼
Downstream Service (e.g. Account-service :8083)
  │  1. CorrelationIdFilter (highest precedence)
  │      → MDC[correlationId, userId] for log correlation, echoed on response
  │  2. HeaderAuthenticationFilter
  │      → reads X-User-Id + X-Role from request, builds a
  │        UsernamePasswordAuthenticationToken with ROLE_<role> authority,
  │        sets SecurityContextHolder
  │  3. Spring Security: requestMatchers
  │      - /api/v1/internal/**  → authenticated (so Feign calls pass)
  │      - /actuator/**         → permitAll
  │      - /swagger-ui/**, /v3/api-docs/** → permitAll
  │      - anyRequest()         → authenticated
  │  4. @PreAuthorize on controller / service methods (fine-grained RBAC)
  │  5. UserContextExtractor (Account-service only — others read X-User-Id directly)
  │      → throws MissingGatewayHeaderException if X-User-Id missing
  │
  ▼
Service layer → Repository → MySQL
                     │
                     └─→ Feign call to peer service (via Eureka, lb://)
                           │
                           └─→ GlobalFeignConfig RequestInterceptor propagates
                               X-Correlation-ID, X-User-Id, X-Role, X-Branch-Code
                     │
                     └─→ Kafka publish (audit + notification topics, async)
```

---

## 4. Authentication & authorization

### Access token (JWT)

| Property | Value |
|---|---|
| Issued by | `identity-services` |
| Validated by | `api-gateway` only (services trust the gateway-injected headers) |
| Lifetime | **15 minutes** (`JWT_ACCESS_TOKEN_EXPIRATION_MS = 900000`) |
| Claims | `sub` = userId, `email`, `role`, `fullName`, `branchCode` |
| Algorithm | HMAC-SHA256 with a shared `JWT_SECRET` |

When the gateway sees `EXPIRED` it returns `401 TOKEN_EXPIRED` so the React
client triggers `POST /auth/refresh`. `INVALID` means tampered / wrong key →
`401 TOKEN_INVALID` and the client re-logs in.

### Refresh token

| Property | Value |
|---|---|
| Lifetime | **7 days** (`JWT_REFRESH_TOKEN_EXPIRATION_MS = 604800000`) |
| Storage | SHA-256 hex digest in `refresh_tokens` table (raw token never persisted) |
| Format | 32 random bytes, Base64 URL-encoded |
| Rotation | Single-use — presenting a valid token revokes it and issues a fresh pair |
| Reuse detection | Presenting an already-revoked token → **all** sessions for the user are terminated as a theft signal |
| Cleanup | `@Scheduled(cron = "0 0 3 * * ?")` daily at 03:00 UTC, deletes expired + stale-revoked rows |

### "Remember me"

The React app stores tokens in either `localStorage` (ticked) or
`sessionStorage` (unticked). The choice is recorded under `bs.rememberMe` so
the page-load bootstrap reads from the right store. See
[`frontend/src/api/tokenStore.js`](frontend/src/api/tokenStore.js).

### Auth endpoints

| Endpoint | Auth | Description |
|---|---|---|
| `POST /auth/signup` | none | Self-service customer registration (issues a token pair) |
| `POST /auth/login` | none | Returns 15-min access + 7-day refresh tokens |
| `POST /auth/refresh` | refresh token as credential | Rotate the token pair |
| `POST /auth/logout` | refresh token as credential | Revoke this single session |
| `POST /auth/logout-all` | JWT | Revoke every refresh token for the caller |
| `POST /auth/forgot-password` | none | Send a 6-digit OTP. **404 if email isn't registered** (direct SMTP via JavaMailSender — does NOT use Kafka) |
| `POST /auth/verify-otp` | none | Exchange OTP for a single-use reset token (15-min) |
| `POST /auth/reset-password` | none | Set the new password using the reset token |
| `POST /api/v1/admin/staff` | JWT (ADMIN) | Create a staff user |

### Roles

`CUSTOMER` · `CSR` · `LOAN_OFFICER` · `BRANCH_MANAGER` · `ADMIN`

Each role can access a defined set of route patterns, defined in
[`api-gateway/.../filter/RouteAuthorizationConfig.java`](api-gateway/src/main/java/com/cts/apigateway/filter/RouteAuthorizationConfig.java).
Fine-grained per-method checks live on each controller via `@PreAuthorize`.

---

## 5. Per-service file map

This section names every Java package in each service and what role it plays.
Useful when you're hunting for "where do I add my new endpoint" or "where is
this validation enforced".

### 5.1 `api-gateway`

WebFlux-based Spring Cloud Gateway. No JPA, no database — it's a stateless
edge layer.

| Package / file | Purpose |
|---|---|
| `ApiGatewayApplication.java` | `@SpringBootApplication` entry point |
| `config/GatewaySecurityConfig.java` | WebFlux security: disables Spring Security defaults (we authenticate manually), wires CORS |
| `filter/JwtAuthenticationFilter.java` | Global filter @ order -1: validates JWT, injects user-context headers, strips `Authorization` |
| `filter/GatewayJwtUtil.java` | JJWT-backed token parser; returns `TokenStatus.VALID / EXPIRED / INVALID` |
| `filter/RateLimitingFilter.java` | Global filter @ order -2: 100 req/min per IP, in-memory bucket. **Single-node only** — production needs Redis-backed |
| `filter/RouteAuthorizationConfig.java` | Path × role allow-list. `isPublic(path)` and `isAuthorized(path, role)` |
| `controller/FallbackController.java` | `/fallback/{service}` returns a 503 envelope when a circuit opens |

Routes themselves live in `config-server/src/main/resources/config-repo/api-gateway.yaml`.

### 5.2 `identity-services`

| Package / file | Purpose |
|---|---|
| `IdentityServicesApplication.java` | Entry point. `@EnableScheduling`, `@EnableAsync`, `@EnableConfigurationProperties` |
| `config/AdminBootstrap.java` | `CommandLineRunner` — seeds a default ADMIN user on first startup |
| `config/CorrelationIdFilter.java` | Identical pattern to other services |
| `config/OpenApiConfig.java` | Swagger metadata for `/swagger-ui.html` on port 8084 |
| `config/SecurityConfig.java` | Provides `BCryptPasswordEncoder`. No Spring Security filter chain — every endpoint is public from a Spring perspective (the gateway is the boundary) |
| `controller/AuthController.java` | `/auth/signup`, `/login`, `/refresh`, `/logout`, `/logout-all` |
| `controller/AdminController.java` | `/api/v1/admin/staff` — ADMIN-only staff CRUD with filtering, pagination, status updates |
| `controller/PasswordController.java` | `/api/auth/change-password` — authenticated self-service |
| `controller/PasswordResetController.java` | `/auth/forgot-password`, `/verify-otp`, `/reset-password` (the OTP path) |
| `dto/` | `SignupRequest`, `LoginRequest`, `TokenRefreshRequest`, `ForgotPasswordRequest`, `VerifyOtpRequest`, `ResetPasswordRequest`, `StaffSignupRequest`, `StaffStatusRequest` — all with strict Jakarta validation |
| `dto/response/` | `AuthResponse`, `TokenRefreshResponse`, `StaffResponse`, `ApiResponse<T>` |
| `entity/User.java` | Login record. `email` + `phoneNumber` unique. `role`, `status`, `branchCode`, `lastLogin` |
| `entity/RefreshToken.java` | Hashed token store with expiry, IP / user-agent tracking |
| `entity/PasswordResetToken.java` | OTP hash + reset-token hash, attempts counter, used flag |
| `entity/Role.java`, `entity/Status.java` | Enums |
| `exception/GlobalExceptionHandler.java` | Maps every custom exception (`UserNotFound`, `InvalidCredentials`, `RefreshToken`, etc.) to a clean `ApiResponse` envelope |
| `repository/UserRepository.java` | Includes paginated staff queries, `countByRole`, `findStaffByBranch` |
| `repository/RefreshTokenRepository.java` | Bulk revoke + stale-token cleanup queries |
| `repository/PasswordResetTokenRepository.java` | Latest-unused lookup by email |
| `security/JwtProperties.java` | `@ConfigurationProperties("jwt")` — secret + expirations |
| `security/JwtService.java` | Generates access tokens, parses claims, signs with HMAC-SHA256 |
| `service/AuthService.java`, `impl/AuthServiceImpl.java` | Signup, login (updates `lastLogin`), refresh, logout, logoutAll, createStaffUser |
| `service/RefreshTokenService.java`, `impl/RefreshTokenServiceImpl.java` | Create / verify-and-rotate / revoke / cleanup, with reuse-detection |
| `service/PasswordResetService.java` | OTP request (404 on unknown email), verify, reset. Direct `PasswordResetMailService` — bypasses Kafka |
| `service/PasswordResetMailService.java` | `@Async` SMTP email. Falls back to logging the OTP in dev if SMTP isn't configured |

### 5.3 `customer-services`

| Package / file | Purpose |
|---|---|
| `CustomerServicesApplication.java` | Entry point |
| `client/BranchClient.java` (+ fallback) | Calls `branch-service` `/api/v1/internal/branches/{code}/active` to validate branches on customer registration |
| `client/AccountServiceClient.java` (+ fallback) | Calls `account-service` to cascade-close accounts when a customer is soft-deleted |
| `config/GlobalFeignConfig.java` | `RequestInterceptor` that propagates `X-Correlation-ID`, `X-User-Id`, `X-Role`, `X-Branch-Code` outbound |
| `config/OpenApiConfig.java` | Swagger on port 8081 |
| `controller/CustomerController.java` | `/customers/me`, `/customers/{customerNo}`, status transitions, soft-delete, listing |
| `controller/KycController.java` | `/customers/{customerNo}/kyc` (submit, approve, reject), `/kyc/pending` |
| `controller/CsrDashboardController.java` | `/api/csr/dashboard`, `/api/csr/customers/search` |
| `controller/SupportTicketController.java` | `/api/support/tickets` — in-memory ticket store with sequence IDs |
| `controller/InternalCustomerController.java` | `/api/v1/internal/customers/{userId}/{kyc-approved\|active\|activate}` for inter-service calls |
| `controller/InternalStatsController.java` | `/api/v1/internal/stats/customers` for the analytics dashboards |
| `dto/CustomerRequestDTO.java`, `KycRequestDTO.java`, `LoanApplicationRequest.java` | Strictly validated (regex for Aadhaar / PAN / Passport / Voter ID, person-name pattern, Indian mobile, Indian PIN code) |
| `entity/Customer.java`, `entity/Kyc.java` | JPA entities with rich constraints + indexes |
| `enums/` | `CustomerStatus`, `KycStatus`, `DocumentType`, `Gender`, `RiskCategory` |
| `exception/GlobalExceptionHandler.java` | Maps `CustomerAlreadyExistsException`, `DuplicateKycException`, `BusinessException`, `KycNotVerifiedException`, etc. |
| `mapper/CustomerMapper.java`, `KycMapper.java` | DTO ↔ entity translation |
| `service/CustomerService.java`, `impl/CustomerServiceImpl.java` | Registration, status transitions, soft-delete with cascade, loan-eligibility evaluation |
| `service/KycService.java`, `impl/KycServiceImpl.java` | Submit / approve / reject with state validation |
| `util/BusinessConstants.java` | Document-number regexes, age limits, loan caps, valid status transitions |

### 5.4 `Account-service`

Cleaned up in May 2026 — `security/` now only has the three standard files.

| Package / file | Purpose |
|---|---|
| `AccountServiceApplication.java` | Entry point with `@EnableFeignClients` |
| `audit/AuditEventMessage.java`, `NotificationEventMessage.java` | Kafka record schemas (must match consumer-side classes) |
| `audit/KafkaAuditProducerConfig.java` | Two `KafkaTemplate`s with idempotent producer config |
| `client/CustomerServiceClient.java`, `BranchServiceClient.java`, `TransactionServiceClient.java` (+ fallbacks) | Feign clients with `@FeignClient(fallback = ...)` |
| `client/dto/KycApiResponse.java`, `TransactionRecordRequest.java` | Wire-format DTOs for Feign |
| `config/GlobalFeignConfig.java`, `OpenApiConfig.java` | Header propagation, Swagger metadata |
| `context/UserContext.java` | Lombok-generated value carrier (userId, username, role, branchCode, customerName, email, phone) |
| `context/UserContextExtractor.java` | Reads the gateway headers; throws `MissingGatewayHeaderException` if `X-User-Id` or `X-Role` is missing |
| `controller/AccountController.java` | `/api/v1/accounts/me`, `/api/v1/accounts/{accountNo}`, statement-notify |
| `controller/AccountApplicationController.java` | Customer: `/api/v1/account-applications/{me, /, /{id}}` |
| `controller/StaffAccountController.java` | `/api/v1/staff/accounts/{my-branch, by-branch, /{id}/freeze, /unfreeze, /close}` |
| `controller/StaffAccountApplicationController.java` | `/api/v1/staff/account-applications/{pending, all, approve, reject}` |
| `controller/StaffCashController.java` | `/api/v1/staff/cash/{deposit, withdrawal}` — counter-cash operations |
| `controller/StaffPendingTransferController.java` | `/api/v1/staff/pending-transfers/{list, approve, reject}` — high-value transfer approval queue |
| `controller/TransferController.java` | `/api/v1/transfers` — customer-initiated transfers (auto-routes to pending if > ₹1L) |
| `controller/PinController.java` | `/api/v1/accounts/{accountNo}/pin/{status, set, change}` |
| `controller/InternalAccountController.java` | `/api/v1/internal/accounts/**` — credit / debit / debit-with-pin for inter-service calls |
| `controller/InternalStatsController.java` | `/api/v1/internal/stats/accounts` and `/by-branch/{branchCode}` |
| `dto/request/` | `AccountApplicationRequest`, `CloseAccountRequest`, `FreezeAccountRequest`, `RejectRequest` |
| `dto/response/` | `AccountResponse`, `AccountApplicationResponse`, `ApiResponse<T>`, `BranchAccountSummary`, `BranchAccountTypeBreakdown` |
| `entity/Account.java` | Balance, status, branch, IFSC, PIN hash, daily limits — with composite branch+status index |
| `entity/AccountApplication.java` | The review queue row |
| `entity/PendingTransfer.java` | High-value transfer awaiting CSR approval (status `PENDING_APPROVAL / APPROVED / REJECTED / CANCELLED`) |
| `enums/` | `AccountStatus`, `AccountType`, `ApplicationStatus` |
| `exception/MissingGatewayHeaderException.java`, `GlobalExceptionHandler.java`, + business exceptions | 401 / 403 / 409 / 422 / 500 mappings + structured envelope |
| `mapper/AccountMapper.java` | Static toApplicationResponse / toAccountResponse |
| `repository/AccountRepository.java` | Atomic `creditAccount`, `debitAccount`, `debitRespectingMinimum`, PIN lockout queries, analytics aggregations |
| `repository/AccountApplicationRepository.java`, `PendingTransferRepository.java` | Branch-scoped queries + pending-sum-since-today for daily-limit enforcement |
| `security/CorrelationIdFilter.java`, `HeaderAuthenticationFilter.java`, `SecurityConfig.java` | The three standard files. Same shape as every other service |
| `service/AccountApplicationService.java`, `impl/AccountApplicationServiceImpl.java` | Apply → review → approve (creates the account, auto-activates the customer) / reject |
| `service/AccountService.java`, `impl/AccountServiceImpl.java` | Read, freeze, unfreeze, close, cascade-close-all |
| `service/TransferService.java` | Validate → PIN check → fund check → daily-limit check → route to immediate execution or pending queue |
| `service/CashService.java` | Counter deposit / withdrawal |
| `service/PinService.java` | Set / change / verify with BCrypt + 5-attempt lock-out for 15 minutes |
| `service/AuditService.java`, `impl/AuditServiceImpl.java` | Publishes to `banking.audit.events` Kafka topic |
| `service/NotificationService.java`, `impl/NotificationServiceImpl.java` | Publishes to `banking.notification.events` Kafka topic |
| `service/BranchValidationService.java`, `KycVerificationService.java` | Thin wrappers around the Feign clients |

### 5.5 `Transaction-service`

| Package / file | Purpose |
|---|---|
| `TransactionServiceApplication.java` | Entry point |
| `config/JacksonConfig.java`, `OpenApiConfig.java` | JSR-310 dates, Swagger metadata |
| `constants/TransactionConstants.java` | Reference-number prefix, daily limits, page caps, masking |
| `controller/TransactionController.java` | All `/api/v1/transactions/**` endpoints + analytics queries |
| `controller/InternalStatsController.java` | `/api/v1/internal/stats/transactions` |
| `dto/request/`, `dto/response/` | `TransactionRequestDto`, `Cancel/ReverseTransactionRequestDto`, `TransactionResponseDto`, `ApiResponse<T>` |
| `model/entity/Transaction.java` | Strong indexes on senderAccount, receiverAccount, status, createdAt; `@PrePersist` defaults; optimistic locking via `@Version` |
| `model/enums/` | `TransactionStatus`, `TransactionType`, `TransactionChannel` |
| `exception/GlobalExceptionHandler.java` + custom exceptions | 404 / 409 / 422 / 503 / 500 mappings |
| `mapper/TransactionMapper.java`, `util/TransactionUtils.java` | Reference-number generator, masking, monetary rounding, idempotency-key construction |
| `repository/TransactionRepository.java` | Account + status + range queries, analytics aggregates |
| `security/CorrelationIdFilter.java`, `HeaderAuthenticationFilter.java`, `SecurityConfig.java` | Standard 3 |
| `service/TransactionService.java`, `impl/TransactionServiceImpl.java` | Initiate (idempotency-key dedup, daily-limit + count check), cancel, reverse (creates linked REVERSAL), markSuccess, markFailed |

### 5.6 `loan-service`

| Package / file | Purpose |
|---|---|
| `LoanServiceApplication.java` | Entry point |
| `client/AccountClient.java`, `CustomerClient.java` (+ fallbacks) | Feign clients for active-checks and PIN-verified debits |
| `client/dto/AccountActiveResponse.java`, `CustomerApiResponse.java` | Wire-format DTOs |
| `config/FeignClientConfig.java`, `GlobalFeignConfig.java`, `OpenApiConfig.java` | Feign error decoder, header propagation, Swagger |
| `controller/LoanController.java` | All `/loans/**` endpoints — apply, decide, disburse, payEmi, schedule, summary, prepay, payments |
| `controller/LoanOfficerDashboardController.java` | `/api/loan-officer/dashboard` |
| `controller/InternalStatsController.java` | `/api/v1/internal/stats/loans*` |
| `dto/request/` | `LoanApplyRequest`, `LoanDecisionRequest`, `EmiPaymentRequest`, `PrepaymentRequest`, `EligibilityCheckRequest` |
| `dto/response/` | `LoanResponse`, `EmiScheduleResponse`, `LoanSummaryResponse`, `PaymentHistoryResponse`, `PrepaymentResponse`, `EligibilityResponse` |
| `entity/Loan.java`, `EmiPayment.java` | Loan state machine + payment ledger |
| `entity/LoanStatus.java`, `LoanType.java` | Enums |
| `exception/` | `BusinessException`, `ValidationException`, `ResourceNotFoundException` + `GlobalExceptionHandler` |
| `repository/LoanRepository.java`, `EmiPaymentRepository.java` | Customer + status queries, outstanding aggregations |
| `security/CorrelationIdFilter.java`, `HeaderAuthenticationFilter.java`, `SecurityConfig.java` | Standard 3 |
| `service/LoanService.java`, `impl/LoanServiceImpl.java` | Apply → decide (EMI math) → disburse (debit customer-service account) → payEmi (with late-payment penalty 2%) → prepay (full = 2% foreclosure charge; partial = EMI recalculated) → close on full payment |
| `util/ApiResponse.java` | Service-local envelope (`message` + `errors` map shape) |

### 5.7 `branch-service`

| Package / file | Purpose |
|---|---|
| `BranchServiceApplication.java` | Entry point |
| `client/AccountStatsClient.java`, `LoanStatsClient.java`, `IdentityStaffClient.java` (+ fallbacks) | Feign clients to aggregate the branch-manager dashboard |
| `config/GlobalFeignConfig.java` | Header propagation |
| `controller/BranchController.java`, `EmployeeController.java`, `BranchManagerDashboardController.java`, `InternalBranchController.java` | CRUD, listing, manager assignment, operating hours, internal validity checks |
| `dto/` | Request + response DTOs (with nested address / contact / operating-hours validation) |
| `entity/` | `Branch` + `BranchAddress` + `BranchContact` + `BranchOperatingHours` + `Employee` |
| `enums/` | `BranchStatus`, `BranchType`, `Department`, `Designation`, `EmployeeStatus` |
| `mapper/BranchMapper.java`, `EmployeeMapper.java` | DTO ↔ entity, includes auto-generated employee codes |
| `repository/` | Custom queries (search, by-state, by-city, today's hours) |
| `service/BranchService.java`, `EmployeeService.java`, + impl/ | Branch + employee CRUD with state validation, manager assignment, transfer |

### 5.8 `notification-service`

Kafka consumer. The only place email / SMS / push templates render — except
for the forgot-password OTP, which `identity-services` sends directly via
SMTP.

| Package / file | Purpose |
|---|---|
| `NotificationServiceApplication.java` | Entry point with `@EnableScheduling` |
| `config/KafkaConfig.java` | Consumer factory, listener-container, DLT recoverer (1 s → 2 s → 4 s backoff → DLT) |
| `config/MailConfig.java` | Standalone Thymeleaf engine for HTML email templates |
| `consumer/NotificationEventConsumer.java` | `@KafkaListener` on `banking.notification.events` (concurrency=3, manual ACK) + DLT listener |
| `controller/NotificationLogController.java` | ADMIN-only inspection: by user, by status, manual retry |
| `controller/NotificationPreferenceController.java` | Per-user channel / category / DND / frequency settings (in-memory store — swap to JPA when needed) |
| `dto/NotificationEventMessage.java` | Kafka record schema (matches producer side) |
| `entity/NotificationLog.java` | Per-channel delivery record with retry count + error message |
| `enums/` | `NotificationStatus`, `NotificationType`, `NotificationPriority` |
| `provider/EmailProvider.java`, `SmsProvider.java`, `PushProvider.java` | Channel interfaces |
| `provider/impl/SmtpEmailProvider.java` | `JavaMailSender` — production-ready |
| `provider/impl/TwilioSmsProvider.java`, `FcmPushProvider.java` | Stubs (SDKs not on the classpath by default) |
| `service/EmailTemplateService.java` | Thymeleaf rendering + a static SMS-template map for 13 notification types |
| `service/NotificationDispatcher.java`, `impl/NotificationDispatcherImpl.java` | Idempotent dispatch with DND window + LOW-priority rate-limit + `@Scheduled` retry every 5 min |
| `security/` | Standard 3 files |

### 5.9 `audit-compliance-service`

| Package / file | Purpose |
|---|---|
| `AuditComplianceServiceApplication.java` | Entry point |
| `client/StatsClients.java` (4 Feign clients) | Pull stats from customer / account / loan / transaction internal endpoints for analytics |
| `client/FeignConfig.java` | Header propagation |
| `config/KafkaConsumerConfig.java`, `KafkaProducerConfig.java` | Consumer (fixed 2-s backoff × 2 → DLT), DLT producer factory |
| `consumer/AuditEventConsumer.java` | `@KafkaListener` on `banking.audit.events` + DLT listener |
| `controller/AuditLogController.java` | ADMIN-only `/api/v1/audit/{logs, summary, actions, entity, user}` |
| `controller/AnalyticsController.java` | Staff-role analytics (spend, revenue, loan portfolio, compliance metrics, customer insights) |
| `controller/InternalAuditController.java` | `POST /api/v1/internal/audit` — REST fallback for non-Kafka publishers (`@Hidden` from Swagger) |
| `dto/AuditEventMessage.java` | Kafka record schema |
| `dto/response/AuditLogResponse.java`, `AuditSummaryResponse.java` | Wire format |
| `entity/AuditLog.java` | **Append-only**: `@PreUpdate` and `@PreRemove` throw `UnsupportedOperationException` so the table can never be mutated post-insert |
| `enums/AuditAction.java`, `AuditStatus.java` | 67 action values + 3 statuses (SUCCESS / FAILURE / PENDING) |
| `repository/AuditLogRepository.java` | `JpaSpecificationExecutor` for dynamic filtering + aggregation queries for the summary endpoint |
| `service/AuditLogService.java`, `impl/AuditLogServiceImpl.java` | Idempotent ingest (dedup on `eventId`), spec-builder queries, summary aggregation |
| `service/AnalyticsService.java` | Fans out to the stats Feign clients; gracefully returns empty maps when downstream is unavailable |
| `security/` | Standard 3 files |

---

## 6. Frontend

React 18 + Vite + Tailwind. Lives entirely under `frontend/src`.

### 6.1 Routing (`App.jsx`)

| Path prefix | Layout | Who can see it |
|---|---|---|
| `/`, `/about`, `/branches`, `/products/:slug`, `/segments/:slug` | `PublicLayout` (marketing nav + footer) | Anyone |
| `/login`, `/login/{role}`, `/signup`, `/forgot-password`, `/unauthorized` | `AuthLayout` (split-screen, brand panel + form) | Anyone |
| `/app/**` | `AppShell` (sidebar + topbar) wrapped in `ProtectedRoute(roles=[CUSTOMER])` | Customers only |
| `/staff/**` | `AppShell` wrapped in `ProtectedRoute(roles=[CSR, BRANCH_MANAGER, LOAN_OFFICER, ADMIN])` | Staff. Inner routes additionally guard for the specific role |

`StaffHomeRedirect` and `RedirectIfAuthed` send users to the right landing
page after login.

### 6.2 API layer (`src/api/`)

| File | What it does |
|---|---|
| `client.js` | The shared **axios** instance. Base URL `http://localhost:8090`. Request interceptor attaches Bearer token + correlation ID. Response interceptor handles `401 TOKEN_EXPIRED` → auto-refresh and retry. Exports `unwrap()` and `errorMessage()` helpers |
| `tokenStore.js` | Storage abstraction — `localStorage` if Remember-me ticked, `sessionStorage` otherwise. `clear()` wipes both defensively |
| `serverErrors.js` | `applyServerErrors(err, setError)` parses the backend error envelope (string `"field: msg; …"` OR structured `errors: {…}` map) and routes each field error to react-hook-form's `setError` |
| `auth.js` | signup / login / refresh / logout / forgot-password / verify-otp / reset-password |
| `customer.js` | Profile CRUD, KYC submission, status transitions, soft-delete |
| `account.js` | My applications + accounts, staff branch operations, freeze / unfreeze / close, cash counter |
| `transfer.js` | PIN management + transfer initiation + pending-transfer approval |
| `transaction.js` | Initiate / get / cancel / reverse + paginated history |
| `loan.js` | Apply / decide / disburse / pay / schedule / summary / prepay / paymentHistory + eligibility |
| `branch.js` | List / search / CRUD / operating-hours / summary / "is open now" |
| `admin.js` | Staff CRUD (ADMIN-only) |
| `notification.js`, `preferences.js`, `support.js`, `analytics.js`, `dashboards.js` | Self-explanatory thin wrappers |

### 6.3 Components

```
components/
├── common/          ← Badge, Button, Card, EmptyState, Input (with `required` asterisk),
│                      Logo, Modal, ProtectedRoute, Skeleton, Spinner, ThemeToggle
├── dashboard/       ← AccountSummaryCard, BarTrend, CategoryDonut, KpiCard,
│                      LineTrend, SpendChart, TransactionRow, chartTheme.js
├── layout/          ← AppShell, NotificationDropdown, PageHeader, Sidebar, Topbar
├── public/          ← PublicLayout
└── staff/           ← BranchAccountsPanel
```

### 6.4 Pages

```
pages/
├── auth/            ← Login (with 5 role-specific portal cards), Signup, ForgotPassword
├── public/          ← Landing, About, Branches, InfoPage, Unauthorized
├── customer/        ← Dashboard, Accounts, AccountApply, AccountDetail, AccountPin,
│                      Kyc, Loans, LoanDetail, Transfer, Transactions, MyPendingTransfers,
│                      Notifications, Profile, Settings, Help
├── shared/          ← ChangePassword, NotificationPreferences, SupportTickets
│                      (used by both customer and staff layouts)
└── staff/           ← CsrDashboard, BranchManagerDashboard, LoanOfficerDashboard,
                       AdminDashboard, AccountApplicationsReview, KycReview, CashCounter,
                       PendingTransfersReview, Analytics, StaffProfile, StaffSettings
```

### 6.5 Context, hooks, utils

| File | Role |
|---|---|
| `context/AuthContext.jsx` | `useAuth()` — user, isAuthenticated, login, logout, signup. Bootstraps from `tokenStore` on mount |
| `context/ThemeContext.jsx` | Light / dark toggle. Pre-paint script in `index.html` prevents flash of wrong theme |
| `hooks/useAsync.js` | One-shot async loader with loading / error / reload |
| `utils/jwt.js` | Lightweight payload decoder (no signature verification — gateway already did that) |
| `utils/format.js` | INR / compact-INR / date / relative-date / mask / initials |
| `utils/roleRoutes.js` | `ROLES` enum + `roleHomePath(role)` |

### 6.6 Validation UX

Every customer-facing form uses **react-hook-form** with `mode: 'onTouched'`
(validate as soon as the user leaves a field). On submit failure:

1. `applyServerErrors(err, setError)` reads the backend envelope and pins each
   field-level error under the right input.
2. If at least one field was annotated, a soft toast says *"Please fix the
   highlighted fields and try again."* If not, the generic `errorMessage()`
   fallback fires.

`Input` / `Select` / `Textarea` accept a `required` prop that renders the red
asterisk and sets `aria-required` / `aria-invalid` for screen-readers.

---

## 7. Kafka event flows

```
account-service / loan-service / transaction-service / customer-service
   │
   │   "ACCOUNT_FROZEN, LOAN_APPROVED, TRANSFER_COMPLETED, …"
   │
   ▼
                  banking.audit.events  (3 partitions, 7-day retention)
                  │
                  ▼
      audit-compliance-service.AuditEventConsumer
        @KafkaListener concurrency=3
        Fixed 2-s backoff × 2 → banking.audit.events-dlt
        Idempotent ingest (dedup on eventId)
        Persists to `audit_logs` (append-only, no UPDATE / DELETE allowed)


account-service / loan-service / transaction-service
   │
   │   "Account approval email, transfer success email, EMI reminder, …"
   │
   ▼
                  banking.notification.events  (3 partitions, 1-day retention)
                  │
                  ▼
      notification-service.NotificationEventConsumer
        @KafkaListener concurrency=3
        Exponential 1 s → 2 s → 4 s → banking.notification.events-dlt
        NotificationDispatcherImpl:
          - per-channel idempotency check (skip if already SENT)
          - DND window (default 22:00–08:00; HIGH priority bypasses)
          - per-user hourly rate limit (LOW priority only)
          - dispatch via SmtpEmailProvider / TwilioSmsProvider stub / FcmPushProvider stub
          - failures retry every 5 min; after `max-attempts` → DLT
```

**Producer config (all producers)**: `enable.idempotence: true`, `acks: all`,
`retries: 3`, type headers off.

**The forgot-password OTP does NOT use Kafka.** It's sent synchronously from
`identity-services` via `JavaMailSender` (`PasswordResetMailService.sendOtp`)
with `@Async` so the HTTP response is fast.

---

## 8. Resilience

### Circuit breaker (Resilience4j defaults, applied at gateway + service-to-service Feign)

| Property | Value |
|---|---|
| `slidingWindowSize` | 10 |
| `minimumNumberOfCalls` | 5 |
| `failureRateThreshold` | 50% |
| `slowCallRateThreshold` | 80% |
| `slowCallDurationThreshold` | 2 s |
| `waitDurationInOpenState` | 60 s |
| `permittedNumberOfCallsInHalfOpenState` | 3 |
| `automaticTransitionFromOpenToHalfOpenEnabled` | true |

### Retry

3 attempts, exponential backoff (500 ms base, 2× multiplier), on
`IOException` / `TimeoutException` / `RetryableException`.

### Timeouts

Feign connect/read = 3 s. Gateway TimeLimiter = 5 s (upstream gets more
headroom than the Feign default).

### Fallbacks

- Every gateway route has `fallbackUri: forward:/fallback/{service-name}` →
  `FallbackController` returns a 503 envelope.
- Every Feign client has a `@FeignClient(fallback = ...)` class. Critical
  fallbacks (e.g. `BranchServiceClientFallback.isBranchActive`) fail-closed;
  best-effort fallbacks (e.g. `TransactionServiceClientFallback.initiate`)
  return synthetic responses so the primary flow doesn't block.

### Rate limiting

Gateway: 100 req/min per IP, in-memory bucket. **Production: replace with
Redis-backed `RequestRateLimiter`** for multi-node.

---

## 9. Observability

### Correlation IDs

The gateway generates `X-Correlation-ID` (UUID v4) if not present and echoes
it in the response. Every downstream service's `CorrelationIdFilter`
(`@Order(HIGHEST_PRECEDENCE)`):

1. Reads `X-Correlation-ID` → `MDC["correlationId"]`
2. Reads `X-User-Id` → `MDC["userId"]`
3. Echoes the correlation ID on the response
4. Cleans MDC in `finally` so thread-pool threads don't leak it

Feign calls propagate the ID via `GlobalFeignConfig.RequestInterceptor`.

### Log format (all services)

```
2024-01-15 14:23:01.456 [http-nio-1-exec-1] [a3f2b1c4-...] [42] INFO  c.c.a.AccountService - ...
                                              ^^^^^^^^^^^^^^  ^^
                                              correlationId   userId
```

### Actuator endpoints (all services)

- `/actuator/health` — liveness + readiness (probe groups configured for Kubernetes)
- `/actuator/metrics`, `/actuator/prometheus`
- `/actuator/refresh` — hot-reload config from Config Server without restart
- `/actuator/gateway/refresh` (gateway only) — re-fetch route definitions

### OpenAPI / Swagger

Every service exposes its own Swagger UI:

| Service | URL |
|---|---|
| identity-services | http://localhost:8084/swagger-ui.html |
| customer-services | http://localhost:8081/swagger-ui.html |
| Account-service | http://localhost:8083/swagger-ui.html |
| Transaction-service | http://localhost:8082/swagger-ui.html |
| loan-service | http://localhost:8085/swagger-ui.html |
| branch-service | http://localhost:8086/swagger-ui.html |
| notification-service | http://localhost:8088/swagger-ui.html |
| audit-compliance-service | http://localhost:8087/swagger-ui.html |

Every controller method has `@Operation(summary, description)` and every
controller class has `@Tag` so the contracts read like real product
documentation.

---

## 10. Testing & coverage

### What's tested

Each service has three focused test classes:

| File | Style | What it covers |
|---|---|---|
| `service/impl/<ServiceImpl>Test.java` | Pure JUnit 5 + Mockito | All public methods of the most business-critical service. Happy path + every failure branch |
| `exception/GlobalExceptionHandlerTest.java` | Direct unit test | One assertion per `@ExceptionHandler` method. Locks the error envelope contract |
| `controller/<Controller>Test.java` | MockMvc `standaloneSetup` | HTTP layer. Happy path + every Jakarta-validation rejection. `JavaTimeModule` registered on the `ObjectMapper`; `PageableHandlerMethodArgumentResolver` registered where the controller accepts `Pageable` |

All services use `@MockitoSettings(strictness = LENIENT)` on the service-impl
test so failure-path stubs don't trip `UnnecessaryStubbingException`.

The default `*ApplicationTests.java` stub is `@Disabled` because a full
`@SpringBootTest` context load needs MySQL + Kafka + Eureka + Config Server,
which we don't spin up by default.

### Running tests + viewing coverage

```bash
cd <service-directory>
mvn test                                # run tests
mvn verify                              # tests + JaCoCo HTML report
start target/site/jacoco/index.html     # open report (Windows)
```

The JaCoCo plugin excludes DTOs, entities, enums, config, and the main
`*Application.class` so the coverage % reflects real business logic, not
Lombok / Spring boilerplate.

### Current test counts

| Service | Tests | Skipped (`@Disabled`) |
|---|---|---|
| identity-services | 47 | 1 |
| customer-services | 54 | 0 |
| Account-service | 49 | 1 |
| Transaction-service | 40 | 1 |
| loan-service | 46 | 1 |
| branch-service | 34 | 1 |
| notification-service | 20 | 1 |
| audit-compliance-service | 25 | 1 |
| **Total** | **315** | **7** |

---

## 11. Local development

### Prerequisites

- JDK 21
- Maven 3.9+
- Node 18+ (for the frontend)
- MySQL 8 (or Docker)
- Optional: Docker for Kafka

### Start order

```
1. MySQL (port 3306)
2. Kafka + Zookeeper:  docker compose -f docker-compose-kafka.yml up -d
3. eureka-server   :   mvn spring-boot:run         (port 8761)
4. config-server   :   mvn spring-boot:run         (port 8888)
5. The 8 business services, in parallel terminals — each:  mvn spring-boot:run
6. api-gateway     :   mvn spring-boot:run         (port 8090, last so all services are registered)
7. frontend        :   cd frontend && npm install && npm run dev   (port 5173)
```

### Default credentials

An ADMIN user is auto-seeded on first identity-services startup
(`AdminBootstrap`):

```
email:    admin@banksphere.com
password: Admin@12345
```

Customer accounts are created via `POST /auth/signup` or the React signup form.

### Required environment variables for production

```
JWT_SECRET             # Base64-encoded HMAC-SHA256 key (≥ 256 bits)
                       # Same value in identity-services AND api-gateway
DB_USERNAME / DB_PASSWORD              # Per-service database credentials
KAFKA_BOOTSTRAP_SERVERS=<broker:9092>
EUREKA_URL=http://eureka:8761/eureka
CONFIG_SERVER_URL=http://config:8888
SMTP_HOST / SMTP_PORT / SMTP_USERNAME / SMTP_PASSWORD   # for OTP + notifications
```

### Kubernetes probes

```yaml
readinessProbe:
  httpGet:
    path: /actuator/health/readiness
    port: <service-port>
livenessProbe:
  httpGet:
    path: /actuator/health/liveness
    port: <service-port>
```

---

## 12. Where things commonly trip people up

| Situation | Where the truth lives |
|---|---|
| Login works but a downstream call returns 401 — which header is missing? | `<service>/security/HeaderAuthenticationFilter.java` — it needs `X-User-Id` AND `X-Role`. Look at the gateway-side header injection in `api-gateway/.../filter/JwtAuthenticationFilter.java` |
| "Why is my Feign call to a peer service unauthenticated?" | Each service's `config/GlobalFeignConfig.java` propagates the headers — confirm your Feign interface lives in `client/` and that the propagation interceptor is being picked up |
| OTP email arriving but not arriving for other notifications | The OTP is **direct SMTP** (`identity-services.PasswordResetMailService`). Other emails are **Kafka → notification-service**. Check the dispatcher's DND / rate-limit / retry / DLT |
| Transfer staying in PENDING_APPROVAL | Amount > ₹1,00,000 routes to the high-value queue. A CSR / Branch Manager has to approve via `/api/v1/staff/pending-transfers/{ref}/approve` |
| Audit events not appearing | Check `banking.audit.events-dlt` for stuck messages. The audit consumer deduplicates on `eventId`, so duplicate publishes are silently dropped — not an error |

---

## 13. Future scalability

| Concern | Current state | Recommended next step |
|---|---|---|
| Rate limiting | In-memory, single-node | Redis-backed `RequestRateLimiter` in api-gateway |
| Service-to-service auth | Header propagation (user context) | mTLS via service mesh (Istio) or SPIFFE/SPIRE |
| Distributed tracing | Correlation ID via MDC | OpenTelemetry + Jaeger / Zipkin |
| Log aggregation | Per-service logfiles | Fluentd / Logstash → Elasticsearch → Kibana |
| Secret management | Environment variables | HashiCorp Vault or Kubernetes Secrets |
| Database scaling | Single MySQL per service | Read replicas + connection pooling (PgBouncer) |
| Async resilience | Kafka with 3 retries → DLT | Dead-letter replay tooling + alerting |
| Config encryption | Plain YAML | Spring Cloud Config encryption (`{cipher}` prefix) |
| Push / SMS providers | Stubs | Wire up Firebase Admin SDK + Twilio SDK |
| Notification preferences | In-memory map | JPA entity + repository |
| Support tickets | In-memory map | JPA entity + Kafka audit |
| Auto-activation of customers | Best-effort Feign call after account approval | Move to event-driven (Kafka topic the customer-service listens on) |
