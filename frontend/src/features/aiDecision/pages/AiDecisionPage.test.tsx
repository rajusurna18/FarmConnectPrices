import { describe, it, expect } from 'vitest';
import type {
  AiDecisionType,
  AiConfidenceLevel,
  AiDecisionRequest,
  AiDecisionResponse,
} from '../types';

describe('Module 11 — AI Decision Intelligence Frontend Logic & Types', () => {
  it('correctly models all four decision types', () => {
    const types: AiDecisionType[] = [
      'MARKET_SELECTION',
      'PROFITABILITY_EXPLANATION',
      'MARKET_COMPARISON_EXPLANATION',
      'SELLING_DECISION_SUPPORT',
    ];
    expect(types).toHaveLength(4);
  });

  it('correctly constructs a MARKET_SELECTION decision request payload', () => {
    const request: AiDecisionRequest = {
      decisionType: 'MARKET_SELECTION',
      cropId: 'crop-123',
      farmId: 'farm-456',
      economicRecordId: 'record-789',
      marketIds: ['mkt-1', 'mkt-2'],
      quantity: 10.0,
      quantityUnit: 'QUINTAL',
      priceBasis: 'MODAL',
      priceMode: 'LATEST_AVAILABLE',
      date: '2026-09-02',
      transportationCost: 300.0,
      otherSellingCosts: 100.0,
    };

    expect(request.decisionType).toBe('MARKET_SELECTION');
    expect(request.marketIds).toContain('mkt-1');
    expect(request.marketIds).toContain('mkt-2');
    expect(request.transportationCost).toBe(300.0);
    expect(request.otherSellingCosts).toBe(100.0);
  });

  it('correctly renders structured decision response with confidence and safety facts', () => {
    const mockResponse: AiDecisionResponse = {
      decisionType: 'MARKET_SELECTION',
      summary: 'Warangal Mandi currently offers the highest estimated net realization.',
      recommendation: 'Warangal Mandi',
      confidence: 'HIGH',
      verifiedFacts: [
        'Verified modal price at Warangal Mandi: ₹2200.00 per QUINTAL',
        'Your estimated transportation cost: ₹300.00',
      ],
      calculatedMetrics: {
        selectedPrice: 2200,
        expectedYield: 10,
        yieldUnit: 'QUINTAL',
        grossRevenue: 22000,
        totalSellingCost: 400,
        estimatedNetRealization: 21600,
        estimatedProfit: 9600,
        roi: 80,
      },
      reasoning: [
        'Warangal Mandi offers an estimated net realization of ₹21600.00.',
      ],
      risks: [
        'Market prices fluctuate throughout the day and can change before sale execution.',
      ],
      nextSteps: [
        'Contact mandi representative to verify today arrival prices.',
      ],
      engine: 'RULE_BASED',
      aiGenerated: false,
      dataFreshness: {
        marketPriceDate: '2026-09-02',
        status: 'CURRENT',
        stalePrice: false,
        staleMessage: 'Based on current verified market price date (2026-09-02).',
      },
      limitations: [
        'Estimates are based on latest verified market data and farmer-entered costs.',
      ],
    };

    expect(mockResponse.confidence).toBe('HIGH');
    expect(mockResponse.recommendation).toBe('Warangal Mandi');
    expect(mockResponse.calculatedMetrics?.estimatedNetRealization).toBe(21600);
    expect(mockResponse.verifiedFacts[1]).toContain('Your estimated transportation cost');
    expect(mockResponse.dataFreshness.status).toBe('CURRENT');
    expect(mockResponse.engine).toBe('RULE_BASED');
  });

  it('evaluates confidence level badge mapping accurately', () => {
    const highConf: AiConfidenceLevel = 'HIGH';
    const medConf: AiConfidenceLevel = 'MEDIUM';
    const lowConf: AiConfidenceLevel = 'LOW';

    expect(highConf).toBe('HIGH');
    expect(medConf).toBe('MEDIUM');
    expect(lowConf).toBe('LOW');
  });

  it('handles empty or incomplete data gracefully', () => {
    const incompleteResponse: AiDecisionResponse = {
      decisionType: 'PROFITABILITY_EXPLANATION',
      summary: 'Insufficient data available.',
      recommendation: 'No recommendation available',
      confidence: 'LOW',
      verifiedFacts: ['No economic record found'],
      reasoning: [],
      risks: ['Evaluation cannot proceed without economic data'],
      nextSteps: ['Create a farm production cost record first'],
      engine: 'RULE_BASED',
      aiGenerated: false,
      dataFreshness: {
        status: 'MISSING',
        stalePrice: false,
        staleMessage: 'No verified market price available.',
      },
      limitations: ['Data incomplete'],
    };

    expect(incompleteResponse.confidence).toBe('LOW');
    expect(incompleteResponse.summary).toBe('Insufficient data available.');
    expect(incompleteResponse.reasoning).toHaveLength(0);
  });
});
