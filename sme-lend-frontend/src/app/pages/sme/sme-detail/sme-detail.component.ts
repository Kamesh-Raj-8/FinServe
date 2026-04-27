import { Component, OnInit, Input, signal, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ApiService } from '../../../core/services/api.service';
import { AuthService } from '../../../core/services/auth.service';
import { SmeResponse, PromoterResponse, BizType, CreateSmeRequest, UpdateSmeRequest } from '../../../core/models';

@Component({ selector: 'app-sme-detail', standalone: true, imports: [CommonModule, RouterLink, ReactiveFormsModule], templateUrl: './sme-detail.component.html' })
export class SmeDetailComponent implements OnInit {
  @Input() smeId!: string;
  private api = inject(ApiService); private fb = inject(FormBuilder);
  readonly auth = inject(AuthService);
  sme = signal<SmeResponse|null>(null); promoters = signal<PromoterResponse[]>([]); loading = signal(true);
  showPromoterForm = signal(false); savingPromoter = signal(false);
  showEditModal = signal(false); savingSme = signal(false);
  alertMsg = signal(''); alertType = signal('success');
  bizTypes: BizType[] = ['PROPRIETORSHIP','PARTNERSHIP','PVT_LTD','LLP','OTHER'];

  pForm = this.fb.group({ promoterName: ['', Validators.required], mobile: ['', Validators.required], ownershipPct: [null as number|null, [Validators.required, Validators.min(0.01), Validators.max(100)]], monthlyIncome: [null as number|null, [Validators.required, Validators.min(0)]] });
  eForm = this.fb.group({ legalName: ['', Validators.required], tradeName: [''], businessType: ['PROPRIETORSHIP', Validators.required], industry: ['', Validators.required], address: ['', Validators.required], gstNo: [''] });
  get pf() { return this.pForm.controls; } get ef() { return this.eForm.controls; }

  ngOnInit() {
    const id = Number(this.smeId);
    this.api.getSme(id).subscribe({ next: r => { this.sme.set(r.data); this.loading.set(false); }, error: () => this.loading.set(false) });
    this.api.listPromoters(id).subscribe({ next: r => this.promoters.set(r.data??[]) });
  }

  openEdit() {
    const s = this.sme(); if (!s) return;
    this.eForm.patchValue({ legalName: s.legalName, tradeName: s.tradeName??'', businessType: s.businessType, industry: s.industry, address: s.address, gstNo: s.gstNo??'' });
    this.showEditModal.set(true);
  }

  saveSme() {
    this.eForm.markAllAsTouched(); if (this.eForm.invalid) return;
    this.savingSme.set(true);
    const v = this.eForm.value;
    const payload = {
      legalName: v.legalName!,
      tradeName: v.tradeName || undefined,
      businessType: v.businessType as BizType,
      industry: v.industry!,
      address: v.address!,
      gstNo: v.gstNo?.toUpperCase() || undefined
    };
    const request$ = this.smeId
      ? this.api.updateSme(Number(this.smeId), payload as UpdateSmeRequest)
      : this.api.createSme(payload as CreateSmeRequest);
    request$.subscribe({
      next: r => { this.savingSme.set(false); this.sme.set(r.data); this.showEditModal.set(false); this.flash(this.smeId ? 'SME updated' : 'SME registered', 'success'); },
      error: e => { this.savingSme.set(false); this.flash(e?.error?.message??'Failed', 'danger'); }
    });
  }

  addPromoter() {
    this.pForm.markAllAsTouched(); if (this.pForm.invalid) return;
    this.savingPromoter.set(true);
    const v = this.pForm.value;
    this.api.addPromoter(Number(this.smeId), { promoterName: v.promoterName!, mobile: v.mobile!, ownershipPct: v.ownershipPct!, monthlyIncome: v.monthlyIncome! }).subscribe({
      next: r => { this.promoters.update(l => [...l, r.data]); this.pForm.reset(); this.showPromoterForm.set(false); this.savingPromoter.set(false); this.flash('Promoter added', 'success'); },
      error: e => { this.savingPromoter.set(false); this.flash(e?.error?.message??'Failed', 'danger'); }
    });
  }

  togglePromoterForm() { this.showPromoterForm.update(v => !v); }
  closePromoterForm() { this.showPromoterForm.set(false); }
  closeEditModal() { this.showEditModal.set(false); this.eForm.reset(); }
  badgeClass(s: string) { return 'badge-status bs-' + s.toLowerCase(); }
  isAdmin() { return this.auth.hasRole('ADMIN'); }
  private flash(msg: string, type: string) { this.alertMsg.set(msg); this.alertType.set(type); setTimeout(() => this.alertMsg.set(''), 4000); }
}
