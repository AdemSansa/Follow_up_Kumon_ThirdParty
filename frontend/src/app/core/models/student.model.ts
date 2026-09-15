import { Parent } from './parent.model';

export type StudentStatus = 'ACTIVE' | 'PAUSED' | 'INACTIVE' | 'COMPLETED' | 'WITHDRAWN';
export type Subject = 'MATH' | 'ENGLISH';

export interface Student {
  id: string;
  firstName: string;
  lastName: string;
  dateOfBirth: string | null;
  gender: string | null;
  phone: string | null;
  email: string | null;
  address: string | null;
  enrollmentDate: string;
  status: StudentStatus;
  level: string | null;
  subjects: Subject[];
  assignedTeacherId: string | null;
  parents: Parent[];
  notes: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface StudentRequest {
  firstName: string;
  lastName: string;
  dateOfBirth?: string | null;
  gender?: string | null;
  phone?: string | null;
  email?: string | null;
  address?: string | null;
  enrollmentDate: string;
  status: StudentStatus;
  level?: string | null;
  subjects?: Subject[];
  assignedTeacherId?: string | null;
  parentIds?: string[];
  notes?: string | null;
}

export const STUDENT_STATUSES: StudentStatus[] = ['ACTIVE', 'PAUSED', 'INACTIVE', 'COMPLETED', 'WITHDRAWN'];
export const SUBJECTS: Subject[] = ['MATH', 'ENGLISH'];
