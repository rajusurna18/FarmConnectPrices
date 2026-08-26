# AGMARKNET Market Price Unit Normalization & Conversion Architecture

## Overview
FarmConnectPrices receives official wholesale agricultural price observations from Government of India AGMARKNET via data.gov.in. AGMARKNET observations are supplied primarily as **₹ / QUINTAL**. 

To provide optimal usability for farmers, buyers, and consumers, FarmConnectPrices preserves the original AGMARKNET observation while allowing users to view prices normalized across three standard agricultural mass units:
1. **₹ / KG**
2. **₹ / QUINTAL**
3. **₹ / TONNE**

---

## Unit Definitions & Conversion Formulas

| Unit | Symbol | Relation to QUINTAL | Relation to KG |
| :--- | :--- | :--- | :--- |
| **QUINTAL** | `QUINTAL` | $1\text{ QUINTAL} = 100\text{ KG}$ | $100\text{ KG}$ |
| **KG** | `KG` | $1\text{ KG} = 0.01\text{ QUINTAL}$ | $1\text{ KG}$ |
| **TONNE** | `TONNE` | $1\text{ TONNE} = 10\text{ QUINTAL}$ | $1000\text{ KG}$ |

### Exact Conversion Matrix:
- **QUINTAL $\rightarrow$ KG**: $\text{Price} \div 100$
- **QUINTAL $\rightarrow$ TONNE**: $\text{Price} \times 10$
- **KG $\rightarrow$ QUINTAL**: $\text{Price} \times 100$
- **KG $\rightarrow$ TONNE**: $\text{Price} \times 1000$
- **TONNE $\rightarrow$ QUINTAL**: $\text{Price} \div 10$
- **TONNE $\rightarrow$ KG**: $\text{Price} \div 1000$

---

## Key Principles & Data Policies

1. **Single Observation Principle**:
   - Firestore `/marketPrices/{priceId}` stores exactly **one** observation document representing the original AGMARKNET report (`unit: "QUINTAL"`).
   - Converted units are **never** stored as duplicate Firestore documents.
2. **Source Transparency**:
   - Every converted API response exposes `unit` (display unit), `sourceUnit` (original AGMARKNET unit), `conversionApplied` (boolean), and `conversionFactor`.
   - The UI displays a clear note: *"Converted from AGMARKNET ₹20,500 / Quintal"* whenever `conversionApplied = true`.
3. **Precision & Financial Safety**:
   - Conversions use Java `BigDecimal` arithmetic with `HALF_UP` rounding scaled to 2 decimal places.
4. **Unsupported Units**:
   - If an observation contains an unknown/unsupported unit (e.g. `BUNCH`), no conversion is attempted; original values are returned with `conversionApplied = false`.
