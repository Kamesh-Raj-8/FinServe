import { Component, OnInit, signal, inject, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators, FormsModule } from '@angular/forms';
import { ApiService } from '../../core/services/api.service';
import { AuthService } from '../../core/services/auth.service';
import { paginate } from '../../core/services/pagination.service';
import { PaginatorComponent } from '../../shared/paginator/paginator.component';
import { ToastService } from '../../core/services/toast.service';
import { DocPreviewComponent } from '../../shared/doc-preview/doc-preview.component';
import {
  SmeResponse, PromoterResponse, KycResponse, AppResponse,
  DocResponse, LoanProductResponse, BizType, DocType, KycDocType, FinDocType,
  KycPromoterDto,
  LoanAccountResponse, ScheduleResponse, OfferResponse
} from '../../core/models';

type WorkflowStep = 'sme' | 'kyc' | 'application' | 'documents' | 'status' | 'loan-details';

@Component({
  selector: 'app-applicant',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, FormsModule, DocPreviewComponent, PaginatorComponent],
  templateUrl: './applicant.component.html'
})
export class ApplicantComponent implements OnInit {
  private api   = inject(ApiService);
  private fb    = inject(FormBuilder);
  readonly auth = inject(AuthService);
  private toast = inject(ToastService);

  // ── Read-Only mode for non-applicant roles viewing this page ──────
  isReadOnly = computed(() => !this.auth.hasRole('APPLICANT'));

  // ── Workflow State ────────────────────────────────────────────────
  step       = signal<WorkflowStep>('sme');
  loadingInit = signal(true);

  // ── SME Data ─────────────────────────────────────────────────────
  mySmes       = signal<SmeResponse[]>([]);
  selectedSme  = signal<SmeResponse | null>(null);
  promoters    = signal<PromoterResponse[]>([]);
  savingSme    = signal(false);
  showSmeForm  = signal(false);

  smeForm = this.fb.group({
    legalName:      ['', Validators.required],
    tradeName:      [''],
    registrationNo: [''],
    businessType:   ['PROPRIETORSHIP', Validators.required],
    industry:       ['', Validators.required],
    address:        ['', Validators.required],
    gstNo:          ['']
  });
  get sf() { return this.smeForm.controls; }
  bizTypes: BizType[] = ['PROPRIETORSHIP','PARTNERSHIP','PVT_LTD','LLP','OTHER'];

  promoterForm = this.fb.group({
    promoterName:   ['', Validators.required],
    mobile:         ['', [Validators.required, Validators.pattern(/^[0-9]{10}$/)]],
    email:          [''],
    ownershipPct:   [null as number|null, [Validators.required, Validators.min(0.01), Validators.max(100)]],
    monthlyIncome:  [null as number|null, [Validators.required, Validators.min(0)]],
    panNumber:      ['', Validators.pattern(/^[A-Z]{5}[0-9]{4}[A-Z]$/)],
    aadhaarNumber:  ['', Validators.pattern(/^[0-9]{12}$/)],
    din:            [''],
    dateOfBirth:    ['']
  });
  get pf() { return this.promoterForm.controls; }
  savingPromoter = signal(false);
  showPromoterForm = signal(false);

  // ── KYC State (auto-initialized from LoanApplication) ──────────
  kycRecord       = signal<KycResponse | null>(null);
  kycPromoterIdInput = 0; // legacy
  kycRecords      = computed(() => this.kycRecord() ? [this.kycRecord()!] : []);
  initializingKyc = signal(false);
  creatingKyc     = signal(false); // kept for legacy guard

  // ── KYC Readiness State ──────────────────────────────────────────
  kycReadiness  = signal<{ready: boolean; failures: string[]; summary: string} | null>(null);
  checkingKyc   = signal(false);

  checkKycReadiness(applicationId: number) {
    this.checkingKyc.set(true);
    this.api.checkKycReadiness(applicationId).subscribe({
      next: r => { this.kycReadiness.set(r.data); this.checkingKyc.set(false); },
      error: () => {
        this.kycReadiness.set({ ready: false, failures: ['Unable to check KYC status.'], summary: '' });
        this.checkingKyc.set(false);
      }
    });
  }

