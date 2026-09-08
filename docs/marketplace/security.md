# Marketplace Security & Authorization — Module 15

## Role Access Matrix

| Action | FARMER | MEDIATOR / BUYER | CUSTOMER |
|---|:---:|:---:|:---:|
| Browse active listings | ✅ | ✅ | ✅ |
| View listing detail | ✅ | ✅ | ✅ |
| Create listing | ✅ | ❌ | ❌ |
| View own listings (`/mine`) | ✅ | ❌ | ❌ |
| Edit own listing | ✅ | ❌ | ❌ |
| Change status of own listing | ✅ | ❌ | ❌ |
| Delete own draft listing | ✅ | ❌ | ❌ |
| Edit another farmer's listing | ❌ | ❌ | ❌ |
| Delete another farmer's listing | ❌ | ❌ | ❌ |

## Firestore Security Rules
Added to `firestore.rules`:
```firestore
match /productListings/{listingId} {
  allow read: if request.auth != null && (resource.data.status == "ACTIVE" || resource.data.ownerUid == request.auth.uid);
  allow create: if request.auth != null && request.resource.data.ownerUid == request.auth.uid;
  allow update, delete: if request.auth != null && resource.data.ownerUid == request.auth.uid;
}
```

## Server-Side Ownership Enforcement
Seller identity `ownerUid` is derived strictly from `FirebaseAuthenticationToken.getUid()`. Any `ownerUid` or `farmerId` supplied in client request payloads is ignored.
