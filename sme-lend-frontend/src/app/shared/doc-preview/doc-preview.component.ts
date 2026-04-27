import { Component, Input, Output, EventEmitter, OnChanges, SimpleChanges, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { DomSanitizer, SafeResourceUrl } from '@angular/platform-browser';
import { DocResponse } from '../../core/models';
import { ApiService } from '../../core/services/api.service';

@Component({
  selector: 'app-doc-preview',
  standalone: true,
  imports: [CommonModule],
  template: `
    @if (doc()) {
      <!-- Backdrop -->
      <div class="doc-modal-backdrop" (click)="close()"></div>

      <!-- Modal -->
      <div class="doc-modal">
        <div class="doc-modal-header">
          <div>
            <div class="doc-modal-title">
              <i class="bi bi-file-earmark-pdf text-danger"></i>
              {{ doc()!.fileName || doc()!.docType }}
            </div>
            <div class="doc-modal-meta">{{ doc()!.docType }} · App #{{ doc()!.applicationId }}</div>
          </div>
          <div style="display:flex;gap:.5rem;">
            @if (doc()!.downloadUrl) {
              <button class="btn btn-primary btn-sm" (click)="download()" [disabled]="downloading()">
                @if (downloading()) { <span class="spinner-border spinner-border-sm"></span> }
                @else { <i class="bi bi-download"></i> }
                Download
              </button>
            } @else if (doc()!.fileUri) {
              <a class="btn btn-primary btn-sm" [href]="doc()!.fileUri" target="_blank" rel="noopener">
                <i class="bi bi-box-arrow-up-right"></i> Open Link
              </a>
            }
            <button class="btn btn-outline-secondary btn-sm" (click)="close()">
              <i class="bi bi-x-lg"></i>
            </button>
          </div>
        </div>

        <div class="doc-modal-body">
          @if (loading()) {
            <div class="doc-modal-loading">
              <div class="spinner-border text-primary"></div>
              <p>Loading preview…</p>
            </div>
          } @else if (error()) {
            <div class="doc-modal-error">
              <i class="bi bi-exclamation-triangle" style="font-size:2.5rem;color:var(--warning);"></i>
              <p>{{ error() }}</p>
              @if (doc()!.fileUri) {
                <a class="btn btn-outline-primary btn-sm" [href]="doc()!.fileUri" target="_blank">Open in new tab</a>
              }
            </div>
          } @else if (blobUrl()) {
            <!-- PDF blob preview -->
            @if (isPdf()) {
              <iframe [src]="safeUrl()" class="doc-iframe" title="Document Preview"></iframe>
            } @else {
              <!-- Image preview -->
              <div class="doc-img-wrap">
                <img [src]="blobUrl()" alt="Document" class="doc-img">
              </div>
            }
          } @else if (doc()!.fileUri) {
            <!-- URL-based document -->
            @if (isImageUrl(doc()!.fileUri!)) {
              <div class="doc-img-wrap">
                <img [src]="doc()!.fileUri" alt="Document" class="doc-img">
              </div>
            } @else {
              <iframe [src]="safeUrlStr(doc()!.fileUri!)" class="doc-iframe" title="Document Preview"></iframe>
            }
          } @else {
            <div class="doc-modal-error">
              <i class="bi bi-file-earmark-x" style="font-size:2.5rem;opacity:.4;"></i>
              <p>No preview available for this document.</p>
            </div>
          }
        </div>
      </div>
    }
  `
})
export class DocPreviewComponent implements OnChanges {
  @Input() docData: DocResponse | null = null;
  readonly doc = signal<DocResponse | null>(null);
  @Output() closed = new EventEmitter<void>();

  loading    = signal(false);
  error      = signal('');
  blobUrl    = signal('');
  downloading = signal(false);
  private _safeUrl: SafeResourceUrl | null = null;

  constructor(private api: ApiService, private sanitizer: DomSanitizer) {}

  ngOnChanges(changes: SimpleChanges) {
    if (changes['docData']) {
      this.doc.set(this.docData);
    }
    const d = this.doc();
    if (d && d.downloadUrl) {
      this.loadBlob(d);
    } else {
      this.blobUrl.set('');
      this.error.set('');
      this.loading.set(false);
    }
  }

  private loadBlob(d: DocResponse) {
    this.loading.set(true);
    this.blobUrl.set('');
    this.error.set('');
    this.api.downloadDoc(d.applicationId, d.documentId).subscribe({
      next: blob => {
        const url = URL.createObjectURL(blob);
        this.blobUrl.set(url);
        this._safeUrl = this.sanitizer.bypassSecurityTrustResourceUrl(url);
        this.loading.set(false);
      },
      error: () => {
        this.loading.set(false);
        this.error.set('Could not load document preview. You can still download it.');
      }
    });
  }

  download() {
    const d = this.doc();
    if (!d?.downloadUrl) return;
    this.downloading.set(true);
    this.api.downloadDoc(d.applicationId, d.documentId).subscribe({
      next: blob => {
        this.downloading.set(false);
        const url = URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url; a.download = d.fileName || 'document';
        a.click();
        URL.revokeObjectURL(url);
      },
      error: () => { this.downloading.set(false); }
    });
  }

  close() {
    if (this.blobUrl()) URL.revokeObjectURL(this.blobUrl());
    this.blobUrl.set('');
    this._safeUrl = null;
    this.error.set('');
    this.closed.emit();
  }

  safeUrl(): SafeResourceUrl {
    return this._safeUrl ?? this.sanitizer.bypassSecurityTrustResourceUrl('');
  }

  safeUrlStr(url: string): SafeResourceUrl {
    return this.sanitizer.bypassSecurityTrustResourceUrl(url);
  }

  isPdf(): boolean {
    const d = this.doc();
    return !!(d?.contentType?.includes('pdf') || d?.fileName?.toLowerCase().endsWith('.pdf'));
  }

  isImageUrl(url: string): boolean {
    return /\.(jpg|jpeg|png|gif|webp|svg)(\?|$)/i.test(url);
  }
}
