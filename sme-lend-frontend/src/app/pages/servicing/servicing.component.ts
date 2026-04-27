import { Component, signal, inject, computed } from '@angular/core';
import { paginate } from '../../core/services/pagination.service';
import { PaginatorComponent } from '../../shared/paginator/paginator.component';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ApiService } from '../../core/services/api.service';
import { AuthService } from '../../core/services/auth.service';
import { ToastService } from '../../core/services/toast.service';
import { ScheduleResponse, LoanAccountResponse } from '../../core/models';

@Component({
  selector: 'app-servicing',
  standalone: true,
  imports: [CommonModule, FormsModule, PaginatorComponent],
  templateUrl: './servicing.component.html'
})
export class ServicingComponent {
  private api   = inject(ApiService);
  readonly auth = inject(AuthService);
  private toast = inject(ToastService);

  isReadOnly = signal(!this.auth.hasRole('SERVICING'));

  loanAccId   = 0;
  schedule    = signal<ScheduleResponse[]>([]);
  loanAcc     = signal<LoanAccountResponse | null>(null);
  loading     = signal(false);
  hasSearched = signal(false);  // ← prevents "no match" on initial load
  notFound    = signal(false);

  load() {
    const id = Number(this.loanAccId);
    if (!id || id < 1) {
      this.toast.warning('Enter a valid Loan Account ID');
      return;
    }
    this.loading.set(true);
    this.hasSearched.set(true);
    this.notFound.set(false);
    this.schedule.set([]);
    this.loanAcc.set(null);

    // getLoanAccount is supplementary metadata only.
    // Its failure must NEVER affect notFound or loading — schedule is the primary source.
    this.api.getLoanAccount(id).subscribe({
      next: r => { this.loanAcc.set(r.data); },
      error: () => { /* silently ignore — schedule result is authoritative */ }
    });

    // Schedule is the primary call — it alone controls loading + notFound
    this.api.getSchedule(id).subscribe({
      next: r => {
        const data = r.data ?? [];
        this.schedule.set(data);
        this.loading.set(false);
        if (data.length === 0) {
          this.notFound.set(true);
        } else {
          this.notFound.set(false);
          this.toast.success('Schedule loaded', `${data.length} installments for Account #${id}`);
        }
      },
      error: e => {
        this.loading.set(false);
        this.notFound.set(true);
        this.toast.error('Account not found', e?.error?.message ?? `No loan account with ID #${id}`);
      }
    });
  }

  clear() {
    this.loanAccId = 0;
    this.schedule.set([]);
    this.loanAcc.set(null);
    this.hasSearched.set(false);
    this.notFound.set(false);
  }

  scheduleSearch = '';
  filteredSchedule = computed<import('../../core/models').ScheduleResponse[]>(() => {
    const q = this.scheduleSearch.trim().toLowerCase();
    return q ? this.schedule().filter(r =>
      (r.status||'').toLowerCase().includes(q) ||
      String(r.installmentNo||'').includes(q)
    ) : this.schedule();
  });
  pgSchedule = paginate(() => this.filteredSchedule());
  onScheduleSearch() { this.pgSchedule.reset(); }

  totalDue  = computed(() => this.schedule().reduce((s, r) => s + r.totalDue, 0));
  totalPaid = computed(() => this.schedule().reduce((s, r) => s + r.amountPaid, 0));
  totalBal  = computed(() => this.schedule().reduce((s, r) => s + r.balanceDue, 0));
  paidPct   = computed(() => { const t = this.totalDue(); return t > 0 ? Math.round(this.totalPaid() / t * 100) : 0; });
  paidColor = computed(() => { const p = this.paidPct(); return p >= 80 ? "#10b981" : p >= 40 ? "#f59e0b" : "#ef4444"; });
  overdueCount() { return this.schedule().filter(r => r.status === 'OVERDUE').length; }
  dueCount()     { return this.schedule().filter(r => r.status === 'DUE').length; }
  statusClass(s: string) { return 'badge-status bs-' + s.toLowerCase(); }
}
