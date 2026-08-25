# MODULE 06 — MARKET FOUNDATION ARCHITECTURE

## 1. System Overview

Module 06 establishes the **Market Master Data Foundation** for **FarmConnectPrices**. It defines official agricultural market locations, market types, status, and supported crop relationships.

This layer acts as the authoritative master reference for trading centers across India (e.g. Mandis, Rythu Bazaars, Wholesale Markets), preparing the system for Module 07 real-time market price data ingestion.

---

## 2. Core Architecture Principles

1. **Read-Only Master Data:**
   - Normal users (`FARMER`, `MEDIATOR_BUYER`, `CUSTOMER`) can search, filter, and inspect market details.
   - Normal users are strictly forbidden from creating, modifying, or deleting master markets or market-crop mappings.

2. **Zero Fabricated Prices:**
   - Module 06 contains **NO pricing, rate trends, historical averages, or fake metrics**.
   - Price panels display explicit UI placeholders: *"Market pricing will be available in Module 07."*

3. **Reuse of Established Master Data:**
   - Reuses Module 05 `locations` (`state`, `district`, `mandal`, `village`, `pincode`) and `crops` master data without duplication.

4. **Mobile-First Discovery:**
   - Includes dedicated mobile bottom sheet drawer (`MarketFilterDrawer`) for touch screens (320px–430px) with minimum 48px touch targets.
   - Desktop layout features inline filters and responsive 3-column card grids.

5. **3D Visual Enhancement:**
   - `MarketNetworkVisual.tsx` provides a lightweight Three.js canvas displaying floating market nodes and network connections without impacting bundle performance.
   - Automatically degrades on mobile (`dpr={1}`) and respects `prefers-reduced-motion`.
