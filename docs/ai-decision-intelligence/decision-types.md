# Module 11 — Decision Types Specification

## Supported Decision Types

### 1. `MARKET_SELECTION`
- **Objective**: Identify which candidate market currently offers the best outcome for a crop and quantity.
- **Inputs**: Crop ID, candidate Market IDs, Quantity, Transport & Selling Costs.
- **Output**: Ranked market recommendation based on `estimatedNetRealization` / `estimatedProfit`, price advantage breakdown, logistics risks, and pre-sale verification steps.

### 2. `PROFITABILITY_EXPLANATION`
- **Objective**: Explain farmer production costs, selling costs, gross revenue, net realization, break-even selling price, and ROI % in plain language.
- **Inputs**: Farm Economic Record ID.
- **Output**: Plain-language breakdown of production vs selling costs, break-even price, and cost-control recommendations.

### 3. `MARKET_COMPARISON_EXPLANATION`
- **Objective**: Compare two or more mandis side-by-side.
- **Inputs**: Crop ID, 2+ Market IDs, Transport & Selling Costs.
- **Output**: Detailed comparison highlighting price differences, freight overheads, net realization margins, and transport risks.

### 4. `SELLING_DECISION_SUPPORT`
- **Objective**: Provide a pre-sale evaluation checklist before committing to harvest transport.
- **Inputs**: Crop ID, Target Quantity.
- **Output**: Checkpoints including market price freshness, break-even price threshold, logistics quote verification, and quality grading standards.
