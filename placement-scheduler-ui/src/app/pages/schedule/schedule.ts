import {
  Component,
  OnInit,
  inject
} from '@angular/core';

import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';

import {
  ScheduleService
} from '../../services/schedule';

import {
  ScheduleItem
} from '../../models/schedule.model';

@Component({
  selector: 'app-schedule',
  imports: [
    CommonModule,
    FormsModule
  ],
  templateUrl: './schedule.html',
  styleUrl: './schedule.css'
})
export class ScheduleComponent implements OnInit {

  private readonly scheduleService =
    inject(ScheduleService);

  schedules: ScheduleItem[] = [];

  loading = false;
  generating = false;
  error = false;

  selectedDate = '';
  selectedCompanyId: number | null = null;
  selectedStudentId: number | null = null;
  selectedRoomId: number | null = null;
  selectedPanelId: number | null = null;

  generationMessage = '';

  ngOnInit(): void {
    this.loadSchedule();
  }

  loadSchedule(): void {

    this.loading = true;
    this.error = false;

    this.scheduleService
      .getSchedule({
        date: this.selectedDate || undefined,
        companyId:
          this.selectedCompanyId ?? undefined,
        studentId:
          this.selectedStudentId ?? undefined,
        roomId:
          this.selectedRoomId ?? undefined,
        panelId:
          this.selectedPanelId ?? undefined
      })
      .subscribe({

        next: (data) => {
          this.schedules = data;
          this.loading = false;
        },

        error: (err) => {
          console.error(
            'Schedule loading failed:',
            err
          );

          this.schedules = [];
          this.loading = false;
          this.error = true;
        }
      });
  }

  generateSchedule(): void {

    if (this.generating) {
      return;
    }

    this.generating = true;
    this.generationMessage = '';
    this.error = false;

    this.scheduleService
      .generateSchedule()
      .subscribe({

        next: (response: any) => {

          console.log(
            'Schedule generated:',
            response
          );

          this.generationMessage =
            response?.message ??
            'Schedule generated successfully.';

          this.generating = false;

          this.loadSchedule();
        },

        error: (err) => {

          console.error(
            'Schedule generation failed:',
            err
          );

          this.generating = false;
          this.error = true;

          this.generationMessage =
            'Unable to generate schedule.';
        }
      });
  }

  refresh(): void {
    this.loadSchedule();
  }

  clearFilters(): void {

    this.selectedDate = '';
    this.selectedCompanyId = null;
    this.selectedStudentId = null;
    this.selectedRoomId = null;
    this.selectedPanelId = null;

    this.loadSchedule();
  }

  get scheduledCount(): number {

    return this.schedules.filter(
      item =>
        item.status === 'SCHEDULED'
    ).length;
  }

  get unscheduledCount(): number {

    return this.schedules.filter(
      item =>
        item.status === 'UNSCHEDULED'
    ).length;
  }

  trackByInterviewId(
    index: number,
    item: ScheduleItem
  ): number {

    return item.interviewId;
  }
}