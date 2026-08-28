import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import {
  RoomUnavailableRequest,
  PanelDropoutRequest,
  CompanyDelayRequest,
  StudentWithdrawRequest,
  ReplanResult,
  ConflictPreviewItem,
  ReplanHistoryItem
} from '../models/replan.model';

@Injectable({
  providedIn: 'root'
})
export class ReplanService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = `${environment.apiUrl}/replan`;

  replanRoomUnavailable(request: RoomUnavailableRequest): Observable<ReplanResult> {
    return this.http.post<ReplanResult>(`${this.apiUrl}/room-unavailable`, request);
  }

  replanPanelDropout(request: PanelDropoutRequest): Observable<ReplanResult> {
    return this.http.post<ReplanResult>(`${this.apiUrl}/panel-drop`, request);
  }

  replanCompanyDelay(request: CompanyDelayRequest): Observable<ReplanResult> {
    return this.http.post<ReplanResult>(`${this.apiUrl}/company-delay`, request);
  }

  replanStudentWithdraw(request: StudentWithdrawRequest): Observable<ReplanResult> {
    return this.http.post<ReplanResult>(`${this.apiUrl}/student-withdraw`, request);
  }

  getRoomImpact(roomId: number, date: string): Observable<ConflictPreviewItem[]> {
    const params = new HttpParams().set('date', date);
    return this.http.get<ConflictPreviewItem[]>(`${this.apiUrl}/room/${roomId}/scheduled`, { params });
  }

  getPanelImpact(panelId: number, date: string): Observable<ConflictPreviewItem[]> {
    const params = new HttpParams().set('date', date);
    return this.http.get<ConflictPreviewItem[]>(`${this.apiUrl}/panel/${panelId}/scheduled`, { params });
  }

  getCompanyImpact(companyId: number, date: string): Observable<ConflictPreviewItem[]> {
    const params = new HttpParams().set('date', date);
    return this.http.get<ConflictPreviewItem[]>(`${this.apiUrl}/company/${companyId}/scheduled`, { params });
  }

  getStudentImpact(studentId: number): Observable<ConflictPreviewItem[]> {
    return this.http.get<ConflictPreviewItem[]>(`${this.apiUrl}/student/${studentId}/scheduled`);
  }

  getReplanHistory(): Observable<ReplanHistoryItem[]> {
    return this.http.get<ReplanHistoryItem[]>(`${this.apiUrl}/history`);
  }

  getReplanHistoryByType(type: string): Observable<ReplanHistoryItem[]> {
    return this.http.get<ReplanHistoryItem[]>(`${this.apiUrl}/history/type/${type}`);
  }

  getReplanHistoryByDate(date: string): Observable<ReplanHistoryItem[]> {
    return this.http.get<ReplanHistoryItem[]>(`${this.apiUrl}/history/date/${date}`);
  }
}
