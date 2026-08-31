import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useCrops } from '../../prices/hooks/useCrops';
import { useMarkets } from '../../markets/hooks/useMarkets';
import { useCompareMarkets } from '../hooks/useDecisionSupport';
import type { MarketCostInput, MarketProfitabilityComparisonResponse } from '../types/decisionSupport';

export const MarketComparisonPage: React.FC = () => {
  const navigate = useNavigate();
  const { data: crops = [], isLoading: isLoadingCrops } = useCrops();
  const { data: markets = [], isLoading: isLoadingMarkets } = useMarkets();
  const compareMutation = useCompareMarkets();

  const [cropId, setCropId] = useState<string>('');
  const [quantity, setQuantity] = useState<number>(10);
  const [quantityUnit, setQuantityUnit] = useState<string>('QUINTAL');
  const [priceBasis, setPriceBasis] = useState<'MODAL' | 'MIN' | 'MAX'>('MODAL');
  const [priceMode, setPriceMode] = useState<'LATEST_AVAILABLE' | 'EXACT_DATE'>('LATEST_AVAILABLE');
  const [date, setDate] = useState<string>('');

  const [selectedMarketCosts, setSelectedMarketCosts] = useState<MarketCostInput[]>([
    { marketId: '', transportationCost: 300, otherSellingCosts: 100 },
    { marketId: '', transportationCost: 100, otherSellingCosts: 50 },
  ]);

  const [comparisonResult, setComparisonResult] = useState<MarketProfitabilityComparisonResponse | null>(null);
  const [formError, setFormError] = useState<string | null>(null);

  const handleAddMarket = () => {
    if (selectedMarketCosts.length >= 5) return;
    setSelectedMarketCosts((prev) => [...prev, { marketId: '', transportationCost: 200, otherSellingCosts: 50 }]);
  };

  const handleRemoveMarket = (index: number) => {
    if (selectedMarketCosts.length <= 2) return;
    setSelectedMarketCosts((prev) => prev.filter((_, i) => i !== index));
  };

  const handleMarketChange = (index: number, field: keyof MarketCostInput, value: string | number) => {
    setSelectedMarketCosts((prev) => {
      const next = [...prev];
      next[index] = { ...next[index], [field]: value };
      return next;
    });
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setFormError(null);

    if (!cropId) {
      setFormError('Please select a crop for comparison.');
      return;
    }
    if (quantity <= 0) {
      setFormError('Quantity must be greater than zero.');
      return;
    }
    if (priceMode === 'EXACT_DATE' && !date) {
      setFormError('Date is required when Exact Date mode is selected.');
      return;
    }

    const validMarkets = selectedMarketCosts.filter((m) => m.marketId.trim() !== '');
    if (validMarkets.length < 2) {
      setFormError('Please select at least 2 distinct markets for comparison.');
      return;
    }

    try {
      const res = await compareMutation.mutateAsync({
        cropId,
        quantity,
        quantityUnit,
        priceBasis,
        priceMode,
        date: priceMode === 'EXACT_DATE' ? date : null,
        markets: validMarkets.map((m) => ({
          marketId: m.marketId,
          transportationCost: Number(m.transportationCost) >= 0 ? Number(m.transportationCost) : 0,
          otherSellingCosts: Number(m.otherSellingCosts) >= 0 ? Number(m.otherSellingCosts) : 0,
        })),
      });
      setComparisonResult(res);
    } catch (err: unknown) {
      const errorObj = err as { response?: { data?: { message?: string } | string }; message?: string };
      const msg =
        (typeof errorObj.response?.data === 'object' ? errorObj.response?.data?.message : errorObj.response?.data) ||
        errorObj.message ||
        'Failed to compare markets.';
      setFormError(msg);
    }
  };

  return (
    <div className="min-h-screen bg-slate-950 text-slate-100 py-8 px-4 sm:px-6 lg:px-8">
      <div className="max-w-5xl mx-auto space-y-8">
        {/* Navigation Breadcrumb */}
        <div className="flex items-center justify-between">
          <button
            onClick={() => navigate('/decision-support')}
            className="flex items-center space-x-2 text-slate-400 hover:text-emerald-400 text-sm font-medium transition"
          >
            <svg className="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M10 19l-7-7m0 0l7-7m-7 7h18" />
            </svg>
            <span>Back to Decision Support</span>
          </button>
          <span className="text-xs font-semibold px-3 py-1 bg-emerald-950 text-emerald-400 border border-emerald-800/60 rounded-full">
            Multi-Market Comparison
          </span>
        </div>

        {/* Header */}
        <div>
          <h1 className="text-2xl sm:text-3xl font-extrabold text-slate-100 tracking-tight">
            Multi-Market Profitability Comparison
          </h1>
          <p className="text-slate-400 text-sm mt-1">
            Compare estimated net realizations across multiple markets after market-specific transport and handling costs.
          </p>
        </div>

        {/* Input Form */}
        <div className="bg-slate-900 border border-slate-800 rounded-xl p-6 shadow-xl space-y-6">
          <form onSubmit={handleSubmit} className="space-y-6">
            {formError && (
              <div className="bg-rose-950/50 border border-rose-800/80 text-rose-300 px-4 py-3 rounded-lg text-sm">
                {formError}
              </div>
            )}

            {/* Global Crop & Basis Parameters */}
            <div className="grid grid-cols-1 md:grid-cols-3 gap-4 pb-4 border-b border-slate-800">
              <div>
                <label className="block text-xs font-semibold text-slate-300 uppercase tracking-wider mb-2">
                  Crop <span className="text-rose-400">*</span>
                </label>
                <select
                  value={cropId}
                  onChange={(e) => setCropId(e.target.value)}
                  disabled={isLoadingCrops}
                  className="w-full bg-slate-950 border border-slate-800 rounded-lg px-3 py-2 text-sm text-slate-100 focus:outline-none focus:border-emerald-500"
                >
                  <option value="">-- Choose Crop --</option>
                  {crops.map((c) => (
                    <option key={c.id} value={c.id}>
                      {c.name} {c.category ? `(${c.category})` : ''}
                    </option>
                  ))}
                </select>
              </div>

              <div className="grid grid-cols-2 gap-2">
                <div>
                  <label className="block text-xs font-semibold text-slate-300 uppercase tracking-wider mb-2">
                    Quantity <span className="text-rose-400">*</span>
                  </label>
                  <input
                    type="number"
                    step="any"
                    min="0.01"
                    value={quantity}
                    onChange={(e) => setQuantity(parseFloat(e.target.value) || 0)}
                    className="w-full bg-slate-950 border border-slate-800 rounded-lg px-3 py-2 text-sm text-slate-100 focus:outline-none focus:border-emerald-500"
                  />
                </div>
                <div>
                  <label className="block text-xs font-semibold text-slate-300 uppercase tracking-wider mb-2">
                    Unit <span className="text-rose-400">*</span>
                  </label>
                  <select
                    value={quantityUnit}
                    onChange={(e) => setQuantityUnit(e.target.value)}
                    className="w-full bg-slate-950 border border-slate-800 rounded-lg px-3 py-2 text-sm text-slate-100 focus:outline-none focus:border-emerald-500"
                  >
                    <option value="QUINTAL">QUINTAL</option>
                    <option value="KG">KG</option>
                    <option value="TONNE">TONNE</option>
                    <option value="BAG">BAG</option>
                    <option value="OTHER">OTHER</option>
                  </select>
                </div>
              </div>

              <div>
                <label className="block text-xs font-semibold text-slate-300 uppercase tracking-wider mb-2">
                  Price Basis & Mode
                </label>
                <div className="grid grid-cols-2 gap-2">
                  <select
                    value={priceBasis}
                    onChange={(e) => setPriceBasis(e.target.value as 'MODAL' | 'MIN' | 'MAX')}
                    className="bg-slate-950 border border-slate-800 rounded-lg px-2.5 py-2 text-xs font-semibold text-slate-200"
                  >
                    <option value="MODAL">Modal Price</option>
                    <option value="MIN">Min Price</option>
                    <option value="MAX">Max Price</option>
                  </select>
                  <select
                    value={priceMode}
                    onChange={(e) => setPriceMode(e.target.value as 'LATEST_AVAILABLE' | 'EXACT_DATE')}
                    className="bg-slate-950 border border-slate-800 rounded-lg px-2.5 py-2 text-xs font-semibold text-slate-200"
                  >
                    <option value="LATEST_AVAILABLE">Latest Available</option>
                    <option value="EXACT_DATE">Exact Date</option>
                  </select>
                </div>
                {priceMode === 'EXACT_DATE' && (
                  <input
                    type="date"
                    value={date}
                    onChange={(e) => setDate(e.target.value)}
                    className="mt-2 w-full bg-slate-950 border border-slate-800 rounded-lg px-3 py-1.5 text-xs text-slate-100"
                  />
                )}
              </div>
            </div>

            {/* Markets List Section */}
            <div className="space-y-4">
              <div className="flex items-center justify-between">
                <h3 className="text-sm font-bold text-slate-200 uppercase tracking-wider">
                  Selected Markets ({selectedMarketCosts.length})
                </h3>
                {selectedMarketCosts.length < 5 && (
                  <button
                    type="button"
                    onClick={handleAddMarket}
                    className="text-xs font-semibold text-emerald-400 hover:text-emerald-300 flex items-center space-x-1"
                  >
                    <span>+ Add Market</span>
                  </button>
                )}
              </div>

              {selectedMarketCosts.map((mc, idx) => (
                <div key={idx} className="bg-slate-950/60 p-4 rounded-xl border border-slate-800 grid grid-cols-1 sm:grid-cols-12 gap-3 items-center">
                  <div className="sm:col-span-5">
                    <label className="block text-[11px] font-medium text-slate-400 mb-1">
                      Market #{idx + 1}
                    </label>
                    <select
                      value={mc.marketId}
                      onChange={(e) => handleMarketChange(idx, 'marketId', e.target.value)}
                      disabled={isLoadingMarkets}
                      className="w-full bg-slate-900 border border-slate-800 rounded-lg px-3 py-2 text-sm text-slate-100 focus:outline-none focus:border-emerald-500"
                    >
                      <option value="">-- Select Market --</option>
                      {markets.map((m) => (
                        <option key={m.id} value={m.id}>
                          {m.name} {m.state ? `(${m.state})` : ''}
                        </option>
                      ))}
                    </select>
                  </div>

                  <div className="sm:col-span-3">
                    <label className="block text-[11px] font-medium text-slate-400 mb-1">
                      Transport Cost (₹)
                    </label>
                    <input
                      type="number"
                      min="0"
                      value={mc.transportationCost}
                      onChange={(e) => handleMarketChange(idx, 'transportationCost', parseFloat(e.target.value) || 0)}
                      className="w-full bg-slate-900 border border-slate-800 rounded-lg px-3 py-2 text-sm text-slate-100"
                    />
                  </div>

                  <div className="sm:col-span-3">
                    <label className="block text-[11px] font-medium text-slate-400 mb-1">
                      Other Costs (₹)
                    </label>
                    <input
                      type="number"
                      min="0"
                      value={mc.otherSellingCosts}
                      onChange={(e) => handleMarketChange(idx, 'otherSellingCosts', parseFloat(e.target.value) || 0)}
                      className="w-full bg-slate-900 border border-slate-800 rounded-lg px-3 py-2 text-sm text-slate-100"
                    />
                  </div>

                  <div className="sm:col-span-1 flex justify-end">
                    {selectedMarketCosts.length > 2 && (
                      <button
                        type="button"
                        onClick={() => handleRemoveMarket(idx)}
                        className="text-slate-500 hover:text-rose-400 p-1"
                        title="Remove market"
                      >
                        <svg className="w-5 h-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                          <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16" />
                        </svg>
                      </button>
                    )}
                  </div>
                </div>
              ))}
            </div>

            <button
              type="submit"
              disabled={compareMutation.isPending}
              className="w-full py-3 bg-emerald-600 hover:bg-emerald-500 disabled:opacity-50 text-white font-bold text-sm rounded-lg shadow-lg transition"
            >
              {compareMutation.isPending ? 'Comparing Markets...' : 'Compare Market Profitability'}
            </button>
          </form>
        </div>

        {/* Results Section */}
        {comparisonResult && (
          <div className="space-y-6">
            {/* Top Market Badge */}
            {comparisonResult.topRealizationMarket && (
              <div className="bg-emerald-950/40 border border-emerald-800/60 rounded-xl p-5 shadow-lg flex flex-col md:flex-row items-start md:items-center justify-between gap-4">
                <div>
                  <span className="text-xs font-bold uppercase tracking-wider text-emerald-400 bg-emerald-900/60 px-3 py-1 rounded-full border border-emerald-700/50">
                    Highest Estimated Net Realization Based On Your Entered Costs
                  </span>
                  <h3 className="text-xl font-extrabold text-slate-100 mt-2">
                    {comparisonResult.topRealizationMarket.market?.name}
                  </h3>
                  <p className="text-xs text-slate-400 mt-1">
                    Verified Price: ₹{comparisonResult.topRealizationMarket.selectedPrice} / {comparisonResult.topRealizationMarket.priceUnit} ({comparisonResult.topRealizationMarket.priceDate})
                  </p>
                </div>
                <div className="text-right">
                  <p className="text-2xl font-black text-emerald-400">
                    ₹{comparisonResult.topRealizationMarket.estimatedNetRealization?.toLocaleString('en-IN')}
                  </p>
                  <p className="text-xs text-slate-400 mt-1">
                    Net / Unit: ₹{comparisonResult.topRealizationMarket.netRealizationPerUnit?.toLocaleString('en-IN')} / {comparisonResult.quantityUnit}
                  </p>
                </div>
              </div>
            )}

            {/* Comparison Table */}
            <div className="bg-slate-900 border border-slate-800 rounded-xl overflow-hidden shadow-xl">
              <div className="p-4 border-b border-slate-800 flex justify-between items-center">
                <h3 className="text-sm font-bold text-slate-200 uppercase tracking-wider">
                  Comparative Results (Ranked by Estimated Net Realization)
                </h3>
              </div>
              <div className="overflow-x-auto">
                <table className="w-full text-left text-sm text-slate-300">
                  <thead className="bg-slate-950 text-slate-400 text-xs uppercase font-semibold border-b border-slate-800">
                    <tr>
                      <th className="py-3.5 px-4">Rank</th>
                      <th className="py-3.5 px-4">Market</th>
                      <th className="py-3.5 px-4">Verified Price</th>
                      <th className="py-3.5 px-4">Selling Costs</th>
                      <th className="py-3.5 px-4 text-right">Est. Gross Revenue</th>
                      <th className="py-3.5 px-4 text-right">Est. Net Realization</th>
                      <th className="py-3.5 px-4 text-right">Net / Unit</th>
                      <th className="py-3.5 px-4 text-right">Selling Break-Even</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-slate-800">
                    {comparisonResult.evaluations.map((evalItem, i) => {
                      if (evalItem.status !== 'SUCCESS') {
                        return (
                          <tr key={i} className="bg-slate-950/40 text-slate-500">
                            <td className="py-3.5 px-4 font-mono text-xs">{i + 1}</td>
                            <td className="py-3.5 px-4 font-medium text-slate-400">{evalItem.market?.name || 'Selected Mandi'}</td>
                            <td colSpan={6} className="py-3.5 px-4 text-amber-400 text-xs font-medium">
                              {evalItem.status === 'NO_VERIFIED_PRICE' ? 'No verified price available' : evalItem.status === 'UNIT_MISMATCH' ? 'Price/Quantity unit mismatch' : evalItem.message}
                            </td>
                          </tr>
                        );
                      }

                      const isWinner = i === 0;
                      return (
                        <tr key={i} className={isWinner ? 'bg-emerald-950/20 font-semibold' : 'hover:bg-slate-800/40'}>
                          <td className="py-3.5 px-4 font-mono text-xs text-slate-400">#{i + 1}</td>
                          <td className="py-3.5 px-4">
                            <div className="font-semibold text-slate-200">{evalItem.market?.name}</div>
                            <div className="text-[11px] text-slate-400">{evalItem.market?.state}</div>
                          </td>
                          <td className="py-3.5 px-4">
                            <div className="text-slate-200">₹{evalItem.selectedPrice} / {evalItem.priceUnit}</div>
                            <div className="text-[11px] text-slate-400">{evalItem.priceDate}</div>
                          </td>
                          <td className="py-3.5 px-4">
                            <div className="text-xs text-slate-300">Total: ₹{evalItem.totalSellingCosts}</div>
                            <div className="text-[11px] text-slate-500">Tr: ₹{evalItem.transportationCost} | Oth: ₹{evalItem.otherSellingCosts}</div>
                          </td>
                          <td className="py-3.5 px-4 text-right font-medium text-slate-200">
                            ₹{evalItem.grossRevenue?.toLocaleString('en-IN')}
                          </td>
                          <td className="py-3.5 px-4 text-right font-bold text-emerald-400">
                            ₹{evalItem.estimatedNetRealization?.toLocaleString('en-IN')}
                          </td>
                          <td className="py-3.5 px-4 text-right text-slate-200">
                            ₹{evalItem.netRealizationPerUnit?.toLocaleString('en-IN')}
                          </td>
                          <td className="py-3.5 px-4 text-right text-sky-400 text-xs">
                            ₹{evalItem.sellingCostBreakEvenPrice?.toLocaleString('en-IN')}
                          </td>
                        </tr>
                      );
                    })}
                  </tbody>
                </table>
              </div>
            </div>

            {/* Disclaimer */}
            <div className="text-xs text-slate-400 bg-slate-900 border border-slate-800 rounded-xl p-4 italic">
              {comparisonResult.disclaimer}
            </div>
          </div>
        )}
      </div>
    </div>
  );
};
