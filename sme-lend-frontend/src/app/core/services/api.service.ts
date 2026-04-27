import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import {
  ApiResponse,
  SmeResponse, CreateSmeRequest, UpdateSmeRequest,
  PromoterResponse, AddPromoterRequest,
  KycResponse, KycActionRequest,
  AppResponse, CreateAppRequest,
  DocResponse, PromoterDocResponse, AddDocRequest, DocType,
  LoanProductResponse, LoanProductRequest, StatusFlag,
  UwReviewResponse, UwDecisionRequest,
  CreateOfferRequest, OfferResponse,
  DisburseRequest, DisburseResponse, LoanAccountResponse, PendingDisbursementDto,
  ScheduleResponse, RepayResponse, PostRepayRequest,
  DelinqResponse, PtpResponse, CreatePtpRequest, PtpStatus,
  PortfolioMetrics, AuditLogResponse,
  UserResponse, CreateUserRequest, RoleResponse,
  EligibilityPolicyRequest, EligibilityPolicyResponse, EligibilityCheckResult,
  FeeConfigRequest, FeeConfigResponse,
  ScorecardResponse, DecisionResponse,
  ChargeRequest, ChargeResponse,
  InitKycRequest,
} from '../models';

@Injectable({ providedIn: 'root' })
export class ApiService {
  private readonly B = environment.apiUrl;
  constructor(private http: HttpClient) {}

  // ══ SME ═══════════════════════════════════════════════════════════
  createSme(b: CreateSmeRequest): Observable<ApiResponse<SmeResponse>> {
    return this.http.post<ApiResponse<SmeResponse>>(`${this.B}/onboarding/smes`, b);
  }
  updateSme(id: number, b: UpdateSmeRequest): Observable<ApiResponse<SmeResponse>> {
    return this.http.put<ApiResponse<SmeResponse>>(`${this.B}/onboarding/smes/${id}`, b);
  }
  listSmes(): Observable<ApiResponse<SmeResponse[]>> {
    return this.http.get<ApiResponse<SmeResponse[]>>(`${this.B}/onboarding/smes`);
  }
  getSme(id: number): Observable<ApiResponse<SmeResponse>> {
    return this.http.get<ApiResponse<SmeResponse>>(`${this.B}/onboarding/smes/${id}`);
  }

  // ══ PROMOTERS ═════════════════════════════════════════════════════
  addPromoter(smeId: number, b: AddPromoterRequest): Observable<ApiResponse<PromoterResponse>> {
    return this.http.post<ApiResponse<PromoterResponse>>(`${this.B}/onboarding/smes/${smeId}/promoters`, b);
  }
  listPromoters(smeId: number): Observable<ApiResponse<PromoterResponse[]>> {
    return this.http.get<ApiResponse<PromoterResponse[]>>(`${this.B}/onboarding/smes/${smeId}/promoters`);
  }
  getPromoter(id: number): Observable<ApiResponse<PromoterResponse>> {
    return this.http.get<ApiResponse<PromoterResponse>>(`${this.B}/onboarding/promoters/${id}`);
  }

  // ══ KYC ═══════════════════════════════════════════════════════════

  /** POST /kyc/initialize — auto-create KYC snapshot from LoanApplication participants */
  initializeKyc(loanApplicationId: number, notes?: string): Observable<ApiResponse<KycResponse>> {
    return this.http.post<ApiResponse<KycResponse>>(`${this.B}/kyc/initialize`, { loanApplicationId, notes });
  }

  /** GET /applications/{appId}/kyc-readiness — check all KYC before submit */
  checkKycReadiness(appId: number): Observable<ApiResponse<{ready: boolean; failures: string[]; summary: string}>> {
    return this.http.get<ApiResponse<{ready: boolean; failures: string[]; summary: string}>>(`${this.B}/kyc/applications/${appId}/readiness`);
  }

  /** GET /kyc/application/{appId} — fetch KYC tied to a specific application */
  getKycByApplication(appId: number): Observable<ApiResponse<KycResponse>> {
    return this.http.get<ApiResponse<KycResponse>>(`${this.B}/kyc/application/${appId}`);
  }

