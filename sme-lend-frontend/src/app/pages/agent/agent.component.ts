import { Component, OnInit, signal, inject, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, FormsModule, Validators } from '@angular/forms';
import { paginate } from '../../core/services/pagination.service';
import { PaginatorComponent } from '../../shared/paginator/paginator.component';
import { ApiService } from '../../core/services/api.service';
import { AuthService } from '../../core/services/auth.service';
import { ToastService } from '../../core/services/toast.service';
import { DocPreviewComponent } from '../../shared/doc-preview/doc-preview.component';
import {
  KycResponse, SmeResponse, PromoterResponse, AppResponse, DocResponse,
  LoanProductResponse, BizType, DocType, KycDocType, PromoterDocResponse
} from '../../core/models';

type AgentTab = 'onboard' | 'queue' | 'applications';

@Component({
  selector: 'app-agent',
  standalone: true,
  imports: [CommonModule, FormsModule, ReactiveFormsModule, DocPreviewComponent, PaginatorComponent],
  templateUrl: './agent.component.html'
})
export class AgentComponent implements OnInit {
  private api   = inject(ApiService);
  private fb    = inject(FormBuilder);
  readonly auth = inject(AuthService);
  private toast = inject(ToastService);

  isReadOnly = signal(!this.auth.hasRole('AGENT'));
  tab        = signal<AgentTab>('onboard');

  bizTypes: BizType[] = ['PROPRIETORSHIP','PARTNERSHIP','PVT_LTD','LLP','OTHER'];
  // Agent uploads KYC/identity documents only
  docTypes: DocType[] = ['PAN','AADHAAR','BUSINESS_REG_CERT','GST_CERTIFICATE','PROMOTER_PHOTO','SHOP_LICENSE','OTHER'];

  // ── KYC documents per promoter (promoter_id → PromoterDocResponse[]) ──
  promoterDocs = signal<Map<number, PromoterDocResponse[]>>(new Map());
  uploadingPromoterDoc = signal<string | null>(null); // "promoterId-docType" while uploading

  readonly financialDocMeta: { type: string; label: string; hint: string }[] = [
    { type: 'BANK_STATEMENT', label: 'Bank Statement', hint: 'Last 6 months' },
    { type: 'ITR',            label: 'ITR',            hint: 'Last 2 years' },
    { type: 'BALANCE_SHEET',  label: 'Balance Sheet',  hint: 'Audited' },
    { type: 'PROFIT_LOSS',    label: 'Profit & Loss',  hint: 'Audited P&L' },
    { type: 'GST_RETURNS',    label: 'GST Returns',    hint: 'GSTR-3B' },
    { type: 'AUDIT_REPORT',   label: 'Audit Report',   hint: 'CA certified' },
  ];

  readonly kycDocMeta: { type: string; label: string; hint: string }[] = [
    { type: 'PAN',               label: 'PAN Card',                   hint: 'Mandatory for tax & credit bureau checks' },
    { type: 'AADHAAR',           label: 'Aadhaar Card',               hint: 'Primary identity and address proof' },
    { type: 'BUSINESS_REG_CERT', label: 'Business Registration Cert', hint: 'Udyam / incorporation certificate' },
    { type: 'GST_CERTIFICATE',   label: 'GST Certificate',            hint: 'GST registration proof' },
    { type: 'PROMOTER_PHOTO',    label: 'Promoter Photo',             hint: 'For liveness / identity matching' },
    { type: 'SHOP_LICENSE',      label: 'Shop/Trade License',         hint: 'Shop & Establishment licence' },
  ];

  // Docs loaded for the selected KYC record in queue
  kycDocs           = signal<DocResponse[]>([]);
  loadingKycDocs    = signal(false);
  selectedKycForDocs = signal<KycResponse | null>(null);

