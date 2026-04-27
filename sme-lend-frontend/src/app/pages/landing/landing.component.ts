import { Component, signal, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { AuthService } from '../../core/services/auth.service';
import { ToastService } from '../../core/services/toast.service';
import { BizType } from '../../core/models';

@Component({
  selector: 'app-landing',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './landing.component.html'
})
export class LandingComponent {
  private fb   = inject(FormBuilder);
  private auth = inject(AuthService);
  private toast = inject(ToastService);

  mode     = signal<'login' | 'register'>('login');
  loading  = signal(false);
  showPwd  = signal(false);
  showPwd2 = signal(false);
  error    = signal('');

  bizTypes: BizType[] = ['PROPRIETORSHIP','PARTNERSHIP','PVT_LTD','LLP','OTHER'];

  loginForm = this.fb.group({
    email:    ['', [Validators.required, Validators.email]],
    password: ['', Validators.required]
  });
  get lf() { return this.loginForm.controls; }

  regForm = this.fb.group({
    fullName:  ['', Validators.required],
    email:     ['', [Validators.required, Validators.email]],
    password:  ['', [Validators.required, Validators.minLength(8)]],
    phone:     ['', [Validators.required, Validators.pattern(/^[0-9]{10}$/)]],
    bankAccountNo: ['', [
      Validators.required,
      Validators.pattern(/^[0-9]{9,18}$/)
    ]],
    ifsc: ['', [
      Validators.required,
      Validators.pattern(/^[A-Z]{4}0[A-Z0-9]{6}$/)
    ]]
  });
  get rf() { return this.regForm.controls; }

  togglePwd() { this.showPwd.update(v => !v); }

  switchMode(m: 'login' | 'register') {
    this.mode.set(m);
    this.error.set('');
    this.loginForm.reset();
    this.regForm.reset();
  }

  login() {
    this.loginForm.markAllAsTouched();
    if (this.loginForm.invalid) return;
    this.loading.set(true); this.error.set('');
    const v = this.loginForm.value;
    this.auth.login({ email: v.email!, password: v.password! }).subscribe({
      next: r => {
        this.loading.set(false);
        if (!r.success) this.error.set(r.message ?? 'Login failed');
      },
      error: e => {
        this.loading.set(false);
        this.error.set(e?.error?.message ?? 'Invalid credentials. Please try again.');
      }
    });
  }

  register() {
    this.regForm.markAllAsTouched();
    if (this.regForm.invalid) return;
    this.loading.set(true); this.error.set('');
    const v = this.regForm.value;
    this.auth.register({
      fullName: v.fullName!, email: v.email!, password: v.password!,
      phone: v.phone || undefined, role: 'APPLICANT',
      bankAccountNo: v.bankAccountNo || '', ifsc: v.ifsc || ''
    }).subscribe({
      next: r => {
        this.loading.set(false);
        if (!r.success) this.error.set(r.message ?? 'Registration failed');
        else this.toast.success('Account created!', 'Welcome to FinServe');
      },
      error: e => {
        this.loading.set(false);
        this.error.set(e?.error?.message ?? 'Registration failed. Please try again.');
      }
    });
  }
}
