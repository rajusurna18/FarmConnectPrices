# Walk-Forward Backtesting Framework

## 🔬 Methodology & Zero Data Leakage

Walk-forward backtesting evaluates model performance by simulating past forecasting conditions without data leakage.

```text
Historical Cutoff Date T_cut
           │
           ▼
    Training Dataset
  (Strictly <= T_cut)
           │
           ▼
   Model Prediction \hat{y}_{T_cut + h}
           │
           ▼
   Actual Observation y_{T_cut + h}
           │
           ▼
   Walk-Forward Residual: e_{m,h} = y_actual - y_pred
```

---

## 🚫 Zero Data Leakage Guarantee

1. **Origin Isolation**: Sliced training set contains **ONLY** observations with $\text{priceDate} \le T_{cut}$.
2. **Origin-Relative Freshness**: Freshness in backtesting is calculated relative to historical forecast origin date ($T_{origin}$), NOT today's date:
   $$D_{gap, backtest} = \text{ChronoUnit.DAYS.between}(\text{latestTrainingObservationDate}, T_{origin})$$
3. **Identical Code Path**: Backtesting invokes the exact same `ForecastModel.forecastPrice(...)` algorithm executed in production.
