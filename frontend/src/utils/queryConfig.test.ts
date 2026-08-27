import { describe, it, expect } from 'vitest';
import { defaultRetry, queryKeys } from './queryConfig';

describe('queryConfig utilities', () => {
  it('defaultRetry returns false immediately when status is 503', () => {
    const error503 = { response: { status: 503 } };
    expect(defaultRetry(0, error503)).toBe(false);
    expect(defaultRetry(1, error503)).toBe(false);
  });

  it('defaultRetry returns true on initial failure for non-503 errors and false thereafter', () => {
    const error500 = { response: { status: 500 } };
    expect(defaultRetry(0, error500)).toBe(true);
    expect(defaultRetry(1, error500)).toBe(false);
  });

  it('queryKeys generates primitive stable array keys', () => {
    expect(queryKeys.states()).toEqual(['locations', 'states']);
    expect(queryKeys.districts('Telangana')).toEqual(['locations', 'districts', 'Telangana']);
    expect(queryKeys.crops()).toEqual(['crops']);
    expect(queryKeys.marketPrices({ state: 'Telangana', unit: 'QUINTAL' })).toEqual([
      'marketPrices',
      'Telangana',
      '',
      '',
      '',
      '',
      'QUINTAL',
      '',
    ]);
  });
});
