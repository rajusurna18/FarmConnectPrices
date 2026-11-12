# FarmConnectPrices — Pilot Launch Readiness Checklist

---

## 1. Pre-Deployment Quality Gates

- [ ] **Frontend Quality Gate**:
  - [ ] `npm run lint` passes with 0 errors and 0 warnings.
  - [ ] `npm run test` passes with 100% success rate (93/93 tests).
  - [ ] `npm run build` generates production bundle in `dist/` with zero errors.
- [ ] **Backend Quality Gate**:
  - [ ] `mvn test` passes with 100% success rate.
  - [ ] Multi-stage Docker container builds cleanly without local path dependencies.
- [ ] **Security & Environment**:
  - [ ] No `.env` or `service-account.json` files tracked in Git (`git status --short` clean).
  - [ ] Production profile `application-prod.yml` active in container (`SPRING_PROFILES_ACTIVE=prod`).
  - [ ] `DISPUTE_CURSOR_SECRET` configured with a minimum of 32 bytes (256 bits).
  - [ ] CORS allowed origins configured to exact production domains.

---

## 2. Firebase & Cloud Infrastructure

- [ ] Firebase project upgraded to **Blaze Plan** with budget alert set to \$10.00/month.
- [ ] Production frontend domain added to Firebase Authentication **Authorized Domains**.
- [ ] Production `firestore.rules` deployed (`firebase deploy --only firestore:rules`).
- [ ] Production `firestore.indexes.json` deployed (`firebase deploy --only firestore:indexes`).
- [ ] Service Account configured via `FIREBASE_SERVICE_ACCOUNT_JSON` environment variable.

---

## 3. Three-Profile Pilot Functional Verification

### A. Profile: FARMER
- [ ] **Onboarding & Profile Setup**: Can sign in, view role indicator as `FARMER`, edit profile, and manage farm details.
- [ ] **Produce Management**: Can register farms, add crop plots, and configure crop varieties.
- [ ] **Listing Creation**: Can create marketplace produce listings with price, minimum order quantity, and location.
- [ ] **Negotiation & Offers**: Can review incoming counter-offers from buyers, accept or reject offers.
- [ ] **Order Fulfillment**: Can view confirmed orders, track order processing, and initiate delivery handover.
- [ ] **Dispute & Issues**: Can report issues on orders, respond to buyer dispute messages, and view audit trail.

### B. Profile: MEDIATOR / BUYER (`MEDIATOR_BUYER`)
- [ ] **Market Intelligence & Price Comparison**: Can browse real-time mandi prices, filter by commodity/district, and view price trend forecasts.
- [ ] **Marketplace Browse**: Can search active produce listings, inspect quality grades, and filter by radius.
- [ ] **Offer Submission**: Can submit structured negotiation proposals with offered unit price and quantity.
- [ ] **Payment & Delivery**: Can checkout order payment, monitor transit status, and confirm delivery receipt.
- [ ] **Reviews & Rating**: Can submit transparent quality reviews and seller feedback.
- [ ] **Disputes**: Can file a dispute on damaged or delayed goods, submit evidence metadata, and voluntarily close dispute upon settlement.

### C. Profile: CUSTOMER
- [ ] **Direct Produce Purchase**: Can browse verified farmer listings and negotiate purchase quantities.
- [ ] **Order & Payment Tracking**: Can track order lifecycle from `PENDING` to `COMPLETED`.
- [ ] **Delivery Tracking**: Can view real-time logistics partner and delivery status updates.
- [ ] **Feedback & Disputes**: Can rate order experience, report missing/substandard produce, and track resolution timeline.

---

## 4. Post-Launch Operations & Health Monitoring

- [ ] **Liveness Check**: Verify `GET /api/v1/health` returns HTTP 200 `{"status":"UP"}`.
- [ ] **Firebase Check**: Verify `GET /api/v1/health/firebase` returns HTTP 200 with initialized state.
- [ ] **Circuit Breaker Status**: Monitor `FirestoreQuotaGuard` metrics to verify normal `CLOSED` operation.
- [ ] **Logging Review**: Verify backend logs show zero sensitive credential prints, tokens, or raw PII.
- [ ] **Rollback Plan Verified**: If severe defects emerge, immediate rollback to commit `56e0159` is documented and tested.
