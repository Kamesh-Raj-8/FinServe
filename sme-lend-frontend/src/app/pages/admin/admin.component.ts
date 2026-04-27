import { Component, OnInit, signal, inject, computed } from '@angular/core';
import { paginate } from '../../core/services/pagination.service';
import { PaginatorComponent } from '../../shared/paginator/paginator.component';
import { CommonModule } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, FormsModule, Validators } from '@angular/forms';
import { ApiService } from '../../core/services/api.service';
import {
  UserResponse, LoanProductResponse, RoleResponse,
  LoanProductRequest, RoleName, StatusFlag,
  KycResponse, AppResponse, AuditLogResponse, DelinqResponse, PortfolioMetrics,
  EligibilityPolicyResponse, EligibilityPolicyRequest,
  FeeConfigResponse, FeeConfigRequest, FeeType, FeeMode
} from '../../core/models';

@Component({
  selector: 'app-admin',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, FormsModule, PaginatorComponent],
  templateUrl: './admin.component.html'
})
export class AdminComponent implements OnInit {
  private api = inject(ApiService);
  private fb  = inject(FormBuilder);

  // ── Active Tab ──────────────────────────────────────────────────
  tab = signal(0);

  // ── Manage Tab Data ──────────────────────────────────────────────
  users    = signal<UserResponse[]>([]);
  products = signal<LoanProductResponse[]>([]);
  roles    = signal<RoleResponse[]>([]);

  loadingUsers    = signal(false);
  creatingUser    = signal(false);
  creatingProduct = signal(false);
  savingProduct   = signal(false);

  // ── Monitor Tab Data ─────────────────────────────────────────────
  monitorKyc        = signal<KycResponse[]>([]);
  monitorApps       = signal<AppResponse[]>([]);
  monitorAuditLogs  = signal<AuditLogResponse[]>([]);
  monitorDelinq     = signal<DelinqResponse[]>([]);

  // ── Search state
  userSearch    = '';
  productSearch = '';

  filteredUsers    = computed(() => {
    const q = this.userSearch.trim().toLowerCase();
    return q ? this.users().filter((u:any) => u.fullName.toLowerCase().includes(q) || u.email.toLowerCase().includes(q) || String(u.userId) === q) : this.users();
  });
  filteredProducts = computed(() => {
    const q = this.productSearch.trim().toLowerCase();
    return q ? this.products().filter((p:any) => p.productName.toLowerCase().includes(q)) : this.products();
  });

  pgUsers     = paginate(() => this.filteredUsers());
  pgProducts  = paginate(() => this.filteredProducts());
  pgMonKyc    = paginate(() => this.monitorKyc());
  pgMonApps   = paginate(() => this.monitorApps());
  pgAuditLogs = paginate(() => this.monitorAuditLogs());
  pgDelinq    = paginate(() => this.monitorDelinq());

  onUserSearch()    { this.pgUsers.reset(); }
  onProductSearch() { this.pgProducts.reset(); }
  // ── Eligibility Policies ──────────────────────────────────────
  policies         = signal<EligibilityPolicyResponse[]>([]);
  loadingPolicies  = signal(false);
  showPolicyForm   = signal(false);
  savingPolicy     = signal(false);
  policyProductId  = 0;
  policyRuleName   = '';
  policyMaxAmount: number | null = null;
  policyMinScore: number | null = null;
  policyRuleExpr   = '';
  pgPolicies       = paginate(() => this.policies());

  // ── Fee Configs ────────────────────────────────────────────────
  fees             = signal<FeeConfigResponse[]>([]);
  loadingFees      = signal(false);
  showFeeForm      = signal(false);
  savingFee        = signal(false);
  feeProductId     = 0;
  feeType: FeeType = 'PROCESSING';
  feeMode: FeeMode = 'FLAT';
  feeValue         = 0;
  pgFees           = paginate(() => this.fees());

  monitorMetrics    = signal<PortfolioMetrics | null>(null);
  loadingMonitor    = signal(false);

  alertMsg  = signal('');
  alertType = signal('success');

  editingProduct   = signal<LoanProductResponse | null>(null);

  allowedRoles: RoleName[] = [
    'AGENT','UNDERWRITER','OPERATIONS','SERVICING','COLLECTIONS','RISK','COMPLIANCE'
  ];

  userForm = this.fb.group({
    fullName:      ['', Validators.required],
    email:         ['', [Validators.required, Validators.email]],
    password:      ['', [Validators.required, Validators.minLength(6)]],
    phone:         [''],
    role:          ['AGENT', Validators.required],
    bankAccountNo: [''],
    ifsc:          ['']
  });
  get uf() { return this.userForm.controls; }

