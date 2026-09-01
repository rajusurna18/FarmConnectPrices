# Module 10 — Calculation Formulas & BigDecimal Strategy

## Formulas

1. **Total Production Cost**:
   $$\text{totalProductionCost} = \sum (\text{productionCostItem.amount})$$

2. **Total Selling Cost**:
   $$\text{totalSellingCost} = \text{transportationCost} + \text{otherSellingCosts}$$

3. **Total Cost**:
   $$\text{totalCost} = \text{totalProductionCost} + \text{totalSellingCost}$$

4. **Gross Revenue**:
   $$\text{grossRevenue} = \text{selectedMarketPrice} \times \text{expectedYield}$$

5. **Estimated Net Realization**:
   $$\text{estimatedNetRealization} = \text{grossRevenue} - \text{totalSellingCost}$$

6. **Estimated Profit**:
   $$\text{estimatedProfit} = \text{estimatedNetRealization} - \text{totalProductionCost}$$

7. **Profit Per Unit**:
   $$\text{profitPerUnit} = \frac{\text{estimatedProfit}}{\text{expectedYield}}$$

8. **Production Cost Per Unit**:
   $$\text{productionCostPerUnit} = \frac{\text{totalProductionCost}}{\text{expectedYield}}$$

9. **Total Cost Per Unit**:
   $$\text{totalCostPerUnit} = \frac{\text{totalCost}}{\text{expectedYield}}$$

10. **Break-Even Selling Price**:
    $$\text{breakEvenSellingPrice} = \frac{\text{totalCost}}{\text{expectedYield}}$$

11. **Estimated ROI %**:
    $$\text{ROI \%} = \left(\frac{\text{estimatedProfit}}{\text{totalCost}}\right) \times 100$$
    *(If $\text{totalCost} = 0$, $\text{ROI} = \text{null}$)*

## Profitability Status Rules
- `PROFITABLE`: `estimatedProfit > 0`
- `BREAK_EVEN`: `estimatedProfit == 0`
- `LOSS`: `estimatedProfit < 0`
- `INSUFFICIENT_DATA`: Missing price or yield <= 0
