export interface ScheduleItem {
  interviewId: number;

  studentId?: number;
  studentCode: string;
  studentName: string;

  companyId?: number;
  companyCode: string;
  companyName: string;
  priorityTier?: string;

  panelId?: number;
  panelCode: string;

  roomId?: number;
  roomCode: string;

  date: string;
  startTime: string;
  endTime: string;

  status: string;
}

export interface OptionItem {
  id: number;
  code: string;
  name: string;
  subtext?: string;
}