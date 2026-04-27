/**
 * FinServe — TypeScript Models
 * All interfaces match Spring Boot DTOs exactly.
 * Backend: http://localhost:8081  |  Auth: JWT Bearer
 */

// ── Enums ──────────────────────────────────────────────────────────
export type RoleName     = 'ADMIN'|'APPLICANT'|'AGENT'|'UNDERWRITER'|'OPERATIONS'|'SERVICING'|'COLLECTIONS'|'RISK'|'COMPLIANCE';
export type AppStatus    = 'DRAFT'|'KYC_PENDING'|'READY_TO_SUBMIT'|'SUBMITTED'|'ROUTED_TO_UW'|'UW_APPROVED'|'UW_REJECTED'|'OFFERED'|'OFFER_ACCEPTED'|'OFFER_REJECTED'|'DISBURSED';
export type KycStatus    = 'PENDING'|'VERIFIED'|'REJECTED';

/** POST /kyc/initialize */
export interface InitKycRequest { loanApplicationId: number; notes?: string; }

// ── KYC promoter snapshot ────────────────────────────────────
export interface KycPromoterDto {
  promoterId:   number;
  promoterName: string;
  ownershipPct: number;
  main:         boolean;  // true → mandatory doc subject
  kycStatus:    string;
  mobile?:      string;
}

export interface KycResponse {
  kycId:              number;
  loanApplicationId?: number;
  smeId:              number;
  smeLegalName?:      string;
  applicantId?:       number;
  applicantEmail?:    string;
  applicantFullName?: string;
  promoters?:         KycPromoterDto[];
  mainPromoterId?:    number;
  createdByUserId?:   number;
  createdByEmail?:    string;
  verificationStatus: KycStatus;
  notes?:             string;
  createdAt?:         string;
  verifiedAt?:        string;
  verifiedByUserId?:  number;
  verifiedByEmail?:   string;
  canEdit?:           boolean;
}

export type OfferStatus  = 'OFFERED'|'ACCEPTED'|'REJECTED'|'EXPIRED';
export type UwDecision   = 'APPROVE'|'REJECT'|'RETURN';
export type DisburseMode = 'NEFT'|'IMPS'|'UPI';
export type RepayMode    = 'CASH'|'UPI'|'IMPS'|'NEFT'|'RTGS'|'OTHER';
export type BizType      = 'PROPRIETORSHIP'|'PARTNERSHIP'|'PVT_LTD'|'LLP'|'OTHER';
export type StatusFlag   = 'ACTIVE'|'INACTIVE';
export type PtpStatus    = 'OPEN'|'KEPT'|'BROKEN'|'CANCELLED';
export type InstStatus   = 'DUE'|'PAID'|'OVERDUE'|'PARTIAL';
// KYC documents (identity & legitimacy — Agent-reviewed)
export type KycDocType = 'PAN'|'AADHAAR'|'BUSINESS_REG_CERT'|'GST_CERTIFICATE'|'PROMOTER_PHOTO'|'SHOP_LICENSE';
// Financial documents (cash flow & risk — Underwriter-reviewed)
export type FinDocType = 'BANK_STATEMENT'|'ITR'|'BALANCE_SHEET'|'PROFIT_LOSS'|'GST_RETURNS'|'AUDIT_REPORT';
// All document types
export type DocType = KycDocType | FinDocType | 'GST' | 'OTHER';

// ── API Wrapper ────────────────────────────────────────────────────
export interface ApiResponse<T> { success: boolean; message: string; data: T; }

// ── Auth ───────────────────────────────────────────────────────────
export interface AuthState { token: string|null; userId: number|null; email: string|null; role: RoleName|null; }
export interface LoginRequest    { email: string; password: string; }
export interface LoginResponse   { userId: number; email: string; role: RoleName; token: string; }
export interface RegisterRequest { fullName: string; email: string; password: string; phone?: string; role: 'APPLICANT'; bankAccountNo: string; ifsc: string; }
export interface RegisterResponse{ userId: number; email: string; role: RoleName; token: string; }

// ── SME ───────────────────────────────────────────────────────────
export interface CreateSmeRequest { legalName: string; tradeName?: string; registrationNo?: string; businessType: BizType; industry: string; address: string; gstNo?: string; }
export interface UpdateSmeRequest { legalName: string; tradeName?: string; registrationNo?: string; businessType: BizType; industry: string; address: string; gstNo?: string; }
export interface SmeResponse      { smeId: number; legalName: string; tradeName?: string; registrationNo?: string; businessType: BizType; industry: string; address: string; gstNo?: string; status: StatusFlag; createdByUserId?: number; createdByEmail?: string; }

// ── Promoter ───────────────────────────────────────────────────────
export interface AddPromoterRequest {
  promoterName:   string;
  mobile:         string;
  email?:         string;
  ownershipPct:   number;
  panNumber?:     string;
  aadhaarNumber?: string;
  din?:           string;
  dateOfBirth?:   string;
}
export interface PromoterResponse   { promoterId: number; smeId: number; promoterName: string; mobile: string; ownershipPct: number; kycStatus: KycStatus; createdByUserId?: number; createdByEmail?: string; }

