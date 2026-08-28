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