# Limitations & Out-of-Scope Capabilities — Module 15

## Scope Boundary
Module 15 is strictly the **Marketplace Product Catalog & Listing Foundation**.

## Explicitly Out of Scope
The following features are intentionally reserved for future marketplace modules and are **NOT** implemented in Module 15:
- Orders and Checkout
- Payments, Escrow, and Transaction Settlement
- Negotiation, Bidding, and Offers
- Buyer-Seller Direct Messaging / Chat
- Logistics Booking and Delivery
- Buyer/Seller Ratings and Reviews
- AI Autonomous Selling or Automated Price Setting
- Concurrency-safe Transactional Inventory Reservation

## Informational `availableQuantity`
In Module 15, `availableQuantity` is informational seller-provided availability. It is not transactionally locked or reserved by concurrent users. Safe concurrent inventory updates will be introduced in future order modules.