  // ── Application Data ─────────────────────────────────────────────
  myApps       = signal<AppResponse[]>([]);
  products     = signal<LoanProductResponse[]>([]);
  creatingApp  = signal(false);
  showAppForm  = signal(false);
  submittingApp = signal<number|null>(null);

  appForm = this.fb.group({
    smeId:           [null as number|null, [Validators.required, Validators.min(1)]],
    productId:       [null as number|null, Validators.required],
    requestedAmount: [null as number|null, [Validators.required, Validators.min(1)]],
    tenorMonths:     [null as number|null, [Validators.required, Validators.min(1)]],
    purposeNote:     ['']
  });
  get af() { return this.appForm.controls; }

  selectedProduct = computed(() => {
    const pid = this.appForm.controls['productId'].value;
    return pid ? this.products().find(p => p.productId === Number(pid)) ?? null : null;
  });

  // ── Documents ─────────────────────────────────────────────────────
  selectedAppForDocs = signal<AppResponse | null>(null);
  docs               = signal<DocResponse[]>([]);
  loadingDocs        = signal(false);
  uploadingDoc       = signal(false);
  selectedDocType: DocType = 'PAN';
  uploadCategory: 'kyc' | 'financial' = 'kyc';
  docUploadMode: 'file' | 'uri' = 'file';
  docUriInput = '';

  readonly kycDocTypes: { type: KycDocType; label: string; required: boolean; hint: string }[] = [
    { type: 'PAN',              label: 'PAN Card',                   required: true,  hint: 'Individual or Business PAN' },
    { type: 'AADHAAR',          label: 'Aadhaar Card',               required: true,  hint: 'Identity & address proof for applicant/promoters' },
    { type: 'BUSINESS_REG_CERT',label: 'Business Registration Cert', required: true,  hint: 'Udyam registration or incorporation certificate' },
    { type: 'GST_CERTIFICATE',  label: 'GST Certificate',            required: false, hint: 'GST registration certificate (if applicable)' },
    { type: 'PROMOTER_PHOTO',   label: 'Promoter Photo',             required: false, hint: 'Recent passport-size photograph' },
    { type: 'SHOP_LICENSE',     label: 'Shop/Trade License',         required: false, hint: 'Shop & Establishment licence' },
  ];

  readonly finDocTypes: { type: FinDocType; label: string; required: boolean; hint: string }[] = [
    { type: 'BANK_STATEMENT', label: 'Bank Statements (6 months)', required: true,  hint: 'Primary cash flow analysis document' },
    { type: 'ITR',            label: 'ITR (Last 2 years)',          required: true,  hint: 'Income Tax Returns for declared income verification' },
    { type: 'BALANCE_SHEET',  label: 'Audited Balance Sheet',       required: true,  hint: 'Assess debt-to-equity and net worth' },
    { type: 'PROFIT_LOSS',    label: 'P&L Statement',               required: true,  hint: 'Audited Profit & Loss statement' },
    { type: 'GST_RETURNS',    label: 'GST Returns (GSTR-3B)',        required: false, hint: 'Verify actual sales/turnover reported' },
    { type: 'AUDIT_REPORT',   label: 'CA Audit Report',             required: false, hint: 'Chartered Accountant Audit Report' },
  ];
  selectedFile: File | null = null;
  fileError = '';


  ngOnInit() {
    this.api.listActiveProducts().subscribe({ next: r => this.products.set(r.data ?? []) });
    this.api.listSmes().subscribe({
      next: r => {
        this.mySmes.set(r.data ?? []);
        if (r.data?.length) {
          this.selectSme(r.data[0]);
        } else {
          this.showSmeForm.set(true);
        }
        this.loadingInit.set(false);
      },
      error: () => this.loadingInit.set(false)
    });
    // Privacy: filter to only this applicant's own applications (cross-viewing prevention)
    this.api.listApps().subscribe({
      next: r => {
        const myId = this.auth.userId();
        const filtered = (r.data ?? []).filter(a =>
          !myId || a.createdByUserId === myId || !this.auth.hasRole('APPLICANT')
        );
        this.myApps.set(filtered);
      }
    });
  }

