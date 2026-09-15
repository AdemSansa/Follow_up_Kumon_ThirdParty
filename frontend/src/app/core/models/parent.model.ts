export type ContactMethod = 'PHONE' | 'EMAIL' | 'SMS' | 'WHATSAPP';

export interface Parent {
  id: string;
  firstName: string;
  lastName: string;
  phone: string | null;
  email: string | null;
  relationship: string;
  preferredContactMethod: ContactMethod;
  notes: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface ParentRequest {
  firstName: string;
  lastName: string;
  phone?: string | null;
  email?: string | null;
  relationship: string;
  preferredContactMethod: ContactMethod;
  notes?: string | null;
}
