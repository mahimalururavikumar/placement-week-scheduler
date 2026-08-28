import {
  Component,
  OnInit,
  ChangeDetectorRef,
  inject
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';

import {
  MetricsService,
  MetricsSummary
} from '../../services/metrics.service';

import {
  ScheduleService,
  ScheduleGenerationResult
} from '../../services/schedule';

import { ReplanService } from '../../services/replan';

@Component({
  selector: 'app-dashboard',
  imports: [CommonModule],
  templateUrl: './dashboard.html',
  styleUrl: './dashboard.css'
})
export class DashboardComponent implements OnInit {
  private metricsService = inject(MetricsService);
  private scheduleService = inject(ScheduleService);
  private replanService = inject(ReplanService);
  private router = inject(Router);
  private cdr = inject(ChangeDetectorRef);

  metrics: MetricsSummary | null = null;

  loading = true;
  generating = false;
  datasetGenerated = true;
  error = false;

  generationResult: ScheduleGenerationResult | null = null;

  // Dataset statistics
  companyCount = 35;
  studentCount = 800;
  roomCount = 20;

  ngOnInit(): void {
    this.loadDashboard();
  }

  loadDashboard(): void {
    this.loading = true;
    this.error = false;

    this.metricsService.getSummary().subscribe({
      next: (data: MetricsSummary) => {
        this.metrics = data;
        this.loading = false;
        this.error = false;
        this.cdr.detectChanges();
      },
      error: (err) => {
        console.error('DASHBOARD: API error', err);
        this.metrics = null;
        this.loading = false;
        this.error = true;
        this.cdr.detectChanges();
      }
    });
  }

  get scheduleState(): 'NOT_GENERATED' | 'GENERATED' | 'REPLANNED' {
    if (!this.metrics) return 'NOT_GENERATED';
    if (this.metrics.replanMovedAppointments > 0) return 'REPLANNED';
    if (this.metrics.scheduledInterviews > 0 || this.metrics.unscheduledInterviews > 0) return 'GENERATED';
    return 'NOT_GENERATED';
  }

  generateSchedule(): void {
    if (this.generating) return;

    this.generating = true;
    this.generationResult = null;

    this.scheduleService.generateSchedule().subscribe({
      next: (res: ScheduleGenerationResult) => {
        this.generationResult = res;
        this.generating = false;
        this.loadDashboard();
      },
      error: (err) => {
        console.error('Failed to generate schedule', err);
        this.generating = false;
        this.cdr.detectChanges();
      }
    });
  }

  resetDataset(): void {
    // Re-generate initial schedule to reset state
    this.generateSchedule();
  }

  navigateTo(path: string): void {
    this.router.navigate([path]);
  }

  refresh(): void {
    this.loadDashboard();
  }
}