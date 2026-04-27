import { Component, OnInit, OnDestroy, AfterViewInit, signal, inject, ElementRef, ViewChild } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ApiService } from '../../core/services/api.service';
import { AuthService } from '../../core/services/auth.service';
import { ToastService } from '../../core/services/toast.service';
import { PortfolioMetrics } from '../../core/models';

declare const Chart: any;

@Component({
  selector: 'app-risk',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './risk.component.html'
})
export class RiskComponent implements OnInit, AfterViewInit, OnDestroy {
  private api   = inject(ApiService);
  readonly auth = inject(AuthService);
  private toast = inject(ToastService);

  isReadOnly = signal(!this.auth.hasAnyRole('RISK'));

  metrics   = signal<PortfolioMetrics | null>(null);
  loading   = signal(true);
  chartReady = signal(false);

  @ViewChild('doughnutCanvas') doughnutRef!: ElementRef<HTMLCanvasElement>;
  @ViewChild('barCanvas')      barRef!: ElementRef<HTMLCanvasElement>;

  private doughnutChart: any = null;
  private barChart: any = null;

  ngOnInit() { this.loadMetrics(); }

  ngAfterViewInit() { this.chartReady.set(true); }

  ngOnDestroy() {
    this.doughnutChart?.destroy();
    this.barChart?.destroy();
  }

  loadMetrics() {
    this.loading.set(true);
    this.api.getPortfolioMetrics().subscribe({
      next: r => {
        this.metrics.set(r.data);
        this.loading.set(false);
        setTimeout(() => this.buildCharts(), 120);
      },
      error: e => {
        this.loading.set(false);
        this.toast.error('Failed to load metrics', e?.error?.message);
      }
    });
  }

  private buildCharts() {
    const m = this.metrics();
    if (!m) return;

    // ── Doughnut: Account Distribution ──────────────────────────
    if (this.doughnutRef?.nativeElement) {
      this.doughnutChart?.destroy();
      this.doughnutChart = new Chart(this.doughnutRef.nativeElement, {
        type: 'doughnut',
        data: {
          labels: ['Active', 'Delinquent', 'Closed'],
          datasets: [{
            data: [
              m.activeLoanAccounts,
              m.delinquentLoanAccounts,
              m.totalLoanAccounts - m.activeLoanAccounts - m.delinquentLoanAccounts
            ],
            backgroundColor: ['#10b981', '#ef4444', '#94a3b8'],
            borderWidth: 0,
            hoverOffset: 6
          }]
        },
        options: {
          responsive: true, maintainAspectRatio: false, cutout: '72%',
          plugins: {
            legend: { position: 'bottom', labels: { usePointStyle: true, padding: 16, font: { family: 'DM Sans', size: 12 } } },
            tooltip: { callbacks: { label: (ctx: any) => ` ${ctx.label}: ${ctx.raw} accounts` } }
          }
        }
      });
    }

    // ── Bar: Bucket DPD Distribution ────────────────────────────
    if (this.barRef?.nativeElement && m.bucketCounts) {
      this.barChart?.destroy();
      const entries = Object.entries(m.bucketCounts);
      const COLORS: Record<string, string> = {
        'CURRENT': '#10b981', 'X': '#10b981',
        '1-30':  '#f59e0b', '31-60': '#f97316', '61-90': '#ef4444', '90+': '#7c3aed'
      };
      this.barChart = new Chart(this.barRef.nativeElement, {
        type: 'bar',
        data: {
          labels: entries.map(([k]) => k),
          datasets: [{
            label: 'Loan Accounts',
            data: entries.map(([, v]) => v),
            backgroundColor: entries.map(([k]) => COLORS[k] ?? '#94a3b8'),
            borderRadius: 6,
            borderSkipped: false
          }]
        },
        options: {
          responsive: true, maintainAspectRatio: false,
          plugins: {
            legend: { display: false },
            tooltip: { callbacks: { label: (ctx: any) => ` ${ctx.raw} accounts in DPD ${ctx.label}` } }
          },
          scales: {
            x: { grid: { display: false }, ticks: { font: { family: 'DM Sans' } } },
            y: { beginAtZero: true, grid: { color: '#f1f5f9' }, ticks: { font: { family: 'DM Sans' }, precision: 0 } }
          }
        }
      });
    }
  }

  delinquencyRate(): string {
    const m = this.metrics();
    if (!m || m.totalLoanAccounts === 0) return '0.00';
    return ((m.delinquentLoanAccounts / m.totalLoanAccounts) * 100).toFixed(2);
  }

  healthScore(): number {
    const m = this.metrics();
    if (!m || m.totalLoanAccounts === 0) return 100;
    const rate = (m.delinquentLoanAccounts / m.totalLoanAccounts) * 100;
    return Math.max(0, Math.round(100 - rate * 3));
  }

  healthColor(): string {
    const s = this.healthScore();
    if (s >= 80) return '#10b981';
    if (s >= 60) return '#f59e0b';
    return '#ef4444';
  }

  bucketEntries(): { bucket: string; count: number; pct: number }[] {
    const m = this.metrics();
    if (!m?.bucketCounts) return [];
    const total = m.totalLoanAccounts || 1;
    return Object.entries(m.bucketCounts).map(([bucket, count]) => ({
      bucket, count, pct: Math.round((count / total) * 100)
    }));
  }

  bucketColor(b: string): string {
    const u = b?.toUpperCase();
    if (u === 'CURRENT' || u === 'X') return '#10b981';
    if (u?.includes('1-30'))  return '#f59e0b';
    if (u?.includes('31-60')) return '#f97316';
    return '#ef4444';
  }
}
