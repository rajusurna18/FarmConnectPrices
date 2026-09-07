# Deterministic Lexicographic Ranking Rules

## Hierarchy for Comparable Economics

When complete selling costs / farm economics are available:
1. **Estimated Net Realization** DESC (Primary Objective)
2. **Estimated Net Profit** DESC (Secondary, if production costs exist)
3. **Current Verified Price (Modal Price)** DESC
4. **Price Freshness** DESC (newer price date)
5. **Market Trend Direction** DESC (`RISING` > `STABLE` > `FALLING` > `UNKNOWN`)
6. **Total Selling Cost** ASC (lower cost preferred)
7. **Market ID** ASC (lexicographical string tie-breaker)

## Hierarchy for Incomplete Economics

When cost information is missing:
1. **Current Verified Price (Modal Price)** DESC
2. **Price Freshness** DESC
3. **Market Trend Direction** DESC
4. **Market ID** ASC

*Incompatible Metric Isolation*: Incompatible metrics (e.g. net realization vs raw modal price) are never directly compared.