  // ── SME ──────────────────────────────────────────────────────────
  selectSme(sme: SmeResponse) {
    this.selectedSme.set(sme);
    this.api.listPromoters(sme.smeId).subscribe({ next: r => this.promoters.set(r.data ?? []) });
    this.api.listKycBySme(sme.smeId).subscribe({ next: r => this.kycRecord.set(r.data?.[0] ?? null) });
    this.appForm.patchValue({ smeId: sme.smeId });
  }

  saveSme() {
    if (this.isReadOnly()) return;
    this.smeForm.markAllAsTouched();
    if (this.smeForm.invalid) return;
    this.savingSme.set(true);
    const v = this.smeForm.value;
    this.api.createSme({
      legalName: v.legalName!, tradeName: v.tradeName||undefined,
      businessType: v.businessType as BizType, industry: v.industry!,
      address: v.address!, gstNo: v.gstNo?.toUpperCase()||undefined
    }).subscribe({
      next: r => {
        this.savingSme.set(false);
        this.mySmes.update(l => [...l, r.data]);
        this.selectSme(r.data);
        this.showSmeForm.set(false);
        this.smeForm.reset({ businessType: 'PROPRIETORSHIP' });
        this.toast.success('SME Registered', r.data.legalName + ' has been registered.');
        this.step.set('kyc');
      },
      error: e => { this.savingSme.set(false); this.toast.error('Error', e?.error?.message ?? 'Failed'); }
    });
  }

  addPromoter() {
    if (this.isReadOnly() || !this.selectedSme()) return;
    this.promoterForm.markAllAsTouched();
    if (this.promoterForm.invalid) return;
    this.savingPromoter.set(true);
    const v = this.promoterForm.value;
    this.api.addPromoter(this.selectedSme()!.smeId, {
      promoterName: v.promoterName!, mobile: v.mobile!, ownershipPct: v.ownershipPct!, monthlyIncome: v.monthlyIncome!
    }).subscribe({
      next: r => {
        this.savingPromoter.set(false);
        this.promoters.update(l => [...l, r.data]);
        this.promoterForm.reset();
        this.showPromoterForm.set(false);
        this.toast.success('Promoter Added', v.promoterName + ' added successfully.');
      },
      error: e => { this.savingPromoter.set(false); this.toast.error('Error', e?.error?.message ?? 'Failed'); }
    });
  }

  // ── KYC: auto-initialize from application ───────────────────────

  /** Called when user reaches the KYC step with an active application. */
  initializeKyc(applicationId: number) {
    if (this.kycRecord()?.loanApplicationId === applicationId) return;
    this.initializingKyc.set(true);
    this.api.initializeKyc(applicationId).subscribe({
      next: r => {
        this.kycRecord.set(r.data);
        this.initializingKyc.set(false);
        this.toast.success('KYC Ready', 'KYC record initialized with ' +
          (r.data.promoters?.length ?? 0) + ' promoter(s).');
      },
      error: e => {
        this.initializingKyc.set(false);
        this.toast.error('KYC Error', e?.error?.message ?? 'Could not initialize KYC.');
      }
    });
  }

  loadKycForApplication(applicationId: number) {
    this.api.getKycByApplication(applicationId).subscribe({
      next: r => this.kycRecord.set(r.data),
      error: () => this.kycRecord.set(null)
    });
  }

  // ── KYC (legacy) ──────────────────────────────────────────────────────────
  submitKyc() {
    if (this.isReadOnly()) return;
    const apps = this.activeApps();
    const targetApp = apps.find(a => ['DRAFT','KYC_PENDING','READY_TO_SUBMIT'].includes(a.status))
                    ?? apps[apps.length - 1];
    if (!targetApp) {
      this.toast.error('No Application', 'Create an application first before initializing KYC.');
      return;
    }
    this.creatingKyc.set(true);
    this.api.initializeKyc(targetApp.applicationId).subscribe({
      next: (r: any) => {
        this.creatingKyc.set(false);
        this.kycRecord.set(r.data);
        this.toast.success('KYC Initialized', 'KYC #' + r.data.kycId + ' created for App #' + targetApp.applicationId);
        this.step.set('application');
      },
      error: (e: any) => { this.creatingKyc.set(false); this.toast.error('KYC Error', e?.error?.message ?? 'Failed'); }
    });
  }

