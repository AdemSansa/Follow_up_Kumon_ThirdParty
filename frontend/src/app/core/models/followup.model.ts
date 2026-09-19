import { MeetingType, PersonSummary } from './followup-plan.model';

export type FollowUpStatus = 'SCHEDULED' | 'CONFIRMED' | 'COMPLETED' | 'MISSED' | 'CANCELLED' | 'RESCHEDULED';

export type Concern = 'ATTENDANCE' | 'HOMEWORK' | 'MOTIVATION' | 'PROGRESS' | 'BEHAVIOR';
export const CONCERNS: Concern[] = ['ATTENDANCE', 'HOMEWORK', 'MOTIVATION', 'PROGRESS', 'BEHAVIOR'];

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
  // Check-in results — null/empty until COMPLETED.
  completedAt: string | null;
  completedBy: string | null;
  progressRating: number | null;
  engagementRating: number | null;
  parentSatisfactionRating: number | null;
  concerns: Concern[];
  summary: string | null;
  recommendations: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface RescheduleRequest {
  scheduledStart: string;
  scheduledEnd: string;
}

export interface ActionItemInput {
  title: string;
  dueDate?: string | null;
  priority?: 'LOW' | 'MEDIUM' | 'HIGH';
}

export interface CompleteFollowUpRequest {
  progressRating: number;
  engagementRating: number;
  parentSatisfactionRating: number;
  concerns: Concern[];
  summary?: string | null;
  recommendations?: string | null;
  actionItems: ActionItemInput[];
}
