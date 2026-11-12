# FarmConnectPrices — Backup & Disaster Recovery Plan

---

## 1. Objectives & Targets

| Metric | Target | Description |
| :--- | :---: | :--- |
| **RPO (Recovery Point Objective)** | **< 24 Hours** | Maximum acceptable data loss window. Supported via daily automated Cloud Firestore exports. |
| **RTO (Recovery Time Objective)** | **< 2 Hours** | Maximum acceptable downtime to restore service after a catastrophic failure. |

---

## 2. Cloud Firestore Automated Backup Strategy

### A. Scheduled Managed Exports
Google Cloud Firestore supports automated daily backups exported to a Google Cloud Storage (GCS) bucket:

1. **Create Cloud Storage Bucket**:
   ```bash
   gcloud storage buckets create gs://farmconnectprices-firestore-backups \
     --project=farmconnectprices \
     --location=asia-south1 \
     --uniform-bucket-level-access
   ```
2. **Configure Daily Export Schedule (Cloud Scheduler + Cloud Function / gcloud)**:
   ```bash
   gcloud firestore export gs://farmconnectprices-firestore-backups/$(date +%Y-%m-%d)
   ```
3. **Bucket Retention Policy**:
   - Set a lifecycle rule on `gs://farmconnectprices-firestore-backups` to transition objects to Coldline after 30 days and delete after 90 days.

---

## 3. Database Restoration Procedure

In the event of accidental data corruption or loss:

1. **Stop Application Writes**:
   - Temporarily suspend the backend container service on Render / Cloud Run to prevent new transactions during restoration.
2. **Import Firestore Data from Target Snapshot**:
   ```bash
   gcloud firestore import gs://farmconnectprices-firestore-backups/YYYY-MM-DD/
   ```
3. **Verify Data Integrity**:
   - Inspect key collections: `users`, `orders`, `payments`, `deliveries`, `reviews`, `disputes`.
4. **Resume Backend Services**:
   - Start the backend container service.
   - Run `/api/v1/health` smoke tests.

---

## 4. Secret & Key Rotation Protocol

| Secret | Rotation Frequency | Rotation Procedure |
| :--- | :---: | :--- |
| **`FIREBASE_SERVICE_ACCOUNT_JSON`** | Every 180 Days | 1. Generate new key in GCP Console IAM.<br>2. Update `FIREBASE_SERVICE_ACCOUNT_JSON` in container environment.<br>3. Restart backend service.<br>4. Revoke old key in GCP IAM. |
| **`DISPUTE_CURSOR_SECRET`** | Annually or on Compromise | 1. Generate new 256-bit secret.<br>2. Update `DISPUTE_CURSOR_SECRET` in container environment.<br>3. Restart backend service (active pagination cursors will reset to page 1). |

---

## 5. Incident Response & Escalation Protocol

1. **Severity 1 (Service Outage / Complete Downtime)**:
   - Symptoms: `/api/v1/health` returning 5xx or connection refused; frontend unable to reach API.
   - Immediate Actions:
     - Check cloud container logs (Render / Cloud Run) for OOM, crash loops, or missing environment variables.
     - Verify Google Cloud / Firebase status dashboard.
     - If container is unhealthy, redeploy latest known-good Git commit `56e0159` or release tag.
2. **Severity 2 (Firestore Quota Exhaustion — HTTP 503)**:
   - Symptoms: `FirestoreQuotaGuard` circuit breaker in `OPEN` state.
   - Immediate Actions:
     - Check Firestore read/write quota metrics in Google Cloud Console.
     - If exceeding Spark free tier (50k reads/day), upgrade to Blaze plan in Firebase Console.
     - QuotaGuard will automatically probe and recover once quota is restored.
3. **Severity 3 (Authentication Failures)**:
   - Symptoms: HTTP 401 on authenticated routes across multiple users.
   - Immediate Actions:
     - Verify Firebase Project ID and Authorized Domains in Firebase Console.
     - Confirm service account key has not expired or been deleted.
