# Ownership & Security Architecture — Module 05

## Core Security Rules
1. **Server-Side Token Ownership**: `ownerUid` is derived strictly from `FirebaseAuthenticationToken.getUid()`. Client-supplied `ownerUid` fields are ignored.
2. **Cross-User Authorization**: Any request by Farmer A to read, modify, or delete a farm owned by Farmer B returns `HTTP 403 Forbidden`.
3. **Role Enforcement**: Only users whose Firestore `users/{uid}` record has `role == 'FARMER'` can access `/api/v1/farms/**`. Users with `MEDIATOR_BUYER` or `CUSTOMER` roles receive `HTTP 403 Forbidden`.
4. **Firestore Rules**:
   - `farms`: `allow read, update, delete: if request.auth.uid == resource.data.ownerUid;`
   - `farmCrops`: `allow read, update, delete: if request.auth.uid == resource.data.ownerUid;`
   - `crops`: `allow read: if request.auth != null; allow write: if false;`
   - `locations`: `allow read: if request.auth != null; allow write: if false;`