// ── KYC ────────────────────────────────────────────────────────────
/** Refactored: sme_id, promoter_id, applicant_id as explicit FK fields */
export interface KycActionRequest { notes?: string; }


// ── Applications ───────────────────────────────────────────────────
export interface CreateAppRequest { smeId: number; productId: number; requestedAmount: number; tenorMonths: number; purposeNote?: string; }
export interface AppResponse      { applicationId: number; smeId: number; smeLegalName?: string; productId: number; productName?: string; requestedAmount: number; tenorMonths: number; purposeNote?: string; status: AppStatus; createdByUserId?: number; createdByEmail?: string; createdDate?: string; submittedAt?: string; }

// ── Documents ──────────────────────────────────────────────────────
export interface AddDocRequest { docType: DocType; fileUri: string; }
export interface DocResponse   {
  documentId: number;
  applicationId: number;
  docType: DocType;
  fileUri?: string;
  fileName?: string;
  contentType?: string;
  downloadUrl?: string;
  uploadedDate?: string;
  lastReplacedDate?: string;
  replaced?: boolean;
}

// ── Loan Products ──────────────────────────────────────────────────
export interface LoanProductRequest  { productName: string; minAmount: number; maxAmount: number; minTenorMonths: number; maxTenorMonths: number; baseInterestRate: number; creditThreshold?: number; minIncomeAmount?: number; maxIncomeAmount?: number; }
export interface LoanProductResponse { productId: number; productName: string; minAmount: number; maxAmount: number; minTenorMonths: number; maxTenorMonths: number; baseInterestRate: number; status: StatusFlag; creditThreshold?: number; minIncomeAmount?: number; maxIncomeAmount?: number; }

// ── Credit Score ────────────────────────────────────────────────────

// ── Underwriting ───────────────────────────────────────────────────
export interface UwDecisionRequest { decision: UwDecision; summaryNote?: string; }
export interface UwReviewResponse  { reviewId: number; applicationId: number; decision: UwDecision; summaryNote?: string; underwriterEmail?: string; newApplicationStatus: AppStatus; }

// ── Offers ─────────────────────────────────────────────────────────
export interface CreateOfferRequest { sanctionedAmount: number; interestRate: number; emiAmount: number; validUntil: string; }
export interface OfferResponse      { offerId: number; applicationId: number; sanctionedAmount: number; interestRate: number; emiAmount: number; validUntil?: string; offerStatus: OfferStatus; createdByEmail?: string; createdByUserId?: number; createdDate?: string; }

// ── Disbursement ───────────────────────────────────────────────────
export interface DisburseRequest     { mode: DisburseMode; transactionRef?: string; disbursementDate: string; }
export interface DisburseResponse    { disbursementId: number; applicationId: number; amount: number; mode: DisburseMode; transactionRef?: string; disbursementDate: string; status: string; loanAccount: LoanAccountResponse; }
export interface LoanAccountResponse { loanAccountId: number; applicationId?: number; accountNumber: string; principalSanctioned: number; interestRate: number; tenorMonths: number; startDate?: string; status: string; }

// ── Schedule ───────────────────────────────────────────────────────
export interface ScheduleResponse { scheduleId: number; loanAccountId: number; installmentNo: number; dueDate: string; principalDue: number; interestDue: number; totalDue: number; amountPaid: number; balanceDue: number; status: InstStatus; penalAmount?: number; }

export interface PromoterDocResponse {
  docId: number;
  promoterId: number;
  docType: DocType;
  fileName?: string;
  contentType?: string;
  downloadUrl?: string;
  uploadStatus: string;
  uploadedDate: string;
  lastReplacedDate?: string;
  replaced?: boolean;
}

// ── Repayment ──────────────────────────────────────────────────────
export interface PostRepayRequest    { loanAccountId: number; scheduleId?: number; amount?: number; mode: string; referenceNo?: string; paymentDate?: string; }
export interface RepayResponse    { repaymentId: number; loanAccountId: number; amount: number; mode: RepayMode; referenceNo?: string; paymentDate: string; }

// ── Offer Acceptance ────────────────────────────────────────────────
export interface OfferAcceptRequest { offerId: number; digitalSignature: string; termsAccepted: boolean; }

// ── Collections ────────────────────────────────────────────────────
export interface CreatePtpRequest    { loanAccountId: number; promiseDate: string; promisedAmount: number; notes?: string; }
export interface PtpResponse { ptpId: number; loanAccountId: number; promiseDate: string; promisedAmount: number; status: PtpStatus; notes?: string; createdByUserId?: number; createdByEmail?: string; createdDate?: string; }
export interface DelinqResponse   { delinquencyId: number; loanAccountId: number; dpd: number; bucket: string; asOfDate?: string; }