  /** GET /kyc/{kycId} — single KYC record with all promoter links */
  getKycById(kycId: number): Observable<ApiResponse<KycResponse>> {
    return this.http.get<ApiResponse<KycResponse>>(`${this.B}/kyc/${kycId}`);
  }

  // createKyc removed — use initializeKyc(loanApplicationId) instead
  listPendingKyc(): Observable<ApiResponse<KycResponse[]>> {
    return this.http.get<ApiResponse<KycResponse[]>>(`${this.B}/kyc/pending`);
  }
  listKycBySme(smeId: number): Observable<ApiResponse<KycResponse[]>> {
    return this.http.get<ApiResponse<KycResponse[]>>(`${this.B}/kyc/smes/${smeId}`);
  }
  verifyKyc(kycId: number, b?: KycActionRequest): Observable<ApiResponse<KycResponse>> {
    return this.http.patch<ApiResponse<KycResponse>>(`${this.B}/kyc/${kycId}/verify`, b ?? {});
  }
  rejectKyc(kycId: number, b?: KycActionRequest): Observable<ApiResponse<KycResponse>> {
    return this.http.patch<ApiResponse<KycResponse>>(`${this.B}/kyc/${kycId}/reject`, b ?? {});
  }

  // ══ APPLICATIONS ══════════════════════════════════════════════════
  createApp(b: CreateAppRequest): Observable<ApiResponse<AppResponse>> {
    return this.http.post<ApiResponse<AppResponse>>(`${this.B}/applications`, b);
  }
  /** Applicant's own apps */
  listApps(): Observable<ApiResponse<AppResponse[]>> {
    return this.http.get<ApiResponse<AppResponse[]>>(`${this.B}/applications`);
  }
  /** Agent / Admin / UW: all applications */
  listAllApps(): Observable<ApiResponse<AppResponse[]>> {
    return this.http.get<ApiResponse<AppResponse[]>>(`${this.B}/applications/all`);
  }
  getApp(id: number): Observable<ApiResponse<AppResponse>> {
    return this.http.get<ApiResponse<AppResponse>>(`${this.B}/applications/${id}`);
  }
  submitApp(id: number): Observable<ApiResponse<AppResponse>> {
    return this.http.patch<ApiResponse<AppResponse>>(`${this.B}/applications/${id}/submit`, {});
  }

  // ══ DOCUMENTS ═════════════════════════════════════════════════════
  addDoc(appId: number, b: AddDocRequest): Observable<ApiResponse<DocResponse>> {
    return this.http.post<ApiResponse<DocResponse>>(`${this.B}/applications/${appId}/documents`, b);
  }
  listDocs(appId: number): Observable<ApiResponse<DocResponse[]>> {
    return this.http.get<ApiResponse<DocResponse[]>>(`${this.B}/applications/${appId}/documents`);
  }
  uploadDoc(appId: number, file: File, docType: DocType): Observable<ApiResponse<DocResponse>> {
    const fd = new FormData();
    fd.append('file', file);
    fd.append('docType', docType);
    return this.http.post<ApiResponse<DocResponse>>(
      `${this.B}/applications/${appId}/documents/upload`, fd);
  }
  uploadPromoterDoc(promoterId: number, file: File, docType: string): Observable<ApiResponse<PromoterDocResponse>> {
    const fd = new FormData();
    fd.append('file', file);
    return this.http.post<ApiResponse<PromoterDocResponse>>(
      `${this.B}/onboarding/promoters/${promoterId}/documents/${docType}/upload`, fd);
  }

  listPromoterDocs(promoterId: number): Observable<ApiResponse<PromoterDocResponse[]>> {
    return this.http.get<ApiResponse<PromoterDocResponse[]>>(
      `${this.B}/onboarding/promoters/${promoterId}/documents`);
  }

  downloadPromoterDoc(promoterId: number, docType: string): Observable<Blob> {
    return this.http.get(
      `${this.B}/onboarding/promoters/${promoterId}/documents/${docType}/download`,
      { responseType: 'blob' });
  }

  downloadDoc(appId: number, docId: number): Observable<Blob> {
    return this.http.get(
      `${this.B}/applications/${appId}/documents/${docId}/download`,
      { responseType: 'blob' }
    );
  }

