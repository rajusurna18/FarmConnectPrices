import { createContext } from 'react';
import type { User } from 'firebase/auth';
import type { UserDocument } from '../types';

export interface AuthContextType {
  currentUser: User | null;
  userDocument: UserDocument | null;
  loading: boolean;
  isAuthenticated: boolean;
  logout: () => Promise<void>;
  refreshUser: () => Promise<void>;
}

export const AuthContext = createContext<AuthContextType | undefined>(undefined);
