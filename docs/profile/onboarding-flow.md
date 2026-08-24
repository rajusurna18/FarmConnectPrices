# User Onboarding & Profile Completion Flow

## Role Assignment Flow
```
Registration / Authentication
             │
             ▼
        users/{uid}
             │
     Role missing/USER?
    ┌────────┴────────┐
   YES               NO
    │                 │
    ▼                 ▼
/onboarding/role   /profile
 (FARMER / MEDIATOR_BUYER / CUSTOMER)
```

## Profile Completion (`profileCompleted: boolean`)
- **FARMER**: Phone number + state + district + mandal + village.
- **MEDIATOR_BUYER**: Phone number + state + district + mandal + village + `businessOrganizationName`.
- **CUSTOMER**: Phone number + state + district + mandal + village + `address`.
