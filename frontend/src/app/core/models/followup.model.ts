import { MeetingType, PersonSummary } from './followup-plan.model';

export type FollowUpStatus = 'SCHEDULED' | 'CONFIRMED' | 'COMPLETED' | 'MISSED' | 'CANCELLED' | 'RESCHEDULED';

export interface FollowUp {
  id: string;
  student: PersonSummary;
  followUpPlanId: string | null;
  parent: PersonSummary | null;
  assignedStaffId: string | null;
  type: MeetingType;
  scheduledStart: string;
  scheduledEnd: string;
  status: FollowUpStatus;
  overdue: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface RescheduleRequest {
  scheduledStart: string;
  scheduledEnd: string;
}
