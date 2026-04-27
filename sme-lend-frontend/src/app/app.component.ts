import { Component, signal, computed, inject } from '@angular/core';
import { RouterOutlet, RouterLink, RouterLinkActive } from '@angular/router';
import { CommonModule, DatePipe } from '@angular/common';
import { AuthService }         from './core/services/auth.service';
import { ToastService }        from './core/services/toast.service';
import { NotificationService } from './core/services/notification.service';
import { RoleName }            from './core/models';
import { roleRoute }           from './core/guards/auth.guard';

interface NavItem {
  icon: string; label: string; route: string;
  roles: RoleName[]; isPrimary?: boolean;
}
interface NavGroup { label: string; items: NavItem[]; }

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [RouterOutlet, RouterLink, RouterLinkActive, CommonModule, DatePipe],
  styles: [`
    /* ─── Layout skeleton ─── */
    .app-shell { display: flex; height: 100dvh; overflow: hidden; background: var(--bg-page, #f4f6fb); }

    /* ─── Sidebar ─── */
    .sidebar {
      width: 260px; flex-shrink: 0;
      background: #0f2027;
      display: flex; flex-direction: column;
      overflow-y: auto; overflow-x: hidden;
      transition: transform .25s ease;
      z-index: 200;
    }
    @media (max-width: 768px) {
      .sidebar { position: fixed; inset: 0 auto 0 0; transform: translateX(-100%); }
      .sidebar.open { transform: translateX(0); box-shadow: 4px 0 24px rgba(0,0,0,.35); }
      .sb-backdrop { display: block !important; }
    }
    .sb-backdrop {
      display: none; position: fixed; inset: 0; background: rgba(0,0,0,.45);
      z-index: 199; opacity: 0; pointer-events: none; transition: opacity .25s;
    }
    .sb-backdrop.show { opacity: 1; pointer-events: all; }

    .sb-brand {
      display: flex; align-items: center; gap: .75rem;
      padding: 1.25rem 1rem 1rem; border-bottom: 1px solid rgba(255,255,255,.08);
    }
    .sb-logo {
      width: 38px; height: 38px; border-radius: 10px;
      background: linear-gradient(135deg, #0d9488, #0891b2);
      display: flex; align-items: center; justify-content: center;
      font-size: 1.2rem; color: #fff; flex-shrink: 0;
    }
    .sb-name { color: #fff; font-weight: 700; font-size: 1rem; font-family: var(--fs-font-head, 'Sora', sans-serif); }
    .sb-sub  { color: rgba(255,255,255,.45); font-size: .7rem; margin-top: 1px; }

    .sb-nav { flex: 1; padding: .75rem 0; overflow-y: auto; }
    .sb-section {
      color: rgba(255,255,255,.35); font-size: .63rem; font-weight: 700;
      letter-spacing: .8px; text-transform: uppercase;
      padding: 1rem 1rem .35rem;
    }
    .sb-link {
      display: flex; align-items: center; gap: .75rem;
      padding: .55rem 1rem; margin: 1px .5rem;
      border-radius: 8px; color: rgba(255,255,255,.65);
      font-size: .82rem; text-decoration: none; transition: all .15s;
    }
    .sb-link:hover { background: rgba(255,255,255,.07); color: #fff; }
    .sb-link.active { background: rgba(13,148,136,.25); color: #5eead4; }
    .sb-link i { font-size: 1rem; flex-shrink: 0; }
    .sb-readonly-badge {
      margin-left: auto; font-size: .62rem;
      color: rgba(255,255,255,.3); background: rgba(255,255,255,.06);
      border-radius: 4px; padding: 1px 5px;
    }

    .sb-footer {
      display: flex; align-items: center; gap: .6rem;
      padding: .875rem 1rem; border-top: 1px solid rgba(255,255,255,.08);
    }
    .sb-avatar {
      width: 32px; height: 32px; border-radius: 50%;
      background: linear-gradient(135deg, #0d9488, #0891b2);
      color: #fff; font-weight: 700; font-size: .85rem;
      display: flex; align-items: center; justify-content: center; flex-shrink: 0;
    }
    .sb-user-name { color: rgba(255,255,255,.9); font-size: .75rem; font-weight: 600; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
    .sb-user-role { color: rgba(255,255,255,.4); font-size: .65rem; }
    .btn-logout {
      background: none; border: 1px solid rgba(255,255,255,.12); border-radius: 7px;
      color: rgba(255,255,255,.5); width: 30px; height: 30px;
      display: flex; align-items: center; justify-content: center;
      cursor: pointer; flex-shrink: 0; transition: all .15s;
    }
    .btn-logout:hover { border-color: #ef4444; color: #ef4444; }

    /* ─── Main area ─── */
    .main-content { flex: 1; display: flex; flex-direction: column; overflow: hidden; min-width: 0; }

    .topbar {
      height: 56px; flex-shrink: 0;
      background: #fff; border-bottom: 1.5px solid #e8f0fe;
      display: flex; align-items: center; gap: .75rem;
      padding: 0 1.25rem;
      box-shadow: 0 1px 4px rgba(0,0,0,.05);
    }
    .hamburger {
      display: none; background: none; border: none;
      font-size: 1.35rem; color: #0f2027; cursor: pointer; padding: .25rem;
    }
    @media (max-width: 768px) { .hamburger { display: flex; } }
    .topbar-title { font-family: var(--fs-font-head, 'Sora', sans-serif); font-size: 1rem; font-weight: 700; color: #0f2027; }
    .topbar-role-chip {
      display: flex; align-items: center; gap: .4rem;
      padding: .3rem .75rem; border-radius: 20px;
      background: #f0fdfa; color: #0d9488;
      font-size: .72rem; font-weight: 700; border: 1.5px solid #ccfbf1;
    }

    /* ─── Notification bell ─── */
    .notif-bell {
      position: relative; background: none; border: none;
      width: 36px; height: 36px; border-radius: 50%;
      display: flex; align-items: center; justify-content: center;
      font-size: 1.1rem; color: #4b7a76; cursor: pointer;
      transition: background .15s;
    }
    .notif-bell:hover { background: #f0fdfa; }
    .notif-badge {
      position: absolute; top: 1px; right: 1px;
      min-width: 17px; height: 17px; border-radius: 9px;
      background: #ef4444; color: #fff; font-size: .58rem;
      font-weight: 700; display: flex; align-items: center; justify-content: center;
      padding: 0 3px; border: 2px solid #fff;
    }
    .notif-backdrop { position: fixed; inset: 0; z-index: 800; }
    .notif-panel {
      position: absolute; top: calc(100% + 8px); right: 0;
      width: 360px; max-height: 480px;
      background: #fff; border: 1.5px solid #e2f0ef; border-radius: 14px;
      box-shadow: 0 12px 40px rgba(0,0,0,.14); z-index: 900;
      display: flex; flex-direction: column; overflow: hidden;
    }
    .notif-panel-header {
      display: flex; align-items: center; justify-content: space-between;
      padding: .875rem 1rem; border-bottom: 1px solid #f0fdfa;
      background: linear-gradient(135deg, #f0fdfa, #ecfeff);
    }
    .notif-panel-title { font-weight: 700; font-size: .88rem; color: #0f2027; }
    .notif-mark-all { font-size: .72rem; color: #0d9488; background: none; border: none; cursor: pointer; font-weight: 600; }
    .notif-panel-body { overflow-y: auto; flex: 1; }
    .notif-empty { display: flex; flex-direction: column; align-items: center; justify-content: center; gap: .5rem; padding: 2.5rem 1rem; color: #9ca3af; text-align: center; font-size: .82rem; }
    .notif-item {
      display: flex; gap: .75rem; padding: .75rem 1rem;
      border-bottom: 1px solid #f8fafc; cursor: pointer; transition: background .12s;
    }
    .notif-item:hover { background: #fafafa; }
    .notif-item.unread { background: #f0fdfa; }
    .notif-item-icon {
      width: 34px; height: 34px; border-radius: 9px; flex-shrink: 0;
      display: flex; align-items: center; justify-content: center; font-size: .9rem;
    }
    .notif-cat-onboarding   { background: #d1fae5; color: #065f46; }
    .notif-cat-underwriting { background: #e0e7ff; color: #3730a3; }
    .notif-cat-offer        { background: #fef3c7; color: #92400e; }
    .notif-cat-disbursement { background: #ccfbf1; color: #0f766e; }
    .notif-cat-servicing    { background: #dbeafe; color: #1e40af; }
    .notif-cat-collections  { background: #fee2e2; color: #991b1b; }
    .notif-cat-default      { background: #f1f5f9; color: #475569; }
    .notif-item-body { flex: 1; min-width: 0; }
    .notif-item-title { font-size: .8rem; font-weight: 600; color: #0f2027; }
    .notif-item-msg   { font-size: .72rem; color: #6b7280; margin-top: 2px; line-height: 1.4; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
    .notif-item-time  { font-size: .65rem; color: #9ca3af; margin-top: 3px; }
    .notif-dot { width: 7px; height: 7px; border-radius: 50%; background: #0d9488; flex-shrink: 0; margin-top: 4px; }

    /* ─── Page content ─── */
    .page { flex: 1; overflow-y: auto; padding: 1.5rem; }
    @media (max-width: 600px) { .page { padding: 1rem; } }

    /* ─── Toast ─── */
    .toast-container {
      position: fixed; top: 1.25rem; right: 1.25rem; z-index: 9999;
      display: flex; flex-direction: column; gap: .625rem;
      max-width: 360px; pointer-events: none;
    }
    .toast-item {
      pointer-events: all; display: flex; align-items: flex-start; gap: .75rem;
      padding: .875rem 1rem; border-radius: 12px;
      box-shadow: 0 8px 24px rgba(0,0,0,.14), 0 2px 6px rgba(0,0,0,.08);
      background: #fff; border-left: 4px solid transparent;
      animation: toast-in .25s ease; min-width: 280px;
    }
    @keyframes toast-in { from { transform: translateX(110%); opacity: 0; } to { transform: none; opacity: 1; } }
    .toast-success { border-left-color: #10b981; } .toast-error { border-left-color: #ef4444; }
    .toast-warning { border-left-color: #f59e0b; } .toast-info  { border-left-color: #3b82f6; }
    .toast-icon { font-size: 1.15rem; flex-shrink: 0; margin-top: .05rem; }
    .toast-success .toast-icon { color: #10b981; } .toast-error   .toast-icon { color: #ef4444; }
    .toast-warning .toast-icon { color: #f59e0b; } .toast-info    .toast-icon { color: #3b82f6; }
    .toast-body { flex: 1; min-width: 0; }
    .toast-title { font-weight: 700; font-size: .85rem; color: #0f2027; line-height: 1.3; }
    .toast-msg   { font-size: .78rem; color: #4b5563; margin-top: .2rem; line-height: 1.4; white-space: pre-line; }
    .toast-close {
      background: none; border: none; cursor: pointer; padding: .1rem .25rem;
      border-radius: 4px; color: #9ca3af; font-size: 1rem; line-height: 1; flex-shrink: 0;
      transition: color .15s, background .15s;
    }
    .toast-close:hover { color: #0f2027; background: #f3f4f6; }
  `],
  template: `
    <!-- Landing page (unauthenticated) -->
    @if (!auth.isLoggedIn()) {
      <router-outlet />
    }

    <!-- Authenticated shell -->
    @if (auth.isLoggedIn()) {
      <div class="sb-backdrop" [class.show]="open()" (click)="close()"></div>

      <div class="app-shell">
        <!-- ═══ Sidebar ═══ -->
        <aside class="sidebar" [class.open]="open()">
          <div class="sb-brand">
            <div class="sb-logo"><i class="bi bi-bank2"></i></div>
            <div>
              <div class="sb-name">FinServe</div>
              <div class="sb-sub">Lending Platform</div>
            </div>
          </div>

          <nav class="sb-nav">
            @for (group of groups(); track group.label) {
              <div class="sb-section">{{ group.label }}</div>
              @for (item of group.items; track item.route) {
                <a class="sb-link" [routerLink]="item.route"
                   routerLinkActive="active" (click)="close()">
                  <i class="bi {{ item.icon }}"></i>
                  <span>{{ item.label }}</span>
                  @if (item.route !== primaryRoute() && !item.isPrimary) {
                    <span class="sb-readonly-badge"><i class="bi bi-eye-fill"></i></span>
                  }
                </a>
              }
            }
          </nav>

          <div class="sb-footer">
            <div class="sb-avatar">{{ init() }}</div>
            <div style="flex:1;min-width:0;overflow:hidden;">
              <div class="sb-user-name">{{ auth.userEmail() }}</div>
              <div class="sb-user-role">{{ auth.userRole() }}</div>
            </div>
            <button class="btn-logout" (click)="logout()" title="Logout">
              <i class="bi bi-box-arrow-right"></i>
            </button>
          </div>
        </aside>

        <!-- ═══ Main content ═══ -->
        <div class="main-content">
          <header class="topbar">
            <button class="hamburger" (click)="toggle()"><i class="bi bi-list"></i></button>
            <span class="topbar-title">FinServe Platform</span>
            <div style="flex:1;"></div>

            <!-- Notification Bell -->
            <div style="position:relative;">
              <button class="notif-bell" (click)="notif.togglePanel()" title="Notifications">
                <i class="bi bi-bell-fill"></i>
                @if (notif.unreadCount() > 0) {
                  <span class="notif-badge">{{ notif.unreadCount() > 99 ? '99+' : notif.unreadCount() }}</span>
                }
              </button>

              @if (notif.panelOpen()) {
                <div class="notif-backdrop" (click)="notif.closePanel()"></div>
                <div class="notif-panel">
                  <div class="notif-panel-header">
                    <span class="notif-panel-title"><i class="bi bi-bell me-1"></i>Notifications</span>
                    @if (notif.unreadCount() > 0) {
                      <button class="notif-mark-all" (click)="notif.markAllRead()">Mark all read</button>
                    }
                  </div>
                  <div class="notif-panel-body">
                    @if (notif.notifications().length === 0) {
                      <div class="notif-empty">
                        <i class="bi bi-bell-slash" style="font-size:2rem;opacity:.3;"></i>
                        <p>No notifications yet</p>
                      </div>
                    }
                    @for (n of notif.notifications(); track n.notificationId) {
                      <div class="notif-item" [class.unread]="!n.isRead"
                           (click)="notif.markOneRead(n.notificationId)">
                        <div class="notif-item-icon" [class]="'notif-cat-' + (n.category ?? 'default').toLowerCase()">
                          @switch (n.category) {
                            @case ('ONBOARDING')   { <i class="bi bi-person-check-fill"></i> }
                            @case ('UNDERWRITING') { <i class="bi bi-clipboard2-check-fill"></i> }
                            @case ('OFFER')        { <i class="bi bi-tags-fill"></i> }
                            @case ('DISBURSEMENT') { <i class="bi bi-bank2"></i> }
                            @case ('SERVICING')    { <i class="bi bi-calendar-check-fill"></i> }
                            @case ('COLLECTIONS')  { <i class="bi bi-exclamation-triangle-fill"></i> }
                            @default              { <i class="bi bi-info-circle-fill"></i> }
                          }
                        </div>
                        <div class="notif-item-body">
                          <div class="notif-item-title">{{ n.title }}</div>
                          <div class="notif-item-msg">{{ n.message }}</div>
                          <div class="notif-item-time">{{ n.createdAt | date:'dd MMM · HH:mm' }}</div>
                        </div>
                        @if (!n.isRead) { <div class="notif-dot"></div> }
                      </div>
                    }
                  </div>
                </div>
              }
            </div>

            <div class="topbar-role-chip">
              <i class="bi bi-person-circle"></i>
              <span>{{ auth.userRole() }}</span>
            </div>
          </header>

          <main class="page">
            <router-outlet />
          </main>
        </div>
      </div>

      <!-- ═══ Toast ═══ -->
      <div class="toast-container">
        @for (t of toast.toasts(); track t.id) {
          <div class="toast-item toast-{{ t.type }}">
            <div class="toast-icon">
              <i class="bi"
                 [class.bi-check-circle-fill]="t.type==='success'"
                 [class.bi-x-circle-fill]="t.type==='error'"
                 [class.bi-exclamation-triangle-fill]="t.type==='warning'"
                 [class.bi-info-circle-fill]="t.type==='info'"></i>
            </div>
            <div class="toast-body">
              <div class="toast-title">{{ t.title }}</div>
              @if (t.message) { <div class="toast-msg">{{ t.message }}</div> }
            </div>
            <button class="toast-close" (click)="toast.dismiss(t.id)"><i class="bi bi-x"></i></button>
          </div>
        }
      </div>
    }
  `
})
export class AppComponent {
  readonly auth  = inject(AuthService);
  readonly toast = inject(ToastService);
  readonly notif = inject(NotificationService);