  // ── Application ───────────────────────────────────────────────────
  createApp() {
    if (this.isReadOnly()) return;
    this.appForm.markAllAsTouched();
    if (this.appForm.invalid) return;
    this.creatingApp.set(true);
    const v = this.appForm.value;
    this.api.createApp({
      smeId: v.smeId!, productId: Number(v.productId!),
      requestedAmount: v.requestedAmount!, tenorMonths: v.tenorMonths!,
      purposeNote: v.purposeNote||undefined
    }).subscribe({
      next: r => {
        this.creatingApp.set(false);
        this.myApps.update(l => [...l, r.data]);
        this.showAppForm.set(false);
        this.appForm.patchValue({ productId: null, requestedAmount: null, tenorMonths: null, purposeNote: '' });
        this.toast.success('Application Created', 'App #' + r.data.applicationId + ' created.');
        this.step.set('documents');
        this.openDocs(r.data);
      },
      error: e => { this.creatingApp.set(false); this.toast.error('Error', e?.error?.message ?? 'Failed'); }
    });
  }

  submitApp(app: AppResponse) {
    if (this.isReadOnly()) return;
    this.submittingApp.set(app.applicationId);
    this.api.submitApp(app.applicationId).subscribe({
      next: r => {
        this.submittingApp.set(null);
        this.myApps.update(l => l.map(a => a.applicationId === app.applicationId ? r.data : a));
        this.toast.success('Submitted!', 'Application #' + app.applicationId + ' sent to underwriting.');
      },
      error: e => { this.submittingApp.set(null); this.toast.error('Error', e?.error?.message ?? 'Failed'); }
    });
  }

  // ── Documents ─────────────────────────────────────────────────────
  openDocs(app: AppResponse) {
    this.selectedAppForDocs.set(app);
    this.loadingDocs.set(true);
    this.step.set('documents');
    this.api.listDocs(app.applicationId).subscribe({
      next: r => { this.docs.set(r.data ?? []); this.loadingDocs.set(false); },
      error: () => this.loadingDocs.set(false)
    });
  }

  onFileSelected(e: Event) {
    this.fileError = '';
    const f = (e.target as HTMLInputElement).files?.[0];
    if (!f) return;
    if (f.type !== 'application/pdf' && !f.type.startsWith('image/')) {
      this.fileError = 'Only PDF and image files accepted'; this.selectedFile = null; return;
    }
    if (f.size > 20 * 1024 * 1024) {
      this.fileError = 'Max 20MB'; this.selectedFile = null; return;
    }
    this.selectedFile = f;
  }

  uploadDoc() {
    if (!this.selectedAppForDocs()) return;
    if (this.docUploadMode === 'uri') {
      if (!this.docUriInput.trim()) { this.toast.warning('Enter a URL'); return; }
      this.uploadingDoc.set(true);
      this.api.addDoc(this.selectedAppForDocs()!.applicationId, { docType: this.selectedDocType, fileUri: this.docUriInput }).subscribe({
        next: r => {
          this.uploadingDoc.set(false);
          this.docs.update(l => [...l, r.data]);
          this.docUriInput = '';
          this.toast.success('Document Linked', 'URL document linked successfully');
        },
        error: e => { this.uploadingDoc.set(false); this.toast.error('Error', e?.error?.message); }
      });
      return;
    }
    if (!this.selectedFile) return;
    this.uploadingDoc.set(true);
    this.api.uploadDoc(this.selectedAppForDocs()!.applicationId, this.selectedFile, this.selectedDocType).subscribe({
      next: r => {
        this.uploadingDoc.set(false);
        this.docs.update(d => [...d, r.data]);
        this.selectedFile = null;
        this.fileError = '';
        (document.getElementById('docFile') as HTMLInputElement).value = '';
        this.toast.success('Uploaded', r.data.fileName + ' uploaded.');
      },
      error: e => { this.uploadingDoc.set(false); this.toast.error('Upload Failed', e?.error?.message ?? 'Error'); }
    });
  }

