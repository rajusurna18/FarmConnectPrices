/**
 * Maps raw Firebase authentication error codes to user-friendly error messages with diagnostic code context.
 */
export const getAuthErrorMessage = (error: unknown): string => {
  if (!error || typeof error !== 'object') {
    return 'An unexpected error occurred. Please try again.';
  }

  const code = (error as { code?: string }).code;
  const rawMessage = (error as { message?: string }).message;

  if (code) {
    console.error(`[FirebaseAuth] Authentication error code: ${code}`, rawMessage);
  } else {
    console.error('[FirebaseAuth] Authentication error:', error);
  }

  switch (code) {
    case 'auth/invalid-credential':
    case 'auth/user-not-found':
    case 'auth/wrong-password':
      return 'Invalid email address or password (auth/invalid-credential). Please check your credentials.';
    case 'auth/email-already-in-use':
      return 'An account with this email address already exists (auth/email-already-in-use). Please log in instead.';
    case 'auth/weak-password':
      return 'Password is too weak (auth/weak-password). Please use at least 6 characters.';
    case 'auth/invalid-email':
      return 'Please enter a valid email address (auth/invalid-email).';
    case 'auth/operation-not-allowed':
      return 'Email/Password sign-in is not enabled in Firebase Console (auth/operation-not-allowed).';
    case 'auth/invalid-api-key':
      return 'Invalid Firebase API key (auth/invalid-api-key). Please verify environment configuration.';
    case 'auth/too-many-requests':
      return 'Too many unsuccessful attempts (auth/too-many-requests). Please try again later.';
    case 'auth/user-disabled':
      return 'This user account has been disabled (auth/user-disabled). Please contact support.';
    case 'auth/network-request-failed':
      return 'Network connection failed (auth/network-request-failed). Please check your internet connection.';
    case 'auth/requires-recent-login':
      return 'Please re-authenticate and try again (auth/requires-recent-login).';
    default:
      return code
        ? `Authentication failed [${code}]: ${rawMessage || 'Please verify details and try again.'}`
        : rawMessage || 'An unexpected error occurred during authentication.';
  }
};