  initializeKycForApp(applicationId: number) {
    this.api.initializeKyc(applicationId).subscribe({
      next: r => {
        this.toast.success('KYC Initialized', `KYC #${r.data.kycId} created with ${r.data.promoters?.length ?? 0} promoter(s).`);
        this.loadQueue();
      },
      error: e => this.toast.error('KYC Error', e?.error?.message ?? 'Could not initialize KYC. Ensure promoters are added first.')
    });
  }

  loadKycDocs(kyc: KycResponse) {
    if (this.selectedKycForDocs()?.kycId === kyc.kycId) {
      this.selectedKycForDocs.set(null);
      this.kycDocs.set([]);
      this.kycUploadAppId = null;
      return;
    }
    this.selectedKycForDocs.set(kyc);
    this.loadingKycDocs.set(true);
    this.kycDocs.set([]);
    this.kycUploadAppId = null;
    const KYC_TYPES = ['PAN','AADHAAR','BUSINESS_REG_CERT','GST_CERTIFICATE','PROMOTER_PHOTO','SHOP_LICENSE'];
    // Load docs from ALL apps belonging to this SME and merge — gives complete KYC picture
    this.api.listAllApps().subscribe({
      next: r => {
        const smeApps = (r.data ?? []).filter(a => a.smeId === kyc.smeId);
        if (smeApps.length === 0) { this.loadingKycDocs.set(false); return; }
        // Use the most recent app as the upload target
        const latestApp = smeApps.sort((a, b) => b.applicationId - a.applicationId)[0];
        this.kycUploadAppId = latestApp.applicationId;
        // Fetch docs from the latest app
        this.api.listDocs(latestApp.applicationId).subscribe({
          next: rd => {
            this.kycDocs.set((rd.data ?? []).filter(d => KYC_TYPES.includes(d.docType as string)));
            this.loadingKycDocs.set(false);
          },
          error: () => this.loadingKycDocs.set(false)
        });
      },
      error: () => this.loadingKycDocs.set(false)
    });
  }

  // ── Upload a KYC document directly from the queue panel ──────────
  kycUploadAppId: number | null = null;
  kycUploadType: DocType = 'PAN';
  kycUploadFile: File | null = null;
  kycUploadUri = '';
  kycUploadMode: 'file' | 'uri' = 'file';
  kycUploading = signal(false);
  KYC_TYPES = ['PAN','AADHAAR','BUSINESS_REG_CERT','GST_CERTIFICATE','PROMOTER_PHOTO','SHOP_LICENSE'] as const;

  onKycFileSelect(event: Event) {
    this.kycUploadFile = (event.target as HTMLInputElement).files?.[0] ?? null;
  }

  uploadKycDoc() {
    if (!this.kycUploadAppId || this.isReadOnly()) return;
    this.kycUploading.set(true);
    const finish = (doc: DocResponse) => {
      this.kycDocs.update(l => [...l.filter(d => d.docType !== doc.docType || d.documentId !== doc.documentId), doc]);
      this.kycUploading.set(false);
      this.kycUploadFile = null;
      this.kycUploadUri = '';
      this.toast.success('Document Uploaded', doc.fileName || doc.docType + ' uploaded.');
    };
    const err = (e: any) => { this.kycUploading.set(false); this.toast.error('Upload Failed', e?.error?.message); };
    if (this.kycUploadMode === 'file') {
      if (!this.kycUploadFile) { this.kycUploading.set(false); return; }
      this.api.uploadDoc(this.kycUploadAppId, this.kycUploadFile, this.kycUploadType).subscribe({ next: r => finish(r.data), error: err });
    } else {
      if (!this.kycUploadUri.trim()) { this.kycUploading.set(false); return; }
      this.api.addDoc(this.kycUploadAppId, { docType: this.kycUploadType, fileUri: this.kycUploadUri }).subscribe({ next: r => finish(r.data), error: err });
    }
  }

  hasKycDoc(type: string): boolean {
    return this.kycDocs().some(d => d.docType === type);
  }