  downloadDoc(doc: DocResponse) {
    if (!doc.downloadUrl) return;
    this.api.downloadDoc(doc.applicationId, doc.documentId).subscribe({
      next: blob => {
        const url = URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url; a.download = doc.fileName || 'document'; a.click();
        URL.revokeObjectURL(url);
      },
      error: () => this.toast.error('Download Failed', 'Could not download file')
    });
  }

  // ── Helpers ───────────────────────────────────────────────────────
  statusClass(s: string) { return 'badge-status bs-' + s.toLowerCase(); }
  kycVerified()  { return this.kycRecords().some(k => k.verificationStatus === 'VERIFIED'); }
  kycPending()   { return this.kycRecords().some(k => k.verificationStatus === 'PENDING'); }
  canSubmit(a: AppResponse) { return ['DRAFT','READY_TO_SUBMIT','KYC_PENDING'].includes(a.status) && !this.isReadOnly(); }
  fmtStatus(s: string) { return s.replace(/_/g,' '); }
  readonly financialDocMeta: { type: string; label: string; hint: string }[] = [
    { type: 'BANK_STATEMENT', label: 'Bank Statement', hint: 'Last 6 months' },
    { type: 'ITR',            label: 'ITR',            hint: 'Last 2 years' },
    { type: 'BALANCE_SHEET',  label: 'Balance Sheet',  hint: 'Audited' },
    { type: 'PROFIT_LOSS',    label: 'Profit & Loss',  hint: 'Audited P&L' },
    { type: 'GST_RETURNS',    label: 'GST Returns',    hint: 'GSTR-3B' },
    { type: 'AUDIT_REPORT',   label: 'Audit Report',   hint: 'CA certified' },
  ];

  activeApps()   { return this.myApps().filter(a => a.smeId === this.selectedSme()?.smeId); }

  /** True when at least one application has been disbursed */
  hasDisbursedApp() { return this.activeApps().some(a => a.status === 'DISBURSED'); }

  /** The first disbursed application for auto-detect (safe, no arrow fn in template) */
  disbursedApp() { return this.activeApps().find(a => a.status === 'DISBURSED') ?? null; }

  isStepDone(key: string): boolean {
    if (key === 'sme')         return this.mySmes().length > 0;
    if (key === 'kyc')         return this.kycVerified();
    if (key === 'application') return this.activeApps().length > 0;
    if (key === 'documents')   return false;
    return false;
  }

  isPipelineDone(currentStatus: string, checkStatus: string): boolean {
    const order = ['DRAFT','KYC_PENDING','READY_TO_SUBMIT','SUBMITTED','ROUTED_TO_UW','UW_APPROVED','OFFERED','OFFER_ACCEPTED','DISBURSED'];
    return order.indexOf(currentStatus) > order.indexOf(checkStatus);
  }

  // ── Template helpers (avoid arrow functions in event bindings) ───
  appsDraft()    { return this.activeApps().filter(a => a.status === 'DRAFT').length; }
  appsInReview() { return this.activeApps().filter(a => ['SUBMITTED','ROUTED_TO_UW','KYC_PENDING'].includes(a.status)).length; }
  appsApproved() { return this.activeApps().filter(a => a.status === 'UW_APPROVED').length; }
  toggleSmeForm()      { this.showSmeForm.update(v => !v); }
  togglePromoterForm() { this.showPromoterForm.update(v => !v); }
  toggleAppForm()      { this.showAppForm.update(v => !v); }

  // ── Document checklist helpers ──────────────────────────────
  hasDoc(type: string): boolean {
    return this.docs().some(d => d.docType === type);
  }
  kycDocsUploaded(): number {
    return this.kycDocTypes.filter(d => this.hasDoc(d.type)).length;
  }
  finDocsUploaded(): number {
    return this.finDocTypes.filter(d => this.hasDoc(d.type)).length;
  }

  // ── Document Preview ─────────────────────────────────────────
  previewDoc = signal<DocResponse | null>(null);
  openPreview(doc: DocResponse) { this.previewDoc.set(doc); }
  closePreview() { this.previewDoc.set(null); }

