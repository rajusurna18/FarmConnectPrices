# Role Security & Authorization Documentation

## Allowed Onboarding Roles
`PUT /api/v1/profile/role` accepts **strictly**:
- `FARMER`
- `MEDIATOR_BUYER`
- `CUSTOMER`

## Forbidden Roles
Requests attempting to claim `ADMIN`, `DELIVERY_PARTNER`, `MIDDLEMAN`, `BUYER`, or invalid roles are rejected with HTTP 400 Bad Request.

## Security Rules Enforcement
- `users/{userId}`: Ownership check (`request.auth.uid == userId`) prevents client modification of `role`, `status`, `uid`, or `createdAt`.
- `farmerProfiles/{userId}`, `mediatorBuyerProfiles/{userId}`, `customerProfiles/{userId}`: Strict UID ownership match. Default deny on all other collections.
