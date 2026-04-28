# SME-Lend — Complete Demo Workflow Guide

End-to-end walkthrough for testing the full loan lifecycle in the browser.

---

## Prerequisites & Startup

### 1. Start MySQL
Ensure MySQL is running on `localhost:3306` with database `sme_lend` (auto-created on first boot).

### 2. Start the Backend
```bash
cd sme-lend-backend
mvn clean spring-boot:run
```
- Runs on **http://localhost:8081**
- DataSeeder auto-creates all system users on first startup
- Swagger UI: **http://localhost:8081/swagger**

### 3. Start the Frontend
```bash
cd sme-lend-frontend
npm install
npm start
```
- Runs on **http://localhost:4200**

### 4. Verify Both Services
| Check | URL | Expected |
|---|---|---|
| Backend health | http://localhost:8081/health | `{"status":"UP"}` |
| Frontend app | http://localhost:4200 | Landing page |
| Swagger docs | http://localhost:8081/swagger | API explorer |

---

## Seeded User Credentials

| Role | Email | Password | Dashboard |
|---|---|---|---|
| ADMIN | admin@finserve.com | Admin@123 | /admin |
| AGENT | agent1@test.com | Pass@123 | /agent |
| UNDERWRITER | underwriter1@test.com | Pass@123 | /underwriting |
| OPERATIONS | operations1@test.com | Pass@123 | /operations |
| SERVICING | servicing1@test.com | Pass@123 | /servicing |
| COLLECTIONS | collections1@test.com | Pass@123 | /collections |
| RISK | risk1@test.com | Pass@123 | /risk |
| COMPLIANCE | compliance1@test.com | Pass@123 | /compliance |

> **APPLICANT** accounts must be self-registered from the landing page or created by ADMIN.

---

## Phase 0 — Admin Setup (Do This First)

Login as **admin@finserve.com / Admin@123** → routed to `/admin`.

### Step 0.1 — Create a Loan Product
Navigate to the **Loan Products** tab.

Click **+ New Product** and fill in:
| Field | Example Value |
|---|---|
| Product Name | SME Working Capital |
| Min Amount (₹) | 100000 |
| Max Amount (₹) | 5000000 |
| Min Tenor (months) | 6 |
| Max Tenor (months) | 60 |
| Base Interest Rate (%) | 12.5 |
| Credit Threshold | 650 |
| Min Income Amount (₹/year) | 600000 |
| Max Income Amount (₹/year) | 10000000 |

Save and confirm the product status shows **ACTIVE**.

### Step 0.2 — Create a Fee Configuration
Navigate to the **Fee Configs** tab, select the product created above, and add:

| Fee Type | Rate (%) | Notes |
|---|---|---|
| PROCESSING | 2.0 | One-time processing |
| PENAL | 0.5 | Per day on overdue amount |
| PREPAYMENT | 1.0 | Prepayment penalty |

### Step 0.3 — Create an Applicant User
Navigate to the **Users** tab → **+ New User**:

| Field | Value |
|---|---|
| Full Name | Test Applicant |
| Email | applicant1@test.com |
| Password | Pass@123 |
| Role | APPLICANT |
| Phone | 9876543210 |

Verify the new user appears with **ACTIVE** status.

> Admin dashboard is **view-only** for SME/application records. All edits are done through role-specific dashboards.

---

## Phase 1 — SME Onboarding (APPLICANT or AGENT)

### Option A — Self-Service (APPLICANT)

1. Open a new browser tab / incognito window
2. Go to **http://localhost:4200**
3. Click **Register** and create an account with role **APPLICANT**, or log in as **applicant1@test.com / Pass@123**
4. You are routed to `/applicant`

### Option B — Agent-Assisted (AGENT)

Log in as **agent1@test.com / Pass@123** → routed to `/agent`.

---

### Step 1.1 — Register the SME Business

In the **Onboarding** tab (either `/applicant` step 1 or `/agent` Onboarding tab):

Click **+ Register SME** and fill in:
| Field | Example Value |
|---|---|
| Legal Name | Sharma Exports Pvt Ltd |
| Trade Name | Sharma Exports |
| Business Type | PVT_LTD |
| Industry | Manufacturing |
| Address | 42 Industrial Estate, Pune, 411001 |
| GST No. | 27AAFCS1234D1Z5 |

Click **Register**. The SME card should appear in the list.

### Step 1.2 — Add a Promoter

Click **+ Add Promoter** on the SME card:

| Field | Example Value | Notes |
|---|---|---|
| Name | Ramesh Sharma | |
| Mobile | 9876543210 | 10-digit |
| Ownership % | 100 | |
| Monthly Income (₹) | 120000 | Used for credit scoring |
| PAN Number | ABCRS1234F | Optional |
| Aadhaar | 123456789012 | Optional |
| Date of Birth | 1985-06-15 | Optional |

