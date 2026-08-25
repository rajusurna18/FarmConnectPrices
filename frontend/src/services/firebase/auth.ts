import {
  createUserWithEmailAndPassword,
  signInWithEmailAndPassword,
  signOut,
  sendPasswordResetEmail,
  sendEmailVerification,
  updateProfile,
} from 'firebase/auth';
import type { UserCredential, User } from 'firebase/auth';
import { auth } from '../../config/firebase';
import { saveDocument } from './firestore';
import { serverTimestamp } from 'firebase/firestore';

/**
 * Register a new user with Email and Password, create the corresponding Firestore user document,
 * update the Firebase profile displayName, and trigger an email verification email.
 */
export const registerWithEmail = async (
  email: string,
  pass: string,
  displayName: string
): Promise<UserCredential> => {
  const credential = await createUserWithEmailAndPassword(auth, email, pass);
  const user = credential.user;

  // Update Auth profile with displayName
  if (displayName) {
    await updateProfile(user, { displayName });
  }

  // Attempt to create user document in Firestore users/{uid} (Backend API creates/initializes on profile request)
  try {
    await saveDocument('users', user.uid, {
      uid: user.uid,
      displayName: displayName || user.email?.split('@')[0] || 'User',
      email: user.email,
      emailVerified: user.emailVerified,
      role: 'USER',
      status: 'ACTIVE',
      createdAt: serverTimestamp(),
      updatedAt: serverTimestamp(),
    });
  } catch (error) {
    console.info('[Firebase Auth] Direct Firestore client write skipped (managed via Spring Boot backend Profile API):', error);
  }

  // Send verification email
  try {
    await sendEmailVerification(user);
  } catch (error) {
    console.warn('[Firebase Auth] Failed to send initial verification email:', error);
  }

  return credential;
};

/**
 * Sign in existing user with Email and Password
 */
export const loginWithEmail = async (email: string, pass: string): Promise<UserCredential> => {
  return await signInWithEmailAndPassword(auth, email, pass);
};

/**
 * Sign out current authenticated user
 */
export const logoutUser = async (): Promise<void> => {
  await signOut(auth);
};

/**
 * Send password reset email
 */
export const sendPasswordReset = async (email: string): Promise<void> => {
  await sendPasswordResetEmail(auth, email);
};

/**
 * Send verification email to currently signed in user
 */
export const sendEmailVerificationMail = async (user?: User | null): Promise<void> => {
  const targetUser = user || auth.currentUser;
  if (!targetUser) {
    throw new Error('No active user to send verification email to.');
  }
  await sendEmailVerification(targetUser);
};

/**
 * Get current user's Firebase ID token for Authorization headers
 */
export const getCurrentIdToken = async (forceRefresh = false): Promise<string | null> => {
  const currentUser = auth.currentUser;
  if (!currentUser) {
    return null;
  }
  return await currentUser.getIdToken(forceRefresh);
};
