import { Routes } from '@angular/router';
import { authGuard, guestGuard, roleGuard } from './core/guards/auth.guard';

/**
 * FinServe Route Configuration
 * Shell (sidebar + topbar + toast) lives in AppComponent.
 * Routes render directly into its <router-outlet />.
 */
export const routes: Routes = [

  // ── Landing (unauthenticated) ──────────────────────────────────
  {
    path: '',
    canActivate: [guestGuard],
    loadComponent: () =>
      import('./pages/landing/landing.component').then(m => m.LandingComponent)
  },

  // ── ADMIN ────────────────────────────────────────────────────
  { path: 'admin',        canActivate: [authGuard, roleGuard('ADMIN')],
    loadComponent: () => import('./pages/admin/admin.component').then(m => m.AdminComponent) },

  // ── APPLICANT ─────────────────────────────────────────────────
  { path: 'applicant',    canActivate: [authGuard, roleGuard('APPLICANT','AGENT','ADMIN')],
    loadComponent: () => import('./pages/applicant/applicant.component').then(m => m.ApplicantComponent) },

  // ── AGENT ────────────────────────────────────────────────────
  { path: 'agent',        canActivate: [authGuard, roleGuard('AGENT','ADMIN')],
    loadComponent: () => import('./pages/agent/agent.component').then(m => m.AgentComponent) },

  // ── UNDERWRITING ──────────────────────────────────────────────
  { path: 'underwriting', canActivate: [authGuard, roleGuard('UNDERWRITER','ADMIN')],
    loadComponent: () => import('./pages/underwriting/underwriting.component').then(m => m.UnderwritingComponent) },

  // ── OPERATIONS ────────────────────────────────────────────────
  { path: 'operations',   canActivate: [authGuard, roleGuard('OPERATIONS','ADMIN')],
    loadComponent: () => import('./pages/operations/operations.component').then(m => m.OperationsComponent) },

  // ── SERVICING ─────────────────────────────────────────────────
  { path: 'servicing',    canActivate: [authGuard, roleGuard('SERVICING','ADMIN')],
    loadComponent: () => import('./pages/servicing/servicing.component').then(m => m.ServicingComponent) },

  // ── REPAYMENTS ────────────────────────────────────────────────
  { path: 'repayments',   canActivate: [authGuard, roleGuard('SERVICING','OPERATIONS','ADMIN')],
    loadComponent: () => import('./pages/repayments/repayments.component').then(m => m.RepaymentsComponent) },

  // ── COLLECTIONS ───────────────────────────────────────────────
  { path: 'collections',  canActivate: [authGuard, roleGuard('COLLECTIONS','ADMIN')],
    loadComponent: () => import('./pages/collections/collections.component').then(m => m.CollectionsComponent) },

  // ── RISK ──────────────────────────────────────────────────────
  { path: 'risk',         canActivate: [authGuard, roleGuard('RISK','ADMIN')],
    loadComponent: () => import('./pages/risk/risk.component').then(m => m.RiskComponent) },

  // ── COMPLIANCE ────────────────────────────────────────────────
  { path: 'compliance',   canActivate: [authGuard, roleGuard('COMPLIANCE','ADMIN')],
    loadComponent: () => import('./pages/compliance/compliance.component').then(m => m.ComplianceComponent) },

  // ── Catch-all ─────────────────────────────────────────────────
  { path: '**', redirectTo: '' }
];
