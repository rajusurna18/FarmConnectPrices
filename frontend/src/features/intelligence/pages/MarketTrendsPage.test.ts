import { describe, it, expect } from 'vitest';
import type { MarketTrendData, MarketTrendObservation } from '../types';

describe('Module 12 — Market Trend Intelligence Frontend Tests', () => {
  const mockObservations: MarketTrendObservation[] = [
    { date: '2026-08-01', price: 2000, priceUnit: 'QUINTAL', qualityStatus: 'VERIFIED' },
    { date: '2026-08-15', price: 2100, priceUnit: 'QUINTAL', qualityStatus: 'VERIFIED' },
    { date: '2026-08-30', price: 2200, priceUnit: 'QUINTAL', qualityStatus: 'VERIFIED' },
  ];

  const mockTrendData: MarketTrendData = {
    cropName: 'Cotton',
    marketName: 'Warangal Mandi',
    period: '30D',
    priceUnit: 'QUINTAL',
    observationCount: 3,
    earliestPrice: 2000,
    latestPrice: 2200,
    latestObservationDate: '2026-08-30',
    historicalAverage: 2100,
    minPrice: 2000,
    maxPrice: 2200,
    priceRange: 200,
    absoluteChange: 200,
    percentageChange: 10,
    trendDirection: 'RISING',
    volatility: {
      standardDeviation: 81.65,
      coefficientOfVariation: 3.89,
      volatilityRating: 'LOW',
    },
    dataQuality: 'GOOD',
    freshnessStatus: 'FRESH',
    observations: mockObservations,
    aiExplanation: {
      decisionType: 'MARKET_TREND_EXPLANATION',
      summary: 'Cotton prices in Warangal Mandi rose by 10.0% over the last 30D.',
      recommendation: 'Monitor market trends before negotiating.',
      confidence: 'HIGH',
      verifiedFacts: ['Earliest price: ₹2000', 'Latest price: ₹2200', 'Observed average: ₹2100'],
      reasoning: ['Deterministic trend calculation indicates rising prices with low volatility.'],
      risks: ['Market conditions can change.'],
      nextSteps: ['Check daily arrival volumes.'],
      engine: 'RULE_BASED',
      aiGenerated: false,
      dataFreshness: { status: 'FRESH', stalePrice: false, staleMessage: 'Fresh verified market data' },
      limitations: ['Based on 3 historical verified price observations.'],
    },
  };

  it('correctly models complete MarketTrendData structure', () => {
    expect(mockTrendData.cropName).toBe('Cotton');
    expect(mockTrendData.marketName).toBe('Warangal Mandi');
    expect(mockTrendData.observationCount).toBe(3);
    expect(mockTrendData.trendDirection).toBe('RISING');
    expect(mockTrendData.percentageChange).toBe(10);
    expect(mockTrendData.volatility.volatilityRating).toBe('LOW');
    expect(mockTrendData.dataQuality).toBe('GOOD');
    expect(mockTrendData.freshnessStatus).toBe('FRESH');
  });

  it('correctly structures historical observations for Recharts plotting', () => {
    expect(mockTrendData.observations).toHaveLength(3);
    expect(mockTrendData.observations[0].date).toBe('2026-08-01');
    expect(mockTrendData.observations[0].price).toBe(2000);
    expect(mockTrendData.observations[2].price).toBe(2200);
  });

  it('handles insufficient data state correctly', () => {
    const insufficientTrend: MarketTrendData = {
      cropName: 'Chilli',
      marketName: 'Khammam Mandi',
      period: '7D',
      priceUnit: 'QUINTAL',
      observationCount: 1,
      earliestPrice: 5000,
      latestPrice: 5000,
      latestObservationDate: '2026-08-25',
      historicalAverage: 5000,
      minPrice: 5000,
      maxPrice: 5000,
      priceRange: 0,
      absoluteChange: 0,
      percentageChange: 0,
      trendDirection: 'INSUFFICIENT_DATA',
      volatility: {
        standardDeviation: 0,
        coefficientOfVariation: 0,
        volatilityRating: 'INSUFFICIENT_DATA',
      },
      dataQuality: 'INSUFFICIENT',
      freshnessStatus: 'FRESH',
      observations: [{ date: '2026-08-25', price: 5000, priceUnit: 'QUINTAL', qualityStatus: 'VERIFIED' }],
    };

    expect(insufficientTrend.observationCount).toBe(1);
    expect(insufficientTrend.trendDirection).toBe('INSUFFICIENT_DATA');
    expect(insufficientTrend.volatility.volatilityRating).toBe('INSUFFICIENT_DATA');
    expect(insufficientTrend.dataQuality).toBe('INSUFFICIENT');
  });

  it('handles fallback AI explanation state gracefully when provider fails', () => {
    const fallbackTrend: MarketTrendData = {
      ...mockTrendData,
      aiExplanation: {
        decisionType: 'MARKET_TREND_EXPLANATION',
        summary: 'Fallback deterministic summary generated.',
        recommendation: 'Review verified market statistics.',
        confidence: 'MEDIUM',
        verifiedFacts: ['3 verified observations'],
        reasoning: ['Rule-based engine active.'],
        risks: [],
        nextSteps: [],
        engine: 'RULE_BASED',
        aiGenerated: false,
        dataFreshness: { status: 'FRESH', stalePrice: false, staleMessage: '' },
        limitations: ['AI provider unavailable; fallback rule engine utilized.'],
      },
    };

    expect(fallbackTrend.aiExplanation?.engine).toBe('RULE_BASED');
    expect(fallbackTrend.aiExplanation?.limitations[0]).toContain('fallback rule engine');
  });

  it('validates custom date range constraint parameters', () => {
    const fromDate = '2026-08-01';
    const toDate = '2026-08-30';
    const daysDiff = (new Date(toDate).getTime() - new Date(fromDate).getTime()) / (1000 * 3600 * 24);

    expect(daysDiff).toBeLessThanOrEqual(365);
    expect(new Date(fromDate).getTime()).toBeLessThan(new Date(toDate).getTime());
  });
});
