# DATA SOURCES & SEED DATA ARCHITECTURE

## 1. Development Reference Seed Data Policy

All initial seed markets provided in `MarketService.java` serve strictly as **Development and Reference Seed Data**.

- Seed records are NOT labeled as live, real-time, or verified real-world market feeds.
- Seed records map to recognized regional agricultural trading hubs (Guntur, Warangal, Hyderabad, Devanahalli).
- Seed records reuse Module 05 location hierarchy and crop master IDs (`crop-paddy`, `crop-chilli`, `crop-tomato`, `crop-cotton`, `crop-turmeric`).

---

## 2. Zero Fabricated Pricing Guarantee

- Module 06 contains **ZERO price fields, zero modal rate figures, zero fake percentage changes, and zero AI predictions**.
- Future price feeds, historical price charts, and arrival volume telemetry are explicitly isolated to **Module 07 — Real Market Price Data**.