// ── Risk ───────────────────────────────────────────────────────────
export interface PortfolioMetrics { totalLoanAccounts: number; activeLoanAccounts: number; delinquentLoanAccounts: number; bucketCounts: Record<string, number>; }

// ── Compliance ─────────────────────────────────────────────────────
export interface AuditLogResponse { auditId: number; action: string; refType: string; refId: number;
  entityType?: string;  // alias for refType
  entityId?: number;    // alias for refId
  actorUserId: number; actorEmail?: string; message?: string; createdDate?: string; }

// ── Admin ──────────────────────────────────────────────────────────
export interface CreateUserRequest { fullName: string; email: string; password: string; phone?: string; role: RoleName; bankAccountNo?: string; ifsc?: string; }
export interface UserResponse      { userId: number; fullName: string; email: string; phone?: string; role: RoleName; status: StatusFlag; }
export interface RoleResponse      { roleId: number; roleName: RoleName; status?: StatusFlag; }

// ══════════════════════════════════════════════════════════════
//  ELIGIBILITY POLICY
// ══════════════════════════════════════════════════════════════
export interface EligibilityPolicyRequest {
  productId:                  number;
  ruleName:                   string;
  ruleExpression?:            string;
  maxAmountCap?:              number;
  minCreditScore?:            number;
  minBusinessVintageMonths?:  number;
  maxExistingLoans?:          number;
  minDscr?:                   number;
}
export interface EligibilityPolicyResponse {
  policyId:                   number;
  productId:                  number;
  productName?:               string;
  ruleName:                   string;
  ruleExpression?:            string;
  maxAmountCap?:              number;
  minCreditScore?:            number;
  minBusinessVintageMonths?:  number;
  maxExistingLoans?:          number;
  minDscr?:                   number;
  status:                     StatusFlag;
  createdAt?:                 string;
}
export interface EligibilityCheckResult {
  eligible:     boolean;
  passedRules:  string[];
  failedRules:  string[];
  summary:      string;
}

// ══════════════════════════════════════════════════════════════
//  FEE CONFIG
// ══════════════════════════════════════════════════════════════
export type FeeType = 'PROCESSING'|'INSURANCE'|'TECH'|'LEGAL'|'OTHER';
export type FeeMode = 'FLAT'|'PERCENT';

export interface FeeConfigRequest {
  productId:      number;
  feeType:        FeeType;
  feeMode:        FeeMode;
  value:          number;
  effectiveFrom?: string;
  effectiveTo?:   string;
}
export interface FeeConfigResponse {
  feeId:          number;
  productId:      number;
  productName?:   string;
  feeType:        FeeType;
  feeMode:        FeeMode;
  value:          number;
  effectiveFrom?: string;
  effectiveTo?:   string;
  status:         StatusFlag;
}
export interface AppliedFeeDto {
  feeType:           string;
  feeMode:           string;
  configuredValue:   number;
  calculatedAmount:  number;
}

// ══════════════════════════════════════════════════════════════
//  SCORECARD + DECISION
// ══════════════════════════════════════════════════════════════
export type ScoreBand    = 'LOW'|'MEDIUM'|'HIGH'|'EXCELLENT';
export type DecisionPath = 'AUTO_APPROVE'|'AUTO_DECLINE'|'ROUTE_TO_UW';

export interface ScorecardResponse {
  scoreId:       number;
  applicationId: number;
  modelVersion?: string;
  inputsJson?:   string;
  scoreValue:    number;
  scoreBand:     ScoreBand;
  scoredAt?:     string;
}
export interface DecisionResponse {
  decisionId:     number;
  applicationId:  number;
  path:           DecisionPath;
  reason:         string;
  triggeredRules? :string;
  decidedAt?:     string;
}

// ══════════════════════════════════════════════════════════════
//  CHARGE
// ══════════════════════════════════════════════════════════════
export type ChargeType   = 'PROCESSING'|'PENAL'|'PREPAYMENT'|'INSURANCE'|'TECH'|'OTHER';
export type ChargeStatus = 'OUTSTANDING'|'WAIVED'|'PAID';

export interface ChargeRequest {
  loanAccountId: number;
  chargeType:    ChargeType;
  amount:        number;
  description?:  string;
  chargeDate?:   string;
}
export interface ChargeResponse {
  chargeId:      number;
  loanAccountId: number;
  chargeType:    ChargeType;
  amount:        number;
  description?:  string;
  chargeDate?:   string;
  status:        ChargeStatus;
  createdAt?:    string;
}

// ── Pending Disbursements ──────────────────────────────────────────
export interface PendingDisbursementDto {
  applicationId:     number;
  smeLegalName:      string;
  smeId:             string;
  productName:       string;
  applicantEmail:    string;
  applicationStatus: string;
  submittedAt?:      string;
  offerId:           number;
  sanctionedAmount:  number;
  interestRate:      number;
  emiAmount:         number;
  offerValidUntil?:  string;
  offerStatus:       string;
  offerCreatedAt?:   string;
}
