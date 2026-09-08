# Listing Lifecycle — Module 15

```text
       DRAFT ──(Publish)──► ACTIVE ──(Pause)──► PAUSED
         ▲                     │                  │
         │                     │                  ▼
     (Re-edit)                 ├──────────────► ACTIVE
                               │
                               ├──(Available Qty = 0)──► SOLD_OUT
                               │
                               └──(Date Expired)──────► EXPIRED
```

## Lifecycle States
- **DRAFT**: Created listing not yet published. Publicly hidden.
- **ACTIVE**: Listing visible in public browse and search.
- **PAUSED**: Temporarily hidden from public browse by farmer.
- **SOLD_OUT**: Available quantity reaches zero.
- **EXPIRED**: Availability date window passed.

## Transition Rules
- `DRAFT → ACTIVE`: Farmer publishes draft.
- `ACTIVE → PAUSED`: Farmer temporarily hides listing.
- `PAUSED → ACTIVE`: Farmer reactivates listing.
- `ACTIVE → SOLD_OUT`: Triggered when `availableQuantity == 0`.
- Deletion is restricted to owned `DRAFT` or unpublished listings.
