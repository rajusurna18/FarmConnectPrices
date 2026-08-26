# Market Discovery & Master Architecture

## Overview
Markets are managed as platform master entities within FarmConnectPrices.

## Architecture
- **Service**: `MarketService.java`
- **Firestore Collection**: `/markets/{marketId}`
- **Fallback**: Reference market master list (`DEFAULT_MARKETS`).

## Market Availability Fallback Rule
When a user selects a State, District, and optional Mandal/Area:
1. **Mandal Markets Available**: If markets exist matching `State + District + Mandal`, show mandal-specific markets.
2. **Mandal Markets Unavailable**: If no markets match the specific Mandal, fall back gracefully to showing district-level markets (`State + District`) with a clear notice:
   > *"Mandal/Area-specific market data unavailable. Showing markets for the selected district."*