Click **Add**. Verify the promoter appears with **KYC: PENDING**.

> The `monthlyIncome` field directly feeds the **DecisionEngine**. Annualised income = 120000 × 12 = ₹14,40,000. If this exceeds `minIncomeAmount` on the product (₹6,00,000), the applicant gets `ScoreBand.EXCELLENT`.

---

## Phase 2 — KYC Process

### Step 2.1 — Create a Loan Application First (Required for KYC)

In the `/applicant` dashboard, go to **Step 3: Application**:

| Field | Example Value |
|---|---|
| Product | SME Working Capital |
| Requested Amount (₹) | 1000000 |
| Tenor (months) | 24 |
| Purpose | Business expansion and working capital |

Click **Create Application**. Note the **Application ID** (e.g., `#1`).

### Step 2.2 — Initialize KYC

Go to **Step 2: KYC**. Click **Initialize KYC** (auto-links to the application). Verify the promoter card shows KYC pending.

### Step 2.3 — Upload KYC Documents (Applicant Side)

In the **Documents** tab, upload documents under the **KYC Documents** section:
- PAN Card (PDF or image)
- Aadhaar Card (PDF or image)
- Business Registration Certificate

### Step 2.4 — Submit the Application

Go back to **Step 3: Application** and click **Submit Application**. Status changes from `DRAFT` → `SUBMITTED`.

> Submission triggers the **DecisionEngine** automatically.

---

## Phase 3 — Agent KYC Verification (AGENT)

Log in as **agent1@test.com / Pass@123** → `/agent`.

### Step 3.1 — Open the KYC Queue

Click the **KYC Queue** tab. The submitted application should appear in the pending list.

### Step 3.2 — Review KYC Details

Click **View** on the application row. The drill-down panel shows:
- SME business details
- Promoter details (name, PAN, mobile, ownership %)
- Uploaded documents with download links

### Step 3.3 — Verify KYC

Click **Verify KYC** after reviewing the documents. Status changes to `VERIFIED`.

> If documents are invalid, click **Reject KYC** with a reason. The applicant can re-upload and re-submit.

---

## Phase 4 — Underwriting (UNDERWRITER)

Log in as **underwriter1@test.com / Pass@123** → `/underwriting`.

### Step 4.1 — View the UW Queue

The application should appear in the queue with status `ROUTED_TO_UW` or `SUBMITTED`.

### Step 4.2 — Review Scorecard

Click the application row to expand the detail panel. Check:
- **Score Value** (300–900)
- **Score Band** (EXCELLENT = income adequate, POOR = income below threshold)
- **Model Version** (should be `v2.0-income-weighted`)
- **Inputs JSON** showing leverage, tenor ratio, income ratio, band

**Scoring logic recap:**
| Condition | Result |
|---|---|
| Annualised income ≥ product minIncome | Band = EXCELLENT |
| Annualised income < product minIncome | Band = POOR, score capped at 699 |
| EXCELLENT + score ≥ 750 | AUTO_APPROVED (no UW action needed) |
| EXCELLENT + score 700–749 | ROUTE_TO_UW → UW can approve |
| POOR (score ≤ 699) | ROUTE_TO_UW → **UW blocked from approving** |

### Step 4.3 — Make a Decision

| Button | Action | Constraint |
|---|---|---|
| **Approve** | Moves to `UW_APPROVED` | Blocked if score < 700 |
| **Reject** | Moves to `UW_REJECTED` | Always allowed |
| **Request Info** | Moves to `ADDITIONAL_INFO_REQUIRED` | Always allowed |

> If you try to approve a POOR-band application (score < 700), the system throws: *"Cannot approve — CIBIL score is X (minimum required: 700). Hard system constraint."*

---

## Phase 5 — Offer Generation (OPERATIONS or auto)

Log in as **operations1@test.com / Pass@123** → `/operations`.

### Step 5.1 — View Approved Applications

Click the **Approved Applications** tab. Applications with status `UW_APPROVED` appear here.

### Step 5.2 — Generate Offer

Click **Generate Offer** on the approved application:
| Field | Value |
|---|---|
| Sanctioned Amount (₹) | 1000000 |
| Interest Rate (%) | 12.5 |
| Tenor (months) | 24 |
| Processing Fee (₹) | Auto-calculated from FeeConfig |

Click **Create Offer**. Status moves to `OFFERED`.

### Step 5.3 — Applicant Accepts Offer

Switch to the **APPLICANT** tab (`/applicant` → **Step 5: Status**).

