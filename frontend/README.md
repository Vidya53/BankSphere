# BankSphere — Frontend

React + Vite + Tailwind frontend for the BankSphere microservice banking platform.

**What's in here:** The public site, full authentication, the customer experience, all five staff dashboards, the analytics tower, and three cross-cutting feature pages (notification preferences, password change, support tickets).

---

## Quick start

```bash
cd BankSphere/frontend
npm install
npm run dev
```

The dev server runs on **http://localhost:5173** and proxies API calls to the gateway at **http://localhost:8090** (configured in `.env`).

Make sure the backend stack is running before launching the UI:

```
config-server  → 8888
eureka-server  → 8761
api-gateway    → 8090   (the only port the frontend talks to)
+ identity-service, customer-service, account-service, transaction-service,
  branch-service, loan-service, notification-service, audit-compliance-service
```

> After updating the gateway routes (in `config-server/src/main/resources/config-repo/api-gateway.yaml`),
> restart **config-server** and **api-gateway** to pick up the new routes.

---

## Feature coverage

### Authentication
- Login / Signup
- Refresh-token rotation on 401
- Role-aware routing

### Customer experience (`/app/*`)
- Dashboard (KPIs, charts, recent transactions)
- My Accounts + Account Detail + Account Application form
- Transactions with filters + pagination
- Money transfer wizard (compose → review → confirm)
- Loans browse + eligibility calculator + my loans
- KYC submission + status
- Profile + Notifications + Settings + Help
- Support tickets (create + list + detail)

### Staff experience (`/staff/*`)
- **CSR dashboard** — pending tickets queue, KYC review queue, recent customers, weekly ticket volume
- **Branch Manager dashboard** — KPIs, targets vs. achievement, staff performance, approvals queue, daily footfall
- **Loan Officer dashboard** — pipeline (Submitted → Disbursed), upcoming + overdue EMIs, disbursement trend, risk distribution
- **Compliance dashboard** — overall score, risk heatmap, full compliance checklist, recent audit events, pending reviews
- **Analytics (cross-domain)** — tabbed view: Spend, Revenue, Loans, Compliance, Customers
- Support queue with status updates
- Settings: change password + notification preferences

### Cross-cutting feature pages (shared between roles)
- **Notification preferences** — channels (email/SMS/push/in-app), categories, frequency, DND quiet hours
- **Password change** — current password verification, real-time strength meter, validation rules
- **Support tickets** — create, list with filters, detail with timeline + conversation thread

---

## Backend endpoints added in this iteration

All routed through the gateway at `http://localhost:8090`:

| Endpoint | Service | Purpose |
|---|---|---|
| `GET  /api/analytics/spend-analysis`       | audit-compliance | Spend categories, daily trend, top merchants |
| `GET  /api/analytics/revenue-trends`       | audit-compliance | Revenue mix and monthly series |
| `GET  /api/analytics/loan-portfolio`       | audit-compliance | Portfolio by product/status/risk + NPA trend |
| `GET  /api/analytics/compliance-metrics`   | audit-compliance | Compliance KPIs and checklist |
| `GET  /api/analytics/customer-insights`    | audit-compliance | Segments, acquisition trend, top cities |
| `GET  /api/csr/dashboard`                  | customer-service | CSR queue, KYC queue, recent customers |
| `GET  /api/branch-manager/dashboard`       | branch-service   | KPIs, targets, staff, approvals queue |
| `GET  /api/loan-officer/dashboard`         | loan-service     | Pipeline, EMIs, disbursement trend, risk |
| `GET  /api/compliance/dashboard`           | audit-compliance | Risk heatmap, audit events, pending reviews |
| `GET/PUT /api/notifications/preferences`   | notification-service | Per-user preferences (channels/categories/DND) |
| `POST /api/auth/change-password`           | identity-service | Authenticated password change |
| `GET/POST /api/support/tickets`            | customer-service | Tickets CRUD with role-based status updates |
| `PATCH /api/support/tickets/{id}/status`   | customer-service | Staff status transitions |

All endpoints return the standard envelope `{ success, statusCode, message, data }`.

Mock data is deterministic (seeded RNG) so charts remain stable across reloads.

---

## Project structure