  // ═══ ONBOARDING TAB ═══════════════════════════════════════════════

  // SME list & selection
  allSmes       = signal<SmeResponse[]>([]);
  loadingSmes   = signal(false);
  selectedSme   = signal<SmeResponse | null>(null);
  smeSearch     = '';
  smeSearched   = signal(false);
  filteredSmes  = signal<SmeResponse[]>([]);

  // SME creation form
  showSmeForm   = signal(false);
  savingSme     = signal(false);
  smeForm = this.fb.group({
    legalName:      ['', Validators.required],
    tradeName:      [''],
    registrationNo: [''],
    businessType:   ['PROPRIETORSHIP' as BizType, Validators.required],
    industry:       ['', Validators.required],
    address:        ['', Validators.required],
    gstNo:          ['']
  });
  get sf() { return this.smeForm.controls; }

  // Promoters
  promoters       = signal<PromoterResponse[]>([]);
  showPromoterForm = signal(false);
  savingPromoter  = signal(false);
  promoterForm = this.fb.group({
    promoterName:   ['', Validators.required],
    mobile:         ['', [Validators.required, Validators.pattern(/^[0-9]{10}$/)]],
    email:          [''],
    ownershipPct:   [null as number | null, [Validators.required, Validators.min(0.01), Validators.max(100)]],
    panNumber:      ['', Validators.pattern(/^[A-Z]{5}[0-9]{4}[A-Z]$/)],
    aadhaarNumber:  ['', Validators.pattern(/^[0-9]{12}$/)],
    din:            [''],
    dateOfBirth:    ['']
  });
  get pf() { return this.promoterForm.controls; }

  // KYC on selected SME
  kycRecords      = signal<KycResponse[]>([]);
  creatingKyc     = signal(false);
  kycPromoterIdInput  = 0;
  kycApplicantIdInput = 0;

  // Loan Application form
  products          = signal<LoanProductResponse[]>([]);
  showAppForm       = signal(false);
  creatingApp       = signal(false);
  submittingApp     = signal<number | null>(null);
  smeApps           = signal<AppResponse[]>([]);
  appForm = this.fb.group({
    productId:       [null as number | null, Validators.required],
    requestedAmount: [null as number | null, [Validators.required, Validators.min(1)]],
    tenorMonths:     [null as number | null, [Validators.required, Validators.min(1)]],
    purposeNote:     ['']
  });
  get af() { return this.appForm.controls; }

  // Document upload on selected application
  selectedAppForDocs = signal<AppResponse | null>(null);

  /** Creator Rule: true when a selected app was created by an Applicant (not agent).
   *  When true, agent view becomes read-only for document uploads and promoter adds. */
  applicantCreatedApp = computed(() => {
    const app = this.selectedAppForDocs();
    if (!app) return false;
    return !!app.createdByEmail && app.createdByEmail !== this.auth.userEmail();
  });
  appDocs            = signal<DocResponse[]>([]);
  selectedFile: File | null = null;
  selectedDocType: DocType = 'PAN';
  docUriInput = '';
  docUploadMode: 'file' | 'uri' = 'file';
  uploadingDoc = signal(false);

  // ── Pagination helpers ──────────────────────────────────────────
  smePage   = signal(1);
  appPage   = signal(1);
  queuePage = signal(1);

  readonly smePageSize   = 10;
  readonly appPageSize   = 10;
  readonly queuePageSize = 10;

  smePageCount()   { return Math.max(1, Math.ceil(this.filteredSmes().length / this.smePageSize)); }
  appPageCount()   { return Math.max(1, Math.ceil(this.filteredApps().length / this.appPageSize)); }
  queuePageCount() { return Math.max(1, Math.ceil(this.pendingKyc().length / this.queuePageSize)); }

  // smesPaged() replaced by pgSmes.paged()
  // appsPaged() replaced by pgApps.paged()
  // queuePaged() replaced by pgQueue.paged()

