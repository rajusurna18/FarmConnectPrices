# Forecast Model Limitations & Future ML Roadmap

## ⚠️ Known Model Limitations

1. **Pure Baseline Assumption**: `WMA_V1` relies strictly on recent historical prices. It does not account for external shocks such as unseasonal rainfall, pest outbreaks, or sudden export policy changes.
2. **Horizon Degradation**: Prediction uncertainty naturally widens as the forecast horizon extends from 1 day to 14 days.
3. **Data Freshness Dependency**: Markets without recent price updates (data gap $> 7$ days) return `INSUFFICIENT_DATA` to prevent misleading recommendations.
4. **Empirical Residual Bounds**: Residual bounds require a minimum of 5 walk-forward backtest samples ($M_h \ge 5$). When fewer samples exist, bounds return `null`.

---

## 🔮 Future ML Roadmap

Future ML models (e.g. `SeasonalForecastModel`, `MachineLearningForecastModel`) can be integrated via the `ForecastModel` interface without changing the REST API contract.

To replace `WMA_V1`, a candidate ML model must demonstrate superior predictive accuracy by achieving lower MAE and MAPE in historical backtests across identical market conditions.