  // ══ ADMIN — USER MANAGEMENT ═══════════════════════════════════════
  listRoles(): Observable<ApiResponse<RoleResponse[]>> {
    return this.http.get<ApiResponse<RoleResponse[]>>(`${this.B}/admin/roles`);
  }
  createUser(b: CreateUserRequest): Observable<ApiResponse<UserResponse>> {
    return this.http.post<ApiResponse<UserResponse>>(`${this.B}/admin/users`, b);
  }
  listUsers(): Observable<ApiResponse<UserResponse[]>> {
    return this.http.get<ApiResponse<UserResponse[]>>(`${this.B}/admin/users`);
  }
  setUserStatus(uid: number, status: StatusFlag): Observable<ApiResponse<UserResponse>> {
    return this.http.patch<ApiResponse<UserResponse>>(
      `${this.B}/admin/users/${uid}/status`, {},
      { params: new HttpParams().set('status', status) }
    );
  }

  // ══ ADMIN — LOAN PRODUCTS ═════════════════════════════════════════
  createProduct(b: LoanProductRequest): Observable<ApiResponse<LoanProductResponse>> {
    return this.http.post<ApiResponse<LoanProductResponse>>(`${this.B}/admin/loan-products`, b);
  }
  listProducts(): Observable<ApiResponse<LoanProductResponse[]>> {
    return this.http.get<ApiResponse<LoanProductResponse[]>>(`${this.B}/admin/loan-products`);
  }
  /** Public endpoint — available to all authenticated roles */
  listActiveProducts(): Observable<ApiResponse<LoanProductResponse[]>> {
    return this.http.get<ApiResponse<LoanProductResponse[]>>(`${this.B}/loan-products`);
  }
  updateProduct(id: number, b: LoanProductRequest): Observable<ApiResponse<LoanProductResponse>> {
    return this.http.put<ApiResponse<LoanProductResponse>>(`${this.B}/admin/loan-products/${id}`, b);
  }
  setProductStatus(id: number, status: StatusFlag): Observable<ApiResponse<LoanProductResponse>> {
    return this.http.patch<ApiResponse<LoanProductResponse>>(
      `${this.B}/admin/loan-products/${id}/status`, {},
      { params: new HttpParams().set('status', status) }
    );
  }
  deleteProduct(id: number): Observable<ApiResponse<void>> {
    return this.http.delete<ApiResponse<void>>(`${this.B}/admin/loan-products/${id}`);
  }

  // ══ ADMIN MONITOR (GET-only cross-role views) ═════════════════════
  monitorAllKyc(): Observable<ApiResponse<KycResponse[]>> {
    return this.http.get<ApiResponse<KycResponse[]>>(`${this.B}/admin/monitor/kyc`);
  }
  monitorAllApps(): Observable<ApiResponse<AppResponse[]>> {
    return this.http.get<ApiResponse<AppResponse[]>>(`${this.B}/admin/monitor/applications`);
  }
  monitorRiskMetrics(): Observable<ApiResponse<PortfolioMetrics>> {
    return this.http.get<ApiResponse<PortfolioMetrics>>(`${this.B}/admin/monitor/risk/metrics`);
  }
  monitorAuditLogs(): Observable<ApiResponse<AuditLogResponse[]>> {
    return this.http.get<ApiResponse<AuditLogResponse[]>>(`${this.B}/admin/monitor/audit-logs`);
  }
  monitorDelinquencies(): Observable<ApiResponse<DelinqResponse[]>> {
    return this.http.get<ApiResponse<DelinqResponse[]>>(`${this.B}/admin/monitor/delinquencies`);
  }

  // ══ UNDERWRITING ══════════════════════════════════════════════════
  getUwQueue(): Observable<ApiResponse<AppResponse[]>> {
    return this.http.get<ApiResponse<AppResponse[]>>(`${this.B}/uw/queue`);
  }
  submitUwDecision(appId: number, b: UwDecisionRequest): Observable<ApiResponse<UwReviewResponse>> {
    return this.http.post<ApiResponse<UwReviewResponse>>(`${this.B}/uw/applications/${appId}/decision`, b);
  }

