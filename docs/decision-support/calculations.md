# Profitability Formulas & Precision Model

## Monetary Calculations
All monetary calculations are performed in Java using `java.math.BigDecimal` with `RoundingMode.HALF_UP` and a 2-decimal scale. Floating-point arithmetic (`double`/`float`) is strictly avoided for money calculations.

## Core Formulas

### 1. Gross Revenue
$$\text{grossRevenue} = \text{selectedPrice} \times \text{quantity}$$

### 2. Total Selling Costs
$$\text{totalSellingCosts} = \text{transportationCost} + \text{otherSellingCosts}$$

### 3. Estimated Net Realization
$$\text{estimatedNetRealization} = \text{grossRevenue} - \text{totalSellingCosts}$$

If \(\text{estimatedNetRealization} < 0\), the raw negative value is preserved and flagged as an estimated loss (`isLoss = true`). The result is never clamped to zero.

### 4. Net Realization Per Unit
$$\text{netRealizationPerUnit} = \frac{\text{estimatedNetRealization}}{\text{quantity}}$$

### 5. Selling-Cost Break-Even Price
$$\text{sellingCostBreakEvenPrice} = \frac{\text{totalSellingCosts}}{\text{quantity}}$$

*Note*: This represents strictly the market price required to recover explicitly entered selling costs (transportation + handling/fees). It is **not** a farm production-cost break-even price.

## Multi-Market Ranking
In multi-market comparison, markets are evaluated independently and ranked by:
$$\text{estimatedNetRealization} \quad \text{DESC}$$

Markets are labeled transparently (e.g. "Highest estimated net realization based on your entered costs").
