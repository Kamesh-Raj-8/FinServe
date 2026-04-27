# SME Lend V2 — Enterprise Lending Platform

Full-stack: **Angular 19** frontend + **Spring Boot 3 / Java 21** backend.

## Project Layout

```
smelend-v2/
├── src/                            ← Angular 19 frontend
│   ├── app/
│   │   ├── core/
│   │   │   ├── models/index.ts     ← All TypeScript interfaces (matches Java DTOs exactly)
│   │   │   ├── services/
│   │   │   │   ├── auth.service.ts ← JWT login/register/logout
│   │   │   │   └── api.service.ts  ← Every backend endpoint
│   │   │   ├── interceptors/       ← Attaches Bearer token to every request
│   │   │   └── guards/             ← authGuard + roleGuard(…roles)
│   │   ├── layout/shell/           ← App shell: sidebar + topbar
│   │   └── pages/
│   │       ├── auth/               ← Login + Register
│   │       ├── dashboard/          ← Role-aware quick links + stats
│   │       ├── sme/                ← List, Form, Detail (with promoters)
│   │       ├── kyc/                ← Create, view, verify/reject (AGENT)
│   │       ├── applications/       ← List, Form, Detail (docs + submit)
│   │       ├── underwriting/       ← UW queue + APPROVE/REJECT/RETURN
│   │       ├── operations/         ← Approved apps, create offer, disburse
│   │       ├── offers/             ← List offers, accept/reject
│   │       ├── servicing/          ← EMI schedule with PAID/DUE/OVERDUE badges
│   │       ├── repayments/         ← Post repayment + history
│   │       └── admin/              ← Users, Loan Products (full CRUD), Roles
│   ├── environments/               ← apiUrl: http://localhost:8081
│   └── styles.css                  ← Sora + DM Sans design system
│
├── sme-lend-backend/               ← Spring Boot backend (unchanged)
│   ├── src/main/resources/application.properties
│   └── pom.xml
│
└── README.md
```

---

## Prerequisites

| Tool         | Version |
|--------------|---------|
| Java         | 21+     |
| Maven        | 3.9+    |
| MySQL        | 8.0+    |
| Node.js      | 20+     |
| Angular CLI  | `npm i -g @angular/cli` |

---

## Step 1 — MySQL

MySQL must be running on **port 3306**.  
The database `sme_lend` is auto-created on first boot (`createDatabaseIfNotExist=true`).

Default credentials (in `application.properties`):
```
spring.datasource.username=root
spring.datasource.password=root1234
```

---

## Step 2 — Run Backend

```bash
cd sme-lend-backend
mvn spring-boot:run
# ✅ http://localhost:8081
# 📖 Swagger UI: http://localhost:8081/swagger
```

`DataSeeder.java` seeds 9 roles + 8 system users automatically on first boot.

---

## Step 3 — Run Frontend

```bash
# From smelend-v2/ root (where angular.json lives)
npm install
ng serve --open
# ✅ http://localhost:4200
```

---

## Demo Credentials (pre-seeded by DataSeeder.java)

| Role         | Email                     | Password  |
|--------------|---------------------------|-----------|
| ADMIN        | admin@smelend.com         | Admin@123 |
| AGENT        | agent1@test.com           | Pass@123  |
| UNDERWRITER  | underwriter1@test.com     | Pass@123  |
| OPERATIONS   | operations1@test.com      | Pass@123  |
| SERVICING    | servicing1@test.com       | Pass@123  |
| COLLECTIONS  | collections1@test.com     | Pass@123  |
| RISK         | risk1@test.com            | Pass@123  |
| COMPLIANCE   | compliance1@test.com      | Pass@123  |
| APPLICANT    | Self-register at `/auth/register` |     |

Click any role badge on the login page to pre-fill credentials, then click **Sign In**.

---

## Role → Module Access

| Role         | Pages accessible                                                                                      |
|--------------|-------------------------------------------------------------------------------------------------------|
| APPLICANT    | Dashboard, SME Onboarding, KYC, Applications, Offers (accept/reject), Servicing                       |
| AGENT        | Everything APPLICANT + **Verify/Reject KYC**                                                          |
| UNDERWRITER  | Dashboard, **Underwriting queue** (APPROVE / REJECT / RETURN)                                         |
| OPERATIONS   | Dashboard, **Approved Apps**, **Create Offers**, **Disburse**, **Repayments**, Servicing               |
| SERVICING    | Dashboard, Servicing schedule                                                                         |
| COLLECTIONS  | (Backend-only in this release — no frontend page for COLLECTIONS/RISK/COMPLIANCE yet)                 |
| ADMIN        | **Everything above** + Admin Panel (Users, Loan Products, Roles)                                      |

---

## Architecture Notes

- **Standalone components**, lazy-loaded routes (no NgModules)
- JWT stored in `localStorage` (`sml_token` + `sml_user`)
- `authInterceptor` attaches `Authorization: Bearer <token>` to every HTTP request
- `authGuard` redirects unauthenticated users → `/auth/login`
- `roleGuard(...roles)` redirects unauthorized roles → `/dashboard`
- API base URL: `src/environments/environment.ts` → `apiUrl: 'http://localhost:8081'`
- All backend `@PreAuthorize` rules are mirrored in Angular route guards

---

## API → Component Mapping

| Endpoint                                        | Component                     |
|-------------------------------------------------|-------------------------------|
| `POST /auth/login`                              | `LoginComponent`              |
| `POST /auth/register`                           | `RegisterComponent`           |
| `GET/POST /onboarding/smes`                     | `SmeListComponent` / `SmeFormComponent` |
| `GET /onboarding/smes/{id}`                     | `SmeDetailComponent`          |
| `GET/POST /onboarding/smes/{id}/promoters`      | `SmeDetailComponent`          |
| `GET/POST /kyc`, `PATCH /kyc/{id}/verify`       | `KycComponent`                |
| `GET/POST /applications`, `PATCH .../submit`    | `AppListComponent` / `AppFormComponent` / `AppDetailComponent` |
| `GET/POST /applications/{id}/documents`         | `AppDetailComponent`          |
| `GET /uw/queue`, `POST /uw/applications/{id}/decision` | `UnderwritingComponent`  |
| `GET /ops/applications/approved`                | `OperationsComponent`         |
| `POST /offers/applications/{id}`, `GET /offers` | `OperationsComponent` / `OffersComponent` |
| `PATCH /offers/{id}/accept`, `.../reject`       | `OffersComponent`             |
| `POST /ops/applications/{id}/disburse`          | `OperationsComponent`         |
| `GET /servicing/loan-accounts/{id}/schedule`    | `ServicingComponent`          |
| `POST /servicing/repayments`, `GET .../loan-accounts/{id}` | `RepaymentsComponent` |
| `GET /admin/roles`                              | `AdminComponent`              |
| `GET/POST /admin/users`, `PATCH .../status`     | `AdminComponent`              |
| `GET/POST /admin/loan-products`, `PUT`, `DELETE`| `AdminComponent`              |
