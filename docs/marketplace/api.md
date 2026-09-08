# Marketplace API Specification — Module 15

Base Path: `/api/v1/marketplace/listings`

## Endpoints

### 1. Create Listing
- **`POST /api/v1/marketplace/listings`**
- **Auth**: Required (`FARMER` role)
- **Request Body**: `CreateListingRequest`
- **Response**: `201 Created` with `ProductListingResponse`

### 2. Public Browse Active Listings
- **`GET /api/v1/marketplace/listings`**
- **Auth**: Required (`FARMER`, `MEDIATOR_BUYER`, `CUSTOMER`)
- **Query Params**:
  - `cropId` (optional)
  - `state` (optional)
  - `district` (optional)
  - `minPrice` (optional)
  - `maxPrice` (optional)
  - `sortBy` (`newest`, `price_asc`, `price_desc`, `quantity_desc`)
  - `page` (default 0)
  - `size` (default 20, max 50)
- **Response**: `200 OK` with `ListingPageResponse`

### 3. Get Farmer's Own Listings
- **`GET /api/v1/marketplace/listings/mine`**
- **Auth**: Required (`FARMER` role)
- **Query Params**: `status` (`ALL`, `ACTIVE`, `PAUSED`, `DRAFT`, `SOLD_OUT`, `EXPIRED`), `page`, `size`
- **Response**: `200 OK` with `ListingPageResponse`

### 4. Get Listing Detail
- **`GET /api/v1/marketplace/listings/{listingId}`**
- **Auth**: Required
- **Response**: `200 OK` with `ProductListingResponse` (Visibility policy enforced: non-ACTIVE listings return 403 if requested by non-owner).

### 5. Update Listing
- **`PUT /api/v1/marketplace/listings/{listingId}`**
- **Auth**: Required (`FARMER` owner)
- **Request Body**: `UpdateListingRequest`
- **Response**: `200 OK` with `ProductListingResponse`

### 6. Update Status
- **`PATCH /api/v1/marketplace/listings/{listingId}/status`**
- **Auth**: Required (`FARMER` owner)
- **Request Body**: `UpdateListingStatusRequest` (`status`)
- **Response**: `200 OK` with `ProductListingResponse`

### 7. Delete Listing
- **`DELETE /api/v1/marketplace/listings/{listingId}`**
- **Auth**: Required (`FARMER` owner)
- **Response**: `204 No Content`
