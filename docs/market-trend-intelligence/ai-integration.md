# Module 11 AI Integration & Non-Predictive Guardrails

## Integration Overview

Module 12 extends Module 11 AI architecture with the `MARKET_TREND_EXPLANATION` decision type.

- **Data Flow**:
  1. `MarketTrendService` computes deterministic metrics.
  2. `AiDecisionContextBuilder` populates `marketTrendContext` in `AiDecisionContext`.
  3. `AiDecisionService` processes decision via `RuleBasedAiDecisionEngine` or configured provider.
  4. `AiDecisionValidator` enforces non-predictive safety guardrails.

## Safety Guardrails

AI explanations summarize historical statistics and current vs average comparisons.

### Forbidden Speculative Terms (Sanitized / Rejected):
- `"price will rise"`
- `"price will fall"`
- `"price will drop"`
- `"price will reach"`
- `"guaranteed price"`
- `"guaranteed profit"`
- `"will increase tomorrow"`
- `"100% certain"`

### Deterministic Confidence Scoring
Confidence (`LOW`, `MEDIUM`, `HIGH`) is derived strictly from data metrics:
- `LOW`: $N < 2$, missing trend data, or unverified prices.
- `MEDIUM`: $2 \le N < 5$ (`LIMITED`) or date freshness is `STALE`.
- `HIGH`: $N \ge 5$ (`GOOD` quality) and date freshness is `FRESH`.

*AI provider success never inflates confidence.*
