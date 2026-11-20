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

/** One entry of GET /api/follow-ups/agenda — a day's check-in with everything needed to run it. */
export interface AgendaItem {
  id: string;
  scheduledStart: string;
  scheduledEnd: string;
  type: MeetingType;
  status: FollowUpStatus;
  overdue: boolean;
  location: string | null;
  student: {
    id: string;
    firstName: string;
    lastName: string;
    level: string | null;
    subjects: ('MATH' | 'ENGLISH')[];
  };
  /** The meeting's parent first (primary: true), then other linked parents. */
  contacts: {
    id: string;
    firstName: string;
    lastName: string;
    relationship: string | null;
    phone: string | null;
    email: string | null;
    preferredContactMethod: 'PHONE' | 'EMAIL' | 'SMS' | 'WHATSAPP' | null;
    primary: boolean;
  }[];
  assignedStaff: { id: string; firstName: string; lastName: string } | null;
  plan: { id: string; frequencyType: string; occurrenceNumber: number; occurrenceCount: number | null } | null;
  lastCheckIn: {
    date: string;
    progressRating: number | null;
    engagementRating: number | null;
    parentSatisfactionRating: number | null;
    concerns: Concern[];
    summary: string | null;
    recommendations: string | null;
  } | null;
  openActionItems: number;
  /** Google Meet state — only for VIDEO_CALL check-ins. */
  meeting: OnlineMeetingInfo | null;
}

export interface OnlineMeetingInfo {
  status: 'NONE' | 'PENDING' | 'CREATED' | 'FAILED' | 'CANCELLED';
  joinUrl: string | null;
  lastError: string | null;
  /** When the parent got the link for the current time (null = not yet). */
  inviteSentAt: string | null;
  parentEmailMissing: boolean;
}

export interface MeetingsStatus {
  googleConfigured: boolean;
  googleMessage: string;
  emailConfigured: boolean;
  automationEnabled: boolean;
  timezone: string;
  createLeadHours: number;
  inviteLeadHours: number;
}
