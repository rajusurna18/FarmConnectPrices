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
  isStatesLoading?: boolean;
  isDistrictsLoading?: boolean;
  isMarketsLoading?: boolean;
  isCropsLoading?: boolean;
}

export const MarketPriceFilterBar: React.FC<MarketPriceFilterBarProps> = ({
  filters,
  onFilterChange,
  availableStates,
  availableDistricts,
  availableCrops,
  availableMarkets,
  isStatesLoading = false,
  isDistrictsLoading = false,
  isMarketsLoading = false,
  isCropsLoading = false,
}) => {
  const handleStateChange = (stateValue: string) => {
    const val = stateValue.trim() ? stateValue.trim() : undefined;
    onFilterChange({
      ...filters,
      state: val,
      district: undefined,
      marketId: undefined,
    });
  };

  const handleDistrictChange = (districtValue: string) => {
    const val = districtValue.trim() ? districtValue.trim() : undefined;
    onFilterChange({
      ...filters,
      district: val,
      marketId: undefined,
    });
  };

  const handleChange = (key: keyof MarketPriceFilterState, value: string) => {
    onFilterChange({
      ...filters,
      [key]: value.trim() ? value.trim() : undefined,
    });
  };

  return (
    <div className="hidden xl:grid grid-cols-8 gap-3 bg-slate-900/80 p-4 rounded-2xl border border-slate-800 backdrop-blur-md text-xs">
      {/* State */}
      <div>
        <label htmlFor="filter-state" className="block text-[11px] font-bold text-slate-400 uppercase tracking-wider mb-1">
          State {isStatesLoading && <span className="text-emerald-400 font-mono text-[10px]">...</span>}
        </label>
        <select
          id="filter-state"
          name="state"
          value={filters.state || ''}
          onChange={(e) => handleStateChange(e.target.value)}
          className="w-full min-h-[42px] px-2.5 py-2 rounded-xl bg-slate-950 border border-slate-800 text-slate-200 focus:outline-none focus:border-emerald-500"
        >
          <option value="">All States ({availableStates.length})</option>
          {availableStates.map((st) => (
            <option key={st} value={st}>
              {st}
            </option>
          ))}
        </select>
      </div>

      {/* District */}
      <div>
        <label htmlFor="filter-district" className="block text-[11px] font-bold text-slate-400 uppercase tracking-wider mb-1">
          District {isDistrictsLoading && <span className="text-emerald-400 font-mono text-[10px]">...</span>}
        </label>
        <select
          id="filter-district"
          name="district"
          value={filters.district || ''}
          onChange={(e) => handleDistrictChange(e.target.value)}
          disabled={!filters.state && availableDistricts.length === 0}
          className="w-full min-h-[42px] px-2.5 py-2 rounded-xl bg-slate-950 border border-slate-800 text-slate-200 focus:outline-none focus:border-emerald-500 disabled:opacity-50"
        >
          <option value="">{filters.state ? `All Districts (${availableDistricts.length})` : 'Select State First'}</option>
          {availableDistricts.map((dist) => (
            <option key={dist} value={dist}>
              {dist}
            </option>
          ))}
        </select>
      </div>

      {/* Market */}
      <div>
        <label htmlFor="filter-market" className="block text-[11px] font-bold text-slate-400 uppercase tracking-wider mb-1">
          Market {isMarketsLoading && <span className="text-emerald-400 font-mono text-[10px]">...</span>}
        </label>
        <select
          id="filter-market"
          name="marketId"
          value={filters.marketId || ''}
          onChange={(e) => handleChange('marketId', e.target.value)}
          className="w-full min-h-[42px] px-2.5 py-2 rounded-xl bg-slate-950 border border-slate-800 text-slate-200 focus:outline-none focus:border-emerald-500 truncate"
        >
          <option value="">All Markets ({availableMarkets.length})</option>
          {availableMarkets.map((m) => (
            <option key={m.id} value={m.id}>
              {m.name}
            </option>
          ))}
        </select>
      </div>

      {/* Crop */}
      <div>
        <label htmlFor="filter-crop" className="block text-[11px] font-bold text-slate-400 uppercase tracking-wider mb-1">
          Commodity {isCropsLoading && <span className="text-emerald-400 font-mono text-[10px]">...</span>}
        </label>
        <select
          id="filter-crop"
          name="cropId"
          value={filters.cropId || ''}
          onChange={(e) => handleChange('cropId', e.target.value)}
          className="w-full min-h-[42px] px-2.5 py-2 rounded-xl bg-slate-950 border border-slate-800 text-slate-200 focus:outline-none focus:border-emerald-500 truncate"
        >
          <option value="">All Crops ({availableCrops.length})</option>
          {availableCrops.map((c) => (
            <option key={c.id} value={c.id}>
              {c.name}
            </option>
          ))}
        </select>
      </div>

      {/* Display Unit */}
      <div>
        <label htmlFor="filter-unit" className="block text-[11px] font-bold text-slate-400 uppercase tracking-wider mb-1">
          Display Unit
        </label>
        <select
          id="filter-unit"
          name="unit"
          value={filters.unit || 'QUINTAL'}
          onChange={(e) => handleChange('unit', e.target.value)}
          className="w-full min-h-[42px] px-2.5 py-2 rounded-xl bg-slate-950 border border-slate-800 text-slate-200 focus:outline-none focus:border-emerald-500 font-medium"
        >
          <option value="QUINTAL">₹ / QUINTAL (100 kg)</option>
          <option value="KG">₹ / KG (1 kg)</option>
          <option value="TONNE">₹ / TONNE (1000 kg)</option>
        </select>
      </div>

      {/* Quality Status */}
      <div>
        <label htmlFor="filter-quality" className="block text-[11px] font-bold text-slate-400 uppercase tracking-wider mb-1">
          Data Quality
        </label>
        <select
          id="filter-quality"
          name="qualityStatus"
          value={filters.qualityStatus || ''}
          onChange={(e) => handleChange('qualityStatus', e.target.value as PriceQualityStatus)}
          className="w-full min-h-[42px] px-2.5 py-2 rounded-xl bg-slate-950 border border-slate-800 text-slate-200 focus:outline-none focus:border-emerald-500"
        >
          <option value="">Verified & Unverified</option>
          <option value="VERIFIED">{QUALITY_STATUS_LABELS.VERIFIED}</option>
          <option value="UNVERIFIED">{QUALITY_STATUS_LABELS.UNVERIFIED}</option>
        </select>
      </div>

      {/* From Date */}
      <div>
        <label htmlFor="filter-from-date" className="block text-[11px] font-bold text-slate-400 uppercase tracking-wider mb-1">
          From Date
        </label>
        <input
          id="filter-from-date"
          name="fromDate"
          type="date"
          value={filters.fromDate || ''}
          onChange={(e) => handleChange('fromDate', e.target.value)}
          className="w-full min-h-[42px] px-2 py-2 rounded-xl bg-slate-950 border border-slate-800 text-slate-200 focus:outline-none focus:border-emerald-500"
        />
      </div>

      {/* To Date */}
      <div>
        <label htmlFor="filter-to-date" className="block text-[11px] font-bold text-slate-400 uppercase tracking-wider mb-1">
          To Date
        </label>
        <input
          id="filter-to-date"
          name="toDate"
          type="date"
          value={filters.toDate || ''}
          onChange={(e) => handleChange('toDate', e.target.value)}
          className="w-full min-h-[42px] px-2 py-2 rounded-xl bg-slate-950 border border-slate-800 text-slate-200 focus:outline-none focus:border-emerald-500"
        />
      </div>
    </div>
  );
};
