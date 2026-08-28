import {
  Injectable,
  inject
} from '@angular/core';

import {
  HttpClient,
  HttpParams
} from '@angular/common/http';

import {
  Observable
} from 'rxjs';

import {
  environment
} from '../../environments/environment';

import {
  ScheduleItem
} from '../models/schedule.model';


export interface ScheduleGenerationResult {

  success: boolean;

  totalCandidates: number;

  scheduled: number;

  unscheduled: number;

  message: string;
}


@Injectable({
  providedIn: 'root'
})
export class ScheduleService {

  private readonly http =
    inject(HttpClient);

  private readonly apiUrl =
    `${environment.apiUrl}/scheduling`;


  /*
   * --------------------------------------------------
   * GET SCHEDULE
   * --------------------------------------------------
   */
  getSchedule(
    filters?: {
      date?: string;

      companyId?: number;

      studentId?: number;

      roomId?: number;

      panelId?: number;
    }
  ): Observable<ScheduleItem[]> {

    let params =
      new HttpParams();


    if (filters?.date) {

      params =
        params.set(
          'date',
          filters.date
        );
    }


    if (
      filters?.companyId != null
    ) {

      params =
        params.set(
          'companyId',
          filters.companyId
        );
    }


    if (
      filters?.studentId != null
    ) {

      params =
        params.set(
          'studentId',
          filters.studentId
        );
    }


    if (
      filters?.roomId != null
    ) {

      params =
        params.set(
          'roomId',
          filters.roomId
        );
    }


    if (
      filters?.panelId != null
    ) {

      params =
        params.set(
          'panelId',
          filters.panelId
        );
    }


    return this.http.get<ScheduleItem[]>(
      `${this.apiUrl}/schedule`,
      {
        params
      }
    );
  }


  /*
   * --------------------------------------------------
   * GENERATE INITIAL SCHEDULE
   * --------------------------------------------------
   */
  generateSchedule():
    Observable<ScheduleGenerationResult> {

    return this.http.post<ScheduleGenerationResult>(
      `${this.apiUrl}/generate`,
      {}
    );
  }

}