  // ── Offer Acceptance ─────────────────────────────────────────────
  offeredApps     = signal<AppResponse[]>([]);
  activeOffer     = signal<OfferResponse | null>(null);
  showAgreement   = signal(false);
  signatureText   = '';
  termsChecked    = false;
  acceptingOffer  = signal(false);
  rejectingOffer  = signal(false);

  /** Single call — builds offersByAppId cache used by openAgreement */
  offersByAppId = new Map<number, OfferResponse>();

  loadOfferedApps() {
    this.api.listOffers().subscribe({
      next: r => {
        this.offersByAppId = new Map((r.data ?? []).map(o => [o.applicationId, o]));
        this.offeredApps.set(this.myApps().filter(a => a.status === 'OFFERED'));
        if (this.offeredApps().length > 0) {
          const first = this.offeredApps()[0];
          const cached = this.offersByAppId.get(first.applicationId);
          if (cached) this.activeOffer.set(cached);
        }
      }
    });
  }

  openAgreement(app: AppResponse) {
    // Use cached map from loadOfferedApps — avoids extra network call
    const cached = this.offersByAppId.get(app.applicationId);
    if (cached) {
      this.activeOffer.set(cached);
      this.showAgreement.set(true);
      this.signatureText = '';
      this.termsChecked = false;
      return;
    }
    // Fallback: fetch fresh if cache miss
    this.api.listOffers().subscribe({
      next: r => {
        const offer = (r.data ?? []).find(o => o.applicationId === app.applicationId);
        if (offer) {
          this.offersByAppId.set(app.applicationId, offer);
          this.activeOffer.set(offer);
          this.showAgreement.set(true);
          this.signatureText = '';
          this.termsChecked = false;
        }
      }
    });
  }

  closeAgreement() {
    this.showAgreement.set(false);
    this.activeOffer.set(null);
    this.signatureText = '';
    this.termsChecked = false;
  }

  acceptOffer() {
    const offer = this.activeOffer();
    if (!offer || !this.signatureText.trim() || !this.termsChecked) return;
    this.acceptingOffer.set(true);
    this.api.acceptOffer(offer.offerId).subscribe({
      next: () => {
        this.acceptingOffer.set(false);
        this.myApps.update(l => l.map(a =>
          a.applicationId === offer.applicationId ? { ...a, status: 'OFFER_ACCEPTED' as any } : a
        ));
        this.closeAgreement();
        this.toast.success('Offer Accepted 🎉', 'Your loan agreement has been signed and submitted.');
      },
      error: e => { this.acceptingOffer.set(false); this.toast.error('Error', e?.error?.message); }
    });
  }

  rejectOffer() {
    const offer = this.activeOffer();
    if (!offer) return;
    this.rejectingOffer.set(true);
    this.api.rejectOffer(offer.offerId).subscribe({
      next: () => {
        this.rejectingOffer.set(false);
        this.myApps.update(l => l.map(a =>
          a.applicationId === offer.applicationId ? { ...a, status: 'OFFER_REJECTED' as any } : a
        ));
        this.closeAgreement();
        this.toast.warning('Offer Declined', 'The offer has been declined.');
      },
      error: e => { this.rejectingOffer.set(false); this.toast.error('Error', e?.error?.message); }
    });
  }

  canAcceptOffer(): boolean {
    return this.signatureText.trim().length >= 3 && this.termsChecked;
  }

  genEmiSchedule(offer: OfferResponse, app: AppResponse): {no:number,due:number,total:number}[] {
    const rows = [];
    const P = offer.sanctionedAmount;
    const n = app.tenorMonths;
    const r = offer.interestRate / 12 / 100;
    for (let i = 1; i <= Math.min(n, 12); i++) {
      rows.push({ no: i, due: Math.round(P / n), total: Math.round(offer.emiAmount) });
    }
    return rows;
  }

  // ── My Loan Details (Applicant view of their loan schedule) ───────
  myLoanAccId    = 0;
  myLoanAcc      = signal<LoanAccountResponse | null>(null);
  mySchedule     = signal<ScheduleResponse[]>([]);
  loanLoading    = signal(false);
  loanSearched   = signal(false);
  loanNotFound   = signal(false);