```
frontend/
├── src/
│   ├── api/                      Axios client + per-domain endpoint modules
│   │   ├── client.js             Interceptors, refresh-token rotation
│   │   ├── analytics.js          /api/analytics/*
│   │   ├── dashboards.js         /api/{csr,branch-manager,loan-officer,compliance}/dashboard
│   │   ├── preferences.js        /api/notifications/preferences + /api/auth/change-password
│   │   ├── support.js            /api/support/tickets
│   │   └── …                     auth, customer, account, transaction, loan, branch, notification
│   ├── components/
│   │   ├── common/               Button, Card, Input, Modal, Spinner, Badge, ProtectedRoute, Logo
│   │   ├── dashboard/            KpiCard, AccountSummaryCard, SpendChart, CategoryDonut,
│   │   │                         BarTrend, LineTrend, TransactionRow
│   │   └── layout/               AppShell, Sidebar (role-aware), Topbar, NotificationDropdown
│   ├── context/                  AuthContext
│   ├── hooks/                    useAsync
│   ├── pages/
│   │   ├── auth/                 Login, Signup
│   │   ├── customer/             Dashboard, Accounts, AccountDetail, AccountApply,
│   │   │                         Transactions, Transfer, Loans, Kyc, Profile,
│   │   │                         Notifications, Settings, Help
│   │   ├── staff/                Analytics, CsrDashboard, BranchManagerDashboard,
│   │   │                         LoanOfficerDashboard, ComplianceDashboard
│   │   ├── shared/               NotificationPreferences, ChangePassword, SupportTickets
│   │   └── public/               Landing, Unauthorized
│   └── utils/                    jwt, format, roleRoutes
├── index.html
├── tailwind.config.js            Axis-style brand palette + design tokens
├── vite.config.js
└── .env                          VITE_API_BASE_URL → http://localhost:8090
```

---

## Routing map

| Role | Lands at | Has access to |
|---|---|---|
| CUSTOMER           | `/app`            | Full customer experience |
| CSR                | `/staff/csr`      | CSR dashboard, support queue, analytics |
| BRANCH_MANAGER     | `/staff/branch`   | Branch dashboard, CSR + loans, analytics |
| LOAN_OFFICER       | `/staff/loans-ops`| Loan dashboard, analytics |
| COMPLIANCE_OFFICER | `/staff/compliance`| Compliance dashboard, analytics |
| ADMIN              | `/staff/analytics` | Everything |

Role enforcement lives in three places:
1. **Gateway** (`RouteAuthorizationConfig.java`) — coarse-grained path → role mapping at the edge.
2. **Service controllers** (`@PreAuthorize`) — fine-grained per-endpoint role checks.
3. **Frontend** (`ProtectedRoute`) — UX-level role guards so users only see their own routes.

---

## Design system

Axis Bank-inspired: deep maroon brand (`#97144D`) with warm gold accents (`#c9a35d`),
Plus Jakarta Sans for display, Inter for body, card-based layouts with subtle shadows,
pill-shaped CTAs.

Reusable component classes live in `src/index.css` under `@layer components`
(`.btn-primary`, `.card`, `.input`, etc.) so they're available as Tailwind utilities throughout.

Recharts powers all visualisations: area, bar (with stacking), line, donut. Charts are
wrapped in `<Card>` shells for consistent surrounding layout.

---

## What "mock data" means in this build

The new analytics + dashboard endpoints return mock data **generated server-side** in
Java (deterministic seeds for stability), not on the frontend. This means:

- No frontend hardcoded values — everything flows over real HTTP.
- Easy to swap the mock generator for real aggregations when the data layer is ready.
- The same endpoint shape works in both dev (mock) and prod (real) — the frontend doesn't change.

The exception is **support tickets**, which use an in-memory `ConcurrentHashMap` in the
backend so tickets created from the UI persist for the lifetime of the JVM. Swap that
`Map` for a JpaRepository when you want persistence.

---

## Next session — what to build

The natural follow-ups:

1. **Full customer-360 search** for CSRs — currently a placeholder in `/staff/customers`.
2. **Real database integration** for support tickets + notification preferences.
3. **WebSocket-backed live tickets** so CSRs see new tickets without refreshing.
4. **PDF export** for analytics dashboards and statements.
5. **EMI payment flow** wired end-to-end on the loan officer dashboard.

The API service layer is structured so each of these can be added without touching
existing pages.
