import { Injectable, inject, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { AuthService } from './auth.service';
import { environment } from '../../../environments/environment';
import { ToastService } from './toast.service';

export interface AppNotification {
  notificationId: number;
  title:          string;
  message:        string;
  category:       string;
  entityType?:    string;
  entityId?:      number;
  isRead:         boolean;
  createdAt:      string;
}

/**
 * Database-as-Queue notification service — HTTP pull model only.
 *
 * Polling strategy (spec-compliant):
 *   • Background: polls GET /notifications/unread-count every 30 s (lightweight — count only).
 *     If the count increased since last check, a toast is shown for new items.
 *   • On-demand: GET /notifications (full payload) is called ONLY when the user
 *     opens the notification panel.
 *   • On mark-read: PATCH /notifications/read-all or /{id}/read updates DB state.
 */
@Injectable({ providedIn: 'root' })
export class NotificationService {
  private http  = inject(HttpClient);
  private auth  = inject(AuthService);
  private toast = inject(ToastService);
  private B     = environment.apiUrl;

  /** Full notification list — populated only when panel is opened */
  notifications = signal<AppNotification[]>([]);

  /** Unread count — updated by lightweight 30s poll */
  unreadCount   = signal(0);

  /** Whether the notification panel is open */
  panelOpen     = signal(false);

  loading = signal(false);

  private pollTimer: ReturnType<typeof setInterval> | null = null;
  private lastUnreadCount = 0;
  private readonly POLL_MS = 30_000;

  // ── Lifecycle ──────────────────────────────────────────────────

  /** Call once after login. Fetches initial count and starts background poll. */
  init() {
    this.fetchUnreadCount();
    this.startPolling();
  }

  /** Call on logout. Stops polling and clears state. */
  destroy() {
    this.stopPolling();
    this.notifications.set([]);
    this.unreadCount.set(0);
    this.lastUnreadCount = 0;
    this.panelOpen.set(false);
  }

  // ── Panel interaction ─────────────────────────────────────────

  /** Toggle the notification panel. Loads full list when opening. */
  togglePanel() {
    const opening = !this.panelOpen();
    this.panelOpen.set(opening);
    if (opening) {
      this.loadFullList();
    }
  }

  closePanel() {
    this.panelOpen.set(false);
  }

  // ── On-demand: load full payload ──────────────────────────────

  /** Called only when the panel is opened — fetches full notification records */
  loadFullList() {
    this.loading.set(true);
    this.http.get<any>(`${this.B}/notifications`).subscribe({
      next: r => {
        const list: AppNotification[] = r.data ?? [];
        this.notifications.set(list);
        // Sync unread count with actual data
        const unread = list.filter(n => !n.isRead).length;
        this.unreadCount.set(unread);
        this.lastUnreadCount = unread;
        this.loading.set(false);
      },
      error: () => this.loading.set(false)
    });
  }

  // ── Mark read ────────────────────────────────────────────────

  markAllRead() {
    this.http.patch(`${this.B}/notifications/read-all`, {}).subscribe({
      next: () => {
        this.notifications.update(l => l.map(n => ({ ...n, isRead: true })));
        this.unreadCount.set(0);
        this.lastUnreadCount = 0;
      }
    });
  }

  markOneRead(id: number) {
    this.http.patch(`${this.B}/notifications/${id}/read`, {}).subscribe({
      next: () => {
        this.notifications.update(l =>
          l.map(n => n.notificationId === id ? { ...n, isRead: true } : n)
        );
        this.unreadCount.update(c => Math.max(0, c - 1));
        this.lastUnreadCount = Math.max(0, this.lastUnreadCount - 1);
      }
    });
  }

  // ── Background polling — count only ──────────────────────────

  private startPolling() {
    this.stopPolling();
    this.pollTimer = setInterval(() => this.fetchUnreadCount(), this.POLL_MS);
  }

  private stopPolling() {
    if (this.pollTimer !== null) {
      clearInterval(this.pollTimer);
      this.pollTimer = null;
    }
  }

  /**
   * Polls GET /notifications/unread-count (lightweight — single number).
   * If count increased, shows a toast and updates the badge.
   * Does NOT fetch full notification content.
   */
  private fetchUnreadCount() {
    if (!this.auth.isLoggedIn()) { this.stopPolling(); return; }

    this.http.get<any>(`${this.B}/notifications/unread-count`).subscribe({
      next: r => {
        const newCount: number = r.data ?? 0;
        if (newCount > this.lastUnreadCount) {
          const delta = newCount - this.lastUnreadCount;
          this.toast.info(
            delta === 1 ? '1 new notification' : `${delta} new notifications`,
            'Open the notification panel to view details.'
          );
          // If panel is open, refresh the full list to show new items
          if (this.panelOpen()) {
            this.loadFullList();
          }
        }
        this.unreadCount.set(newCount);
        this.lastUnreadCount = newCount;
      },
      error: () => {}
    });
  }
}
