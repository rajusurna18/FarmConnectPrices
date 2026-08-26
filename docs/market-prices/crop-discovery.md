# Crop Master Discovery vs. Price Availability

## Overview
FarmConnectPrices separates canonical Crop Master discovery from daily AGMARKNET price observation availability.

## Concepts
1. **Canonical Crop Master**:
   - Service: `CropMasterService.java`
   - Firestore Collection: `/crops/{cropId}`
   - Represents all platform crops (Paddy, Wheat, Maize, Cotton, Red Chilli, Tomato, Onion, Groundnut, Turmeric, Sugarcane, Pulses, Mango).
2. **Price Availability**:
   - Derived from `/marketPrices/{priceId}` records.
   - Shows which crops currently have active AGMARKNET price observations for a selected market or date.

## Commodity Mapping
External AGMARKNET commodity strings (e.g. "Chilli Red", "Paddy(Dhan)") are mapped to canonical crop IDs via `MandiMappingService`. Unknown commodities are logged and skipped without polluting the Crop Master.
