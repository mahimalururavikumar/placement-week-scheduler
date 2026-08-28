import {
  Component,
  OnInit,
  ChangeDetectorRef,
  inject
} from '@angular/core';

import { CommonModule } from '@angular/common';

import {
  MetricsService,
  MetricsSummary
} from '../../services/metrics.service';

@Component({
  selector: 'app-dashboard',
  imports: [CommonModule],
  templateUrl: './dashboard.html',
  styleUrl: './dashboard.css'
})
export class DashboardComponent implements OnInit {

  private metricsService = inject(MetricsService);
  private cdr = inject(ChangeDetectorRef);

  metrics: MetricsSummary | null = null;

  loading = true;
  error = false;

  ngOnInit(): void {
    this.loadDashboard();
  }

  loadDashboard(): void {

    console.log('DASHBOARD: loading started');

    this.loading = true;
    this.error = false;

    this.metricsService.getSummary().subscribe({

      next: (data: MetricsSummary) => {

        console.log('DASHBOARD: API response received', data);

        this.metrics = data;

        this.loading = false;
        this.error = false;

        this.cdr.detectChanges();

        console.log(
          'DASHBOARD: loading finished',
          {
            loading: this.loading,
            error: this.error,
            metrics: this.metrics
          }
        );
      },

      error: (err) => {

        console.error(
          'DASHBOARD: API error',
          err
        );

        this.metrics = null;

        this.loading = false;
        this.error = true;

        this.cdr.detectChanges();

        console.log(
          'DASHBOARD: error state',
          {
            loading: this.loading,
            error: this.error
          }
        );
      },

      complete: () => {

        console.log(
          'DASHBOARD: request completed'
        );

        this.loading = false;

        this.cdr.detectChanges();
      }

    });
  }

  refresh(): void {
    this.loadDashboard();
  }
}