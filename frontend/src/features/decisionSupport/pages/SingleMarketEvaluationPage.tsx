import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useCrops } from '../../prices/hooks/useCrops';
import { useMarkets } from '../../markets/hooks/useMarkets';
import { useEvaluateMarket } from '../hooks/useDecisionSupport';
import { ProfitabilitySummaryCard } from '../components/ProfitabilitySummaryCard';
import type { MarketEvaluationResponse } from '../types/decisionSupport';

export const SingleMarketEvaluationPage: React.FC = () => {
  const navigate = useNavigate();
  const { data: crops = [], isLoading: isLoadingCrops } = useCrops();
  const { data: markets = [], isLoading: isLoadingMarkets } = useMarkets();
  const evaluateMutation = useEvaluateMarket();

  const [cropId, setCropId] = useState<string>('');
  const [marketId, setMarketId] = useState<string>('');
  const [quantity, setQuantity] = useState<number>(10);
  const [quantityUnit, setQuantityUnit] = useState<string>('QUINTAL');
  const [priceBasis, setPriceBasis] = useState<'MODAL' | 'MIN' | 'MAX'>('MODAL');
  const [priceMode, setPriceMode] = useState<'LATEST_AVAILABLE' | 'EXACT_DATE'>('LATEST_AVAILABLE');
  const [date, setDate] = useState<string>('');
  const [transportationCost, setTransportationCost] = useState<number>(300);
  const [otherSellingCosts, setOtherSellingCosts] = useState<number>(100);

  const [result, setResult] = useState<MarketEvaluationResponse | null>(null);
  const [formError, setFormError] = useState<string | null>(null);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setFormError(null);

    if (!cropId) {
      setFormError('Please select a crop.');
      return;
    }
    if (!marketId) {
      setFormError('Please select a market.');
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

    try {
      const res = await evaluateMutation.mutateAsync({
        cropId,
        marketId,
        quantity,
        quantityUnit,
        priceBasis,
        priceMode,
        date: priceMode === 'EXACT_DATE' ? date : null,
        transportationCost: transportationCost >= 0 ? transportationCost : 0,
        otherSellingCosts: otherSellingCosts >= 0 ? otherSellingCosts : 0,
      });
      setResult(res);
    } catch (err: unknown) {
      const errorObj = err as { response?: { data?: { message?: string } | string }; message?: string };
      const msg =
        (typeof errorObj.response?.data === 'object' ? errorObj.response?.data?.message : errorObj.response?.data) ||
        errorObj.message ||
        'Failed to evaluate market profitability.';
      setFormError(msg);
    }
  };

  return (
    <div className="min-h-screen bg-slate-950 text-slate-100 py-8 px-4 sm:px-6 lg:px-8">
      <div className="max-w-4xl mx-auto space-y-8">
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
            Farmer Profitability Estimator
          </span>
        </div>

        {/* Page Header */}
        <div>
          <h1 className="text-2xl sm:text-3xl font-extrabold text-slate-100 tracking-tight">
            Single Market Selling Economics
          </h1>
          <p className="text-slate-400 text-sm mt-1">
            Calculate your estimated gross revenue, total selling costs, net realization, and break-even selling price based on verified mandi prices.
          </p>
        </div>

        {/* Form Card */}
        <div className="bg-slate-900 border border-slate-800 rounded-xl p-6 shadow-xl">
          <form onSubmit={handleSubmit} className="space-y-6">
            {formError && (
              <div className="bg-rose-950/50 border border-rose-800/80 text-rose-300 px-4 py-3 rounded-lg text-sm">
                {formError}
              </div>
            )}

            <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
              {/* Crop Selector */}
              <div>
                <label className="block text-xs font-semibold text-slate-300 uppercase tracking-wider mb-2">
                  Select Crop <span className="text-rose-400">*</span>
                </label>
                <select
                  value={cropId}
                  onChange={(e) => setCropId(e.target.value)}
                  disabled={isLoadingCrops}
                  className="w-full bg-slate-950 border border-slate-800 rounded-lg px-3 py-2.5 text-sm text-slate-100 focus:outline-none focus:border-emerald-500 transition"
                >
                  <option value="">-- Choose Crop --</option>
                  {crops.map((c) => (
                    <option key={c.id} value={c.id}>
                      {c.name} {c.category ? `(${c.category})` : ''}
                    </option>
                  ))}
                </select>
              </div>

              {/* Market Selector */}
              <div>
                <label className="block text-xs font-semibold text-slate-300 uppercase tracking-wider mb-2">
                  Select Market <span className="text-rose-400">*</span>
                </label>
                <select
                  value={marketId}
                  onChange={(e) => setMarketId(e.target.value)}
                  disabled={isLoadingMarkets}
                  className="w-full bg-slate-950 border border-slate-800 rounded-lg px-3 py-2.5 text-sm text-slate-100 focus:outline-none focus:border-emerald-500 transition"
                >
                  <option value="">-- Choose Mandi / Market --</option>
                  {markets.map((m) => (
                    <option key={m.id} value={m.id}>
                      {m.name} {m.state ? `(${m.state})` : ''}
                    </option>
                  ))}
                </select>
              </div>

              {/* Quantity & Unit */}
              <div className="grid grid-cols-2 gap-3">
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
                    className="w-full bg-slate-950 border border-slate-800 rounded-lg px-3 py-2.5 text-sm text-slate-100 focus:outline-none focus:border-emerald-500 transition"
                  />
                </div>
                <div>
                  <label className="block text-xs font-semibold text-slate-300 uppercase tracking-wider mb-2">
                    Unit <span className="text-rose-400">*</span>
                  </label>
                  <select
                    value={quantityUnit}
                    onChange={(e) => setQuantityUnit(e.target.value)}
                    className="w-full bg-slate-950 border border-slate-800 rounded-lg px-3 py-2.5 text-sm text-slate-100 focus:outline-none focus:border-emerald-500 transition"
                  >
                    <option value="QUINTAL">QUINTAL</option>
                    <option value="KG">KG</option>
                    <option value="TONNE">TONNE</option>
                    <option value="BAG">BAG</option>
                    <option value="OTHER">OTHER</option>
                  </select>
                </div>
              </div>

              {/* Price Basis */}
              <div>
                <label className="block text-xs font-semibold text-slate-300 uppercase tracking-wider mb-2">
                  Price Basis
                </label>
                <div className="grid grid-cols-3 gap-2">
                  {(['MODAL', 'MIN', 'MAX'] as const).map((basis) => (
                    <button
                      key={basis}
                      type="button"
                      onClick={() => setPriceBasis(basis)}
                      className={`py-2 text-xs font-semibold rounded-lg border transition ${
                        priceBasis === basis
                          ? 'bg-emerald-950 text-emerald-400 border-emerald-700'
                          : 'bg-slate-950 text-slate-400 border-slate-800 hover:border-slate-700'
                      }`}
                    >
                      {basis === 'MODAL' ? 'Modal Price' : basis === 'MIN' ? 'Min Price' : 'Max Price'}
                    </button>
                  ))}
                </div>
              </div>

              {/* Price Mode */}
              <div>
                <label className="block text-xs font-semibold text-slate-300 uppercase tracking-wider mb-2">
                  Price Mode
                </label>
                <div className="grid grid-cols-2 gap-2">
                  <button
                    type="button"
                    onClick={() => setPriceMode('LATEST_AVAILABLE')}
                    className={`py-2 px-3 text-xs font-semibold rounded-lg border transition ${
                      priceMode === 'LATEST_AVAILABLE'
                        ? 'bg-emerald-950 text-emerald-400 border-emerald-700'
                        : 'bg-slate-950 text-slate-400 border-slate-800 hover:border-slate-700'
                    }`}
                  >
                    Latest Available
                  </button>
                  <button
                    type="button"
                    onClick={() => setPriceMode('EXACT_DATE')}
                    className={`py-2 px-3 text-xs font-semibold rounded-lg border transition ${
                      priceMode === 'EXACT_DATE'
                        ? 'bg-emerald-950 text-emerald-400 border-emerald-700'
                        : 'bg-slate-950 text-slate-400 border-slate-800 hover:border-slate-700'
                    }`}
                  >
                    Exact Date
                  </button>
                </div>
              </div>

              {/* Date Input (Conditional) */}
              {priceMode === 'EXACT_DATE' && (
                <div>
                  <label className="block text-xs font-semibold text-slate-300 uppercase tracking-wider mb-2">
                    Exact Date <span className="text-rose-400">*</span>
                  </label>
                  <input
                    type="date"
                    value={date}
                    onChange={(e) => setDate(e.target.value)}
                    className="w-full bg-slate-950 border border-slate-800 rounded-lg px-3 py-2.5 text-sm text-slate-100 focus:outline-none focus:border-emerald-500 transition"
                  />
                </div>
              )}

              {/* Transportation Cost */}
              <div>
                <label className="block text-xs font-semibold text-slate-300 uppercase tracking-wider mb-2">
                  Transportation Cost (₹)
                </label>
                <input
                  type="number"
                  min="0"
                  step="any"
                  value={transportationCost}
                  onChange={(e) => setTransportationCost(parseFloat(e.target.value) || 0)}
                  placeholder="300"
                  className="w-full bg-slate-950 border border-slate-800 rounded-lg px-3 py-2.5 text-sm text-slate-100 focus:outline-none focus:border-emerald-500 transition"
                />
              </div>

              {/* Other Selling Costs */}
              <div>
                <label className="block text-xs font-semibold text-slate-300 uppercase tracking-wider mb-2">
                  Other Selling Costs (₹)
                </label>
                <input
                  type="number"
                  min="0"
                  step="any"
                  value={otherSellingCosts}
                  onChange={(e) => setOtherSellingCosts(parseFloat(e.target.value) || 0)}
                  placeholder="Loading, market fees, handling"
                  className="w-full bg-slate-950 border border-slate-800 rounded-lg px-3 py-2.5 text-sm text-slate-100 focus:outline-none focus:border-emerald-500 transition"
                />
              </div>
            </div>

            <button
              type="submit"
              disabled={evaluateMutation.isPending}
              className="w-full py-3 bg-emerald-600 hover:bg-emerald-500 disabled:opacity-50 text-white font-bold text-sm rounded-lg shadow-lg hover:shadow-emerald-900/40 transition duration-200"
            >
              {evaluateMutation.isPending ? 'Calculating Selling Economics...' : 'Evaluate Profitability'}
            </button>
          </form>
        </div>

        {/* Results Card */}
        {result && <ProfitabilitySummaryCard evaluation={result} />}
      </div>
    </div>
  );
};
