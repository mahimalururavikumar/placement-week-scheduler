import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import {
  RouterLink,
  RouterLinkActive,
  RouterOutlet
} from '@angular/router';
import { NotificationService, ToastMessage } from './services/notification.service';
import { Observable } from 'rxjs';

@Component({
  selector: 'app-root',
  imports: [
    CommonModule,
    RouterOutlet,
    RouterLink,
    RouterLinkActive
  ],
  templateUrl: './app.html',
  styleUrl: './app.css'
})
export class App {
  title = 'Placement Week Scheduler';
  isSidebarOpen = false;

  private readonly notificationService = inject(NotificationService);
  toasts$: Observable<ToastMessage[]> = this.notificationService.getToasts();

  toggleSidebar(): void {
    this.isSidebarOpen = !this.isSidebarOpen;
  }

  closeSidebar(): void {
    this.isSidebarOpen = false;
  }

  dismissToast(id: string): void {
    this.notificationService.dismiss(id);
  }
}