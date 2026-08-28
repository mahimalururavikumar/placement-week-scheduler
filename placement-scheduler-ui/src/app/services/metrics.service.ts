import { Injectable, inject } from '@angular/core';

import { HttpClient } from '@angular/common/http';
import { environment } from '../../environments/environment';

import {
  Observable,
  timeout
} from 'rxjs';

export interface MetricsSummary {

  totalInterviewCandidates: number;

  scheduledInterviews: number;

  unscheduledInterviews: number;

  totalActiveStudents: number;

  studentsWithInterview: number;

  schedulingSuccessPercentage: number;

  studentCoveragePercentage: number;

  roomUtilizationPercentage: number;

  panelUtilizationPercentage: number;

  averageStudentWaitingMinutes: number;

  replanMovedAppointments: number;
}

@Injectable({
  providedIn: 'root'
})
export class MetricsService {

  private http = inject(HttpClient);

  private readonly apiUrl = `${environment.apiUrl}/metrics`;

  getSummary(): Observable<MetricsSummary> {

    console.log(
      'METRICS SERVICE: requesting /summary'
    );

    return this.http
      .get<MetricsSummary>(
        `${this.apiUrl}/summary`
      )
      .pipe(
        timeout(10000)
      );
  }
}