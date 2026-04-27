import { Injectable, signal, computed } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';
import { Observable, tap } from 'rxjs';
import { environment } from '../../../environments/environment';
import { ApiResponse, AuthState, LoginRequest, LoginResponse, RegisterRequest, RegisterResponse, RoleName } from '../models';
import { roleRoute } from '../guards/auth.guard';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly TK = 'fs_token';
  private readonly UK = 'fs_user';

  private _state = signal<AuthState>(this.loadState());

  readonly state      = computed(() => this._state());
  readonly isLoggedIn = computed(() => !!this._state().token);
  readonly userRole   = computed(() => this._state().role);
  readonly userEmail  = computed(() => this._state().email);
  readonly userId     = computed(() => this._state().userId);

  constructor(private http: HttpClient, private router: Router) {}

  login(body: LoginRequest): Observable<ApiResponse<LoginResponse>> {
    return this.http.post<ApiResponse<LoginResponse>>(`${environment.apiUrl}/auth/login`, body)
      .pipe(tap(r => {
        if (r.success && r.data) {
          this.persist(r.data);
          // Redirect immediately to role-specific dashboard
          this.router.navigate([roleRoute(r.data.role)]);
        }
      }));
  }

  register(body: RegisterRequest): Observable<ApiResponse<RegisterResponse>> {
    return this.http.post<ApiResponse<RegisterResponse>>(`${environment.apiUrl}/auth/register`, body)
      .pipe(tap(r => {
        if (r.success && r.data) {
          this.persist(r.data);
          this.router.navigate([roleRoute(r.data.role)]);
        }
      }));
  }

  logout(): void {
    localStorage.removeItem(this.TK);
    localStorage.removeItem(this.UK);
    this._state.set({ token: null, userId: null, email: null, role: null });
    this.router.navigate(['/']);
  }

  getToken(): string | null { return this._state().token; }
  hasRole(r: RoleName): boolean { return this._state().role === r; }
  hasAnyRole(...rs: RoleName[]): boolean { return rs.includes(this._state().role as RoleName); }

  private persist(d: LoginResponse | RegisterResponse): void {
    const st: AuthState = { token: d.token, userId: d.userId, email: d.email, role: d.role };
    localStorage.setItem(this.TK, d.token);
    localStorage.setItem(this.UK, JSON.stringify({ userId: d.userId, email: d.email, role: d.role }));
    this._state.set(st);
  }

  private loadState(): AuthState {
    const t = localStorage.getItem(this.TK);
    const u = localStorage.getItem(this.UK);
    if (t && u) {
      try { const p = JSON.parse(u); return { token: t, ...p }; } catch { /* ignore */ }
    }
    return { token: null, userId: null, email: null, role: null };
  }
}