  goSmePage(n: number)   { this.smePage.set(Math.max(1, Math.min(n, this.smePageCount()))); }
  goAppPage(n: number)   { this.appPage.set(Math.max(1, Math.min(n, this.appPageCount()))); }
  goQueuePage(n: number) { this.queuePage.set(Math.max(1, Math.min(n, this.queuePageCount()))); }

  smeRange()   { const p = this.smePage()-1; const total=this.filteredSmes().length; return `${p*this.smePageSize+1}–${Math.min((p+1)*this.smePageSize, total)} of ${total}`; }
  appRange()   { const p = this.appPage()-1;  const total=this.filteredApps().length;  return `${p*this.appPageSize+1}–${Math.min((p+1)*this.appPageSize, total)} of ${total}`; }
  queueRange() { const p = this.queuePage()-1; const total=this.pendingKyc().length; return `${p*this.queuePageSize+1}–${Math.min((p+1)*this.queuePageSize, total)} of ${total}`; }

  // Reset page when search fires
  searchSmesP()  { this.searchSmes(); this.smePage.set(1); this.pgSmes.reset(); }
  searchAppsP()  { this.searchApps(); this.appPage.set(1); this.pgApps.reset(); }

  // ═══ KYC QUEUE TAB ════════════════════════════════════════════════
  pendingKyc    = signal<KycResponse[]>([]);
  loadingQueue  = signal(false);
  actionKyc     = signal<KycResponse | null>(null);
  actionType    = signal<'verify' | 'reject'>('verify');
  actionNotes   = '';
  acting        = signal(false);
  openDrills    = signal<Record<number, 'sme'|'promoter'|'applicant'|null>>({});
  drillSme      = signal<SmeResponse | null>(null);
  drillPromoter = signal<PromoterResponse | null>(null);
  drillLoading  = signal<number | null>(null);

  // ═══ APPLICATIONS TAB ═════════════════════════════════════════════
  allApps       = signal<AppResponse[]>([]);
  appSearch     = '';
  appSearched   = signal(false);
  filteredApps  = signal<AppResponse[]>([]);

  pgSmes  = paginate(() => this.filteredSmes());
  pgQueue = paginate(() => this.pendingKyc());
  pgApps  = paginate(() => this.filteredApps());

  onSmeSearch()  { this.pgSmes.reset(); }
  onAppSearch()  { this.pgApps.reset(); }
  loadingApps   = signal(false);
  viewedApp     = signal<AppResponse | null>(null);
  viewedDocs    = signal<DocResponse[]>([]);

  ngOnInit() {
    this.loadAllSmes();
    this.loadQueue();
    this.loadAllApps();
    this.api.listActiveProducts().subscribe({ next: r => this.products.set(r.data ?? []) });
  }

  // ── SME ─────────────────────────────────────────────────────────
  loadAllSmes() {
    this.loadingSmes.set(true);
    this.api.listSmes().subscribe({
      next: r => { this.allSmes.set(r.data ?? []); this.loadingSmes.set(false); },
      error: () => this.loadingSmes.set(false)
    });
  }

  searchSmes() {
    this.smeSearched.set(true);
    const q = this.smeSearch.trim().toLowerCase();
    if (!q) { this.filteredSmes.set(this.allSmes()); return; }
    this.filteredSmes.set(this.allSmes().filter(s =>
      s.legalName.toLowerCase().includes(q) || s.industry.toLowerCase().includes(q) ||
      String(s.smeId) === q || (s.gstNo ?? '').toLowerCase().includes(q)
    ));
  }

