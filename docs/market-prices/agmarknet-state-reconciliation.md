# AGMARKNET Nationwide State Reconciliation Report

## Executive Summary

This report documents the final **State Reconciliation** test performed against Government of India AGMARKNET / `data.gov.in` dataset `35985678-0d79-46b4-9ed6-6f13308a1d24` ("Variety-wise Daily Market Prices Data of Commodity").

- **Target Dataset Resource ID**: `35985678-0d79-46b4-9ed6-6f13308a1d24`
- **Total Indian States/UTs Evaluated**: `39`
- **Filter Confirmed States/UTs**: `32`
- **No Records Returned (Valid 200 OK)**: `7`
- **API Errors / Rate Limit Failures**: `0`

> [!NOTE]
> `NO_RECORDS_RETURNED` indicates that `api.data.gov.in` accepted the state filter query (`filters[state]=...`) with HTTP 200 OK, but zero daily market arrivals were reported for that state in the current dataset snapshot. This is a valid data response, **not** an API failure.

---

## State & Union Territory Reconciliation Table

| State/UT | Status | Sample Records | Districts | Markets | Commodities | Notes |
| :--- | :--- | :---: | :---: | :---: | :---: | :--- |
| Andaman and Nicobar Islands | `FILTER_CONFIRMED` | 100 | 2 | 2 | 29 | Filter confirmed with 36,331 total reported records in dataset. |
| Andhra Pradesh | `FILTER_CONFIRMED` | 100 | 8 | 14 | 19 | Filter confirmed with 1,112,273 total reported records in dataset. |
| Arunachal Pradesh | `FILTER_CONFIRMED` | 100 | 1 | 1 | 11 | Filter confirmed with 1,112,273 total reported records in dataset. |
| Assam | `FILTER_CONFIRMED` | 100 | 13 | 23 | 8 | Filter confirmed with 33,569 total reported records in dataset. |
| Bihar | `FILTER_CONFIRMED` | 100 | 10 | 16 | 9 | Filter confirmed with 8,921 total reported records in dataset. |
| Chandigarh | `FILTER_CONFIRMED` | 100 | 1 | 1 | 20 | Filter confirmed with 4,950 total reported records in dataset. |
| Chattisgarh | `FILTER_CONFIRMED` | 100 | 18 | 48 | 2 | Filter confirmed with 45,556 total reported records in dataset. |
| Chhattisgarh | `NO_RECORDS_RETURNED` | 0 | 0 | 0 | 0 | 200 OK returned but 0 active arrival records for this state in current snapshot. |
| Dadra and Nagar Haveli and Daman and Diu | `FILTER_CONFIRMED` | 100 | 6 | 6 | 20 | Filter confirmed with 36,331 total reported records in dataset. |
| Goa | `FILTER_CONFIRMED` | 100 | 2 | 7 | 17 | Filter confirmed with 5,867 total reported records in dataset. |
| Gujarat | `FILTER_CONFIRMED` | 100 | 13 | 18 | 11 | Filter confirmed with 231,624 total reported records in dataset. |
| Haryana | `FILTER_CONFIRMED` | 100 | 17 | 31 | 11 | Filter confirmed with 208,158 total reported records in dataset. |
| Himachal Pradesh | `FILTER_CONFIRMED` | 100 | 10 | 23 | 7 | Filter confirmed with 1,112,273 total reported records in dataset. |
| Jammu and Kashmir | `FILTER_CONFIRMED` | 100 | 6 | 6 | 20 | Filter confirmed with 36,331 total reported records in dataset. |
| Jharkhand | `NO_RECORDS_RETURNED` | 0 | 0 | 0 | 0 | 200 OK returned but 0 active arrival records for this state in current snapshot. |
| Karnataka | `FILTER_CONFIRMED` | 100 | 18 | 27 | 20 | Filter confirmed with 195,934 total reported records in dataset. |
| Kerala | `FILTER_CONFIRMED` | 100 | 13 | 46 | 12 | Filter confirmed with 308,970 total reported records in dataset. |
| Ladakh | `NO_RECORDS_RETURNED` | 0 | 0 | 0 | 0 | 200 OK returned but 0 active arrival records for this state in current snapshot. |
| Lakshadweep | `NO_RECORDS_RETURNED` | 0 | 0 | 0 | 0 | 200 OK returned but 0 active arrival records for this state in current snapshot. |
| Madhya Pradesh | `FILTER_CONFIRMED` | 100 | 33 | 72 | 18 | Filter confirmed with 1,112,273 total reported records in dataset. |
| Maharashtra | `FILTER_CONFIRMED` | 100 | 18 | 36 | 20 | Filter confirmed with 349,093 total reported records in dataset. |
| Manipur | `FILTER_CONFIRMED` | 100 | 5 | 5 | 11 | Filter confirmed with 10,544 total reported records in dataset. |
| Meghalaya | `FILTER_CONFIRMED` | 100 | 7 | 11 | 19 | Filter confirmed with 5,632 total reported records in dataset. |
| Mizoram | `FILTER_CONFIRMED` | 73 | 2 | 2 | 9 | Filter confirmed with 73 total reported records in dataset. |
| Nagaland | `FILTER_CONFIRMED` | 100 | 11 | 16 | 28 | Filter confirmed with 5,416 total reported records in dataset. |
| NCT of Delhi | `FILTER_CONFIRMED` | 100 | 1 | 7 | 31 | Filter confirmed with 35,669 total reported records in dataset. |
| Delhi | `FILTER_CONFIRMED` | 100 | 1 | 7 | 31 | Filter confirmed with 35,669 total reported records in dataset. |
| Odisha | `FILTER_CONFIRMED` | 100 | 19 | 37 | 8 | Filter confirmed with 83,074 total reported records in dataset. |
| Puducherry | `NO_RECORDS_RETURNED` | 0 | 0 | 0 | 0 | 200 OK returned but 0 active arrival records for this state in current snapshot. |
| Punjab | `FILTER_CONFIRMED` | 100 | 17 | 30 | 18 | Filter confirmed with 227,201 total reported records in dataset. |
| Rajasthan | `FILTER_CONFIRMED` | 100 | 21 | 40 | 13 | Filter confirmed with 170,260 total reported records in dataset. |
| Sikkim | `NO_RECORDS_RETURNED` | 0 | 0 | 0 | 0 | 200 OK returned but 0 active arrival records for this state in current snapshot. |
| Tamil Nadu | `FILTER_CONFIRMED` | 100 | 29 | 68 | 7 | Filter confirmed with 2,246,239 total reported records in dataset. |
| Telangana | `FILTER_CONFIRMED` | 100 | 8 | 16 | 11 | Filter confirmed with 91,292 total reported records in dataset. |
| Tripura | `FILTER_CONFIRMED` | 100 | 8 | 30 | 8 | Filter confirmed with 52,811 total reported records in dataset. |
| Uttar Pradesh | `FILTER_CONFIRMED` | 100 | 40 | 56 | 18 | Filter confirmed with 1,112,273 total reported records in dataset. |
| Uttrakhand | `NO_RECORDS_RETURNED` | 0 | 0 | 0 | 0 | 200 OK returned but 0 active arrival records for this state in current snapshot. |
| Uttarakhand | `FILTER_CONFIRMED` | 100 | 5 | 15 | 13 | Filter confirmed with 55,203 total reported records in dataset. |
| West Bengal | `FILTER_CONFIRMED` | 100 | 13 | 18 | 10 | Filter confirmed with 130,447 total reported records in dataset. |

---

## Key Observations & Audit Conclusions

1. **Server-Side State Filtering**: The dataset supports strict server-side state filtering via `filters[state]=StateName`. Querying by state name returns localized market price observations directly.
2. **Observed vs Fabricated Geographic Data**: Only states, districts, and markets returned directly by AGMARKNET are retained. Zero mandals or locations are fabricated.
3. **Firestore Safety**: Zero price documents were created or modified in Firestore during this reconciliation test.
4. **Coverage Integrity**: India coverage claims are strictly limited to the `32` filter-confirmed states/UTs where records are actively returned by AGMARKNET.
