# Location Master Architecture — Module 05

## Overview
Location taxonomy follows the standard Indian administrative hierarchy:
```
COUNTRY (India)
   ↓
STATE (e.g. Telangana, Andhra Pradesh, Karnataka)
   ↓
DISTRICT (e.g. Warangal, Guntur, Devanahalli)
   ↓
MANDAL / TEHSIL (e.g. Enumamula, Bahadurpura)
   ↓
VILLAGE (e.g. Desrajupalle)
```

## Security
Farmers select location taxonomy values when registering farms. Master location reference endpoints (`/api/v1/locations`) are read-only. Normal users cannot write or alter master location records.
