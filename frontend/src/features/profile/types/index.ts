export interface Location {
  state?: string | null;
  district?: string | null;
  mandal?: string | null;
  village?: string | null;
}

export type PrimaryRole = 'FARMER' | 'MEDIATOR_BUYER' | 'CUSTOMER';

export type RoleType = PrimaryRole | 'USER' | 'ADMIN';

export const ROLE_DISPLAY_NAMES: Record<string, string> = {
  FARMER: 'Farmer',
  MEDIATOR_BUYER: 'Mediator / Buyer',
  CUSTOMER: 'Customer',
  USER: 'User',
};

export interface Profile {
  uid: string;
  displayName: string;
  email: string;
  emailVerified: boolean;
  role: RoleType;
  roleDisplayName?: string;
  status: string;
  phoneNumber?: string | null;
  location?: Location | null;
  businessOrganizationName?: string | null;
  address?: string | null;
  profileCompleted: boolean;
}

export interface UpdateProfilePayload {
  displayName?: string;
  phoneNumber?: string;
  location?: Location;
  businessOrganizationName?: string;
  address?: string;
}

export interface RoleSelectionPayload {
  role: PrimaryRole;
}
