import { Component, OnInit, signal, inject, computed } from '@angular/core';
import { paginate } from '../../core/services/pagination.service';
import { PaginatorComponent } from '../../shared/paginator/paginator.component';
import { CommonModule } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators, FormsModule } from '@angular/forms';
import { HttpClient } from '@angular/common/http';
import { ApiService } from '../../core/services/api.service';
import {
  AppResponse, UwDecision, SmeResponse, PromoterResponse, KycResponse, DocResponse,
  ScorecardResponse, DecisionResponse, EligibilityCheckResult
} from '../../core/models';
import { environment } from '../../../environments/environment';
import { DocPreviewComponent } from '../../shared/doc-preview/doc-preview.component';

@Component({
  selector: 'app-underwriting',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, FormsModule, DocPreviewComponent, PaginatorComponent],
  templateUrl: './underwriting.component.html'
})
export class UnderwritingComponent implements OnInit {
  private api  = inject(ApiService);
  private http = inject(HttpClient);
  private fb   = inject(FormBuilder);
  private B    = environment.apiUrl;

  queue    = signal<AppResponse[]>([]);
  loading  = signal(true);
  selected = signal<AppResponse|null>(null);

  // Cross-reference data loaded when an app is selected
  smeDetail    = signal<SmeResponse|null>(null);
  promoters    = signal<PromoterResponse[]>([]);
  kycRecords   = signal<KycResponse[]>([]);
  documents    = signal<DocResponse[]>([]);
  loadingRef   = signal(false);
  activeTab    = signal<'details'|'sme'|'kyc'|'promoters'|'docs'|'scoring'>('details');

  scorecard   = signal<ScorecardResponse | null>(null);
  decision    = signal<DecisionResponse | null>(null);
  eligibility = signal<EligibilityCheckResult | null>(null);
  loadingScore = signal(false);
  deciding  = signal(false);
  alertMsg  = signal('');
  alertType = signal('success');

  decisions = [
    { value: 'APPROVE', label: 'Approve',      color: 'btn-success' },
    { value: 'REJECT',  label: 'Reject',       color: 'btn-danger'  },
    { value: 'RETURN',  label: 'Return to App', color: 'btn-warning' }
  ];

  form = this.fb.group({
    decision:    ['APPROVE', Validators.required],
    summaryNote: ['']
  });
  get f() { return this.form.controls; }

  // ── Queue search + paginator ────────────────────────────────────────
  searchTerm = signal('');

  filteredQueue = computed(() => {
    const term = this.searchTerm().toLowerCase().trim();
    if (!term) return this.queue();
    return this.queue().filter(a =>
      String(a.applicationId).includes(term) ||
      (a.smeLegalName?.toLowerCase().includes(term) ?? false) ||
      (a.status?.toLowerCase().includes(term) ?? false)
    );
  });

  pgQueue = paginate(() => this.filteredQueue());

  ngOnInit() { this.load(); }

  load() {
    this.loading.set(true);
    this.api.getUwQueue().subscribe({
      next: r => { this.queue.set(r.data ?? []); this.loading.set(false); },
      error: () => this.loading.set(false)
    });
  }

  select(a: AppResponse) {
    this.selected.set(a);
    this.form.reset({ decision: 'APPROVE' });
    this.activeTab.set('details');
    this.loadCrossReference(a.applicationId);
  }

  loadCrossReference(appId: number) {
    this.loadingRef.set(true);
    this.loadScoring(appId);
    this.smeDetail.set(null);
    this.promoters.set([]);
    this.kycRecords.set([]);
    this.documents.set([]);

    // SME
    this.http.get<any>(`${this.B}/uw/applications/${appId}/sme`).subscribe({
      next: r => this.smeDetail.set(r.data),
      error: () => {}
    });
    // KYC
    this.http.get<any>(`${this.B}/uw/applications/${appId}/kyc`).subscribe({
      next: r => this.kycRecords.set(r.data ?? []),
      error: () => {}
    });
    // Promoters
    this.http.get<any>(`${this.B}/uw/applications/${appId}/promoters`).subscribe({
      next: r => this.promoters.set(r.data ?? []),
      error: () => {}
    });
    // Documents
    this.http.get<any>(`${this.B}/uw/applications/${appId}/documents`).subscribe({
      next: r => { this.documents.set(r.data ?? []); this.loadingRef.set(false); },
      error: () => this.loadingRef.set(false)
    });
  }