  productForm = this.fb.group({
    productName:      ['', Validators.required],
    minAmount:        [null as number | null, [Validators.required, Validators.min(1)]],
    maxAmount:        [null as number | null, [Validators.required, Validators.min(1)]],
    minTenorMonths:   [null as number | null, [Validators.required, Validators.min(1)]],
    maxTenorMonths:   [null as number | null, [Validators.required, Validators.min(1)]],
    baseInterestRate:       [null as number | null, [Validators.required, Validators.min(0.01)]],
    creditThreshold:        [null as number | null, [Validators.min(300), Validators.max(900)]],
    minIncomeAmount:        [null as number | null],
    maxIncomeAmount:        [null as number | null],    delinquencyFinePerDay:  [null as number | null, [Validators.min(0)]]
  });
  get pf() { return this.productForm.controls; }

  ngOnInit(): void {
    this.loadUsers();
    this.loadProducts();
    this.api.listRoles().subscribe({ next: r => this.roles.set(r.data ?? []) });
  }

  setTab(n: number): void {
    this.tab.set(n);
    if (n === 2) this.loadMonitor();
  }

  // ── User Management ──────────────────────────────────────────────
  loadUsers(): void {
    this.loadingUsers.set(true);
    this.api.listUsers().subscribe({
      next: r => { this.users.set(r.data ?? []); this.loadingUsers.set(false); },
      error: () => this.loadingUsers.set(false)
    });
  }

  createUser(): void {
    this.userForm.markAllAsTouched();
    if (this.userForm.invalid) return;
    this.creatingUser.set(true);
    const v = this.userForm.value;
    this.api.createUser({
      fullName: v.fullName!, email: v.email!, password: v.password!,
      phone: v.phone || undefined, role: v.role as RoleName,
      bankAccountNo: v.bankAccountNo || undefined, ifsc: v.ifsc || undefined
    }).subscribe({
      next: r => {
        this.creatingUser.set(false);
        this.users.update(l => [...l, r.data]);
        this.userForm.reset({ role: 'AGENT' });
        this.flash(`User ${r.data.email} created`, 'success');
      },
      error: e => { this.creatingUser.set(false); this.flash(e?.error?.message ?? 'Failed', 'danger'); }
    });
  }

  toggleUserStatus(u: UserResponse): void {
    const next: StatusFlag = u.status === 'ACTIVE' ? 'INACTIVE' : 'ACTIVE';
    this.api.setUserStatus(u.userId, next).subscribe({
      next: r => { this.users.update(l => l.map(x => x.userId === u.userId ? r.data : x)); this.flash(`${r.data.email} set to ${r.data.status}`, 'success'); },
      error: e => this.flash(e?.error?.message ?? 'Failed', 'danger')
    });
  }

  // ── Product Management ───────────────────────────────────────────
  loadProducts(): void {
    this.api.listProducts().subscribe({ next: r => this.products.set(r.data ?? []) });
  }

  createProduct(): void {
    this.productForm.markAllAsTouched();
    if (this.productForm.invalid) return;
    this.creatingProduct.set(true);
    this.api.createProduct(this.buildPayload()).subscribe({
      next: r => {
        this.creatingProduct.set(false);
        this.products.update(l => [...l, r.data]);
        this.productForm.reset();
        this.flash(`Loan product "${r.data.productName}" created`, 'success');
      },
      error: e => { this.creatingProduct.set(false); this.flash(e?.error?.message ?? 'Failed', 'danger'); }
    });
  }

  openEdit(p: LoanProductResponse): void {
    this.editingProduct.set(p);
    this.productForm.patchValue({
      productName: p.productName, minAmount: p.minAmount, maxAmount: p.maxAmount,
      minTenorMonths: p.minTenorMonths, maxTenorMonths: p.maxTenorMonths, baseInterestRate: p.baseInterestRate,
      creditThreshold: p.creditThreshold ?? null, minIncomeAmount: p.minIncomeAmount ?? null,
      maxIncomeAmount: p.maxIncomeAmount ?? null
    });
  }

  saveEdit(): void {
    this.productForm.markAllAsTouched();
    if (this.productForm.invalid || !this.editingProduct()) return;
    this.savingProduct.set(true);
    const id = this.editingProduct()!.productId;
    this.api.updateProduct(id, this.buildPayload()).subscribe({
      next: r => {
        this.savingProduct.set(false);
        this.products.update(l => l.map(x => x.productId === id ? r.data : x));
        this.editingProduct.set(null); this.productForm.reset();
        this.flash(`"${r.data.productName}" updated`, 'success');
      },
      error: e => { this.savingProduct.set(false); this.flash(e?.error?.message ?? 'Update failed', 'danger'); }
    });
  }

  toggleProductStatus(p: LoanProductResponse): void {
    const next: StatusFlag = p.status === 'ACTIVE' ? 'INACTIVE' : 'ACTIVE';
    this.api.setProductStatus(p.productId, next).subscribe({
      next: r => { this.products.update(l => l.map(x => x.productId === p.productId ? r.data : x)); this.flash(`Product ${next}`, 'success'); },
      error: e => this.flash(e?.error?.message ?? 'Failed', 'danger')
    });
  }


