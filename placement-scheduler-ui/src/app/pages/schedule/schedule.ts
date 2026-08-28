import { Component, OnInit, ChangeDetectorRef, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { finalize, timeout } from 'rxjs';

import { ScheduleService, ScheduleGenerationResult } from '../../services/schedule';
import { ScheduleItem, OptionItem } from '../../models/schedule.model';

export interface TimeSlotGroup {
  time: string;
  items: ScheduleItem[];
}

@Component({
  selector: 'app-schedule',
  imports: [CommonModule, FormsModule],
  templateUrl: './schedule.html',
  styleUrl: './schedule.css'
})
export class ScheduleComponent implements OnInit {
  private readonly scheduleService = inject(ScheduleService);
  private readonly cdr = inject(ChangeDetectorRef);

  allSchedules: ScheduleItem[] = [];
  filteredSchedules: ScheduleItem[] = [];
  timeGroups: TimeSlotGroup[] = [];

  loading = false;
  generating = false;
  error = false;
  generationMessage = '';

  // Available Filter Options
  availableDates: string[] = ['2026-08-24', '2026-08-25', '2026-08-26', '2026-08-27'];
  availableCompanies: OptionItem[] = [];
  availableRooms: OptionItem[] = [];
  availablePanels: OptionItem[] = [];
  availableTiers: string[] = ['TIER_1', 'TIER_2', 'TIER_3'];

  // Active Filters
  selectedDate = '';
  selectedCompanyId: number | null = null;
  selectedTier = '';
  selectedRoomId: number | null = null;
  selectedPanelId: number | null = null;
  selectedStatus = '';

  viewMode: 'grid' | 'timeline' = 'timeline';

  ngOnInit(): void {
    this.loadSchedule(true);
  }

  loadSchedule(showLoading = true): void {
    if (this.generating) return;
    if (showLoading) this.loading = true;
    this.error = false;
    this.cdr.detectChanges();

    this.scheduleService
      .getSchedule({
        date: this.selectedDate || undefined,
        companyId: this.selectedCompanyId ?? undefined,
        roomId: this.selectedRoomId ?? undefined,
        panelId: this.selectedPanelId ?? undefined
      })
      .pipe(
        timeout(15000),
        finalize(() => {
          if (showLoading) this.loading = false;
          this.cdr.detectChanges();
        })
      )
      .subscribe({
        next: (data: ScheduleItem[]) => {
          this.allSchedules = data;
          this.extractDropdownOptions(data);
          this.applyLocalFilters();
          this.error = false;
          this.cdr.detectChanges();
        },
        error: (err) => {
          console.error('SCHEDULE: load failed', err);
          this.error = true;
          this.cdr.detectChanges();
        }
      });
  }

  extractDropdownOptions(items: ScheduleItem[]): void {
    this.availableCompanies = this.scheduleService.extractCompanies(items);
    this.availableRooms = this.scheduleService.extractRooms(items);
    this.availablePanels = this.scheduleService.extractPanels(items);
  }

  applyLocalFilters(): void {
    let result = [...this.allSchedules];

    if (this.selectedTier) {
      result = result.filter(i => i.priorityTier === this.selectedTier);
    }
    if (this.selectedStatus) {
      result = result.filter(i => i.status === this.selectedStatus);
    }

    this.filteredSchedules = result;
    this.groupSchedulesByTime(result);
    this.cdr.detectChanges();
  }

  groupSchedulesByTime(items: ScheduleItem[]): void {
    const map = new Map<string, ScheduleItem[]>();
    items.forEach(item => {
      const timeKey = item.startTime ? item.startTime.substring(0, 5) : 'Unassigned';
      if (!map.has(timeKey)) {
        map.set(timeKey, []);
      }
      map.get(timeKey)!.push(item);
    });

    const groups: TimeSlotGroup[] = [];
    Array.from(map.keys())
      .sort((a, b) => (a === 'Unassigned' ? 1 : b === 'Unassigned' ? -1 : a.localeCompare(b)))
      .forEach(time => {
        groups.push({
          time,
          items: map.get(time)!
        });
      });

    this.timeGroups = groups;
  }

  onFilterChange(): void {
    this.loadSchedule(true);
  }

  clearFilters(): void {
    this.selectedDate = '';
    this.selectedCompanyId = null;
    this.selectedTier = '';
    this.selectedRoomId = null;
    this.selectedPanelId = null;
    this.selectedStatus = '';
    this.generationMessage = '';
    this.loadSchedule(true);
  }

  refresh(): void {
    this.generationMessage = '';
    this.loadSchedule(true);
  }

  generateSchedule(): void {
    if (this.generating || this.loading) return;

    this.generating = true;
    this.error = false;
    this.generationMessage = '';
    this.cdr.detectChanges();

    this.scheduleService
      .generateSchedule()
      .pipe(
        timeout(120000),
        finalize(() => {
          this.generating = false;
          this.cdr.detectChanges();
        })
      )
      .subscribe({
        next: (response: ScheduleGenerationResult) => {
          this.generationMessage = response.message || 'Schedule generated successfully.';
          this.loadSchedule(false);
        },
        error: (err) => {
          console.error('SCHEDULE: generation failed', err);
          this.error = true;
          this.generationMessage = 'Unable to generate schedule.';
          this.cdr.detectChanges();
        }
      });
  }

  get activeFilterCount(): number {
    let count = 0;
    if (this.selectedDate) count++;
    if (this.selectedCompanyId != null) count++;
    if (this.selectedTier) count++;
    if (this.selectedRoomId != null) count++;
    if (this.selectedPanelId != null) count++;
    if (this.selectedStatus) count++;
    return count;
  }

  get scheduledCount(): number {
    return this.filteredSchedules.filter(i => i.status === 'SCHEDULED').length;
  }

  get unscheduledCount(): number {
    return this.filteredSchedules.filter(i => i.status === 'UNSCHEDULED').length;
  }

  get scheduleGenerated(): boolean {
    return this.allSchedules.length > 0;
  }
}