  selectSme(sme: SmeResponse) {
    this.selectedSme.set(sme);
    this.showSmeForm.set(false);
    this.showAppForm.set(false);
    this.selectedAppForDocs.set(null);
    this.api.listPromoters(sme.smeId).subscribe({ next: r => this.promoters.set(r.data ?? []) });
    this.api.listKycBySme(sme.smeId).subscribe({ next: r => this.kycRecords.set(r.data ?? []) });
    this.api.listAllApps().subscribe({ next: r => this.smeApps.set((r.data ?? []).filter(a => a.smeId === sme.smeId)) });
  }

  saveSme() {
    this.smeForm.markAllAsTouched();
    if (this.smeForm.invalid || this.isReadOnly()) return;
    this.savingSme.set(true);
    const v = this.smeForm.value;
    this.api.createSme({
      legalName: v.legalName!, tradeName: v.tradeName || undefined,
      businessType: v.businessType as BizType, industry: v.industry!,
      address: v.address!, gstNo: v.gstNo?.toUpperCase() || undefined
    }).subscribe({
      next: r => {
        this.savingSme.set(false);
        this.allSmes.update(l => [r.data, ...l]);
        this.smeForm.reset({ businessType: 'PROPRIETORSHIP' });
        this.showSmeForm.set(false);
        this.selectSme(r.data);
        this.toast.success('SME Registered', r.data.legalName + ' onboarded successfully.');
      },
      error: e => { this.savingSme.set(false); this.toast.error('Error', e?.error?.message ?? 'Failed'); }
    });
  }

  // ── PROMOTERS ───────────────────────────────────────────────────
  addPromoter() {
    this.promoterForm.markAllAsTouched();
    if (this.promoterForm.invalid || !this.selectedSme() || this.isReadOnly()) return;
    this.savingPromoter.set(true);
    const v = this.promoterForm.value;
    this.api.addPromoter(this.selectedSme()!.smeId, {
      promoterName: v.promoterName!, mobile: v.mobile!, ownershipPct: v.ownershipPct!
    }).subscribe({
      next: r => {
        this.savingPromoter.set(false);
        this.promoters.update(l => [...l, r.data]);
        this.promoterForm.reset();
        this.showPromoterForm.set(false);
        this.toast.success('Promoter Added', v.promoterName + ' added.');
      },
      error: e => { this.savingPromoter.set(false); this.toast.error('Error', e?.error?.message ?? 'Failed'); }
    });
  }

  // ── KYC ─────────────────────────────────────────────────────────
  /**
   * F1 FIX: was calling POST /kyc (non-existent endpoint).
   * Now correctly calls POST /kyc/initialize with loanApplicationId.
   * Requires a selected application — picks the latest DRAFT or KYC_PENDING app.
   */
  submitKyc() {
    if (!this.selectedSme() || this.isReadOnly()) return;
    // Find the latest application for this SME to initialize KYC against
    const apps = this.smeApps();
    const targetApp = apps.find(a =>
      ['DRAFT','KYC_PENDING','READY_TO_SUBMIT'].includes(a.status)
    ) ?? apps[apps.length - 1];
    if (!targetApp) {
      this.toast.error('No Application', 'Create an application first before initializing KYC.');
      return;
    }
    this.creatingKyc.set(true);
    this.api.initializeKyc(targetApp.applicationId).subscribe({
      next: r => {
        this.creatingKyc.set(false);
        this.kycRecords.update(l => {
          const exists = l.find(k => k.kycId === r.data.kycId);
          return exists ? l.map(k => k.kycId === r.data.kycId ? r.data : k) : [...l, r.data];
        });
        this.kycApplicantIdInput = 0;
        this.toast.success('KYC Initialized', 'KYC #' + r.data.kycId + ' created for App #' + targetApp.applicationId + '.');
      },
      error: e => { this.creatingKyc.set(false); this.toast.error('Error', e?.error?.message ?? 'KYC initialization failed'); }
    });
  }

