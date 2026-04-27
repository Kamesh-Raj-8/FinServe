import { Component, OnInit, signal, inject, computed } from '@angular/core';
import { paginate } from '../../core/services/pagination.service';
import { PaginatorComponent } from '../../shared/paginator/paginator.component';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ApiService } from '../../core/services/api.service';
import { AuthService } from '../../core/services/auth.service';
import { AuditLogResponse } from '../../core/models';

@Component({
  selector: 'app-compliance',
  standalone: true,
  imports: [CommonModule, FormsModule, PaginatorComponent],
  templateUrl: './compliance.component.html'
})
export class ComplianceComponent implements OnInit {
  private api   = inject(ApiService);
  readonly auth = inject(AuthService);

  isReadOnly = signal(!this.auth.hasRole('COMPLIANCE'));

  auditLogs   = signal<AuditLogResponse[]>([]);
  filtered    = signal<AuditLogResponse[]>([]);
  loading     = signal(false);
  alertMsg    = signal('');
  alertType   = signal('success');

  filterRefType  = '';
  filterRefId    = 0;
  filterActorId  = 0;
  filterAction   = '';

  pgAuditLogs = paginate(() => this.auditLogs());
  searchAudit = '';
  auditFiltered = computed(() => {
    const q = this.searchAudit.trim().toLowerCase();
    const base = this.filtered();
    return q ? base.filter(l =>
      (l.action||'').toLowerCase().includes(q) ||
      (l.actorEmail||'').toLowerCase().includes(q) ||
      (l.refType||'').toLowerCase().includes(q) ||
      String(l.refId||'').includes(q)
    ) : base;
  });
  pgFiltered  = paginate(() => this.auditFiltered());
  onAuditSearch() { this.pgFiltered.reset(); }


  ngOnInit() { this.loadAll(); }

  loadAll() {
    this.loading.set(true);
    this.api.listAuditLogs().subscribe({
      next: r => {
        const logs = (r.data ?? []).sort((a, b) =>
          (b.createdDate ?? '').localeCompare(a.createdDate ?? ''));
        this.auditLogs.set(logs);
        this.filtered.set(logs);
        this.loading.set(false);
      },
      error: e => { this.loading.set(false); this.alertMsg.set(e?.error?.message ?? 'Failed'); this.alertType.set('danger'); }
    });
  }

  applyFilter() {
    let logs = this.auditLogs();
    if (this.filterAction) {
      logs = logs.filter(l => l.action.toLowerCase().includes(this.filterAction.toLowerCase()));
    }
    if (this.filterRefType) {
      logs = logs.filter(l => l.refType.toLowerCase().includes(this.filterRefType.toLowerCase()));
    }
    if (this.filterRefId > 0) {
      logs = logs.filter(l => l.refId === this.filterRefId);
    }
    if (this.filterActorId > 0) {
      logs = logs.filter(l => l.actorUserId === this.filterActorId);
    }
    this.filtered.set(logs);
  }

  clearFilter() {
    this.filterAction = '';
    this.filterRefType = '';
    this.filterRefId = 0;
    this.filterActorId = 0;
    this.filtered.set(this.auditLogs());
  }

  actionBadge(action: string): string {
    const a = action?.toLowerCase();
    if (a?.includes('created')) return 'bs-pending';
    if (a?.includes('verified') || a?.includes('approved') || a?.includes('accepted')) return 'bs-verified';
    if (a?.includes('rejected') || a?.includes('deleted')) return 'bs-rejected';
    if (a?.includes('updated') || a?.includes('submitted')) return 'bs-draft';
    return 'bs-draft';
  }

  formatDate(d?: string): string {
    if (!d) return '—';
    try { return new Date(d).toLocaleString('en-IN', { dateStyle: 'medium', timeStyle: 'short' }); }
    catch { return d; }
  }
}
