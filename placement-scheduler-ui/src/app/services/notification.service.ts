import { Injectable } from '@angular/core';
import { BehaviorSubject, Observable } from 'rxjs';

export interface ToastMessage {
  id: string;
  type: 'success' | 'error' | 'warning' | 'info';
  title: string;
  message: string;
  duration?: number;
}

@Injectable({
  providedIn: 'root'
})
export class NotificationService {
  private readonly toasts$ = new BehaviorSubject<ToastMessage[]>([]);

  getToasts(): Observable<ToastMessage[]> {
    return this.toasts$.asObservable();
  }

  show(type: 'success' | 'error' | 'warning' | 'info', title: string, message: string, duration: number = 6000): void {
    const id = Math.random().toString(36).substring(2, 9);
    const toast: ToastMessage = { id, type, title, message, duration };
    
    const current = this.toasts$.getValue();
    this.toasts$.next([...current, toast]);

    if (duration > 0) {
      setTimeout(() => {
        this.dismiss(id);
      }, duration);
    }
  }

  showError(title: string, message: string): void {
    this.show('error', title, message, 8000);
  }

  showSuccess(title: string, message: string): void {
    this.show('success', title, message, 4000);
  }

  showWarning(title: string, message: string): void {
    this.show('warning', title, message, 6000);
  }

  showInfo(title: string, message: string): void {
    this.show('info', title, message, 4000);
  }

  dismiss(id: string): void {
    const current = this.toasts$.getValue().filter(t => t.id !== id);
    this.toasts$.next(current);
  }

  clearAll(): void {
    this.toasts$.next([]);
  }
}
