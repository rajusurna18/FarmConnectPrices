# Module 11 — AI Decision Intelligence Foundation

Module 11 introduces the **AI Decision Intelligence Foundation** for **FarmConnectPrices**. It acts as a reasoning, explanation, and decision-support layer for farmers, operating directly on top of trusted market prices (Module 07), market intelligence (Module 08), selling costs (Module 09), and farm production costs (Module 10).

## Fundamental Architecture Rule
```
TRUSTED DATA (Modules 01-10)
        ↓
AUTHORITATIVE CALCULATIONS (Modules 09-10)
        ↓
AI CONTEXT BUILDER
        ↓
DECISION ENGINE (Rule-Based / External Provider Abstraction)
        ↓
VALIDATOR & SAFETY GUARDRAILS
        ↓
STRUCTURED FARMER RESPONSE
```

Existing backend systems remain the **authoritative source of truth**. The AI layer **never invents prices, costs, yields, break-even prices, or ROI calculations**.

## Feature Highlights
- **4 Initial Decision Types**: `MARKET_SELECTION`, `PROFITABILITY_EXPLANATION`, `MARKET_COMPARISON_EXPLANATION`, `SELLING_DECISION_SUPPORT`.
- **Deterministic Confidence Engine**: Evaluates `HIGH`, `MEDIUM`, or `LOW` confidence based on data freshness, market coverage, and input completeness.
- **Provider Abstraction & Fallback Engine**: `AiDecisionEngine` interface with deterministic `RuleBasedAiDecisionEngine` fallback. Functions without external LLM API key dependencies.
- **Safety Guardrails**: Sanitizes speculative/guaranteed financial or agricultural claims. Enforces disclaimers.
- **Role & Security Control**: Restricted to `FARMER` role. Unauthenticated requests receive HTTP 401; non-farmers receive HTTP 403.
