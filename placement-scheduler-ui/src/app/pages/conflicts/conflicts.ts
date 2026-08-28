import { Component, OnInit, ChangeDetectorRef, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { finalize } from 'rxjs';

import { ReplanService } from '../../services/replan';
import { ScheduleService } from '../../services/schedule';
import { OptionItem } from '../../models/schedule.model';
import { ReplanResult, ConflictPreviewItem } from '../../models/replan.model';

export type DisruptionType = 'ROOM_UNAVAILABLE' | 'COMPANY_DELAY' | 'STUDENT_WITHDRAW' | 'PANEL_DROPOUT';

@Component({
  selector: 'app-conflicts',
  imports: [CommonModule, FormsModule],
  templateUrl: './conflicts.html',
  styleUrl: './conflicts.css'
})
export class ConflictsComponent implements OnInit {
  private readonly replanService = inject(ReplanService);
  private readonly scheduleService = inject(ScheduleService);
  private readonly cdr = inject(ChangeDetectorRef);

  activeDisruption: DisruptionType | null = null;
  loading = false;
  replanning = false;

  // Dropdown Lists
  availableDates: string[] = ['2026-08-24', '2026-08-25', '2026-08-26', '2026-08-27'];
  availableRooms: OptionItem[] = [];
  availableCompanies: OptionItem[] = [];
  availablePanels: OptionItem[] = [];
  availableStudents: OptionItem[] = [];

  // Form Models
  selectedDate = '2026-08-24';
  selectedRoomId: number | null = null;
  selectedCompanyId: number | null = null;
  selectedPanelId: number | null = null;
  selectedStudentId: number | null = null;
  newStartTime = '11:00';
  disruptionReason = '';

  // Impact Preview Items
  impactPreview: ConflictPreviewItem[] = [];
  loadingImpact = false;

  // Replan Output
  lastReplanResult: ReplanResult | null = null;
  lastDisruptionName = '';
  errorMessage = '';

  ngOnInit(): void {
    this.loadDropdownData();
  }

  loadDropdownData(): void {
    this.loading = true;
    this.scheduleService.getSchedule().pipe(
      finalize(() => {
        this.loading = false;
        this.cdr.detectChanges();
      })
    ).subscribe({
      next: (items) => {
        this.availableRooms = this.scheduleService.extractRooms(items);
        this.availableCompanies = this.scheduleService.extractCompanies(items);
        this.availablePanels = this.scheduleService.extractPanels(items);
        this.availableStudents = this.scheduleService.extractStudents(items);

        // Preselect default values
        if (this.availableRooms.length > 0) this.selectedRoomId = this.availableRooms[0].id;
        if (this.availableCompanies.length > 0) this.selectedCompanyId = this.availableCompanies[0].id;
        if (this.availablePanels.length > 0) this.selectedPanelId = this.availablePanels[0].id;
        if (this.availableStudents.length > 0) this.selectedStudentId = this.availableStudents[0].id;

        this.cdr.detectChanges();
      },
      error: (err) => {
        console.error('Failed to load schedule items for dropdowns', err);
        this.cdr.detectChanges();
      }
    });
  }

  selectDisruption(type: DisruptionType): void {
    this.activeDisruption = type;
    this.lastReplanResult = null;
    this.errorMessage = '';
    this.impactPreview = [];
    this.fetchImpactPreview();
    this.cdr.detectChanges();
  }

  closeForm(): void {
    this.activeDisruption = null;
    this.impactPreview = [];
    this.cdr.detectChanges();
  }

  fetchImpactPreview(): void {
    if (!this.activeDisruption) return;

    this.loadingImpact = true;
    this.impactPreview = [];
    this.cdr.detectChanges();

    const onComplete = (items: ConflictPreviewItem[]) => {
      this.impactPreview = items;
      this.loadingImpact = false;
      this.cdr.detectChanges();
    };

    const onError = () => {
      this.loadingImpact = false;
      this.cdr.detectChanges();
    };

    if (this.activeDisruption === 'ROOM_UNAVAILABLE' && this.selectedRoomId) {
      this.replanService.getRoomImpact(Number(this.selectedRoomId), this.selectedDate).subscribe({ next: onComplete, error: onError });
    } else if (this.activeDisruption === 'PANEL_DROPOUT' && this.selectedPanelId) {
      this.replanService.getPanelImpact(Number(this.selectedPanelId), this.selectedDate).subscribe({ next: onComplete, error: onError });
    } else if (this.activeDisruption === 'COMPANY_DELAY' && this.selectedCompanyId) {
      this.replanService.getCompanyImpact(Number(this.selectedCompanyId), this.selectedDate).subscribe({ next: onComplete, error: onError });
    } else if (this.activeDisruption === 'STUDENT_WITHDRAW' && this.selectedStudentId) {
      this.replanService.getStudentImpact(Number(this.selectedStudentId)).subscribe({ next: onComplete, error: onError });
    } else {
      this.loadingImpact = false;
      this.cdr.detectChanges();
    }
  }

  executeConflict(): void {
    if (!this.activeDisruption || this.replanning) return;

    this.replanning = true;
    this.lastReplanResult = null;
    this.errorMessage = '';
    this.cdr.detectChanges();

    if (this.activeDisruption === 'ROOM_UNAVAILABLE') {
      if (!this.selectedRoomId) {
        this.replanning = false;
        this.errorMessage = 'Please select an affected room.';
        this.cdr.detectChanges();
        return;
      }
      this.lastDisruptionName = `Room Down (${this.getRoomCode(Number(this.selectedRoomId))})`;
      this.replanService.replanRoomUnavailable({
        roomId: Number(this.selectedRoomId),
        date: this.selectedDate
      }).subscribe({
        next: (res) => { this.handleReplanSuccess(res); },
        error: (err) => { this.handleReplanError(err); }
      });
    } else if (this.activeDisruption === 'COMPANY_DELAY') {
      if (!this.selectedCompanyId) {
        this.replanning = false;
        this.errorMessage = 'Please select a company.';
        this.cdr.detectChanges();
        return;
      }
      this.lastDisruptionName = `Company Delay (${this.getCompanyName(Number(this.selectedCompanyId))})`;
      const formattedTime = this.newStartTime.length === 5 ? `${this.newStartTime}:00` : this.newStartTime;
      this.replanService.replanCompanyDelay({
        companyId: Number(this.selectedCompanyId),
        date: this.selectedDate,
        newStartTime: formattedTime
      }).subscribe({
        next: (res) => { this.handleReplanSuccess(res); },
        error: (err) => { this.handleReplanError(err); }
      });
    } else if (this.activeDisruption === 'STUDENT_WITHDRAW') {
      if (!this.selectedStudentId) {
        this.replanning = false;
        this.errorMessage = 'Please select a student.';
        this.cdr.detectChanges();
        return;
      }
      this.lastDisruptionName = `Student Withdrawal (${this.getStudentName(Number(this.selectedStudentId))})`;
      this.replanService.replanStudentWithdraw({
        studentId: Number(this.selectedStudentId)
      }).subscribe({
        next: (res) => { this.handleReplanSuccess(res); },
        error: (err) => { this.handleReplanError(err); }
      });
    } else if (this.activeDisruption === 'PANEL_DROPOUT') {
      if (!this.selectedPanelId) {
        this.replanning = false;
        this.errorMessage = 'Please select a panel.';
        this.cdr.detectChanges();
        return;
      }
      this.lastDisruptionName = `Panel Dropout (${this.getPanelCode(Number(this.selectedPanelId))})`;
      this.replanService.replanPanelDropout({
        panelId: Number(this.selectedPanelId),
        date: this.selectedDate
      }).subscribe({
        next: (res) => { this.handleReplanSuccess(res); },
        error: (err) => { this.handleReplanError(err); }
      });
    }
  }

  handleReplanSuccess(result: ReplanResult): void {
    this.replanning = false;
    this.lastReplanResult = result;
    this.cdr.detectChanges();
    this.scheduleService.getSchedule().subscribe();
  }

  handleReplanError(err: any): void {
    this.replanning = false;
    console.error('Replan failed', err);
    this.errorMessage = err?.error?.message || 'Replan operation failed to execute.';
    this.cdr.detectChanges();
  }

  // Label Helpers
  getRoomCode(id: number): string {
    return this.availableRooms.find(r => Number(r.id) === Number(id))?.code || `Room #${id}`;
  }

  getCompanyName(id: number): string {
    return this.availableCompanies.find(c => Number(c.id) === Number(id))?.name || `Company #${id}`;
  }

  getPanelCode(id: number): string {
    return this.availablePanels.find(p => Number(p.id) === Number(id))?.code || `Panel #${id}`;
  }

  getStudentName(id: number): string {
    return this.availableStudents.find(s => Number(s.id) === Number(id))?.name || `Student #${id}`;
  }

  getChangeTypeBadge(change: any): string {
    if (!change.newDate && !change.newStartTime) return 'CANCELLED';
    if (change.oldStartTime !== change.newStartTime || change.oldRoomId !== change.newRoomId || change.oldPanelId !== change.newPanelId) {
      return 'MOVED';
    }
    return 'UNCHANGED';
  }
}
