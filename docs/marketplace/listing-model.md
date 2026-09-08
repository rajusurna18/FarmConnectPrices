# Listing Model — Module 15

## ProductListing Field Definitions

| Field | Type | Description | Protected |
|---|---|---|---|
| `listingId` | String | Unique identifier (`listing_<uuid>`) | Yes |
| `ownerUid` | String | Authenticated Firebase UID of seller | Yes |
| `farmerProfileId` | String | Associated farmer profile identifier | Yes |
| `cropId` | String | Reference to Crop Master ID | Yes (on edit) |
| `cropName` | String | Display name derived from Crop Master | Yes |
| `quantity` | Double | Total listed quantity (> 0) | No |
| `availableQuantity` | Double | Informational availability (>= 0, <= quantity) | No |
| `unit` | String | Unit of measure (`QUINTAL`, `KG`, `TON`, `BAG`, `PIECE`, `LITRE`) | No |
| `askingPrice` | Double | Farmer-entered seller asking price (> 0) | No |
| `priceUnit` | String | Unit for asking price | No |
| `location` | LocationDto | Regional location (state, district, mandal, village) | No |
| `description` | String | Optional seller notes | No |
| `qualityGrade` | String | `PREMIUM`, `GRADE_A`, `GRADE_B`, `STANDARD`, `UNSPECIFIED` | No |
| `harvestDate` | String | Optional ISO harvest date (`YYYY-MM-DD`) | No |
| `availableFrom` | String | Optional ISO availability start date | No |
| `status` | String | `DRAFT`, `ACTIVE`, `PAUSED`, `SOLD_OUT`, `EXPIRED` | No |
| `createdAt` | String | Server-generated timestamp | Yes |
| `updatedAt` | String | Server-generated timestamp | Yes |
