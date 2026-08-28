import { Component, OnInit, ChangeDetectorRef, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { finalize } from 'rxjs';

import { ReplanService } from '../../services/replan';
import { ReplanHistoryItem } from '../../models/replan.model';

@Component({
  selector: 'app-replan-history',
  imports: [CommonModule, FormsModule],
  templateUrl: './replan-history.html',
  styleUrl: './replan-history.css'
})
export class ReplanHistoryComponent implements OnInit {
  private readonly replanService = inject(ReplanService);
  private readonly cdr = inject(ChangeDetectorRef);

  historyItems: ReplanHistoryItem[] = [];
  filteredItems: ReplanHistoryItem[] = [];
  loading = false;
  error = false;

  selectedType = '';
  selectedDate = '';

  disruptionTypes = [
    { label: 'All Disruptions', value: '' },
    { label: '🚪 Room Unavailable', value: 'ROOM_UNAVAILABLE' },
    { label: '🏢 Company Delay', value: 'COMPANY_DELAY' },
    { label: '👤 Student Withdraw', value: 'STUDENT_WITHDRAW' },
    { label: '👥 Panel Dropout', value: 'PANEL_DROPOUT' }
  ];

  ngOnInit(): void {
    this.loadHistory();
  }

  loadHistory(): void {
    this.loading = true;
    this.error = false;
    this.cdr.detectChanges();

    this.replanService.getReplanHistory().pipe(
      finalize(() => {
        this.loading = false;
        this.cdr.detectChanges();
      })
    ).subscribe({
      next: (data) => {
        this.historyItems = data;
        this.applyFilters();
        this.cdr.detectChanges();
      },
      error: (err) => {
        console.error('Failed to load replan history', err);
        this.error = true;
        this.cdr.detectChanges();
      }
    });
  }

  applyFilters(): void {
    let result = [...this.historyItems];

    if (this.selectedType) {
      result = result.filter(item => item.disruptionType === this.selectedType);
    }
    if (this.selectedDate) {
      result = result.filter(item => item.oldDate === this.selectedDate || item.newDate === this.selectedDate);
    }

    this.filteredItems = result;
    this.cdr.detectChanges();
  }

  refresh(): void {
    this.loadHistory();
  }

  getMovedCount(): number {
    return this.filteredItems.filter(i => i.moved).length;
  }

  getCancelledCount(): number {
    return this.filteredItems.filter(i => i.cancelled).length;
  }

  getDisruptionIcon(type: string): string {
    switch (type) {
      case 'ROOM_UNAVAILABLE': return '🚪';
      case 'COMPANY_DELAY': return '🏢';
      case 'STUDENT_WITHDRAW': return '👤';
      case 'PANEL_DROPOUT': return '👥';
      default: return '⚡';
    }
  }

  formatDisruptionName(type: string): string {
    switch (type) {
      case 'ROOM_UNAVAILABLE': return 'Room Unavailable';
      case 'COMPANY_DELAY': return 'Company Delay';
      case 'STUDENT_WITHDRAW': return 'Student Withdrawal';
      case 'PANEL_DROPOUT': return 'Panel Dropout';
      default: return type;
    }
  }
}
