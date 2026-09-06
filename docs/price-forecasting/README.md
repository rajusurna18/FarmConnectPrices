# Module 13 — Price Prediction & Forecasting Foundation

Welcome to the **Price Prediction & Forecasting Foundation** documentation for **FarmConnectPrices**.

This module introduces a controlled, verifiable future-price forecasting foundation. Based on historical verified market-price data, the system estimates future price ranges for supported horizons (1, 3, 7, and 14 days), backtests performance metrics (MAE, MAPE), computes deterministic confidence levels, provides AI-driven explanations without false certainty, and enables farmers to run read-only scenario profitability evaluations.

---

## 📚 Table of Contents

1. [Architecture Overview](architecture.md)
2. [Forecast Model Specification (`WMA_V1`)](forecast-model.md)
3. [Walk-Forward Backtesting Framework](backtesting.md)
4. [Error Metrics (MAE & MAPE)](metrics.md)
5. [Deterministic Confidence & Empirical Residual Uncertainty Bounds](confidence.md)
6. [REST API Documentation](api.md)
7. [Limitations & Roadmap](limitations.md)

---

## 🎯 Core Principles

1. **Estimate, Not Guarantee**: All UI elements, APIs, and AI explanations explicitly communicate forecasts as statistical estimates with empirical uncertainty bounds.
2. **Authoritative Market Data**: Forecasts rely strictly on historical verified market price records from Modules 07–12. No fabricated prices or LLM-generated predictions.
3. **Pure Recursive Baseline (`WMA_V1`)**: Weighted Moving Average giving higher weights to recent verified observations without arbitrary drift tuning parameters.
4. **Empirical Residual Bounds**: Lower/upper bounds are derived from horizon-specific historical walk-forward backtest error standard deviation ($S_h$).
5. **Farmer Decision Support**: Integrated with Module 10 economics for read-only scenario profitability evaluation.
