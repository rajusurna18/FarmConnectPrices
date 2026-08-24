export interface Location {
  state?: string | null;
  district?: string | null;
  mandal?: string | null;
  village?: string | null;
}

export type RoleType = 'USER' | 'FARMER' | 'BUYER' | 'MIDDLEMAN' | 'DELIVERY_PARTNER' | 'ADMIN';

export interface Profile {
  uid: string;
  displayName: string;
  email: string;
  emailVerified: boolean;
  role: RoleType;
  status: string;
  phoneNumber?: string | null;
  location?: Location | null;
  profileCompleted: boolean;
}

export interface UpdateProfilePayload {
  displayName?: string;
  phoneNumber?: string;
  location?: Location;
}

export interface RoleSelectionPayload {
  role: 'FARMER' | 'BUYER';
}
