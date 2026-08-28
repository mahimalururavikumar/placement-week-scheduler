export interface RoomUnavailableRequest {
  roomId: number;
  date: string;
}

export interface PanelDropoutRequest {
  panelId: number;
  date: string;
}

export interface CompanyDelayRequest {
  companyId: number;
  date: string;
  newStartTime: string;
}

export interface StudentWithdrawRequest {
  studentId: number;
}

export interface ReplanChange {
  interviewId: number;
  studentId?: number;
  studentCode?: string;
  companyId?: number;
  companyCode?: string;

  oldDate: string | null;
  oldStartTime: string | null;
  oldEndTime: string | null;
  oldRoomId: number | null;
  oldPanelId: number | null;

  newDate: string | null;
  newStartTime: string | null;
  newEndTime: string | null;
  newRoomId: number | null;
  newPanelId: number | null;

  reason: string;
}

export interface ReplanResult {
  roomId?: number;
  roomCode?: string;

  companyId?: number;
  companyCode?: string;
  companyName?: string;
  oldStartTime?: string;
  newStartTime?: string;

  panelId?: number;
  panelCode?: string;

  studentId?: number;
  studentCode?: string;
  studentName?: string;

  affectedAppointments: number;
  movedAppointments?: number;
  unscheduledAppointments?: number;
  cancelledAppointments?: number;
  unchangedAppointments: number;

  changes: ReplanChange[];
}

export interface ConflictPreviewItem {
  interviewId: number;
  date: string;
  startTime: string;
  endTime: string;
  studentCode: string;
  studentName: string;
  companyCode: string;
  companyName: string;
  panelCode: string;
  roomCode: string;
  status: string;
}

export interface ReplanHistoryItem {
  auditId: number;
  interviewId: number;
  disruptionType: string;
  replannedAt: string;

  oldDate: string | null;
  oldStartTime: string | null;
  oldEndTime: string | null;
  oldRoomId: number | null;
  oldPanelId: number | null;

  newDate: string | null;
  newStartTime: string | null;
  newEndTime: string | null;
  newRoomId: number | null;
  newPanelId: number | null;

  moved: boolean;
  cancelled: boolean;
  reason: string;
}
