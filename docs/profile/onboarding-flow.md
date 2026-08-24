# Onboarding & Role Selection Flow

## Flow Diagram

```
User Registration / Login
           │
           ▼
 Authenticated Route Guard
           │
           ├──────────────────────────────┐
           ▼                              ▼
     Role = "USER"             Role = "FARMER" | "BUYER"
           │                              │
           ▼                              ▼
  Redirect to /onboarding/role        Direct to /profile / /dashboard
           │
           ▼
Select Role (FARMER / BUYER)
           │
           ▼
 PUT /api/v1/profile/role
           │
           ▼
Redirect to /profile/edit (Complete Profile)
```

## Profile Completion Criteria
`profileCompleted` is set to `true` when all of the following are non-empty:
1. `phoneNumber`
2. `location.state`
3. `location.district`
4. `location.mandal`
5. `location.village`
