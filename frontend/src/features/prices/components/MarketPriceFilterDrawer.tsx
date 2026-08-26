import React from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import { X, Filter, RefreshCw } from 'lucide-react';
import type { MarketPriceFilterState, PriceQualityStatus } from '../types';
import { QUALITY_STATUS_LABELS } from '../types';

interface MarketPriceFilterDrawerProps {
  isOpen: boolean;
  onClose: () => void;
  filters: MarketPriceFilterState;
  onApply: (filters: MarketPriceFilterState) => void;
  onReset: () => void;
  availableStates: string[];
  availableDistricts: string[];
  availableCrops: { id: string; name: string }[];
  availableMarkets: { id: string; name: string }[];
}

export const MarketPriceFilterDrawer: React.FC<MarketPriceFilterDrawerProps> = ({
  isOpen,
  onClose,
  filters,
  onApply,
  onReset,
  availableStates,
  availableDistricts,
  availableCrops,
  availableMarkets,
}) => {
  const [localFilters, setLocalFilters] = React.useState<MarketPriceFilterState>(filters);

  React.useEffect(() => {
    setLocalFilters(filters);
  }, [filters]);

  const handleChange = (key: keyof MarketPriceFilterState, value: string) => {
    setLocalFilters((prev) => ({
      ...prev,
      [key]: value.trim() ? value : undefined,
    }));
  };

  const handleApply = () => {
    onApply(localFilters);
    onClose();
  };

  const handleReset = () => {
    setLocalFilters({});
    onReset();
    onClose();
  };

  return (
    <AnimatePresence>
      {isOpen && (
        <div className="fixed inset-0 z-50 flex justify-end select-none">
          {/* Backdrop */}
          <motion.div
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            exit={{ opacity: 0 }}
            onClick={onClose}
            className="fixed inset-0 bg-slate-950/80 backdrop-blur-sm"
          />

          {/* Drawer Content */}
          <motion.div
            initial={{ x: '100%' }}
            animate={{ x: 0 }}
            exit={{ x: '100%' }}
            transition={{ type: 'spring', damping: 25, stiffness: 200 }}
            className="relative w-full max-w-md bg-slate-950 border-l border-slate-800 h-full p-6 flex flex-col justify-between overflow-y-auto z-10"
          >
            <div className="space-y-6">
              <div className="flex items-center justify-between border-b border-slate-900 pb-4">
                <div className="flex items-center space-x-2">
                  <Filter className="w-5 h-5 text-emerald-400" />
                  <h2 className="text-lg font-bold text-white">Filter Market Prices</h2>
                </div>
                <button
                  onClick={onClose}
                  className="p-2 text-slate-400 hover:text-white rounded-xl bg-slate-900 border border-slate-800 min-h-[44px] min-w-[44px] flex items-center justify-center"
                >
                  <X className="w-5 h-5" />
                </button>
              </div>

              <div className="space-y-4 text-xs">
                {/* State */}
                <div>
                  <label className="block text-xs font-semibold text-slate-300 mb-1.5">State</label>
                  <select
                    value={localFilters.state || ''}
                    onChange={(e) => handleChange('state', e.target.value)}
                    className="w-full min-h-[48px] px-3.5 py-2.5 rounded-xl bg-slate-900 border border-slate-800 text-slate-200 text-sm focus:outline-none focus:border-emerald-500"
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
                  <label className="block text-xs font-semibold text-slate-300 mb-1.5">District</label>
                  <select
                    value={localFilters.district || ''}
                    onChange={(e) => handleChange('district', e.target.value)}
                    className="w-full min-h-[48px] px-3.5 py-2.5 rounded-xl bg-slate-900 border border-slate-800 text-slate-200 text-sm focus:outline-none focus:border-emerald-500"
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
                  <label className="block text-xs font-semibold text-slate-300 mb-1.5">Market</label>
                  <select
                    value={localFilters.marketId || ''}
                    onChange={(e) => handleChange('marketId', e.target.value)}
                    className="w-full min-h-[48px] px-3.5 py-2.5 rounded-xl bg-slate-900 border border-slate-800 text-slate-200 text-sm focus:outline-none focus:border-emerald-500"
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
                  <label className="block text-xs font-semibold text-slate-300 mb-1.5">Supported Crop / Commodity</label>
                  <select
                    value={localFilters.cropId || ''}
                    onChange={(e) => handleChange('cropId', e.target.value)}
                    className="w-full min-h-[48px] px-3.5 py-2.5 rounded-xl bg-slate-900 border border-slate-800 text-slate-200 text-sm focus:outline-none focus:border-emerald-500"
                  >
                    <option value="">All Crops</option>
                    {availableCrops.map((c) => (
                      <option key={c.id} value={c.id}>
                        {c.name}
                      </option>
                    ))}
                  </select>
                </div>

                {/* Data Quality */}
                <div>
                  <label className="block text-xs font-semibold text-slate-300 mb-1.5">Data Quality Status</label>
                  <select
                    value={localFilters.qualityStatus || ''}
                    onChange={(e) => handleChange('qualityStatus', e.target.value as PriceQualityStatus)}
                    className="w-full min-h-[48px] px-3.5 py-2.5 rounded-xl bg-slate-900 border border-slate-800 text-slate-200 text-sm focus:outline-none focus:border-emerald-500"
                  >
                    <option value="">Verified & Unverified Data</option>
                    <option value="VERIFIED">{QUALITY_STATUS_LABELS.VERIFIED}</option>
                    <option value="UNVERIFIED">{QUALITY_STATUS_LABELS.UNVERIFIED}</option>
                  </select>
                </div>

                {/* Business Date */}
                <div>
                  <label className="block text-xs font-semibold text-slate-300 mb-1.5">Business Date</label>
                  <input
                    type="date"
                    value={localFilters.priceDate || ''}
                    onChange={(e) => handleChange('priceDate', e.target.value)}
                    className="w-full min-h-[48px] px-3.5 py-2.5 rounded-xl bg-slate-900 border border-slate-800 text-slate-200 text-sm focus:outline-none focus:border-emerald-500"
                  />
                </div>
              </div>
            </div>

            <div className="pt-6 border-t border-slate-900 space-y-3">
              <button
                onClick={handleApply}
                className="w-full min-h-[48px] py-3 rounded-xl bg-emerald-600 hover:bg-emerald-500 text-white font-bold text-sm shadow-lg shadow-emerald-950/50 transition-all flex items-center justify-center space-x-2"
              >
                <span>Apply Price Filters</span>
              </button>
              <button
                onClick={handleReset}
                className="w-full min-h-[48px] py-3 rounded-xl bg-slate-900 hover:bg-slate-800 text-slate-300 font-semibold text-xs border border-slate-800 transition-colors flex items-center justify-center space-x-1.5"
              >
                <RefreshCw className="w-3.5 h-3.5" />
                <span>Reset Filters</span>
              </button>
            </div>
          </motion.div>
        </div>
      )}
    </AnimatePresence>
  );
};
