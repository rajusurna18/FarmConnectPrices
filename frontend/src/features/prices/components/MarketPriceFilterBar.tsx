import React from 'react';
import type { MarketPriceFilterState, PriceQualityStatus } from '../types';
import { QUALITY_STATUS_LABELS } from '../types';

interface MarketPriceFilterBarProps {
  filters: MarketPriceFilterState;
  onFilterChange: (filters: MarketPriceFilterState) => void;
  availableStates: string[];
  availableDistricts: string[];
  availableCrops: { id: string; name: string }[];
  availableMarkets: { id: string; name: string }[];
}

export const MarketPriceFilterBar: React.FC<MarketPriceFilterBarProps> = ({
  filters,
  onFilterChange,
  availableStates,
  availableDistricts,
  availableCrops,
  availableMarkets,
}) => {
  const handleChange = (key: keyof MarketPriceFilterState, value: string) => {
    onFilterChange({
      ...filters,
      [key]: value.trim() ? value : undefined,
    });
  };

  return (
    <div className="hidden lg:grid grid-cols-6 gap-3 bg-slate-900/80 p-4 rounded-2xl border border-slate-800 backdrop-blur-md text-xs">
      {/* State */}
      <div>
        <label className="block text-[11px] font-bold text-slate-400 uppercase tracking-wider mb-1">State</label>
        <select
          value={filters.state || ''}
          onChange={(e) => handleChange('state', e.target.value)}
          className="w-full min-h-[42px] px-2.5 py-2 rounded-xl bg-slate-950 border border-slate-800 text-slate-200 focus:outline-none focus:border-emerald-500"
        >
          <option value="">All States</option>
          {availableStates.map((st) => (
            <option key={st} value={st}>
              {st}
            </option>
          ))}
        </select>
      </div>

      {/* District */}
      <div>
        <label className="block text-[11px] font-bold text-slate-400 uppercase tracking-wider mb-1">District</label>
        <select
          value={filters.district || ''}
          onChange={(e) => handleChange('district', e.target.value)}
          className="w-full min-h-[42px] px-2.5 py-2 rounded-xl bg-slate-950 border border-slate-800 text-slate-200 focus:outline-none focus:border-emerald-500"
        >
          <option value="">All Districts</option>
          {availableDistricts.map((dist) => (
            <option key={dist} value={dist}>
              {dist}
            </option>
          ))}
        </select>
      </div>

      {/* Market */}
      <div>
        <label className="block text-[11px] font-bold text-slate-400 uppercase tracking-wider mb-1">Market</label>
        <select
          value={filters.marketId || ''}
          onChange={(e) => handleChange('marketId', e.target.value)}
          className="w-full min-h-[42px] px-2.5 py-2 rounded-xl bg-slate-950 border border-slate-800 text-slate-200 focus:outline-none focus:border-emerald-500 truncate"
        >
          <option value="">All Markets</option>
          {availableMarkets.map((m) => (
            <option key={m.id} value={m.id}>
              {m.name}
            </option>
          ))}
        </select>
      </div>

      {/* Crop */}
      <div>
        <label className="block text-[11px] font-bold text-slate-400 uppercase tracking-wider mb-1">Commodity / Crop</label>
        <select
          value={filters.cropId || ''}
          onChange={(e) => handleChange('cropId', e.target.value)}
          className="w-full min-h-[42px] px-2.5 py-2 rounded-xl bg-slate-950 border border-slate-800 text-slate-200 focus:outline-none focus:border-emerald-500 truncate"
        >
          <option value="">All Crops</option>
          {availableCrops.map((c) => (
            <option key={c.id} value={c.id}>
              {c.name}
            </option>
          ))}
        </select>
      </div>

      {/* Quality Status */}
      <div>
        <label className="block text-[11px] font-bold text-slate-400 uppercase tracking-wider mb-1">Data Quality</label>
        <select
          value={filters.qualityStatus || ''}
          onChange={(e) => handleChange('qualityStatus', e.target.value as PriceQualityStatus)}
          className="w-full min-h-[42px] px-2.5 py-2 rounded-xl bg-slate-950 border border-slate-800 text-slate-200 focus:outline-none focus:border-emerald-500"
        >
          <option value="">Verified & Unverified</option>
          <option value="VERIFIED">{QUALITY_STATUS_LABELS.VERIFIED}</option>
          <option value="UNVERIFIED">{QUALITY_STATUS_LABELS.UNVERIFIED}</option>
        </select>
      </div>

      {/* Date */}
      <div>
        <label className="block text-[11px] font-bold text-slate-400 uppercase tracking-wider mb-1">Business Date</label>
        <input
          type="date"
          value={filters.priceDate || ''}
          onChange={(e) => handleChange('priceDate', e.target.value)}
          className="w-full min-h-[42px] px-2.5 py-2 rounded-xl bg-slate-950 border border-slate-800 text-slate-200 focus:outline-none focus:border-emerald-500"
        />
      </div>
    </div>
  );
};