  // ══ OPERATIONS ════════════════════════════════════════════════════
  listApprovedApps(): Observable<ApiResponse<AppResponse[]>> {
    return this.http.get<ApiResponse<AppResponse[]>>(`${this.B}/ops/applications/approved`);
  }
  createOffer(appId: number, b: CreateOfferRequest): Observable<ApiResponse<OfferResponse>> {
    return this.http.post<ApiResponse<OfferResponse>>(`${this.B}/offers/applications/${appId}`, b);
  }
  getOffer(offerId: number): Observable<ApiResponse<OfferResponse>> {
    return this.http.get<ApiResponse<OfferResponse>>(`${this.B}/offers/${offerId}`);
  }
  acceptOffer(offerId: number): Observable<ApiResponse<OfferResponse>> {
    return this.http.patch<ApiResponse<OfferResponse>>(`${this.B}/offers/${offerId}/accept`, {});
  }
  rejectOffer(offerId: number): Observable<ApiResponse<OfferResponse>> {
    return this.http.patch<ApiResponse<OfferResponse>>(`${this.B}/offers/${offerId}/reject`, {});
  }
  listOffers(): Observable<ApiResponse<OfferResponse[]>> {
    return this.http.get<ApiResponse<OfferResponse[]>>(`${this.B}/offers`);
  }
  listPendingDisbursements(): Observable<ApiResponse<PendingDisbursementDto[]>> {
    return this.http.get<ApiResponse<PendingDisbursementDto[]>>(`${this.B}/ops/pending-disbursements`);
  }

  disburse(appId: number, b: DisburseRequest): Observable<ApiResponse<DisburseResponse>> {
    return this.http.post<ApiResponse<DisburseResponse>>(`${this.B}/ops/applications/${appId}/disburse`, b);
  }
  getLoanAccount(id: number): Observable<ApiResponse<LoanAccountResponse>> {
    return this.http.get<ApiResponse<LoanAccountResponse>>(`${this.B}/ops/loan-accounts/${id}`);
  }

  /** GET /loan-accounts/by-application/{appId} */
  getLoanAccountByApplication(appId: number): Observable<ApiResponse<LoanAccountResponse>> {
    return this.http.get<ApiResponse<LoanAccountResponse>>(`${this.B}/servicing/loan-accounts/by-application/${appId}`);
  }

  // ══ SERVICING ═════════════════════════════════════════════════════
  getSchedule(loanAccountId: number): Observable<ApiResponse<ScheduleResponse[]>> {
    return this.http.get<ApiResponse<ScheduleResponse[]>>(
      `${this.B}/servicing/loan-accounts/${loanAccountId}/schedule`);
  }
  postRepayment(b: PostRepayRequest): Observable<ApiResponse<RepayResponse>> {
    return this.http.post<ApiResponse<RepayResponse>>(`${this.B}/servicing/repayments`, b);
  }
  listRepayments(loanAccountId: number): Observable<ApiResponse<RepayResponse[]>> {
    return this.http.get<ApiResponse<RepayResponse[]>>(
      `${this.B}/servicing/repayments/loan-accounts/${loanAccountId}`);
  }

  // ══ COLLECTIONS ═══════════════════════════════════════════════════
  listAllDelinquencies(): Observable<ApiResponse<DelinqResponse[]>> {
    return this.http.get<ApiResponse<DelinqResponse[]>>(`${this.B}/collections/delinquencies`);
  }
  createPtp(b: CreatePtpRequest): Observable<ApiResponse<PtpResponse>> {
    return this.http.post<ApiResponse<PtpResponse>>(`${this.B}/collections/ptp`, b);
  }
  listPtps(loanAccountId: number): Observable<ApiResponse<PtpResponse[]>> {
    return this.http.get<ApiResponse<PtpResponse[]>>(
      `${this.B}/collections/loan-accounts/${loanAccountId}/ptp`);
  }
  updatePtpStatus(ptpId: number, status: PtpStatus): Observable<ApiResponse<PtpResponse>> {
    return this.http.patch<ApiResponse<PtpResponse>>(
      `${this.B}/collections/ptp/${ptpId}/status`, {},
      { params: new HttpParams().set('status', status) }
    );
  }