  open = signal(false);

  init         = computed(() => (this.auth.userEmail() ?? '?').charAt(0).toUpperCase());
  primaryRoute = computed(() => roleRoute(this.auth.userRole()!));

  private NAV: NavGroup[] = [
    {
      label: 'My Workspace',
      items: [
        { icon: 'bi-person-workspace',  label: 'Applicant Portal', route: '/applicant',    roles: ['APPLICANT'],   isPrimary: true },
        { icon: 'bi-person-badge',      label: 'Agent Workspace',  route: '/agent',        roles: ['AGENT'],       isPrimary: true },
        { icon: 'bi-shield-lock',       label: 'Admin Panel',      route: '/admin',        roles: ['ADMIN'],       isPrimary: true },
        { icon: 'bi-clipboard2-check',  label: 'Underwriting',     route: '/underwriting', roles: ['UNDERWRITER'], isPrimary: true },
        { icon: 'bi-send-check',        label: 'Operations',       route: '/operations',   roles: ['OPERATIONS'],  isPrimary: true },
        { icon: 'bi-calendar3',         label: 'Loan Servicing',   route: '/servicing',    roles: ['SERVICING'],   isPrimary: true },
        { icon: 'bi-cash-coin',         label: 'Repayments',       route: '/repayments',   roles: ['SERVICING'],   isPrimary: true },
        { icon: 'bi-collection',        label: 'Collections',      route: '/collections',  roles: ['COLLECTIONS'], isPrimary: true },
        { icon: 'bi-graph-up-arrow',    label: 'Risk Dashboard',   route: '/risk',         roles: ['RISK'],        isPrimary: true },
        { icon: 'bi-shield-check',      label: 'Compliance',       route: '/compliance',   roles: ['COMPLIANCE'],  isPrimary: true },
      ]
    },
    {
      label: 'System Monitor',
      items: [
        { icon: 'bi-person-workspace',  label: 'Applicant View',   route: '/applicant',    roles: ['ADMIN'] },
        { icon: 'bi-person-badge',      label: 'Agent View',       route: '/agent',        roles: ['ADMIN'] },
        { icon: 'bi-clipboard2-check',  label: 'Underwriting',     route: '/underwriting', roles: ['ADMIN'] },
        { icon: 'bi-send-check',        label: 'Operations',       route: '/operations',   roles: ['ADMIN'] },
        { icon: 'bi-calendar3',         label: 'Servicing',        route: '/servicing',    roles: ['ADMIN'] },
        { icon: 'bi-collection',        label: 'Collections',      route: '/collections',  roles: ['ADMIN'] },
        { icon: 'bi-graph-up-arrow',    label: 'Risk Analytics',   route: '/risk',         roles: ['ADMIN'] },
        { icon: 'bi-shield-check',      label: 'Compliance Log',   route: '/compliance',   roles: ['ADMIN'] },
      ]
    }
  ];

  groups = computed(() => {
    const role = this.auth.userRole();
    return this.NAV
      .map(g => ({ ...g, items: g.items.filter(i => role && i.roles.includes(role)) }))
      .filter(g => g.items.length > 0);
  });

  constructor() {
    if (this.auth.isLoggedIn()) { this.notif.init(); }
  }

  toggle() { this.open.update(v => !v); }
  close()  { this.open.set(false); }
  logout() { this.notif.destroy(); this.auth.logout(); }
}
