export interface ScheduleItem {
  interviewId: number;
  studentId: number;
  studentCode: string;
  studentName: string;

  companyId: number;
  companyCode: string;
  companyName: string;

  panelId: number;
  panelCode: string;

  roomId: number;
  roomCode: string;

  date: string;
  startTime: string;
  endTime: string;

  status: string;
}