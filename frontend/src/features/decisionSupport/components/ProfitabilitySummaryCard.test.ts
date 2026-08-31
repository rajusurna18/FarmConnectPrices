import { describe, it, expect } from 'vitest';
import type { MarketEvaluationResponse } from '../types/decisionSupport';

describe('Decision Support Frontend Types & Evaluation Logic', () => {
  it('correctly identifies successful evaluation response structure', () => {
    const mockEval: MarketEvaluationResponse = {
      status: 'SUCCESS',
      message: 'Evaluation completed successfully.',
      crop: { id: 'CROP1', name: 'Rice' },
      market: { id: 'MKT1', name: 'Warangal Mandi' },
      selectedPrice: 2800,
      priceBasis: 'MODAL',
      priceUnit: 'QUINTAL',
      quantity: 10,
      quantityUnit: 'QUINTAL',
      grossRevenue: 28000,
      transportationCost: 300,
      otherSellingCosts: 100,
      totalSellingCosts: 400,
      estimatedNetRealization: 27600,
      netRealizationPerUnit: 2760,
      sellingCostBreakEvenPrice: 40,
      currency: 'INR',
      priceDate: '2026-08-31',
      qualityStatus: 'VERIFIED',
      isStalePrice: false,
      isLoss: false,
    };

    expect(mockEval.status).toBe('SUCCESS');
    expect(mockEval.grossRevenue).toBe(28000);
    expect(mockEval.totalSellingCosts).toBe(400);
    expect(mockEval.estimatedNetRealization).toBe(27600);
    expect(mockEval.netRealizationPerUnit).toBe(2760);
    expect(mockEval.sellingCostBreakEvenPrice).toBe(40);
  });

  it('correctly handles unit mismatch status', () => {
    const mockEval: MarketEvaluationResponse = {
      status: 'UNIT_MISMATCH',
      message: 'Price and quantity units are incompatible.',
    };

    expect(mockEval.status).toBe('UNIT_MISMATCH');
    expect(mockEval.message).toBe('Price and quantity units are incompatible.');
  });
});
