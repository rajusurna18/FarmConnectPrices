# Forecast Model Specification (`WEIGHTED_MOVING_AVERAGE_V1`)

## 📐 Mathematical Model Overview

Model Identifier: `WEIGHTED_MOVING_AVERAGE_V1` (`WMA_V1`)

`WMA_V1` is a pure, transparent Weighted Moving Average baseline that gives higher weights to recent verified observations without arbitrary drift tuning parameters.

---

## 🔢 Lookback Window & Weight Normalization

- **Lookback Window**: Default 30 calendar days preceding the latest observation date ($T_{latest}$).
- **Observations Used ($K$)**: Up to $K = 14$ most recent verified observations within the lookback window.
- **Linear Decay Weights**:
  $$w_i = i \quad \text{for } i \in \{1 \dots K\}$$
  Where $i=K$ represents the newest observation.
- **Weight Normalization**:
  $$\bar{w}_i = \frac{w_i}{\sum_{j=1}^K w_j}$$

---

## 🔄 Pure Recursive Multi-Horizon Equation

For horizon $h \in \{1, 3, 7, 14\}$ calendar days:
- **1-Day Step ($h = 1$)**:
  $$\hat{y}_{t+1} = \sum_{i=1}^K \bar{w}_i \cdot y_i$$
- **Multi-Step Recursive ($k \in \{2 \dots h\}$)**:
  $$\hat{y}_{t+k} = \sum_{i=1}^{K-k+1} \bar{w}_i \cdot y_{i+k-1} + \sum_{j=1}^{k-1} \bar{w}_{K-k+1+j} \cdot \hat{y}_{t+j}$$

---

## 📅 Calendar-Day Horizon & Missing-Date Semantics

- **Calendar-Day Projection**: Forecast horizons represent projections for $T_{latest} + h$ calendar days into the future, **NOT** observation index steps.
- **No Data Fabrication**: Missing dates within the lookback window are **NEVER** filled with fabricated or interpolated prices. The raw, verified historical sequence is preserved intact.
- **Minimum Observations ($M_{min}$)**:
  - `1_DAY`: Minimum 5 valid observations
  - `3_DAYS`: Minimum 5 valid observations
  - `7_DAYS`: Minimum 7 valid observations
  - `14_DAYS`: Minimum 10 valid observations
  - If observations $< M_{min}(h)$, return `confidence: INSUFFICIENT_DATA` and `forecastPrice: null`.