  // ── Monitor: GET-Only Cross-Role View ────────────────────────────
  loadMonitor(): void {
    this.loadingMonitor.set(true);
    this.api.monitorAllKyc().subscribe({ next: r => this.monitorKyc.set(r.data ?? []) });
    this.api.monitorAllApps().subscribe({ next: r => { this.monitorApps.set(r.data ?? []); this.loadingMonitor.set(false); } });
    this.api.monitorRiskMetrics().subscribe({ next: r => this.monitorMetrics.set(r.data) });
    this.api.monitorAuditLogs().subscribe({ next: r => this.monitorAuditLogs.set((r.data ?? []).slice(0, 50)) });
    this.api.monitorDelinquencies().subscribe({ next: r => this.monitorDelinq.set(r.data ?? []) });
  }

  // ── Helpers ─────────────────────────────────────────────────────

  // ── Eligibility Policies ──────────────────────────────────────
  loadPolicies() {
    this.loadingPolicies.set(true);
    this.api.listEligibilityPolicies().subscribe({
      next: r => { this.policies.set(r.data ?? []); this.loadingPolicies.set(false); },
      error: () => this.loadingPolicies.set(false)
    });
  }

  createPolicy() {
    if (!this.policyProductId || !this.policyRuleName.trim()) {
      this.flash('Product and rule name are required', 'danger'); return;
    }
    this.savingPolicy.set(true);
    const req: EligibilityPolicyRequest = {
      productId: this.policyProductId,
      ruleName: this.policyRuleName,
      ruleExpression: this.policyRuleExpr || undefined,
      maxAmountCap: this.policyMaxAmount ?? undefined,
      minCreditScore: this.policyMinScore ?? undefined,
    };
    this.api.createEligibilityPolicy(req).subscribe({
      next: r => {
        this.savingPolicy.set(false); this.showPolicyForm.set(false);
        this.policies.update(l => [r.data, ...l]);
        this.flash('Eligibility policy created', 'success');
      },
      error: e => { this.savingPolicy.set(false); this.flash(e?.error?.message ?? 'Failed', 'danger'); }
    });
  }

  deactivatePolicy(id: number) {
    this.api.deactivatePolicy(id).subscribe({
      next: () => { this.policies.update(l => l.filter(p => p.policyId !== id)); this.flash('Policy deactivated', 'success'); },
      error: e => this.flash(e?.error?.message ?? 'Failed', 'danger')
    });
  }

  // ── Fee Configs ────────────────────────────────────────────────
  loadFees() {
    this.loadingFees.set(true);
    this.api.listFeeConfigs().subscribe({
      next: r => { this.fees.set(r.data ?? []); this.loadingFees.set(false); },
      error: () => this.loadingFees.set(false)
    });
  }

  createFee() {
    if (!this.feeProductId || !this.feeValue) {
      this.flash('Product and value are required', 'danger'); return;
    }
    this.savingFee.set(true);
    const req: FeeConfigRequest = {
      productId: this.feeProductId, feeType: this.feeType,
      feeMode: this.feeMode, value: this.feeValue,
    };
    this.api.createFeeConfig(req).subscribe({
      next: r => {
        this.savingFee.set(false); this.showFeeForm.set(false);
        this.fees.update(l => [r.data, ...l]);
        this.flash('Fee config created', 'success');
      },
      error: e => { this.savingFee.set(false); this.flash(e?.error?.message ?? 'Failed', 'danger'); }
    });
  }

  deactivateFee(id: number) {
    this.api.deactivateFee(id).subscribe({
      next: () => { this.fees.update(l => l.filter(f => f.feeId !== id)); this.flash('Fee deactivated', 'success'); },
      error: e => this.flash(e?.error?.message ?? 'Failed', 'danger')
    });
  }

  badgeClass(s: string): string { return 'badge-status bs-' + s.toLowerCase(); }
  cancelEdit(): void { this.editingProduct.set(null); this.productForm.reset(); }

  private buildPayload(): LoanProductRequest {
    const v = this.productForm.value;
    return {
      productName: v.productName!, minAmount: v.minAmount!, maxAmount: v.maxAmount!,
      minTenorMonths: v.minTenorMonths!, maxTenorMonths: v.maxTenorMonths!,
      baseInterestRate: v.baseInterestRate!,
      creditThreshold: v.creditThreshold ?? undefined,
      minIncomeAmount: v.minIncomeAmount ?? undefined,
      maxIncomeAmount: v.maxIncomeAmount ?? undefined,    };
  }

  private flash(m: string, t: string): void {
    this.alertMsg.set(m); this.alertType.set(t);
    setTimeout(() => this.alertMsg.set(''), 5000);
  }
}
