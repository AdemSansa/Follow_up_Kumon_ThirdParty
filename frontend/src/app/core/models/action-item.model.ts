import { PersonSummary } from './followup-plan.model';

export type ActionItemStatus = 'TODO' | 'IN_PROGRESS' | 'DONE' | 'CANCELLED';
export type ActionPriority = 'LOW' | 'MEDIUM' | 'HIGH';

export interface ActionItem {
  id: string;
  followUpId: string;
  student: PersonSummary;
  assignedTo: string | null;
  title: string;
  description: string | null;
  dueDate: string | null;
  priority: ActionPriority;
  status: ActionItemStatus;
  completedAt: string | null;
  createdAt: string;
}
