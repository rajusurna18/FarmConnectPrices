# Data Quality & Unit Consistency Policy

## Data Quality Rules
1. **Verified Data Only**: By default, only records with `qualityStatus = VERIFIED` enter market intelligence calculations.
2. **Exclusion of Rejected Data**: Records with `qualityStatus = REJECTED` are strictly excluded.
3. **Unit & Currency Consistency**:
   - Currency must be `INR`.
   - Unit must match target unit (e.g. `QUINTAL`).
   - Incompatible units are excluded from direct comparison rather than being converted implicitly.
