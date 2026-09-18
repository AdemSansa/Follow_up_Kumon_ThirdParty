export type FrequencyType =
  | 'WEEKLY'
  | 'EVERY_2_WEEKS'
  | 'MONTHLY'
  | 'EVERY_2_MONTHS'
  | 'EVERY_3_MONTHS'
  | 'EVERY_6_MONTHS'
  | 'YEARLY'
  | 'CUSTOM';

export type IntervalUnit = 'DAYS' | 'WEEKS' | 'MONTHS';

export type MeetingType =
  | 'PARENT_MEETING'
  | 'PHONE_CALL'
  | 'VIDEO_CALL'
  | 'IN_PERSON'
  | 'EMAIL_CHECK_IN'
  | 'INTERNAL_REVIEW';

export type ReminderOffset =
  | 'SEVEN_DAYS_BEFORE'
  | 'TWENTY_FOUR_HOURS_BEFORE'
  | 'TWO_HOURS_BEFORE'
  | 'THIRTY_MINUTES_BEFORE';

export type DayOfWeekName =
  | 'MONDAY'
  | 'TUESDAY'
  | 'WEDNESDAY'
  | 'THURSDAY'
  | 'FRIDAY'
  | 'SATURDAY'
  | 'SUNDAY';

export interface PersonSummary {
  id: string;
  firstName: string;
  lastName: string;
}

export interface FollowUpPlan {
  id: string;
  student: PersonSummary;
  parent: PersonSummary | null;
  assignedStaffId: string | null;
  frequencyType: FrequencyType;
  customIntervalValue: number | null;
  customIntervalUnit: IntervalUnit | null;
  startDate: string;
  preferredDay: DayOfWeekName | null;
  preferredTime: string | null;
  meetingDurationMinutes: number;
  location: string | null;
  meetingType: MeetingType;
  active: boolean;
  nextFollowUpDate: string | null;
  reminderOffsets: ReminderOffset[];
  createdAt: string;
  updatedAt: string;
}

export interface FollowUpPlanRequest {
  studentId: string;
  parentId?: string | null;
  assignedStaffId?: string | null;
  frequencyType: FrequencyType;
  customIntervalValue?: number | null;
  customIntervalUnit?: IntervalUnit | null;
  startDate: string;
  preferredDay?: DayOfWeekName | null;
  preferredTime?: string | null;
  meetingDurationMinutes?: number | null;
  location?: string | null;
  meetingType: MeetingType;
  reminderOffsets?: ReminderOffset[];
  active?: boolean;
}

export const FREQUENCY_TYPES: FrequencyType[] = [
  'WEEKLY',
  'EVERY_2_WEEKS',
  'MONTHLY',
  'EVERY_2_MONTHS',
  'EVERY_3_MONTHS',
  'EVERY_6_MONTHS',
  'YEARLY',
  'CUSTOM',
];

export const INTERVAL_UNITS: IntervalUnit[] = ['DAYS', 'WEEKS', 'MONTHS'];

export const MEETING_TYPES: MeetingType[] = [
  'PARENT_MEETING',
  'PHONE_CALL',
  'VIDEO_CALL',
  'IN_PERSON',
  'EMAIL_CHECK_IN',
  'INTERNAL_REVIEW',
];

export const REMINDER_OFFSETS: ReminderOffset[] = [
  'SEVEN_DAYS_BEFORE',
  'TWENTY_FOUR_HOURS_BEFORE',
  'TWO_HOURS_BEFORE',
  'THIRTY_MINUTES_BEFORE',
];

export const DAYS_OF_WEEK: DayOfWeekName[] = [
  'MONDAY',
  'TUESDAY',
  'WEDNESDAY',
  'THURSDAY',
  'FRIDAY',
  'SATURDAY',
  'SUNDAY',
];
