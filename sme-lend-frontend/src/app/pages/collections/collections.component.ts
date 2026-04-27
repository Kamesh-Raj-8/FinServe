import { Component, OnInit, signal, inject, computed } from '@angular/core';
import { paginate } from '../../core/services/pagination.service';
import { PaginatorComponent } from '../../shared/paginator/paginator.component';
import { CommonModule } from '@angular/common';
import { FormsModule, FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ApiService } from '../../core/services/api.service';
import { AuthService } from '../../core/services/auth.service';
import { ToastService } from '../../core/services/toast.service';
import { DelinqResponse, PtpResponse, CreatePtpRequest, PtpStatus,
  ChargeResponse, ChargeRequest, ChargeType } from '../../core/models';

@Component({
  selector: 'app-collections',
  standalone: true,
  imports: [CommonModule, FormsModule, ReactiveFormsModule, PaginatorComponent],
  templateUrl: './collections.component.html'
})
export class CollectionsComponent implements OnInit {
  private api   = inject(ApiService);
  private fb    = inject(FormBuilder);
  readonly auth  = inject(AuthService);
  private toast = inject(ToastService);

  /** Minimum date for PTP promise date picker — must be tomorrow */
  readonly minPtpDate = new Date(Date.now() + 86400000).toISOString().split('T')[0];

  delinquencies  = signal<DelinqResponse[]>([]);
  ptps           = signal<PtpResponse[]>([]);
  loadingDelinq  = signal(false);
  loadingPtps    = signal(false);
  creatingPtp    = signal(false);
  updatingPtp    = signal(false);

  lookupAccountId = 0;
  hasSearchedPtp  = signal(false);
  alertMsg  = signal('');
  alertType = signal('success');

  ptpStatuses: PtpStatus[] = ['OPEN','KEPT','BROKEN','CANCELLED'];

  ptpForm = this.fb.group({
    loanAccountId:  [null as number|null, [Validators.required, Validators.min(1)]],
    promiseDate:    ['', Validators.required],
    promisedAmount: [null as number|null, [Validators.required, Validators.min(1)]],
    notes: ['']
  });
  get pf() { return this.ptpForm.controls; }

  pgPtps = paginate(() => this.ptps());


  ngOnInit() { this.loadDelinquencies(); }

  loadDelinquencies() {
    this.loadingDelinq.set(true);
    this.api.listAllDelinquencies().subscribe({
      next: r => { this.delinquencies.set(r.data ?? []); this.loadingDelinq.set(false); },
      error: () => this.loadingDelinq.set(false)
    });
  }

  loadPtps() {
    if (!this.lookupAccountId) return;
    this.loadingPtps.set(true);
    this.hasSearchedPtp.set(true);
    this.api.listPtps(this.lookupAccountId).subscribe({
      next: r => { this.ptps.set(r.data ?? []); this.loadingPtps.set(false); },
      error: () => this.loadingPtps.set(false)
    });
  }

  createPtp() {
    this.ptpForm.markAllAsTouched();
    if (this.ptpForm.invalid) return;
    this.creatingPtp.set(true);
    const v = this.ptpForm.value;
    const req: CreatePtpRequest = {
      loanAccountId: v.loanAccountId!,
      promiseDate: v.promiseDate!,
      promisedAmount: v.promisedAmount!,
      notes:         v.notes || undefined,
      
    };
    this.api.createPtp(req).subscribe({
      next: r => {
        this.creatingPtp.set(false);
        this.ptpForm.reset();
        this.ptps.update(l => [...l, r.data]);
        this.flash('PTP created successfully', 'success');
      },
      error: e => { this.creatingPtp.set(false); this.flash(e?.error?.message ?? 'Failed', 'danger'); }
    });
  }

  updateStatus(ptp: PtpResponse, status: PtpStatus) {
    this.updatingPtp.set(true);
    this.api.updatePtpStatus(ptp.ptpId, status).subscribe({
      next: r => {
        this.updatingPtp.set(false);
        this.ptps.update(l => l.map(p => p.ptpId === ptp.ptpId ? r.data : p));
        this.flash(`PTP #${ptp.ptpId} → ${status}`, 'success');
      },
      error: e => { this.updatingPtp.set(false); this.flash(e?.error?.message ?? 'Failed', 'danger'); }
    });
  }

  bucketColor(bucket: string): string {
    const b = bucket?.toUpperCase();
    if (b === 'X' || b === 'CURRENT') return 'bs-verified';
    if (b === '1-30') return 'bs-pending';
    if (b === '31-60') return 'bs-draft';
    if (b === '61-90') return 'bs-rejected';
    return 'bs-rejected';
  }

  ptpBadge(s: PtpStatus): string {
    if (s === 'KEPT') return 'bs-verified';
    if (s === 'OPEN') return 'bs-pending';
    if (s === 'BROKEN') return 'bs-rejected';
    return 'bs-draft';
  }

  // ── Search
  delinqSearch = '';
  filteredDelinq = computed(() => {
    const q = this.delinqSearch.trim().toLowerCase();
    return q ? this.delinquencies().filter(d =>
      (d.bucket||''  ).toLowerCase().includes(q) ||
      String(d.loanAccountId||''  ).includes(q) ||
      String(d.dpd||''  ).includes(q)
    ) : this.delinquencies();
  });
  pgDelinq = paginate(() => this.filteredDelinq());
  onDelinqSearch() { this.pgDelinq.reset(); }

  // ── Charges ─────────────────────────────────────────────────────
  charges        = signal<ChargeResponse[]>([]);
  loadingCharges = signal(false);
  chargeLoanId   = '';
  chargeType: ChargeType = 'PENAL';
  chargeAmount   = 0;
  chargeDesc     = '';
  postingCharge  = signal(false);
  pgCharges      = paginate(() => this.charges());

  loadCharges() {
    const id = Number(this.chargeLoanId);
    if (!id) return;
    this.loadingCharges.set(true);
    this.api.listCharges(id).subscribe({
      next: r => { this.charges.set(r.data ?? []); this.loadingCharges.set(false); },
      error: () => this.loadingCharges.set(false)
    });
  }

  postCharge() {
    const id = Number(this.chargeLoanId);
    if (!id || !this.chargeAmount) return;
    this.postingCharge.set(true);
    const req: ChargeRequest = {
      loanAccountId: id, chargeType: this.chargeType,
      amount: this.chargeAmount, description: this.chargeDesc
    };
    this.api.postCharge(req).subscribe({
      next: r => {
        this.postingCharge.set(false);
        this.charges.update(l => [r.data, ...l]);
        this.chargeAmount = 0; this.chargeDesc = '';
        this.flash('Charge #' + r.data.chargeId + ' posted', 'success');
      },
      error: e => { this.postingCharge.set(false); this.flash(e?.error?.message ?? 'Failed', 'danger'); }
    });
  }

    waivingCharge = signal<number | null>(null);

  waiveCharge(chargeId: number) {
    this.waivingCharge.set(chargeId);
    this.api.waiveCharge(chargeId).subscribe({
      next: r => {
        this.waivingCharge.set(null);
        this.charges.update(l => l.map(c => c.chargeId === chargeId ? r.data : c));
        this.toast.success('Charge Waived', `Charge #${chargeId} has been waived successfully.`);
      },
      error: e => {
        this.waivingCharge.set(null);
        this.toast.error('Waive Failed', e?.error?.message ?? 'Could not waive charge');
      }
    });
  }

  private flash(m: string, t: string) {
    this.alertMsg.set(m); this.alertType.set(t);
    setTimeout(() => this.alertMsg.set(''), 5000);
  }
}
