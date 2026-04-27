import { Component, OnInit, signal, inject, computed } from '@angular/core';
import { paginate } from '../../core/services/pagination.service';
import { PaginatorComponent } from '../../shared/paginator/paginator.component';
import { CommonModule } from '@angular/common';
import { FormsModule, FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ApiService } from '../../core/services/api.service';
import { ToastService } from '../../core/services/toast.service';
import { AuthService } from '../../core/services/auth.service';
import { RepayResponse, RepayMode } from '../../core/models';

@Component({
  selector: 'app-repayments',
  standalone: true,
  imports: [CommonModule, FormsModule, ReactiveFormsModule, PaginatorComponent],
  templateUrl: './repayments.component.html'
})
export class RepaymentsComponent implements OnInit {
  private api  = inject(ApiService);
  private fb   = inject(FormBuilder);
  private toast = inject(ToastService);
  readonly auth = inject(AuthService);

  tab         = signal(0);
  repayments  = signal<RepayResponse[]>([]);
  loadingList = signal(false);
  hasSearched = signal(false);
  posting     = signal(false);
  loanAccId   = 0;

  /** EMI schedule for quick-pay — loaded when loanAccountId is entered */
  schedule    = signal<import('../../core/models').ScheduleResponse[]>([]);
  scheduleLoading = signal(false);
  selectedScheduleId = signal<number | null>(null);

  modes: RepayMode[] = ['UPI','NEFT','IMPS','RTGS','CASH','OTHER'];

  /** True when the logged-in user can post repayments (SERVICING or OPERATIONS) */
  canPost = computed(() => {
    const role = this.auth.userRole();
    return role === 'SERVICING' || role === 'OPERATIONS' || role === 'ADMIN';
  });

  postForm = this.fb.group({
    loanAccountId: [null as number | null, [Validators.required, Validators.min(1)]],
    scheduleId:    [null as number | null],  // optional — auto-fills amount
    amount:        [null as number | null, [Validators.min(0.01)]],
    mode:          ['UPI', Validators.required],
    referenceNo:   [''],
    paymentDate:   [new Date().toISOString().split('T')[0], Validators.required],
  });
  get pf() { return this.postForm.controls; }

  /** Load EMI schedule for the entered loan account */
  loadSchedule(): void {
    const accId = this.pf['loanAccountId'].value;
    if (!accId || accId < 1) { this.schedule.set([]); return; }
    this.scheduleLoading.set(true);
    this.api.getSchedule(accId).subscribe({
      next: r => { this.schedule.set((r.data ?? []).filter((s: any) => s.status !== 'PAID')); this.scheduleLoading.set(false); },
      error: () => { this.schedule.set([]); this.scheduleLoading.set(false); }
    });
  }

  /** Select a schedule row — auto-fills amount from balanceDue */
  selectSchedule(s: import('../../core/models').ScheduleResponse): void {
    this.selectedScheduleId.set(s.scheduleId);
    this.pf['scheduleId'].setValue(s.scheduleId);
    this.pf['amount'].setValue(s.balanceDue ?? s.totalDue);
  }

  clearSchedule(): void {
    this.selectedScheduleId.set(null);
    this.pf['scheduleId'].setValue(null);
    this.pf['amount'].setValue(null);
  }

  repaySearch = '';
  filteredRepayments = computed(() => {
    const q = this.repaySearch.trim().toLowerCase();
    return q ? this.repayments().filter(r =>
      (r.referenceNo||'').toLowerCase().includes(q) ||
      String(r.amount||'').includes(q)
    ) : this.repayments();
  });
  pgRepayments = paginate(() => this.filteredRepayments());
  onRepaySearch() { this.pgRepayments.reset(); }

  ngOnInit(): void { /* nothing to pre-load */ }

  loadHistory(): void {
    if (!this.loanAccId) return;
    this.loadingList.set(true);
    this.hasSearched.set(true);
    this.api.listRepayments(this.loanAccId).subscribe({
      next: r  => { this.repayments.set(r.data ?? []); this.loadingList.set(false); },
      error: () => this.loadingList.set(false)
    });
  }

  post(): void {
    this.postForm.markAllAsTouched();
    if (this.postForm.invalid) return;
    this.posting.set(true);
    const v = this.postForm.value;
    const accId = v.loanAccountId!;
    this.api.postRepayment({
      loanAccountId: accId,
      scheduleId:    v.scheduleId ?? undefined,
      amount:        v.scheduleId ? undefined : v.amount!,  // auto when scheduleId given
      mode:          v.mode as RepayMode,
      referenceNo:   v.referenceNo || undefined,
      paymentDate:   v.paymentDate || new Date().toISOString().split('T')[0]
    }).subscribe({
      next: r => {
        this.posting.set(false);
        // Fix: reset entire form then restore defaults so validators don't fire
        this.postForm.reset({
          loanAccountId: accId,             // keep loan account for convenience
          amount:        null,
          mode:          'UPI',
          referenceNo:   '',
          paymentDate:   new Date().toISOString().split('T')[0]
        });
        this.postForm.markAsUntouched();    // ← prevents spurious validation errors
        this.postForm.markAsPristine();
        this.selectedScheduleId.set(null);
        this.schedule.update(s => s.filter(row => row.scheduleId !== v.scheduleId));
        if (this.loanAccId === accId) {
          this.repayments.update(l => [r.data, ...l]);
        }
        this.toast.success('Repayment Posted',
          `₹${r.data.amount.toLocaleString('en-IN')} posted · Ref: ${r.data.referenceNo ?? 'N/A'}`);
      },
      error: e => {
        this.posting.set(false);
        this.toast.error('Post Failed', e?.error?.message ?? 'Unable to post repayment');
      }
    });
  }
}
