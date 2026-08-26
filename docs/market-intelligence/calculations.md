# Market Intelligence Mathematical Calculations

## Formula Specifications

### 1. Price Difference
```
priceDifference = highestModalPrice - lowestModalPrice
```

### 2. Percentage Difference
```
percentageDifference = ((highestModalPrice - lowestModalPrice) / lowestModalPrice) * 100
```
- **Zero Denominator Handling**: If `lowestModalPrice == 0`, returns `null`.

### 3. Absolute & Percentage Change
```
absoluteChange = latestModalPrice - firstModalPrice
percentageChange = ((latestModalPrice - firstModalPrice) / firstModalPrice) * 100
```
- **Zero Denominator Handling**: If `firstModalPrice == 0`, returns `null`.

### 4. Average Modal Price
```
averageModalPrice = sum(modalPrice) / numberOfObservations
```

### 5. Trend Direction Classification
- `INCREASING`: `latestModalPrice > firstModalPrice`
- `DECREASING`: `latestModalPrice < firstModalPrice`
- `STABLE`: `latestModalPrice == firstModalPrice`
- `INSUFFICIENT_DATA`: Fewer than 2 observation points.

## Prohibition of AI/Predictive Models
All calculations are strictly deterministic functions. No machine learning or statistical forecasting is performed.
