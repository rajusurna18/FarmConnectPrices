# Module 14 — Smart Selling Decision Engine

## Overview

The **Smart Selling Decision Engine** unifies the market intelligence, price history, trend analysis, price forecasting, farm economics, and AI decision intelligence built in Modules 07–13 into a single farmer-facing decision support workflow.

It helps farmers evaluate selling options across multiple candidate markets based on:
- Verified current market prices
- Farmer-specific production & selling costs
- Estimated Net Realization and Estimated Profit
- 30-Day historical trend direction
- Model-based price forecasts (1D, 3D, 7D, 14D)
- Consequence-verified trade-off analysis between markets
- AI-generated human-readable explanations with strict safety guardrails

---

## Core Principles

1. **Decision Support, Not Autonomous Selling**: The farmer remains the sole decision maker. The system does not execute automated trades, place orders, negotiate, or guarantee future prices/profits.
2. **Deterministic Engine First**: All rankings, net realizations, net profits, confidence levels, and trade-offs are computed deterministically. The AI layer (Module 11) is strictly explanation-only.
3. **No False Certainty**: Forecasts are marked as scenario projections (`isScenario=true`) and do not override current verified economic rankings.