  // ── APPLICATIONS ────────────────────────────────────────────────
  createApp() {
    this.appForm.markAllAsTouched();
    if (this.appForm.invalid || !this.selectedSme() || this.isReadOnly()) return;
    this.creatingApp.set(true);
    const v = this.appForm.value;
    this.api.createApp({
      smeId: this.selectedSme()!.smeId, productId: Number(v.productId!),
      requestedAmount: v.requestedAmount!, tenorMonths: v.tenorMonths!,
      purposeNote: v.purposeNote || undefined
    }).subscribe({
      next: r => {
        this.creatingApp.set(false);
        this.smeApps.update(l => [...l, r.data]);
        this.appForm.reset();
        this.showAppForm.set(false);
        this.toast.success('Application Created', 'App #' + r.data.applicationId + ' created.');
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
        this.smeApps.update(l => l.map(a => a.applicationId === r.data.applicationId ? r.data : a));
        this.toast.success('Application Submitted', 'App #' + r.data.applicationId + ' sent to underwriting.');
      },
      error: e => { this.submittingApp.set(null); this.toast.error('Error', e?.error?.message ?? 'Failed'); }
    });
  }

  // ── DOCUMENT UPLOAD ─────────────────────────────────────────────
  openDocs(app: AppResponse) {
    this.selectedAppForDocs.set(app);
    this.api.listDocs(app.applicationId).subscribe({ next: r => this.appDocs.set(r.data ?? []) });
  }

  onFileSelected(event: Event) {
    const input = event.target as HTMLInputElement;
    this.selectedFile = input.files?.[0] ?? null;
  }

  uploadDoc() {
    if (this.isReadOnly()) return;
    const app = this.selectedAppForDocs();
    if (!app) return;

    if (this.docUploadMode === 'file') {
      if (!this.selectedFile) { this.toast.warning('Select a file first'); return; }
      this.uploadingDoc.set(true);
      this.api.uploadDoc(app.applicationId, this.selectedFile, this.selectedDocType).subscribe({
        next: r => {
          this.uploadingDoc.set(false);
          this.appDocs.update(l => [...l, r.data]);
          this.selectedFile = null;
          this.toast.success('Document Uploaded', r.data.fileName + ' uploaded.');
        },
        error: e => { this.uploadingDoc.set(false); this.toast.error('Upload Failed', e?.error?.message); }
      });
    } else {
      if (!this.docUriInput.trim()) { this.toast.warning('Enter a URL first'); return; }
      this.uploadingDoc.set(true);
      this.api.addDoc(app.applicationId, { docType: this.selectedDocType, fileUri: this.docUriInput }).subscribe({
        next: r => {
          this.uploadingDoc.set(false);
          this.appDocs.update(l => [...l, r.data]);
          this.docUriInput = '';
          this.toast.success('Document Linked', 'URL document added.');
        },
        error: e => { this.uploadingDoc.set(false); this.toast.error('Failed', e?.error?.message); }
      });
    }
  }

  downloadDoc(doc: DocResponse) {
    if (!doc.downloadUrl) return;
    this.api.downloadDoc(doc.applicationId, doc.documentId).subscribe({
      next: blob => {
        const url = URL.createObjectURL(blob);
        const a = document.createElement('a'); a.href = url; a.download = doc.fileName || 'doc'; a.click();
        URL.revokeObjectURL(url);
      },
      error: () => this.toast.error('Download Failed', 'Could not fetch file')
    });
  }

  // ── KYC QUEUE ───────────────────────────────────────────────────
  loadQueue() {
    this.loadingQueue.set(true);
    this.api.listPendingKyc().subscribe({
      next: r => { this.pendingKyc.set(r.data ?? []); this.loadingQueue.set(false); },
      error: () => this.loadingQueue.set(false)
    });
  }

