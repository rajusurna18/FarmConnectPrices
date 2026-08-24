# Role Security & Authorization Model

## Threat Prevention & Security Model

1. **Client Identity Non-Trust**: The client never supplies a `uid` parameter in request bodies or query params to establish identity. The Spring Boot backend derives UID strictly from the verified Firebase ID Token stored in `SecurityContextHolder`.
2. **Privilege Escalation Defense**:
   - `PUT /api/v1/profile/role` accepts **only** `FARMER` or `BUYER`. Requests attempting to claim `ADMIN`, `MIDDLEMAN`, or `DELIVERY_PARTNER` are rejected with HTTP 400 Bad Request.
   - `firestore.rules` enforces that clients cannot modify `role`, `status`, `uid`, or `createdAt` fields directly on `users/{userId}` documents.
3. **Cross-Tenant Access Denial**:
   - Users can only read and write their own documents (`farmerProfiles/{userId}` and `buyerProfiles/{userId}`) where `request.auth.uid == userId`.
   - Default deny rule remains active for all unspecified paths.
