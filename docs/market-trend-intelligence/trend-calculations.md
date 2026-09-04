# Trend Calculations & Mathematical Formulas

This document details the exact mathematical formulas, precision requirements, threshold configurations, and edge case handling implemented in `MarketTrendService.java`.

## Data Types & Rounding Behavior

- **Data Types**: All monetary values, percentage changes, standard deviations, and averages are calculated using `BigDecimal` or rounded cleanly using `RoundingMode.HALF_UP` to **2 decimal places**.
- **Observation Filtering**: Only records where `qualityStatus == 'VERIFIED'`, non-negative prices, and `minPrice <= modalPrice <= maxPrice` are included.

## Core Formulas

### 1. Percentage Change
$$\text{percentageChange} = \left(\frac{\text{latestPrice} - \text{earliestPrice}}{\text{earliestPrice}}\right) \times 100$$
- **Zero Denominator Protection**: If $\text{earliestPrice} \le 0.0$, $\text{percentageChange}$ is returned as `null`.

### 2. Absolute Change
$$\text{absoluteChange} = \text{latestPrice} - \text{earliestPrice}$$

### 3. Price Range
$$\text{priceRange} = \text{maximumPrice} - \text{minimumPrice}$$

### 4. Arithmetic Mean ($\mu$)
$$\mu = \frac{\sum_{i=1}^N \text{modalPrice}_i}{N}$$

### 5. Population Standard Deviation ($\sigma$)
$$\sigma = \sqrt{\frac{\sum_{i=1}^N (\text{modalPrice}_i - \mu)^2}{N}}$$

### 6. Coefficient of Variation ($CV$) & Volatility Classification
$$CV = \left(\frac{\sigma}{\mu}\right) \times 100\%$$

- **Zero Mean Handling**: If $\mu == 0.0$, $CV = 0.0\%$.
- **Insufficient Observations**: If $N < 3$, Volatility is reported as `INSUFFICIENT_DATA`.
- **Classification Boundaries**:
  - $CV < 5.0\%$ $\rightarrow$ `LOW`
  - $5.0\% \le CV \le 15.0\%$ $\rightarrow$ `MEDIUM`
  - $CV > 15.0\%$ $\rightarrow$ `HIGH`

## Configurable Trend Thresholds

Configured via `application.yml`:
- `trend.rising-threshold-percent`: `2.0`
- `trend.falling-threshold-percent`: `2.0`

### Classification Rules:
- If $N < 2$ $\rightarrow$ `INSUFFICIENT_DATA`
- If $\text{percentageChange} > +2.0\%$ $\rightarrow$ `RISING`
- If $\text{percentageChange} < -2.0\%$ $\rightarrow$ `FALLING`
- If $-2.0\% \le \text{percentageChange} \le +2.0\%$ $\rightarrow$ `STABLE`
