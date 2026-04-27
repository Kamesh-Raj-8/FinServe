import { Injectable, signal } from '@angular/core';

export type ToastType = 'success' | 'error' | 'warning' | 'info';

export interface Toast {
  id: number;
  type: ToastType;
  title: string;
  message?: string;
  duration: number;
}

@Injectable({ providedIn: 'root' })
export class ToastService {
  private _counter = 0;
  readonly toasts = signal<Toast[]>([]);

  show(type: ToastType, title: string, message?: string, duration = 4000) {
    const id = ++this._counter;
    const toast: Toast = { id, type, title, message, duration };
    this.toasts.update(t => [...t, toast]);
    setTimeout(() => this.dismiss(id), duration);
    return id;
  }

  success(title: string, msg?: string) { return this.show('success', title, msg); }
  error(title: string, msg?: string)   { return this.show('error', title, msg, 6000); }
  warning(title: string, msg?: string) { return this.show('warning', title, msg); }
  info(title: string, msg?: string)    { return this.show('info', title, msg); }

  dismiss(id: number) {
    this.toasts.update(t => t.filter(x => x.id !== id));
  }
}