  /** Auto-discover loanAccountId from applicationId, then load schedule */
  autoLoadScheduleForApp(applicationId: number) {
    if (!applicationId) return;
    this.loanLoading.set(true);
    this.loanSearched.set(true);
    this.loanNotFound.set(false);
    this.mySchedule.set([]);
    this.myLoanAcc.set(null);

    this.api.getLoanAccountByApplication(applicationId).subscribe({
      next: r => {
        const loanId = r.data?.loanAccountId;
        if (!loanId) { this.loanLoading.set(false); this.loanNotFound.set(true); return; }
        this.myLoanAccId = loanId;
        this.myLoanAcc.set(r.data);
        this.loadMySchedule();
      },
      error: () => {
        this.loanLoading.set(false);
        this.loanNotFound.set(true);
      }
    });
  }

  loadMySchedule() {
    const id = Number(this.myLoanAccId);
    if (!id || id < 1) { return; }
    this.loanLoading.set(true);
    this.loanSearched.set(true);
    this.loanNotFound.set(false);
    this.mySchedule.set([]);
    this.myLoanAcc.set(null);
    this.api.getLoanAccount(id).subscribe({
      next: r => { this.myLoanAcc.set(r.data); },
      error: () => {}
    });
    this.api.getSchedule(id).subscribe({
      next: r => {
        const data = r.data ?? [];
        this.mySchedule.set(data);
        this.loanLoading.set(false);
        this.loanNotFound.set(data.length === 0);
        if (data.length > 0) this.toast.success('Schedule loaded', data.length + ' installments found');
      },
      error: e => {
        this.loanLoading.set(false);
        this.loanNotFound.set(true);
        this.toast.error('Not found', e?.error?.message ?? 'Loan account not found');
      }
    });
  }
  pgMySchedule = paginate(() => this.mySchedule());
  pgMySmes = paginate(() => this.mySmes());
  appSearch = '';
  filteredApps = computed(() => {
    const q = this.appSearch.trim().toLowerCase();
    return q ? this.myApps().filter(a => String(a.applicationId) === q.replace('#','') || a.status.toLowerCase().includes(q)) : this.myApps();
  });
  pgActiveApps = paginate(() => this.filteredApps());
  // pgProducts: products used in select dropdown — no pagination needed
  pgOfferedApps = paginate(() => this.offeredApps());
  pgDocs = paginate(() => this.docs());


  loanTotalDue  = computed(() => this.mySchedule().reduce((s, r) => s + r.totalDue, 0));
  loanTotalPaid = computed(() => this.mySchedule().reduce((s, r) => s + r.amountPaid, 0));
  loanTotalBal  = computed(() => this.mySchedule().reduce((s, r) => s + r.balanceDue, 0));
  loanPaidPct   = computed(() => { const t = this.loanTotalDue(); return t > 0 ? Math.round(this.loanTotalPaid() / t * 100) : 0; });
  loanPaidColor = computed(() => { const p = this.loanPaidPct(); return p >= 80 ? "#10b981" : p >= 40 ? "#f59e0b" : "#ef4444"; });

  onSlotFileSelected(event: Event, docType: string) {
    const input = event.target as HTMLInputElement;
    const file  = input.files?.[0];
    const app   = this.selectedAppForDocs();
    if (!file || !app) return;
    this.api.uploadDoc(app.applicationId, file, docType as any).subscribe({
      next: r => {
        const doc = r.data;
        this.docs.update(l => [...l.filter(d => d.docType !== doc.docType), doc]);
        this.toast.success((doc as any).replaced ? 'Document Replaced' : 'Document Uploaded',
          `${docType} ${(doc as any).replaced ? 'replaced' : 'uploaded'} successfully.`);
        input.value = '';
      },
      error: e => this.toast.error('Upload Failed', e?.error?.message ?? 'Error')
    });
  }

  editSme() {
    const sme = this.selectedSme();
    if (!sme) return;
    // Populate the SME form with existing values for edit
    this.smeForm.patchValue({
      legalName:      sme.legalName,
      tradeName:      sme.tradeName ?? '',
      registrationNo: sme.registrationNo ?? '',
      businessType:   sme.businessType,
      industry:       sme.industry,
      address:        sme.address,
      gstNo:          sme.gstNo ?? '',
    });
    this.showSmeForm.set(true);
  }
}
