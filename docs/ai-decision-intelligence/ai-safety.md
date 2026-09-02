# Module 11 — AI Safety, Guardrails & Trust Framework

## Core Principles
1. **Authoritative Calculation Supremacy**: AI reasoning never recalculates or overrides monetary math. All net realization, total production/selling costs, break-even prices, and ROI values originate directly from Module 09 and Module 10 calculation services.
2. **Forbidden Speculative Claims**: Output is scanned and sanitized by `AiDecisionValidator`. Phrases such as "definitely sell today", "price will rise tomorrow", "100% certain", or "guaranteed profit" are prohibited and sanitized.
3. **Data Freshness Disclosure**: Every response explicitly includes price observation timestamps, staleness flags, and disclaimers.
4. **Deterministic Confidence**: Confidence (`HIGH`/`MEDIUM`/`LOW`) is evaluated programmatically based on data freshness and input completeness.

## Role Restrictions & Isolation
- Restricted strictly to `FARMER` profile role.
- Farmers can only request decision evaluations for farms and economic records where `record.ownerUid == authenticatedUid`. Cross-farmer access attempts are denied (HTTP 403).
