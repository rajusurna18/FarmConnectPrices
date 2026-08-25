import React, { useEffect, useState, useCallback } from 'react';
import { onAuthStateChanged } from 'firebase/auth';
import type { User } from 'firebase/auth';
import { auth } from '../../../config/firebase';
import { logoutUser } from '../../../services/firebase';
import type { UserDocument } from '../types';
import { AuthContext } from './AuthContext';

export const AuthProvider: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const [currentUser, setCurrentUser] = useState<User | null>(null);
  const [userDocument, setUserDocument] = useState<UserDocument | null>(null);
  const [loading, setLoading] = useState<boolean>(true);

  const syncUserAuthSnapshot = useCallback((user: User | null) => {
    if (!user) {
      setUserDocument(null);
      return;
    }
    setUserDocument({
      uid: user.uid,
      displayName: user.displayName || user.email?.split('@')[0] || 'User',
      email: user.email || '',
      emailVerified: user.emailVerified,
      role: 'USER',
      status: 'ACTIVE',
    });
  }, []);

  const refreshUser = useCallback(async () => {
    if (auth.currentUser) {
      await auth.currentUser.reload();
      setCurrentUser(auth.currentUser);
      syncUserAuthSnapshot(auth.currentUser);
    }
  }, [syncUserAuthSnapshot]);

  useEffect(() => {
    const unsubscribe = onAuthStateChanged(auth, (user) => {
      setCurrentUser(user);
      syncUserAuthSnapshot(user);
      setLoading(false);
    });

    return () => unsubscribe();
  }, [syncUserAuthSnapshot]);

  const handleLogout = async () => {
    await logoutUser();
    setCurrentUser(null);
    setUserDocument(null);
  };

  return (
    <AuthContext.Provider
      value={{
        currentUser,
        userDocument,
        loading,
        isAuthenticated: !!currentUser,
        logout: handleLogout,
        refreshUser,
      }}
    >
      {children}
    </AuthContext.Provider>
  );
};
