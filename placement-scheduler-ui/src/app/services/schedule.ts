import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable, tap } from 'rxjs';
import { environment } from '../../environments/environment';
import { ScheduleItem, OptionItem } from '../models/schedule.model';

export interface ScheduleGenerationResult {
  success: boolean;
  totalCandidates: number;
  scheduled: number;
  unscheduled: number;
  studentCoverage?: number;
  message: string;
}

@Injectable({
  providedIn: 'root'
})
export class ScheduleService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = `${environment.apiUrl}/scheduling`;

  private cachedSchedule: ScheduleItem[] = [];

  getSchedule(filters?: {
    date?: string;
    companyId?: number;
    studentId?: number;
    roomId?: number;
    panelId?: number;
  }): Observable<ScheduleItem[]> {
    let params = new HttpParams();

    if (filters?.date) {
      params = params.set('date', filters.date);
    }
    if (filters?.companyId != null) {
      params = params.set('companyId', filters.companyId);
    }
    if (filters?.studentId != null) {
      params = params.set('studentId', filters.studentId);
    }
    if (filters?.roomId != null) {
      params = params.set('roomId', filters.roomId);
    }
    if (filters?.panelId != null) {
      params = params.set('panelId', filters.panelId);
    }

    return this.http.get<ScheduleItem[]>(`${this.apiUrl}/schedule`, { params }).pipe(
      tap(items => {
        if (!filters || Object.keys(filters).length === 0) {
          this.cachedSchedule = items;
        }
      })
    );
  }

  generateSchedule(): Observable<ScheduleGenerationResult> {
    return this.http.post<ScheduleGenerationResult>(`${this.apiUrl}/generate`, {});
  }

  getCachedSchedule(): ScheduleItem[] {
    return this.cachedSchedule;
  }

  private parseId(code: string | undefined, defaultId: number): number {
    if (!code) return defaultId;
    const match = code.match(/\d+/);
    return match ? parseInt(match[0], 10) : defaultId;
  }

  extractRooms(items: ScheduleItem[] = this.cachedSchedule): OptionItem[] {
    const map = new Map<string, OptionItem>();
    items.forEach((item, index) => {
      if (item.roomCode && !map.has(item.roomCode)) {
        const id = item.roomId ?? this.parseId(item.roomCode, index + 1);
        map.set(item.roomCode, {
          id,
          code: item.roomCode,
          name: `Room ${item.roomCode}`
        });
      }
    });
    return Array.from(map.values()).sort((a, b) => a.code.localeCompare(b.code));
  }

  extractCompanies(items: ScheduleItem[] = this.cachedSchedule): OptionItem[] {
    const map = new Map<string, OptionItem>();
    items.forEach((item, index) => {
      if (item.companyName && !map.has(item.companyName)) {
        const id = item.companyId ?? this.parseId(item.companyCode, index + 1);
        map.set(item.companyName, {
          id,
          code: item.companyCode || `CMP${id}`,
          name: item.companyName,
          subtext: item.priorityTier ? `Tier: ${item.priorityTier}` : undefined
        });
      }
    });
    return Array.from(map.values()).sort((a, b) => a.name.localeCompare(b.name));
  }

  extractPanels(items: ScheduleItem[] = this.cachedSchedule): OptionItem[] {
    const map = new Map<string, OptionItem>();
    items.forEach((item, index) => {
      if (item.panelCode && !map.has(item.panelCode)) {
        const id = item.panelId ?? this.parseId(item.panelCode, index + 1);
        map.set(item.panelCode, {
          id,
          code: item.panelCode,
          name: `Panel ${item.panelCode}`,
          subtext: item.companyName
        });
      }
    });
    return Array.from(map.values()).sort((a, b) => a.code.localeCompare(b.code));
  }

  extractStudents(items: ScheduleItem[] = this.cachedSchedule): OptionItem[] {
    const map = new Map<string, OptionItem>();
    items.forEach((item, index) => {
      if (item.studentCode && !map.has(item.studentCode)) {
        const id = item.studentId ?? this.parseId(item.studentCode, index + 1);
        map.set(item.studentCode, {
          id,
          code: item.studentCode,
          name: `${item.studentCode} - ${item.studentName}`
        });
      }
    });
    return Array.from(map.values()).sort((a, b) => a.code.localeCompare(b.code));
  }
}