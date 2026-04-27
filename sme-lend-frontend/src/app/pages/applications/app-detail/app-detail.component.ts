import { Component, OnInit, Input, signal, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { FormsModule } from '@angular/forms';
import { ApiService } from '../../../core/services/api.service';
import { AuthService } from '../../../core/services/auth.service';
import { AppResponse, DocResponse, DocType } from '../../../core/models';

@Component({
  selector: 'app-app-detail',
  standalone: true,
  imports: [CommonModule, RouterLink, ReactiveFormsModule, FormsModule],
  templateUrl: './app-detail.component.html'
})
export class AppDetailComponent implements OnInit {
  @Input() appId!: string;
  private api  = inject(ApiService);
  private fb   = inject(FormBuilder);
  readonly auth = inject(AuthService);

  app       = signal<AppResponse|null>(null);
  docs      = signal<DocResponse[]>([]);
  loading   = signal(true);
  submitting  = signal(false);
  uploadMode  = signal<'file'|'uri'>('file');   // toggle between PDF upload and legacy URI
  addingDoc   = signal(false);
  alertMsg    = signal('');
  alertType   = signal('success');

  // Upload state
  selectedFile: File | null = null;
  selectedDocType: DocType = 'PAN';
  fileError = '';

  docTypes: DocType[] = ['PAN','AADHAAR','GST','SHOP_LICENSE','BANK_STATEMENT','OTHER'];

  // Legacy URI form
  docForm = this.fb.group({
    docType: ['PAN', Validators.required],
    fileUri: ['', Validators.required]
  });
  get df() { return this.docForm.controls; }

  ngOnInit() {
    const id = Number(this.appId);
    this.api.getApp(id).subscribe({
      next: r => { this.app.set(r.data); this.loading.set(false); },
      error: () => this.loading.set(false)
    });
    this.api.listDocs(id).subscribe({ next: r => this.docs.set(r.data ?? []) });
  }

  // ── Submit Application ──────────────────────────────────────────
  submitApp() {
    if (!confirm('Submit this application for underwriting review?')) return;
    this.submitting.set(true);
    this.api.submitApp(Number(this.appId)).subscribe({
      next: r => { this.submitting.set(false); this.app.set(r.data); this.flash('Application submitted to underwriting', 'success'); },
      error: e => { this.submitting.set(false); this.flash(e?.error?.message ?? 'Failed', 'danger'); }
    });
  }

  // ── PDF File Upload ─────────────────────────────────────────────
  onFileSelected(event: Event) {
    const input = event.target as HTMLInputElement;
    this.fileError = '';
    if (input.files && input.files.length > 0) {
      const f = input.files[0];
      if (f.type !== 'application/pdf' && !f.type.startsWith('image/')) {
        this.fileError = 'Only PDF and image files are accepted.';
        this.selectedFile = null;
        return;
      }
      if (f.size > 20 * 1024 * 1024) {
        this.fileError = 'File size must be under 20 MB.';
        this.selectedFile = null;
        return;
      }
      this.selectedFile = f;
    }
  }

  uploadDoc() {
    if (!this.selectedFile) { this.fileError = 'Please select a file.'; return; }
    this.addingDoc.set(true);
    this.api.uploadDoc(Number(this.appId), this.selectedFile, this.selectedDocType).subscribe({
      next: r => {
        this.docs.update(d => [...d, r.data]);
        this.selectedFile = null;
        this.fileError = '';
        (document.getElementById('fileInput') as HTMLInputElement).value = '';
        this.addingDoc.set(false);
        this.flash('Document uploaded successfully', 'success');
      },
      error: e => { this.addingDoc.set(false); this.flash(e?.error?.message ?? 'Upload failed', 'danger'); }
    });
  }

  // ── Legacy URI add ──────────────────────────────────────────────
  addDoc() {
    this.docForm.markAllAsTouched();
    if (this.docForm.invalid) return;
    this.addingDoc.set(true);
    const v = this.docForm.value;
    this.api.addDoc(Number(this.appId), { docType: v.docType as DocType, fileUri: v.fileUri! }).subscribe({
      next: r => {
        this.docs.update(d => [...d, r.data]);
        this.docForm.reset({ docType: 'PAN' });
        this.addingDoc.set(false);
        this.flash('Document link added', 'success');
      },
      error: e => { this.addingDoc.set(false); this.flash(e?.error?.message ?? 'Failed', 'danger'); }
    });
  }

  // ── Download ────────────────────────────────────────────────────
  downloadDoc(doc: DocResponse) {
    if (!doc.downloadUrl) return;
    this.api.downloadDoc(Number(this.appId), doc.documentId).subscribe({
      next: blob => {
        const url = URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = doc.fileName || 'document';
        a.click();
        URL.revokeObjectURL(url);
      },
      error: () => this.flash('Download failed', 'danger')
    });
  }

  // ── Helpers ─────────────────────────────────────────────────────
  canSubmit() { return ['DRAFT','READY_TO_SUBMIT'].includes(this.app()?.status ?? '') && !this.isAdmin(); }
  isAdmin()   { return this.auth.hasRole('ADMIN'); }
  isOwnerOrAgent() {
    const role = this.auth.userRole();
    return role === 'ADMIN' || role === 'AGENT' ||
           this.app()?.createdByUserId === this.auth.userId();
  }
  formatStatus(s: string) { return s.replace(/_/g, ' '); }
  statusClass(s: string)  { return 'badge-status bs-' + s.toLowerCase(); }

  isAfter(current: string, step: string) {
    const order = ['DRAFT','KYC_PENDING','READY_TO_SUBMIT','SUBMITTED','ROUTED_TO_UW','UW_APPROVED','OFFERED','OFFER_ACCEPTED','DISBURSED'];
    return order.indexOf(current) > order.indexOf(step);
  }

  private flash(msg: string, type: string) {
    this.alertMsg.set(msg); this.alertType.set(type);
    setTimeout(() => this.alertMsg.set(''), 5000);
  }
}
