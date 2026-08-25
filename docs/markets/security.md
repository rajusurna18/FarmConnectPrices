# SECURITY SPECIFICATION — MARKET MASTER DATA

## 1. Role Access Matrix

| Role | Read Markets | Filter Markets | View Market Crops | Write / Create / Edit Markets |
| :--- | :---: | :---: | :---: | :---: |
| **FARMER** | ✅ Allowed | ✅ Allowed | ✅ Allowed | ❌ Forbidden |
| **MEDIATOR_BUYER** | ✅ Allowed | ✅ Allowed | ✅ Allowed | ❌ Forbidden |
| **CUSTOMER** | ✅ Allowed | ✅ Allowed | ✅ Allowed | ❌ Forbidden |
| **Unauthenticated** | ❌ Forbidden | ❌ Forbidden | ❌ Forbidden | ❌ Forbidden |

---

## 2. Security Enforcement Mechanisms

1. **Spring Security Layer ([`SecurityConfig.java`](file:///c:/Users/rajus/OneDrive/Desktop/FarmConnectPrices/backend/src/main/java/com/farmlink/api/config/SecurityConfig.java)):**
   - Matches `/api/v1/markets/**` requiring `.authenticated()`.
   - Rejects unauthenticated requests with `401 Unauthorized`.
   - Exposes zero mutation endpoints (`POST`, `PUT`, `DELETE`) for markets.

2. **Firestore Rules Layer ([`firestore.rules`](file:///c:/Users/rajus/OneDrive/Desktop/FarmConnectPrices/firestore.rules)):**
   - `match /markets/{marketId}`: `allow read: if request.auth != null; allow write: if false;`
   - `match /marketCrops/{marketCropId}`: `allow read: if request.auth != null; allow write: if false;`
   - Prevents normal users from tampering with market master data directly via client SDKs.
