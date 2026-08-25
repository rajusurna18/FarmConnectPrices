import React from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import { X, Filter, RotateCcw, Check } from 'lucide-react';
import { MARKET_TYPE_LABELS } from '../types';
import type { MarketFilterState } from '../types';

interface MarketFilterDrawerProps {
  isOpen: boolean;
  onClose: () => void;
  filters: MarketFilterState;
  onApply: (filters: MarketFilterState) => void;
  onReset: () => void;
  availableStates: string[];
  availableDistricts: string[];
  availableCrops: Array<{ id: string; name: string }>;
}

export const MarketFilterDrawer: React.FC<MarketFilterDrawerProps> = ({
  isOpen,
  onClose,
  filters,
  onApply,
  onReset,
  availableStates,
  availableDistricts,
  availableCrops,
}) => {
  const [localFilters, setLocalFilters] = React.useState<MarketFilterState>(filters);

  React.useEffect(() => {
    setLocalFilters(filters);
  }, [filters, isOpen]);

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
        <>
          {/* Backdrop */}
          <motion.div
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            exit={{ opacity: 0 }}
            onClick={onClose}
            className="fixed inset-0 z-50 bg-slate-950/80 backdrop-blur-sm"
          />

          {/* Bottom Sheet Drawer */}
          <motion.div
            initial={{ y: '100%' }}
            animate={{ y: 0 }}
            exit={{ y: '100%' }}
            transition={{ type: 'spring', damping: 25, stiffness: 220 }}
            className="fixed bottom-0 left-0 right-0 z-50 bg-slate-900 border-t border-slate-800 rounded-t-3xl p-5 max-h-[85vh] overflow-y-auto shadow-2xl flex flex-col justify-between"
          >
            {/* Sheet Handle & Header */}
            <div>
              <div className="w-12 h-1.5 bg-slate-700 rounded-full mx-auto mb-4" />
              <div className="flex items-center justify-between pb-4 border-b border-slate-800">
                <div className="flex items-center space-x-2">
                  <Filter className="w-5 h-5 text-emerald-400" />
                  <h3 className="text-lg font-bold text-white">Filter Markets</h3>
                </div>
                <button
                  onClick={onClose}
                  className="w-10 h-10 min-w-[44px] min-h-[44px] rounded-full bg-slate-800 text-slate-400 hover:text-white flex items-center justify-center"
                >
                  <X className="w-5 h-5" />
                </button>
              </div>

              {/* Filter Controls Stack */}
              <div className="space-y-4 py-4">
                {/* State Filter */}
                <div>
                  <label className="block text-xs font-semibold text-slate-300 uppercase tracking-wider mb-1.5">
                    State
                  </label>
                  <select
                    value={localFilters.state || ''}
                    onChange={(e) => setLocalFilters((prev) => ({ ...prev, state: e.target.value || undefined }))}
                    className="w-full min-h-[48px] px-3.5 py-2.5 rounded-xl bg-slate-950 border border-slate-700 text-slate-100 text-sm focus:outline-none focus:border-emerald-500"
                  >
                    <option value="">All States</option>
                    {availableStates.map((st) => (
                      <option key={st} value={st}>
                        {st}
                      </option>
                    ))}
                  </select>
                </div>

                {/* District Filter */}
                <div>
                  <label className="block text-xs font-semibold text-slate-300 uppercase tracking-wider mb-1.5">
                    District
                  </label>
                  <select
                    value={localFilters.district || ''}
                    onChange={(e) => setLocalFilters((prev) => ({ ...prev, district: e.target.value || undefined }))}
                    className="w-full min-h-[48px] px-3.5 py-2.5 rounded-xl bg-slate-950 border border-slate-700 text-slate-100 text-sm focus:outline-none focus:border-emerald-500"
                  >
                    <option value="">All Districts</option>
                    {availableDistricts.map((dist) => (
                      <option key={dist} value={dist}>
                        {dist}
                      </option>
                    ))}
                  </select>
                </div>

                {/* Market Type Filter */}
                <div>
                  <label className="block text-xs font-semibold text-slate-300 uppercase tracking-wider mb-1.5">
                    Market Type
                  </label>
                  <select
                    value={localFilters.type || ''}
                    onChange={(e) => setLocalFilters((prev) => ({ ...prev, type: e.target.value || undefined }))}
                    className="w-full min-h-[48px] px-3.5 py-2.5 rounded-xl bg-slate-950 border border-slate-700 text-slate-100 text-sm focus:outline-none focus:border-emerald-500"
                  >
                    <option value="">All Market Types</option>
                    {Object.entries(MARKET_TYPE_LABELS).map(([val, label]) => (
                      <option key={val} value={val}>
                        {label}
                      </option>
                    ))}
                  </select>
                </div>

                {/* Crop Filter */}
                <div>
                  <label className="block text-xs font-semibold text-slate-300 uppercase tracking-wider mb-1.5">
                    Supported Crop
                  </label>
                  <select
                    value={localFilters.cropId || ''}
                    onChange={(e) => setLocalFilters((prev) => ({ ...prev, cropId: e.target.value || undefined }))}
                    className="w-full min-h-[48px] px-3.5 py-2.5 rounded-xl bg-slate-950 border border-slate-700 text-slate-100 text-sm focus:outline-none focus:border-emerald-500"
                  >
                    <option value="">All Crops</option>
                    {availableCrops.map((c) => (
                      <option key={c.id} value={c.id}>
                        {c.name}
                      </option>
                    ))}
                  </select>
                </div>
              </div>
            </div>

            {/* Action CTAs */}
            <div className="pt-4 border-t border-slate-800 grid grid-cols-2 gap-3 mt-2">
              <button
                type="button"
                onClick={handleReset}
                className="w-full min-h-[48px] py-3 rounded-xl bg-slate-800 hover:bg-slate-700 text-slate-200 font-semibold text-sm flex items-center justify-center space-x-2"
              >
                <RotateCcw className="w-4 h-4" />
                <span>Reset</span>
              </button>
              <button
                type="button"
                onClick={handleApply}
                className="w-full min-h-[48px] py-3 rounded-xl bg-emerald-600 hover:bg-emerald-500 text-white font-semibold text-sm flex items-center justify-center space-x-2 shadow-lg shadow-emerald-950/50"
              >
                <Check className="w-4 h-4" />
                <span>Apply Filters</span>
              </button>
            </div>
          </motion.div>
        </>
      )}
    </AnimatePresence>
  );
};
