import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from '../services/auth.service';
import { RoleName } from '../models';

export const authGuard: CanActivateFn = () => {
  const auth   = inject(AuthService);
  const router = inject(Router);
  if (auth.isLoggedIn()) return true;
  router.navigate(['/']);
  return false;
};

/** Redirect logged-in users away from the landing page to their role dashboard */
export const guestGuard: CanActivateFn = () => {
  const auth   = inject(AuthService);
  const router = inject(Router);
  if (!auth.isLoggedIn()) return true;
  router.navigate([roleRoute(auth.userRole()!)]);
  return false;
};

export function roleGuard(...roles: RoleName[]): CanActivateFn {
  return () => {
    const auth   = inject(AuthService);
    const router = inject(Router);
    if (!auth.isLoggedIn()) { router.navigate(['/']); return false; }
    if (roles.length === 0 || auth.hasAnyRole(...roles)) return true;
    // Redirect to own role dashboard instead of /dashboard
    router.navigate([roleRoute(auth.userRole()!)]);
    return false;
  };
}

export function roleRoute(role: RoleName): string {
  const map: Record<RoleName, string> = {
    ADMIN:       '/admin',
    APPLICANT:   '/applicant',
    AGENT:       '/agent',
    UNDERWRITER: '/underwriting',
    OPERATIONS:  '/operations',
    SERVICING:   '/servicing',
    COLLECTIONS: '/collections',
    RISK:        '/risk',
    COMPLIANCE:  '/compliance',
  };
  return map[role] ?? '/applicant';
}
