import React, { useEffect, useState, useCallback } from 'react';
import { onAuthStateChanged } from 'firebase/auth';
import type { User } from 'firebase/auth';
import { auth } from '../../../config/firebase';
import { fetchDocument, logoutUser } from '../../../services/firebase';
import type { UserDocument } from '../types';
import { AuthContext } from './AuthContext';

export const AuthProvider: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const [currentUser, setCurrentUser] = useState<User | null>(null);
  const [userDocument, setUserDocument] = useState<UserDocument | null>(null);
  const [loading, setLoading] = useState<boolean>(true);

  const fetchUserData = useCallback(async (user: User | null) => {
    if (!user) {
      setUserDocument(null);
      return;
    }
    try {
      const docResult = await fetchDocument<UserDocument>('users', user.uid);
      if (docResult) {
        setUserDocument(docResult.data);
      } else {
        setUserDocument({
          uid: user.uid,
          displayName: user.displayName || user.email?.split('@')[0] || 'User',
          email: user.email || '',
          emailVerified: user.emailVerified,
          role: 'USER',
          status: 'ACTIVE',
        });
      }
    } catch (error) {
      console.warn('[AuthContext] Error fetching user document:', error);
    }
  }, []);

  const refreshUser = useCallback(async () => {
    if (auth.currentUser) {
      await auth.currentUser.reload();
      setCurrentUser(auth.currentUser);
      await fetchUserData(auth.currentUser);
    }
  }, [fetchUserData]);

  useEffect(() => {
    const unsubscribe = onAuthStateChanged(auth, async (user) => {
      setCurrentUser(user);
      if (user) {
        await fetchUserData(user);
      } else {
        setUserDocument(null);
      }
      setLoading(false);
    });

    return () => unsubscribe();
  }, [fetchUserData]);

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
