# Firestore Production Quota & Billing Strategy Guide

## Executive Summary
This guide outlines the production quota options for **FarmConnectPrices**. It documents the Google Cloud / Firebase Firestore Free Tier limits, details billing options, and provides an architectural roadmap for operating within or beyond default daily limits.

---

## 1. Firebase Firestore Free Tier Specifications

The Google Cloud / Firebase Standard Free Tier (Spark Plan) provides the following daily limits per project:

- **Document Reads**: 50,000 document reads / day
- **Document Writes**: 20,000 document writes / day
- **Document Deletes**: 20,000 document deletes / day
- **Stored Data**: 1 GiB total storage
- **Outbound Network**: 10 GiB / month

When daily document reads exceed 50,000, GCP Firestore returns `RESOURCE_EXHAUSTED`. `FirestoreQuotaGuard` converts this into controlled HTTP 503 SERVICE_UNAVAILABLE responses, preventing application crashes or invalid fake data rendering.

---

## 2. Production Architecture Options

### Option A: Free Tier Operation (50,000 Reads/Day Limit)
- **Target Workload**: Development, staging, and small-scale testing.
- **Architectural Safeguards**:
  - Spring `@Cacheable(sync = true)` active on `crops`, `markets`, `states`, `districts`.
  - Single Probe Owner (`HALF_OPEN`) circuit breaker preventing probe stampedes.
  - React Query 30-minute master data cache with `defaultRetry` (0 retries on 503).
  - Explicit limits (`limit(100)`) on market price queries.

### Option B: Enable Firebase Blaze Plan (Pay-As-You-Go)
- **Target Workload**: High-concurrency production deployments.
- **Cost Structure** (GCP Standard Pricing):
  - Document Reads: \$0.06 per 100,000 document reads.
  - Document Writes: \$0.18 per 100,000 document writes.
- **Activation Step**:
  1. Open [Firebase Console Project Overview](https://console.firebase.google.com/).
  2. Select **Upgrade Plan** -> **Blaze (Pay as you go)**.
  3. Attach Google Cloud Billing Account.
  4. Set GCP Budget Alerts (e.g., \$10/month alert).

### Option C: Precomputed Aggregates / Read Models
- **Target Workload**: Scaled analytics over millions of historical price records.
- **Architecture**:
  - Mandi ingestion computes daily summary documents (e.g., `dailyPriceSummaries/YYYY-MM-DD`).
  - Frontend queries precomputed summaries (1 document read per view) instead of scanning raw observations.

---

## 3. Quota Recovery Lifecycle

```
           [CLOSED] Normal Operation (Live Firestore Stream)
                              │
                    RESOURCE_EXHAUSTED
                              ▼
           [OPEN] Cooldown Active (HTTP 503, 0 Firestore calls)
                              │
                     60-second Expiry
                              ▼
     [HALF_OPEN] Single Probe Owner (1 Thread calls Firestore)
                   ┌──────────┴──────────┐
                Success               Failure
                   │                     │
                   ▼                     ▼
          Transition to CLOSED    Return to OPEN (New Cooldown)
```

---

## 4. Manual Console Action Required for Billing Upgrade
Application source code **cannot** increase Google's Firestore quota limit. To upgrade quota for production traffic, navigate to [Firebase Console Billing Settings](https://console.firebase.google.com/) and upgrade to the Blaze Plan.
