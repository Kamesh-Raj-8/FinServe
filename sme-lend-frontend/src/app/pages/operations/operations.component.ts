import { Component, OnInit, signal, inject, computed } from '@angular/core';
import { paginate } from '../../core/services/pagination.service';
import { PaginatorComponent } from '../../shared/paginator/paginator.component';
import { CommonModule, CurrencyPipe, DatePipe } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators, FormsModule } from '@angular/forms';
import { ApiService } from '../../core/services/api.service';
import { ToastService } from '../../core/services/toast.service';
import { AppResponse, DisburseMode, DisburseResponse, PendingDisbursementDto } from '../../core/models';

@Component({
  selector: 'app-operations',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, FormsModule, PaginatorComponent],
  templateUrl: './operations.component.html'
})
export class OperationsComponent implements OnInit {
  private api   = inject(ApiService);
  private fb    = inject(FormBuilder);
  private toast = inject(ToastService);

  tab = signal(0);
  /** Minimum date for offer valid-until picker — must be tomorrow */
  readonly minOfferDate = new Date(Date.now() + 86400000).toISOString().split('T')[0];


  // ── Approved apps (UW_APPROVED) ──────────────────────────────────
  approvedApps   = signal<AppResponse[]>([]);
  loadingApproved = signal(true);
  selectedApp    = signal<AppResponse | null>(null);
  creatingOffer  = signal(false);

  // ── Pending disbursements (OFFER_ACCEPTED) ─────────────────────────
  pendingDisb      = signal<PendingDisbursementDto[]>([]);
  loadingPendingDisb = signal(true);
  selectedPending  = signal<PendingDisbursementDto | null>(null);

  // ── Disbursement ───────────────────────────────────────────────────
  disbursing     = signal(false);
  disburseResult = signal<DisburseResponse | null>(null);

  modes: DisburseMode[] = ['NEFT', 'IMPS', 'UPI'];

  // ── Forms ──────────────────────────────────────────────────────────
  offerForm = this.fb.group({
    sanctionedAmount: [null as number|null, [Validators.required, Validators.min(1)]],
    interestRate:     [null as number|null, [Validators.required, Validators.min(0.01)]],
    emiAmount:        [null as number|null, [Validators.required, Validators.min(1)]],
    validUntil:       ['', Validators.required]
  });

  disburseForm = this.fb.group({
    applicationId:    [null as number|null, [Validators.required, Validators.min(1)]],
    mode:             ['NEFT', Validators.required],
    transactionRef:   [''],
    disbursementDate: [new Date().toISOString().substring(0, 10), Validators.required]
  });

  get of() { return this.offerForm.controls;   }
  get df() { return this.disburseForm.controls; }

  // ── Search ─────────────────────────────────────────────────────────
  opsSearch = '';
  filteredApps = computed(() => {
    const q = this.opsSearch.trim().toLowerCase();
    return q ? this.approvedApps().filter(a =>
      String(a.applicationId).includes(q) ||
      (a.smeLegalName || '').toLowerCase().includes(q)
    ) : this.approvedApps();
  });
  pgApps = paginate(() => this.filteredApps());
  onOpsSearch() { this.pgApps.reset(); }

  pendingSearch = '';
  filteredPending = computed(() => {
    const q = this.pendingSearch.trim().toLowerCase();
    return q ? this.pendingDisb().filter(p =>
      String(p.applicationId).includes(q) ||
      p.smeLegalName.toLowerCase().includes(q) ||
      p.applicantEmail.toLowerCase().includes(q)
    ) : this.pendingDisb();
  });
  pgPending = paginate(() => this.filteredPending());
  onPendingSearch() { this.pgPending.reset(); }

  // ── Lifecycle ──────────────────────────────────────────────────────
  ngOnInit() { this.loadAll(); }

  loadAll() {
    this.loadApproved();
    this.loadPendingDisbursements();
  }

  loadApproved() {
    this.loadingApproved.set(true);
    this.api.listApprovedApps().subscribe({
      next: r => { this.approvedApps.set(r.data ?? []); this.loadingApproved.set(false); },
      error: () => this.loadingApproved.set(false)
    });
  }

  loadPendingDisbursements() {
    this.loadingPendingDisb.set(true);
    this.api.listPendingDisbursements().subscribe({
      next: r => { this.pendingDisb.set(r.data ?? []); this.loadingPendingDisb.set(false); },
      error: () => this.loadingPendingDisb.set(false)
    });
  }

  // ── Offer ──────────────────────────────────────────────────────────
  createOffer() {
    this.offerForm.markAllAsTouched();
    if (this.offerForm.invalid || !this.selectedApp()) return;
    this.creatingOffer.set(true);
    const v = this.offerForm.value;
    this.api.createOffer(this.selectedApp()!.applicationId, {
      sanctionedAmount: v.sanctionedAmount!,
      interestRate:     v.interestRate!,
      emiAmount:        v.emiAmount!,
      validUntil:       v.validUntil!
    }).subscribe({
      next: r => {
        this.creatingOffer.set(false);
        this.selectedApp.set(null);
        this.offerForm.reset();
        this.toast.success('Offer created', `Offer #${r.data.offerId} for ₹${r.data.sanctionedAmount?.toLocaleString()}`);
        this.loadAll(); // refresh both lists
      },
      error: e => { this.creatingOffer.set(false); this.toast.error('Failed', e?.error?.message ?? 'Could not create offer'); }
    });
  }

  // ── Disbursement ───────────────────────────────────────────────────

  /** Pre-fill the disburse form when a pending item is clicked */
  selectForDisbursal(p: PendingDisbursementDto) {
    this.selectedPending.set(p);
    this.disburseForm.patchValue({
      applicationId: p.applicationId,
      mode: 'NEFT',
      transactionRef: '',
      disbursementDate: new Date().toISOString().substring(0, 10)
    });
    this.disburseResult.set(null);
    this.tab.set(2);  // switch to Disburse tab
  }

  disburse() {
    this.disburseForm.markAllAsTouched();
    if (this.disburseForm.invalid) return;
    this.disbursing.set(true);
    this.disburseResult.set(null);
    const v = this.disburseForm.value;
    this.api.disburse(v.applicationId!, {
      mode:             v.mode as DisburseMode,
      transactionRef:   v.transactionRef || undefined,
      disbursementDate: v.disbursementDate!
    }).subscribe({
      next: r => {
        this.disbursing.set(false);
        this.disburseResult.set(r.data);
        this.selectedPending.set(null);
        this.toast.success('Disbursed!', `Loan Account #${r.data.loanAccount.loanAccountId} created`);
        this.loadPendingDisbursements(); // remove from pending list
      },
      error: e => { this.disbursing.set(false); this.toast.error('Disbursement failed', e?.error?.message ?? 'Unknown error'); }
    });
  }

  formatCurrency(n?: number): string {
    if (n == null) return '—';
    return '₹' + n.toLocaleString('en-IN');
  }

  statusClass(s: string) { return 'badge-status bs-' + s.toLowerCase(); }
}
