# FarmConnectPrices — Authentication Workflows

Detailed breakdown of user registration, login, password reset, email verification, and token verification flows.

## 1. User Registration Flow

```text
User ──► Register Form ──► registerWithEmail() ──► Firebase Auth (createUser)
                                                          │
                                                          ▼
                                                  Create users/{uid} in Firestore
                                                          │
                                                          ▼
                                                  Send Verification Email
                                                          │
                                                          ▼
                                                  Redirect /verify-email
```

1. User submits Full Name, Email, Password, and Password Confirmation on `/register`.
2. `registerWithEmail()` creates the identity in Firebase Authentication.
3. Firebase updates the user's `displayName`.
4. Creates corresponding document in Firestore `users/{uid}` with default `role: "USER"` and `status: "ACTIVE"`.
5. Triggers Firebase verification email.
6. User is redirected to `/verify-email`.

---

## 2. Login & Token Verification Flow

```text
User ──► Login Form ──► loginWithEmail() ──► AuthState Updated ──► Redirect /dashboard
                                                                        │
                                                                 Axios API Request
                                                                Bearer <ID_TOKEN>
                                                                        │
                                                                        ▼
                                                             FirebaseAuthFilter
                                                                        │
                                                                        ▼
                                                             GET /api/v1/users/me
```

1. User enters Email and Password on `/login`.
2. `loginWithEmail()` authenticates against Firebase.
3. Firebase `onAuthStateChanged` updates `AuthContext` state.
4. User accesses `/dashboard`.
5. Frontend calls `GET /api/v1/users/me`. Axios interceptor attaches `Authorization: Bearer <token>`.
6. Backend `FirebaseAuthFilter` verifies token via Firebase Admin SDK and populates `SecurityContext`.
7. `UserController` returns `UserResponse` JSON.

---

## 3. Password Reset Flow

1. User enters Email on `/forgot-password`.
2. `sendPasswordReset()` triggers Firebase password reset email.
3. User receives email containing reset link.

---

## 4. Email Verification Flow

1. On registration or visiting `/verify-email`, status is displayed.
2. User clicks **Resend verification email** to trigger `sendEmailVerificationMail()`.
3. User clicks **I've verified my email** to reload user token state (`refreshUser()`).