  loadScoring(appId: number) {
    this.loadingScore.set(true);
    this.scorecard.set(null); this.decision.set(null); this.eligibility.set(null);
    this.api.getScorecard(appId).subscribe({ next: r => this.scorecard.set(r.data), error: () => {} });
    this.api.getDecision(appId).subscribe({ next: r => this.decision.set(r.data), error: () => {} });
    this.api.checkEligibility(appId).subscribe({
      next: r => { this.eligibility.set(r.data); this.loadingScore.set(false); },
      error: () => this.loadingScore.set(false)
    });
  }

  runScoring(appId: number) {
    this.loadingScore.set(true);
    this.api.runScoring(appId).subscribe({
      next: r => { this.decision.set(r.data); this.loadScoring(appId); },
      error: () => this.loadingScore.set(false)
    });
  }

  // ── Score-based approval gate (driven entirely by backend ScorecardResponse) ──

  /**
   * Returns false when the backend signals isApproveDisabled (POOR band).
   * Falls back to scoreBand check so the gate works even on cached scorecards
   * that pre-date the isApproveDisabled field.
   */
  get canApprove(): boolean {
    const sc = this.scorecard();
    if (!sc) return true; // no scorecard yet — backend will enforce on submit
    if (sc.isApproveDisabled !== undefined) return !sc.isApproveDisabled;
    return sc.scoreBand !== 'POOR';
  }

  get scoreBlockReason(): string | null {
    const sc = this.scorecard();
    if (!sc) return null;
    if (sc.scoreBand === 'POOR') {
      const threshold = sc.thresholdScore ?? 'the product threshold';
      return `Score ${sc.scoreValue} (POOR) is below the product threshold of ${threshold}. `
           + `APPROVE is disabled — only REJECT or RETURN is available for below-threshold applications.`;
    }
    return null;
  }

  // ── Colour helpers ───────────────────────────────────────────────────

  scoreBandColor(band: string): string {
    const map: Record<string, string> = {
      EXCELLENT: '#15803d', HIGH: '#0d9488', MEDIUM: '#d97706',
      FAIR: '#d97706', POOR: '#dc2626', LOW: '#dc2626'
    };
    return map[band] ?? '#6b7280';
  }

  scoreBandBg(band: string): string {
    const map: Record<string, string> = {
      EXCELLENT: '#dcfce7', HIGH: '#f0fdfa', MEDIUM: '#fffbeb',
      FAIR: '#fffbeb', POOR: '#fee2e2', LOW: '#fee2e2'
    };
    return map[band] ?? '#f9fafb';
  }

  decisionBg(): string {
    const d = this.decision();
    if (!d) return '#f9fafb';
    return this.scoreBandBg(
      d.path === 'AUTO_APPROVE' ? 'EXCELLENT' :
      d.path === 'AUTO_DECLINE' ? 'POOR' : 'FAIR'
    );
  }

  decisionColor(path: string): string {
    const map: Record<string, string> = {
      AUTO_APPROVE: '#15803d', ROUTE_TO_UW: '#0d9488', AUTO_DECLINE: '#dc2626'
    };
    return map[path] ?? '#6b7280';
  }

  decide() {
    this.form.markAllAsTouched();
    if (this.form.invalid || !this.selected()) return;
    this.deciding.set(true);
    const v = this.form.value;
    this.api.submitUwDecision(this.selected()!.applicationId, {
      decision: v.decision as UwDecision,
      summaryNote: v.summaryNote || undefined
    }).subscribe({
      next: r => {
        this.deciding.set(false);
        this.queue.update(q => q.filter(a => a.applicationId !== this.selected()!.applicationId));
        this.selected.set(null);
        const d = r.data.decision;
        this.flash(`Decision: ${d} — Status → ${r.data.newApplicationStatus}`,
          d === 'APPROVE' ? 'success' : d === 'REJECT' ? 'danger' : 'warning');
      },
      error: e => { this.deciding.set(false); this.flash(e?.error?.message ?? 'Failed', 'danger'); }
    });
  }