The offer details appear. Click **Accept Offer**, enter a digital signature (any text), check the T&C checkbox, click **Confirm**. Status moves to `OFFER_ACCEPTED`.

---

## Phase 6 — Disbursement (OPERATIONS)

Back in **operations1@test.com / Pass@123** → `/operations`.

### Step 6.1 — Disburse the Loan

Click the **Pending Disbursements** tab. The accepted offer appears.

Click **Disburse**:
| Field | Value |
|---|---|
| Disbursed Amount (₹) | 1000000 |
| Disbursement Date | Today |
| Reference No. | TRN-2026-001 |

Confirm disbursement. The system:
- Creates a **Loan Account** (e.g., `#1`)
- Generates the full **Repayment Schedule** (EMI table)
- Deducts processing fee from disbursed amount
- Status moves to `DISBURSED`

---

## Phase 7 — Loan Servicing (SERVICING)

Log in as **servicing1@test.com / Pass@123** → `/servicing`.

### Step 7.1 — Look Up the Loan Account

Enter the **Loan Account ID** (e.g., `1`) and click Search.

### Step 7.2 — View Repayment Schedule

The EMI table shows all installments:
| Column | Description |
|---|---|
| # | Installment number |
| Due Date | Payment due date |
| Principal | Principal component |
| Interest | Interest component |
| Total Due | Full EMI amount |
| Penal | Accrued penal charges |
| Status | DUE / PAID / OVERDUE |

---

## Phase 8 — Repayments (SERVICING or OPERATIONS)

Navigate to `/repayments` (accessible to SERVICING, OPERATIONS, ADMIN).

### Step 8.1 — Post a Repayment

Enter the Loan Account ID and click **Load Schedule**.

**Option A — Pay specific EMI:**
Click **Pay** next to an installment. The amount pre-fills.

**Option B — Custom amount:**
Enter a custom amount and click **Post Repayment**.

Verify the installment status changes to `PAID` and the progress bar updates.

### Step 8.2 — Simulate Overdue (for Collections Demo)

To test collections: skip posting a repayment for the earliest due installment and proceed to the next phase. The DPD will be calculated from the due date to today.

---

## Phase 9 — Collections (COLLECTIONS)

Log in as **collections1@test.com / Pass@123** → `/collections`.

### Step 9.1 — Delinquency Register

The **Delinquency Register** auto-loads on page open. It:
1. Scans `repayment_schedule` for any unpaid installment with `dueDate < today`
2. Computes/upserts the delinquency record for each overdue loan
3. Returns **all rows from the delinquency table** (including scheduler-populated records)

| Column | Description |
|---|---|
| Loan Account | Loan account ID |
| DPD | Days Past Due |
| Bucket | CURRENT / DPD_1_30 / DPD_31_60 / DPD_61_90 / DPD_90_PLUS |
| As Of Date | Date the DPD was last computed |

### Step 9.2 — Create a PTP (Promise to Pay)

In the **Create PTP** panel (right column):
| Field | Example |
|---|---|
| Loan Account ID | 1 |
| Promise Date | 7 days from today |
| Promised Amount (₹) | 50000 |
| Notes | Borrower committed to paying by end of month |

Click **Create PTP**. The borrower receives an in-app notification.

### Step 9.3 — Look Up PTPs

In the **PTP Lookup** panel, enter Loan Account ID `1` and click Search. The PTP list shows with status `OPEN`.

Update the status using:
- **Kept** — borrower paid as promised
- **Broken** — borrower did not pay
- **Cancel** — arrangement cancelled

### Step 9.4 — Post a Charge

In the **Charges** section:
| Field | Value |
|---|---|
| Loan Account ID | 1 |
| Charge Type | PENAL (COLLECTIONS role) / All types (ADMIN role) |
| Amount (₹) | 500 |
| Description | Manual penal charge for 30 DPD |

Click **Post**. The charge appears in the table with status `OUTSTANDING`.

Click **Waive** to waive a charge — it moves to `WAIVED`.

> COLLECTIONS role sees only **PENAL** in the charge type dropdown. ADMIN sees all types (PROCESSING, INSURANCE, TECH, PENAL, etc.).

---

## Phase 10 — Risk Monitoring (RISK)

Log in as **risk1@test.com / Pass@123** → `/risk`.

View portfolio-level metrics:
- Total Active Loans
- Total Outstanding Principal
- DPD Bucket distribution (CURRENT, 1-30, 31-60, 61-90, 90+)
- NPA Rate
- Average Loan Size

---

## Phase 11 — Compliance Audit (COMPLIANCE)

Log in as **compliance1@test.com / Pass@123** → `/compliance`.