  openDrill(kyc: KycResponse, type: 'sme'|'promoter'|'applicant') {
    const cur = this.openDrills()[kyc.kycId];
    if (cur === type) { this.openDrills.update(d => ({ ...d, [kyc.kycId]: null })); return; }
    this.openDrills.update(d => ({ ...d, [kyc.kycId]: type }));
    this.drillLoading.set(kyc.kycId);
    if (type === 'sme' && kyc.smeId) {
      this.api.getSme(kyc.smeId).subscribe({
        next: r => { this.drillSme.set(r.data); this.drillLoading.set(null); },
        error: () => this.drillLoading.set(null)
      });
    } else if (type === 'promoter' && kyc.promoters?.length) {
      this.api.getPromoter(kyc.promoters?.[0]?.promoterId ?? 0).subscribe({
        next: r => { this.drillPromoter.set(r.data); this.drillLoading.set(null); },
        error: () => this.drillLoading.set(null)
      });
    } else { this.drillLoading.set(null); }
  }

  isDrillOpen(kycId: number, type: string) { return this.openDrills()[kycId] === type; }
  isDrillLoading(kycId: number) { return this.drillLoading() === kycId; }

  openAction(k: KycResponse, t: 'verify'|'reject') { this.actionKyc.set(k); this.actionType.set(t); this.actionNotes = ''; }

  doAction() {
    const k = this.actionKyc(); if (!k) return;
    this.acting.set(true);
    const req = { notes: this.actionNotes || undefined };
    const obs = this.actionType() === 'verify' ? this.api.verifyKyc(k.kycId, req) : this.api.rejectKyc(k.kycId, req);
    obs.subscribe({
      next: () => {
        this.acting.set(false);
        this.pendingKyc.update(l => l.filter(x => x.kycId !== k.kycId));
        this.actionKyc.set(null);
        this.openDrills.update(d => ({ ...d, [k.kycId]: null }));
        this.toast.success(
          this.actionType() === 'verify' ? 'KYC Verified ✓' : 'KYC Rejected',
          'KYC #' + k.kycId + ' has been ' + this.actionType() + 'd.'
        );
      },
      error: e => { this.acting.set(false); this.toast.error('Error', e?.error?.message ?? 'Failed'); }
    });
  }

  // ── ALL APPLICATIONS TAB ────────────────────────────────────────
  loadAllApps() {
    this.loadingApps.set(true);
    this.api.listAllApps().subscribe({
      next: r => { this.allApps.set(r.data ?? []); this.loadingApps.set(false); },
      error: () => this.loadingApps.set(false)
    });
  }

  searchApps() {
    this.appSearched.set(true);
    const q = this.appSearch.trim().toLowerCase();
    if (!q) { this.filteredApps.set(this.allApps()); return; }
    this.filteredApps.set(this.allApps().filter(a =>
      String(a.applicationId) === q || (a.smeLegalName ?? '').toLowerCase().includes(q) ||
      a.status.toLowerCase().includes(q)
    ));
  }

  viewAppDetail(app: AppResponse) {
    this.viewedApp.set(app);
    this.api.listDocs(app.applicationId).subscribe({ next: r => this.viewedDocs.set(r.data ?? []) });
  }

  // ── Helpers ─────────────────────────────────────────────────────
  statusClass(s: string)  { return 'badge-status bs-' + s.toLowerCase(); }
  fmtStatus(s: string)    { return s.replace(/_/g,' '); }
  canSubmit(a: AppResponse) { return ['DRAFT','READY_TO_SUBMIT','KYC_PENDING'].includes(a.status) && !this.isReadOnly(); }
  smesActive()      { return this.allSmes().filter(s => s.status === 'ACTIVE').length; }
  appsKycPending()  { return this.allApps().filter(a => a.status === 'KYC_PENDING').length; }
  appsInReview()    { return this.allApps().filter(a => ['SUBMITTED','ROUTED_TO_UW'].includes(a.status)).length; }
  appsApproved()    { return this.allApps().filter(a => a.status === 'UW_APPROVED').length; }
  pendingCount()    { return this.pendingKyc().length; }
  kycWithPromoter() { return this.pendingKyc().filter(k => (k.promoters?.length ?? 0) > 0).length; }
  kycWithApplicant(){ return this.pendingKyc().filter(k => !!k.applicantId).length; }
  toggleSmeForm()      { this.showSmeForm.update(v => !v); }

