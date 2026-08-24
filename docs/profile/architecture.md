# Profile & Role Architecture

## System Overview
FarmConnectPrices establishes a clear 3-tier separation of user identity, core user account metadata, and role-specific profile document structures.

```
Firebase Auth Identity (uid, email, emailVerified)
        │
        ▼
   users/{uid} (Core account: uid, displayName, role, status)
        │
        ├─────────────────────────┐
        ▼                         ▼
farmerProfiles/{uid}      buyerProfiles/{uid}
```

## Data Isolation Rationale
- **`users/{uid}`**: Kept lightweight, containing only fundamental identity metadata necessary for system-wide authentication and authorization.
- **Role Profiles (`farmerProfiles/{uid}` / `buyerProfiles/{uid}`)**: Extensible isolated documents housing domain-specific attributes (location foundation, phone number, completion flags) without bloating core account data.
- **Future Extensibility**: Collections for future roles (`middlemanProfiles`, `deliveryProfiles`, `adminProfiles`) can be cleanly plugged into this hierarchy without modifying existing schemas.
