import type { User } from 'firebase/auth';

export interface UserDocument {
  uid: string;
  displayName: string;
  email: string;
  emailVerified: boolean;
  role: 'USER' | 'FARMER' | 'MEDIATOR_BUYER' | 'CUSTOMER' | 'ADMIN' | string;
  status: 'ACTIVE' | 'INACTIVE';
  createdAt?: unknown;
  updatedAt?: unknown;
}

export type AuthUser = User;

export interface RegisterFormData {
  fullName: string;
  email: string;
  pass: string;
  confirmPass: string;
}

export interface LoginFormData {
  email: string;
  pass: string;
}

export interface ForgotPasswordFormData {
  email: string;
}