  // ── Document Preview ─────────────────────────────────────────
  previewDoc = signal<DocResponse | null>(null);
  openPreview(doc: DocResponse) { this.previewDoc.set(doc); }
  closePreview() { this.previewDoc.set(null); }
  togglePromoterForm() { this.showPromoterForm.update(v => !v); }
  toggleAppForm()      { this.showAppForm.update(v => !v); }

  /** Creator Rule: show in-browser toast notifying operator that applicant
   *  needs to upload documents. No email/SMS — browser notification only. */
  notifyApplicant() {
    this.toast.info(
      'Applicant Notified',
      'The applicant has been reminded to upload the required documents via their dashboard.'
    );
  }

  /** Upload or replace a KYC document for a specific promoter */
  uploadPromoterKycDoc(promoterId: number, docType: string, file: File) {
    const key = `${promoterId}-${docType}`;
    this.uploadingPromoterDoc.set(key);
    this.api.uploadPromoterDoc(promoterId, file, docType).subscribe({
      next: r => {
        this.uploadingPromoterDoc.set(null);
        const doc = r.data;
        const current = this.promoterDocs().get(promoterId) ?? [];
        const updated = [...current.filter(d => d.docType !== doc.docType), doc];
        const map = new Map(this.promoterDocs());
        map.set(promoterId, updated);
        this.promoterDocs.set(map);
        this.toast.success(
          doc.replaced ? 'Document Replaced' : 'Document Uploaded',
          `${docType} ${doc.replaced ? 'replaced' : 'uploaded'} for promoter.`
        );
      },
      error: e => {
        this.uploadingPromoterDoc.set(null);
        this.toast.error('Upload Failed', e?.error?.message ?? 'Could not upload document');
      }
    });
  }

  /** Load KYC docs for all promoters of the selected application's SME */
  loadPromoterDocs(smeId: number) {
    this.api.listPromoters(smeId).subscribe({
      next: r => {
        const promoters = r.data ?? [];
        const map = new Map(this.promoterDocs());
        for (const p of promoters) {
          this.api.listPromoterDocs(p.promoterId).subscribe({
            next: pd => { map.set(p.promoterId, pd.data ?? []); this.promoterDocs.set(new Map(map)); }
          });
        }
      }
    });
  }

  promoterDocUploading(promoterId: number, docType: string): boolean {
    return this.uploadingPromoterDoc() === `${promoterId}-${docType}`;
  }

  getPromoterDoc(promoterId: number, docType: string) {
    return (this.promoterDocs().get(promoterId) ?? []).find(d => d.docType === docType) ?? null;
  }

  onSlotFileSelected(event: Event, docType: string) {
    const input = event.target as HTMLInputElement;
    const file  = input.files?.[0];
    const app   = this.selectedAppForDocs();
    if (!file || !app) return;
    this.uploadingDoc.set(true);
    this.api.uploadDoc(app.applicationId, file, docType as any).subscribe({
      next: r => {
        this.uploadingDoc.set(false);
        const doc = r.data;
        this.appDocs.update(l => [...l.filter(d => d.docType !== doc.docType), doc]);
        this.toast.success((doc as any).replaced ? 'Document Replaced' : 'Document Uploaded',
          `${docType} ${(doc as any).replaced ? 'replaced' : 'uploaded'} successfully.`);
        input.value = '';
      },
      error: e => { this.uploadingDoc.set(false); this.toast.error('Upload Failed', e?.error?.message ?? 'Error'); }
    });
  }

  getAppDocByType(docType: string) {
    return this.appDocs().find(d => d.docType === docType) ?? null;
  }
}
