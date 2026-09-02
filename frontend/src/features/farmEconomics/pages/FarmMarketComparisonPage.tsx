import React, { useState, useEffect } from 'react';
import { useSearchParams, useNavigate } from 'react-router-dom';
import { useFarmEconomics, useCompareFarmEconomics } from '../hooks/useFarmEconomics';
import { useMarkets } from '../../markets/hooks/useMarkets';
import {
  ArrowLeft,
  BarChart2,
  Award,
  CheckCircle,
  ShieldAlert,
} from 'lucide-react';

export const FarmMarketComparisonPage: React.FC = () => {
  const [searchParams] = useSearchParams();
  const navigate = useNavigate();

  const recordIdParam = searchParams.get('recordId') || '';

  const { data: records } = useFarmEconomics();
  const { data: markets } = useMarkets();
  const compareMutation = useCompareFarmEconomics();

  const [selectedRecordId, setSelectedRecordId] = useState(recordIdParam);
  const [selectedMarketIds, setSelectedMarketIds] = useState<string[]>([]);
  const [priceBasis, setPriceBasis] = useState<'MIN' | 'MODAL' | 'MAX'>('MODAL');
  const [priceMode, setPriceMode] = useState<'LATEST_AVAILABLE' | 'EXACT_DATE'>('LATEST_AVAILABLE');

  useEffect(() => {
    if (recordIdParam) {
      setSelectedRecordId(recordIdParam);
    } else if (records && records.length > 0 && !selectedRecordId) {
      setSelectedRecordId(records[0].id);
    }
  }, [recordIdParam, records, selectedRecordId]);

  // Pre-select first 3 markets when markets load
  useEffect(() => {
    if (markets && markets.length > 0 && selectedMarketIds.length === 0) {
      setSelectedMarketIds(markets.slice(0, 3).map((m) => m.id));
    }
  }, [markets, selectedMarketIds.length]);

  const toggleMarketSelection = (marketId: string) => {
    setSelectedMarketIds((prev) =>
      prev.includes(marketId) ? prev.filter((id) => id !== marketId) : [...prev, marketId]
    );
  };

  const handleCompare = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!selectedRecordId || selectedMarketIds.length === 0) return;

    await compareMutation.mutateAsync({
      economicRecordId: selectedRecordId,
      priceBasis,
      priceMode,
      markets: selectedMarketIds.map((id) => ({
        marketId: id,
        transportationCost: 300,
        otherSellingCosts: 100,
      })),
    });
  };

  const comparison = compareMutation.data;

  return (
    <div className="space-y-6 max-w-5xl mx-auto">
      {/* Top Header */}
      <div className="flex items-center gap-4">
        <button
          onClick={() => navigate('/farm-economics')}
          className="p-2 bg-slate-800 hover:bg-slate-700 text-slate-300 rounded-lg transition-colors"
        >
          <ArrowLeft className="w-5 h-5" />
        </button>
        <div>
          <h1 className="text-2xl font-bold text-white flex items-center gap-2">
            <BarChart2 className="text-blue-400 w-6 h-6" />
            Market Profitability Comparison
          </h1>
          <p className="text-slate-400 text-sm">
            Compare crop production costs against live prices in multiple markets to identify the highest estimated profit.
          </p>
        </div>
      </div>

      {/* Form Controls */}
      <form onSubmit={handleCompare} className="bg-slate-800/60 border border-slate-700/60 rounded-xl p-6 space-y-4 shadow-md">
        <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
          <div>
            <label className="block text-xs font-medium text-slate-300 mb-1">Select Crop Economic Record *</label>
            <select
              value={selectedRecordId}
              onChange={(e) => setSelectedRecordId(e.target.value)}
              required
              className="w-full px-4 py-2.5 bg-slate-900 border border-slate-700 text-white rounded-lg text-xs focus:outline-none focus:border-emerald-500"
            >
              <option value="">-- Choose Crop Record --</option>
              {records?.map((r) => (
                <option key={r.id} value={r.id}>
                  {r.cropName} ({r.farmName} - {r.season})
                </option>
              ))}
            </select>
          </div>

          <div>
            <label className="block text-xs font-medium text-slate-300 mb-1">Price Basis</label>
            <select
              value={priceBasis}
              onChange={(e) => setPriceBasis(e.target.value as 'MODAL' | 'MIN' | 'MAX')}
              className="w-full px-4 py-2.5 bg-slate-900 border border-slate-700 text-white rounded-lg text-xs focus:outline-none focus:border-emerald-500"
            >
              <option value="MODAL">MODAL Price</option>
              <option value="MIN">MIN Price</option>
              <option value="MAX">MAX Price</option>
            </select>
          </div>

          <div>
            <label className="block text-xs font-medium text-slate-300 mb-1">Price Mode</label>
            <select
              value={priceMode}
              onChange={(e) => setPriceMode(e.target.value as 'LATEST_AVAILABLE' | 'EXACT_DATE')}
              className="w-full px-4 py-2.5 bg-slate-900 border border-slate-700 text-white rounded-lg text-xs focus:outline-none focus:border-emerald-500"
            >
              <option value="LATEST_AVAILABLE">Latest Available Price</option>
              <option value="EXACT_DATE">Exact Date</option>
            </select>
          </div>
        </div>

        {/* Market Selection Checklist */}
        <div>
          <label className="block text-xs font-medium text-slate-300 mb-2">
            Select Target Markets to Compare ({selectedMarketIds.length} selected):
          </label>
          <div className="grid grid-cols-2 sm:grid-cols-4 gap-2.5 max-h-48 overflow-y-auto p-3 bg-slate-900/60 rounded-xl border border-slate-700/50">
            {markets?.map((m) => {
              const isSelected = selectedMarketIds.includes(m.id);
              return (
                <button
                  key={m.id}
                  type="button"
                  onClick={() => toggleMarketSelection(m.id)}
                  className={`p-2.5 rounded-lg border text-left text-xs transition-all ${
                    isSelected
                      ? 'bg-emerald-600/20 border-emerald-500/50 text-white font-medium'
                      : 'bg-slate-800/40 border-slate-700/40 text-slate-400 hover:text-slate-200'
                  }`}
                >
                  <div className="truncate font-semibold">{m.name}</div>
                  <div className="text-[10px] text-slate-400">{m.district}</div>
                </button>
              );
            })}
          </div>
        </div>

        <div className="flex justify-end pt-2">
          <button
            type="submit"
            disabled={compareMutation.isPending || !selectedRecordId || selectedMarketIds.length === 0}
            className="inline-flex items-center gap-2 px-6 py-2.5 bg-blue-600 hover:bg-blue-500 text-white text-xs font-semibold rounded-lg transition-colors shadow-lg shadow-blue-900/30 disabled:opacity-50"
          >
            <BarChart2 className="w-4 h-4" />
            {compareMutation.isPending ? 'Comparing...' : 'Compare Selected Markets'}
          </button>
        </div>
      </form>

      {/* Comparison Results */}
      {comparison && (
        <div className="space-y-6">
          {/* Ranking Header Card */}
          <div className="p-5 bg-gradient-to-r from-slate-900 via-slate-800 to-slate-900 rounded-xl border border-emerald-500/40 flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4 shadow-lg">
            <div className="flex items-center gap-3">
              <div className="p-3 bg-emerald-500/20 text-emerald-300 rounded-xl border border-emerald-500/30">
                <Award className="w-7 h-7" />
              </div>
              <div>
                <h3 className="text-lg font-bold text-white">Market Profitability Ranking</h3>
                <p className="text-xs text-emerald-400 font-medium">{comparison.rankingSummary}</p>
              </div>
            </div>
            <div className="text-xs text-slate-400 italic">
              * Ranked by highest estimated profit
            </div>
          </div>

          {/* Markets Comparison Table / List */}
          <div className="grid grid-cols-1 gap-4">
            {comparison.evaluations.map((evalItem, idx) => {
              const isTop = idx === 0 && evalItem.status === 'SUCCESS';
              return (
                <div
                  key={evalItem.market?.id || idx}
                  className={`p-5 rounded-xl border transition-all ${
                    isTop
                      ? 'bg-slate-800/90 border-emerald-500/60 shadow-lg shadow-emerald-950/40 ring-1 ring-emerald-500/30'
                      : 'bg-slate-800/50 border-slate-700/50 hover:border-slate-600'
                  }`}
                >
                  <div className="flex flex-col md:flex-row md:items-center justify-between gap-4">
                    <div className="space-y-1">
                      <div className="flex items-center gap-2">
                        {isTop && <CheckCircle className="w-5 h-5 text-emerald-400 shrink-0" />}
                        <h4 className="text-lg font-bold text-white">{evalItem.market?.name}</h4>
                        {isTop && (
                          <span className="px-2.5 py-0.5 rounded-full text-[10px] font-extrabold bg-emerald-500/20 text-emerald-300 border border-emerald-500/40">
                            HIGHEST ESTIMATED PROFIT
                          </span>
                        )}
                      </div>
                      <div className="text-xs text-slate-400">
                        Location: {evalItem.market?.district}, {evalItem.market?.state}
                      </div>
                    </div>

                    {evalItem.status !== 'SUCCESS' ? (
                      <div className="text-xs text-amber-400 bg-amber-500/10 px-3 py-1.5 rounded-lg border border-amber-500/20">
                        {evalItem.message}
                      </div>
                    ) : (
                      <div className="flex flex-wrap items-center gap-6">
                        <div>
                          <span className="text-[10px] text-slate-400 block">Market Price</span>
                          <strong className="text-sm font-bold text-white">
                            ₹{evalItem.selectedPrice?.toLocaleString('en-IN')} / {evalItem.priceUnit}
                          </strong>
                        </div>

                        <div>
                          <span className="text-[10px] text-slate-400 block">Gross Revenue</span>
                          <strong className="text-sm font-bold text-blue-400">
                            ₹{evalItem.grossRevenue?.toLocaleString('en-IN')}
                          </strong>
                        </div>

                        <div>
                          <span className="text-[10px] text-slate-400 block">Estimated Profit</span>
                          <strong
                            className={`text-base font-extrabold ${
                              (evalItem.estimatedProfit || 0) >= 0 ? 'text-emerald-400' : 'text-red-400'
                            }`}
                          >
                            ₹{evalItem.estimatedProfit?.toLocaleString('en-IN')}
                          </strong>
                        </div>

                        <div>
                          <span className="text-[10px] text-slate-400 block">Estimated ROI</span>
                          <strong className="text-sm font-bold text-emerald-300">
                            {evalItem.roi !== null && evalItem.roi !== undefined ? `${evalItem.roi}%` : 'N/A'}
                          </strong>
                        </div>
                      </div>
                    )}
                  </div>
                </div>
              );
            })}
          </div>

          <div className="p-4 bg-slate-900/60 border border-slate-800 rounded-xl text-xs text-slate-400 flex items-start gap-2.5">
            <ShieldAlert className="w-4 h-4 text-amber-400 shrink-0 mt-0.5" />
            <div>
              <strong className="text-slate-300">Market Ranking Disclaimer:</strong> Ranking represents "Highest estimated profit based on available verified price data and entered costs". Actual market realizations may vary due to quality grading, local transportation costs, and mandi commissions.
            </div>
          </div>
        </div>
      )}
    </div>
  );
};
