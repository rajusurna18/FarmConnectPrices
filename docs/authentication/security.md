# FarmConnectPrices — Authentication Security Policy

Documentation detailing security policies, rules, and token handling practices.

## Security Principles

1. **No Password Storage:** Passwords are never sent to or stored in Spring Boot or Firestore. All credentials are handled exclusively by Firebase Authentication.
2. **Stateless API:** Spring Boot uses `SessionCreationPolicy.STATELESS`. No server-side sessions, cookies, or JSESSIONID headers are used.
3. **No Manual Token Storage:** ID tokens are not manually stored in `localStorage` or `sessionStorage`. They are retrieved dynamically from the Firebase SDK instance (`currentUser.getIdToken()`).
4. **Token Verification:** The backend does not trust any user-supplied `uid` in body/params. User identity is derived strictly from the verified Firebase ID Token.
5. **No Token Logging:** `FirebaseAuthFilter` never logs raw tokens or authorization headers.
6. **Firestore Authorization:** Client access is strictly locked to `users/{userId}` where `request.auth.uid == userId`. Modifying sensitive fields (`role`, `status`, `uid`, `createdAt`) is blocked by security rules.
