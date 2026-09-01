# Module 10 — Security & Authorization Architecture

## Backend Security Controls
1. **Server-Derived Owner UID**: `ownerUid` is extracted directly from the verified Firebase ID token (`FirebaseAuthenticationToken.getUid()`). Client-supplied `ownerUid` claims are ignored.
2. **Role Enforcement**: Every endpoint executes `farmService.verifyFarmerRole(ownerUid)`. If the authenticated user's role is not `FARMER` (e.g. `MEDIATOR_BUYER` or `CUSTOMER`), the request is rejected with `403 FORBIDDEN`.
3. **Cross-Tenant Isolation**: A farmer can only access, edit, or delete economic records where `record.ownerUid == authenticatedUid`. Attempting to access another farmer's record throws `AccessDeniedException` (HTTP 403).
4. **Market Price Protection**: Clients cannot override market price values or verification status. Prices are fetched exclusively by backend services from the verified Module 07/09 market price pipeline.

## Firestore Security Rules
```javascript
match /farmEconomics/{recordId} {
  allow read, update, delete: if request.auth != null && request.auth.uid == resource.data.ownerUid;
  allow create: if request.auth != null && request.auth.uid == request.resource.data.ownerUid;
}
```