  // ══ RISK ══════════════════════════════════════════════════════════
  getPortfolioMetrics(): Observable<ApiResponse<PortfolioMetrics>> {
    return this.http.get<ApiResponse<PortfolioMetrics>>(`${this.B}/risk/portfolio/metrics`);
  }

  // ══ COMPLIANCE ════════════════════════════════════════════════════
  listAuditLogs(): Observable<ApiResponse<AuditLogResponse[]>> {
    return this.http.get<ApiResponse<AuditLogResponse[]>>(`${this.B}/compliance/audit-logs`);
  }

  // ── Eligibility Policies (Admin) ──────────────────────────────────
  listEligibilityPolicies(): Observable<ApiResponse<EligibilityPolicyResponse[]>> {
    return this.http.get<ApiResponse<EligibilityPolicyResponse[]>>(`${this.B}/admin/eligibility-policies`);
  }
  listPoliciesByProduct(productId: number): Observable<ApiResponse<EligibilityPolicyResponse[]>> {
    return this.http.get<ApiResponse<EligibilityPolicyResponse[]>>(`${this.B}/admin/eligibility-policies/product/${productId}`);
  }
  createEligibilityPolicy(b: EligibilityPolicyRequest): Observable<ApiResponse<EligibilityPolicyResponse>> {
    return this.http.post<ApiResponse<EligibilityPolicyResponse>>(`${this.B}/admin/eligibility-policies`, b);
  }
  deactivatePolicy(id: number): Observable<ApiResponse<void>> {
    return this.http.delete<ApiResponse<void>>(`${this.B}/admin/eligibility-policies/${id}`);
  }
  checkEligibility(applicationId: number): Observable<ApiResponse<EligibilityCheckResult>> {
    return this.http.get<ApiResponse<EligibilityCheckResult>>(`${this.B}/admin/eligibility-check/${applicationId}`);
  }

  // ── Fee Configs (Admin/Ops) ───────────────────────────────────────
  listFeeConfigs(): Observable<ApiResponse<FeeConfigResponse[]>> {
    return this.http.get<ApiResponse<FeeConfigResponse[]>>(`${this.B}/admin/fee-configs`);
  }
  listFeesByProduct(productId: number): Observable<ApiResponse<FeeConfigResponse[]>> {
    return this.http.get<ApiResponse<FeeConfigResponse[]>>(`${this.B}/admin/fee-configs/product/${productId}`);
  }
  createFeeConfig(b: FeeConfigRequest): Observable<ApiResponse<FeeConfigResponse>> {
    return this.http.post<ApiResponse<FeeConfigResponse>>(`${this.B}/admin/fee-configs`, b);
  }
  deactivateFee(id: number): Observable<ApiResponse<void>> {
    return this.http.delete<ApiResponse<void>>(`${this.B}/admin/fee-configs/${id}`);
  }

  // ── Scorecard + Decision ──────────────────────────────────────────
  getScorecard(applicationId: number): Observable<ApiResponse<ScorecardResponse>> {
    return this.http.get<ApiResponse<ScorecardResponse>>(`${this.B}/uw/applications/${applicationId}/scorecard`);
  }
  getDecision(applicationId: number): Observable<ApiResponse<DecisionResponse>> {
    return this.http.get<ApiResponse<DecisionResponse>>(`${this.B}/uw/applications/${applicationId}/decision-result`);
  }
  runScoring(applicationId: number): Observable<ApiResponse<DecisionResponse>> {
    return this.http.post<ApiResponse<DecisionResponse>>(`${this.B}/uw/applications/${applicationId}/rescore`, {});
  }

  // ── Charges ───────────────────────────────────────────────────────
  postCharge(b: ChargeRequest): Observable<ApiResponse<ChargeResponse>> {
    return this.http.post<ApiResponse<ChargeResponse>>(`${this.B}/collections/charges`, b);
  }
  listCharges(loanAccountId: number): Observable<ApiResponse<ChargeResponse[]>> {
    return this.http.get<ApiResponse<ChargeResponse[]>>(`${this.B}/collections/charges/loan/${loanAccountId}`);
  }
  waiveCharge(chargeId: number): Observable<ApiResponse<ChargeResponse>> {
    return this.http.patch<ApiResponse<ChargeResponse>>(`${this.B}/collections/charges/${chargeId}/waive`, {});
  }

}