  downloadDoc(doc: DocResponse) {
    if (!doc.downloadUrl) return;
    this.api.downloadDoc(this.selected()!.applicationId, doc.documentId).subscribe({
      next: blob => {
        const url = URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url; a.download = doc.fileName || 'document'; a.click();
        URL.revokeObjectURL(url);
      }
    });
  }

  // ── Document Preview ─────────────────────────────────────────
  previewDoc = signal<DocResponse | null>(null);
  openPreview(doc: DocResponse) { this.previewDoc.set(doc); }
  closePreview() { this.previewDoc.set(null); }

  // ── Document split ─────────────────────────────────────────────
  docTab = signal<'financial'|'kyc'>('financial');

  readonly KYC_TYPES = ['PAN','AADHAAR','BUSINESS_REG_CERT','GST_CERTIFICATE','PROMOTER_PHOTO','SHOP_LICENSE'];
  readonly FIN_TYPES = ['BANK_STATEMENT','ITR','BALANCE_SHEET','PROFIT_LOSS','GST_RETURNS','AUDIT_REPORT'];

  readonly finDocMeta: { type: string; label: string; purpose: string }[] = [
    { type: 'BANK_STATEMENT', label: 'Bank Statements (6 mo)',  purpose: 'Cash flow analysis & EMI obligations' },
    { type: 'ITR',            label: 'ITR (Last 2 Years)',      purpose: 'Declared income & financial stability' },
    { type: 'BALANCE_SHEET',  label: 'Audited Balance Sheet',   purpose: 'Debt-to-equity ratio & net worth' },
    { type: 'PROFIT_LOSS',    label: 'P&L Statement',           purpose: 'Business profitability trend' },
    { type: 'GST_RETURNS',    label: 'GSTR-3B Returns',         purpose: 'Actual sales/turnover verification' },
    { type: 'AUDIT_REPORT',   label: 'CA Audit Report',         purpose: 'Third-party financial validation' },
  ];

  readonly kycDocMeta: { type: string; label: string; purpose: string }[] = [
    { type: 'PAN',               label: 'PAN Card',                   purpose: 'Tax & credit bureau verification' },
    { type: 'AADHAAR',           label: 'Aadhaar Card',               purpose: 'Identity & address proof' },
    { type: 'BUSINESS_REG_CERT', label: 'Business Registration Cert', purpose: 'Legal entity verification' },
    { type: 'GST_CERTIFICATE',   label: 'GST Certificate',            purpose: 'GST registration proof' },
    { type: 'PROMOTER_PHOTO',    label: 'Promoter Photo',             purpose: 'Liveness / identity matching' },
    { type: 'SHOP_LICENSE',      label: 'Shop/Trade License',         purpose: 'Business establishment proof' },
  ];

  financialDocs() { return this.documents().filter(d => this.FIN_TYPES.includes(d.docType as string) || (!this.KYC_TYPES.includes(d.docType as string) && !this.FIN_TYPES.includes(d.docType as string))); }
  kycDocs()       { return this.documents().filter(d => this.KYC_TYPES.includes(d.docType as string)); }
  hasDoc(type: string) { return this.documents().some(d => d.docType === type); }
  finDocsCount()  { return this.documents().filter(d => this.FIN_TYPES.includes(d.docType as string)).length; }
  kycDocsCount()  { return this.documents().filter(d => this.KYC_TYPES.includes(d.docType as string)).length; }

  statusClass(s: string) { return 'badge-status bs-' + s.toLowerCase(); }
  kycBadge(s: string)    { return 'badge-status bs-' + s.toLowerCase(); }

  private flash(m: string, t: string) {
    this.alertMsg.set(m); this.alertType.set(t);
    setTimeout(() => this.alertMsg.set(''), 6000);
  }
}