The audit log table shows every state-changing action:
| Column | Description |
|---|---|
| Timestamp | When the action occurred |
| Actor | User who performed the action |
| Entity Type | SME / Application / LoanAccount / KYC / etc. |
| Action | CREATE / UPDATE / APPROVE / REJECT / etc. |
| Reference ID | The entity ID affected |

Filter by actor to trace all actions by a specific user.

---

## Phase 12 — Admin Monitoring (ADMIN)

Log in as **admin@finserve.com / Admin@123** → `/admin`.

### Monitor Tab — Full System Visibility

| Section | What It Shows |
|---|---|
| SMEs | All registered businesses, their status, promoters |
| Applications | All applications across all users, full pipeline |
| KYC | All KYC records, pending queue |
| Offers | All offers issued |
| Delinquencies | System-wide delinquency overview |
| Risk Metrics | Portfolio risk snapshot |
| Audit Logs | Complete action history |

> Admin sees everything but **cannot create or edit** SME records or applications — view-only enforcement is in the UI and backend.

---

## Automated Nightly Scheduler

The `PenalSchedulingService` runs at **midnight (00:00)** daily:

1. Finds all **ACTIVE** loan accounts
2. For each: identifies overdue unpaid installments
3. Calculates `finePerDay × DPD` from the product's PENAL fee config
4. Posts a consolidated **PENAL charge** to the loan
5. Adds the penal amount to the **next DUE installment** (raises total EMI due)
6. Refreshes the DPD/delinquency record
7. Sends an in-app + WebSocket notification to the borrower

To trigger this manually during a demo (without waiting for midnight), call via Swagger:

```
POST /actuator  (not exposed by default — trigger via IDE or cron expression change)
```

Or temporarily change the cron in `PenalSchedulingService.java`:
```java
@Scheduled(cron = "0 * * * * *")  // every minute — for demo only
```

---

## Key Business Rules to Demonstrate

| Rule | How to Trigger | Expected Outcome |
|---|---|---|
| Income-based scoring | Monthly income × 12 ≥ minIncomeAmount | Band = EXCELLENT, eligible for auto-approval |
| Auto-approve threshold | EXCELLENT band + score ≥ 750 | Application auto-approved, no UW action |
| UW hard lock | Score < 700 (POOR band) | UW approve button blocked with error |
| PENAL-only charges | Log in as COLLECTIONS | Charge type dropdown shows only PENAL |
| Admin view-only | Log in as ADMIN, open any SME | Edit/New Application buttons disabled |
| Delinquency negative check | Skip a repayment past due date | Loan appears in Delinquency Register |

---

## Postman / Swagger Quick-Test Checklist

Use **http://localhost:8081/swagger** to test directly.

1. **Authenticate**: `POST /auth/login` → copy the `token` from response
2. **Authorize in Swagger**: Click **Authorize** → enter `Bearer <token>`
3. **Verify delinquencies**: `GET /collections/delinquencies` → should return all delinquency rows
4. **Check scorecard**: `GET /uw/applications/{applicationId}/scorecard` → verify `modelVersion: v2.0-income-weighted`
5. **Post charge**: `POST /collections/charges` with `chargeType: PENAL`
6. **View schedule**: `GET /servicing/loan-accounts/{loanAccountId}/schedule`
7. **Get risk metrics**: `GET /risk/portfolio/metrics`

### If `GET /collections/delinquencies` returns `[]`
- Confirm repayment schedules exist: `GET /servicing/loan-accounts/{id}/schedule`
- Confirm at least one installment has `dueDate < today` AND `status != PAID`
- The endpoint auto-refreshes delinquency records on every call — if no overdue schedules exist, only pre-existing delinquency table rows are returned
- Check `http://localhost:8081/swagger` for any 401/403 — ensure token has `COLLECTIONS` or `ADMIN` role

---

## Complete Role Flow Summary

```
[APPLICANT/AGENT]           [AGENT]               [UNDERWRITER]
  Register SME          →   KYC Verify         →   Review Score
  Add Promoters             Upload Docs             Approve / Reject
  Apply for Loan                                         ↓
       ↓                                          [OPERATIONS]
  Submit Application    ─────────────────────→   Generate Offer
  (DecisionEngine runs)                           Disburse Loan
       ↓                                               ↓
  Accept Offer          ←───────────────────── [SERVICING]
                                                  View Schedule
                                                       ↓
                                               [REPAYMENTS]
                                                  Post Repayment
                                                       ↓
                                               [COLLECTIONS]
                                                  DPD Monitor
                                                  Create PTP
                                                  Post Charges
                                                       ↓
                         [RISK]                [COMPLIANCE]
                         Portfolio Metrics      Audit Logs
                               ↑
                         [ADMIN]
                         Full Monitor View
                         User & Product Mgmt
```