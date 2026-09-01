import React, { useState } from 'react';
import { useParams, useNavigate, Link } from 'react-router-dom';
import { useFarmEconomic, useEvaluateFarmEconomics } from '../hooks/useFarmEconomics';
import { useMarkets } from '../../markets/hooks/useMarkets';
import {
  ArrowLeft,
  Sprout,
  BarChart2,
  AlertCircle,
  TrendingUp,
  DollarSign,
  Info,
  Layers,
} from 'lucide-react';

export const FarmEconomicDetailPage: React.FC = () => {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();

  const { data: record, isLoading, error } = useFarmEconomic(id || '');
  const { data: markets } = useMarkets();
  const evaluateMutation = useEvaluateFarmEconomics();

  const [selectedMarketId, setSelectedMarketId] = useState('');
  const [priceBasis, setPriceBasis] = useState<'MIN' | 'MODAL' | 'MAX'>('MODAL');
  const [priceMode, setPriceMode] = useState<'LATEST_AVAILABLE' | 'EXACT_DATE'>('LATEST_AVAILABLE');
  const [date, setDate] = useState('');

  const evaluation = evaluateMutation.data;

  const handleEvaluate = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!id || !selectedMarketId) return;

    await evaluateMutation.mutateAsync({
      economicRecordId: id,
      marketId: selectedMarketId,
      priceBasis,
      priceMode,
      date: priceMode === 'EXACT_DATE' ? date : undefined,
    });
  };

  if (isLoading) {
    return (
      <div className="flex items-center justify-center min-h-[400px]">
        <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-emerald-500"></div>
      </div>
    );
  }

  if (error || !record) {
    return (
      <div className="p-6 bg-red-500/10 border border-red-500/20 rounded-xl text-red-400 flex items-center gap-3">
        <AlertCircle className="w-6 h-6" />
        <span>Failed to load economic record.</span>
      </div>
    );
  }

  return (
    <div className="space-y-6 max-w-5xl mx-auto">
      {/* Top Header */}
      <div className="flex items-center justify-between">
        <div className="flex items-center gap-4">
          <button
            onClick={() => navigate('/farm-economics')}
            className="p-2 bg-slate-800 hover:bg-slate-700 text-slate-300 rounded-lg transition-colors"
          >
            <ArrowLeft className="w-5 h-5" />
          </button>
          <div>
            <h1 className="text-2xl font-bold text-white flex items-center gap-2">
              <Sprout className="text-emerald-400 w-6 h-6" />
              {record.cropName || 'Crop Economics'}
            </h1>
            <p className="text-slate-400 text-sm">
              Farm: <strong className="text-slate-200">{record.farmName || record.farmId}</strong> | Season: <strong className="text-emerald-400">{record.season}</strong>
            </p>
          </div>
        </div>

        <Link
          to={`/farm-economics/compare?recordId=${record.id}`}
          className="inline-flex items-center gap-2 px-4 py-2.5 bg-blue-600 hover:bg-blue-500 text-white font-medium rounded-lg transition-colors text-sm shadow-md"
        >
          <BarChart2 className="w-4 h-4" />
          Compare Markets
        </Link>
      </div>

      {/* Production & Selling Cost Breakdown Card */}
      <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
        {/* Production Costs */}
        <div className="bg-slate-800/60 border border-slate-700/60 rounded-xl p-6 space-y-4 shadow-md">
          <h2 className="text-lg font-semibold text-white border-b border-slate-700/60 pb-3 flex items-center gap-2">
            <Layers className="w-5 h-5 text-amber-400" />
            Production Costs by Category
          </h2>

          <div className="space-y-2">
            {record.productionCosts?.map((c, i) => (
              <div key={i} className="flex justify-between items-center py-2 px-3 bg-slate-900/50 rounded-lg text-sm">
                <div>
                  <span className="font-semibold text-slate-200">{c.category}</span>
                  {c.description && <span className="text-slate-400 text-xs block">{c.description}</span>}
                </div>
                <span className="font-bold text-amber-400">₹{(c.amount || 0).toLocaleString('en-IN')}</span>
              </div>
            ))}
          </div>

          <div className="border-t border-slate-700/60 pt-3 flex justify-between items-center text-sm font-bold">
            <span className="text-slate-300">Total Production Cost:</span>
            <span className="text-amber-400 text-lg">₹{(record.totalProductionCost || 0).toLocaleString('en-IN')}</span>
          </div>
        </div>

        {/* Allocations & Selling Costs */}
        <div className="bg-slate-800/60 border border-slate-700/60 rounded-xl p-6 space-y-4 shadow-md flex flex-col justify-between">
          <div className="space-y-4">
            <h2 className="text-lg font-semibold text-white border-b border-slate-700/60 pb-3 flex items-center gap-2">
              <DollarSign className="w-5 h-5 text-blue-400" />
              Allocation & Selling Expenses
            </h2>

            <div className="space-y-3 text-sm">
              <div className="flex justify-between py-1 border-b border-slate-700/40 text-slate-300">
                <span>Cultivated Area:</span>
                <strong className="text-white">{record.cultivatedArea} {record.cultivatedAreaUnit}</strong>
              </div>
              <div className="flex justify-between py-1 border-b border-slate-700/40 text-slate-300">
                <span>Expected Yield:</span>
                <strong className="text-white">{record.expectedYield} {record.yieldUnit}</strong>
              </div>
              <div className="flex justify-between py-1 border-b border-slate-700/40 text-slate-300">
                <span>Transportation Cost:</span>
                <strong className="text-blue-400">₹{(record.sellingCosts?.transportationCost || 0).toLocaleString('en-IN')}</strong>
              </div>
              <div className="flex justify-between py-1 border-b border-slate-700/40 text-slate-300">
                <span>Other Selling Costs:</span>
                <strong className="text-blue-400">₹{(record.sellingCosts?.otherSellingCosts || 0).toLocaleString('en-IN')}</strong>
              </div>
            </div>
          </div>

          <div className="bg-slate-900/60 p-4 rounded-xl border border-slate-700/50 space-y-2 mt-4">
            <div className="flex justify-between text-xs text-slate-400">
              <span>Total Crop Costs (Production + Selling):</span>
              <strong className="text-amber-300">₹{(record.totalCost || 0).toLocaleString('en-IN')}</strong>
            </div>
            <div className="flex justify-between text-sm font-bold text-white pt-1 border-t border-slate-800">
              <span>Break-Even Selling Price:</span>
              <span className="text-emerald-400">₹{(record.breakEvenSellingPrice || 0).toLocaleString('en-IN')} / {record.yieldUnit}</span>
            </div>
          </div>
        </div>
      </div>

      {/* Evaluate Profitability Section */}
      <div className="bg-slate-800/60 border border-slate-700/60 rounded-xl p-6 space-y-6 shadow-lg">
        <div className="flex items-center justify-between border-b border-slate-700/60 pb-3">
          <div>
            <h2 className="text-xl font-bold text-white flex items-center gap-2">
              <TrendingUp className="w-6 h-6 text-emerald-400" />
              Live Market Profitability Evaluator
            </h2>
            <p className="text-slate-400 text-xs mt-0.5">
              Select a target market to evaluate expected revenue, net profit, and estimated ROI using verified market prices.
            </p>
          </div>
        </div>

        <form onSubmit={handleEvaluate} className="grid grid-cols-1 sm:grid-cols-4 gap-4 items-end bg-slate-900/50 p-4 rounded-xl border border-slate-700/40">
          <div>
            <label className="block text-xs font-medium text-slate-300 mb-1">Target Market *</label>
            <select
              value={selectedMarketId}
              onChange={(e) => setSelectedMarketId(e.target.value)}
              required
              className="w-full px-3 py-2 bg-slate-900 border border-slate-700 text-white rounded-lg text-xs focus:outline-none focus:border-emerald-500"
            >
              <option value="">-- Choose Market --</option>
              {markets?.map((m: any) => (
                <option key={m.id} value={m.id}>
                  {m.name} ({m.location?.district})
                </option>
              ))}
            </select>
          </div>

          <div>
            <label className="block text-xs font-medium text-slate-300 mb-1">Price Basis</label>
            <select
              value={priceBasis}
              onChange={(e) => setPriceBasis(e.target.value as any)}
              className="w-full px-3 py-2 bg-slate-900 border border-slate-700 text-white rounded-lg text-xs focus:outline-none focus:border-emerald-500"
            >
              <option value="MODAL">MODAL Price (Recommended)</option>
              <option value="MIN">MIN Price</option>
              <option value="MAX">MAX Price</option>
            </select>
          </div>

          <div>
            <label className="block text-xs font-medium text-slate-300 mb-1">Price Date Mode</label>
            <select
              value={priceMode}
              onChange={(e) => setPriceMode(e.target.value as any)}
              className="w-full px-3 py-2 bg-slate-900 border border-slate-700 text-white rounded-lg text-xs focus:outline-none focus:border-emerald-500"
            >
              <option value="LATEST_AVAILABLE">Latest Available Price</option>
              <option value="EXACT_DATE">Exact Date</option>
            </select>
          </div>

          {priceMode === 'EXACT_DATE' && (
            <div>
              <label className="block text-xs font-medium text-slate-300 mb-1">Select Date</label>
              <input
                type="date"
                value={date}
                onChange={(e) => setDate(e.target.value)}
                required
                className="w-full px-3 py-2 bg-slate-900 border border-slate-700 text-white rounded-lg text-xs focus:outline-none focus:border-emerald-500"
              />
            </div>
          )}

          <button
            type="submit"
            disabled={evaluateMutation.isPending || !selectedMarketId}
            className="w-full py-2 px-4 bg-emerald-600 hover:bg-emerald-500 text-white text-xs font-semibold rounded-lg transition-colors shadow-md disabled:opacity-50"
          >
            {evaluateMutation.isPending ? 'Calculating...' : 'Evaluate Profitability'}
          </button>
        </form>

        {/* Evaluation Output */}
        {evaluation && (
          <div className="space-y-6 pt-2">
            {evaluation.status !== 'SUCCESS' ? (
              <div className="p-4 bg-amber-500/10 border border-amber-500/20 rounded-xl text-amber-300 flex items-center gap-3 text-sm">
                <AlertCircle className="w-5 h-5 shrink-0" />
                <span>{evaluation.message || 'No verified price data available for selected market.'}</span>
              </div>
            ) : (
              <>
                {/* Warnings / Badges */}
                {evaluation.stalePrice && (
                  <div className="p-3 bg-amber-500/10 border border-amber-500/20 rounded-lg text-amber-300 text-xs flex items-center gap-2">
                    <Info className="w-4 h-4 shrink-0" />
                    <span>{evaluation.staleMessage}</span>
                  </div>
                )}

                {/* Primary Profit Metrics */}
                <div className="grid grid-cols-1 sm:grid-cols-4 gap-4">
                  <div className="p-4 bg-slate-900/70 border border-slate-700/50 rounded-xl">
                    <div className="text-xs text-slate-400">Verified Market Price</div>
                    <div className="text-xl font-bold text-white mt-1">
                      ₹{evaluation.selectedPrice?.toLocaleString('en-IN')}
                    </div>
                    <div className="text-xs text-slate-400">{evaluation.priceBasis} ({evaluation.priceUnit})</div>
                  </div>

                  <div className="p-4 bg-slate-900/70 border border-slate-700/50 rounded-xl">
                    <div className="text-xs text-slate-400">Gross Revenue</div>
                    <div className="text-xl font-bold text-blue-400 mt-1">
                      ₹{evaluation.grossRevenue?.toLocaleString('en-IN')}
                    </div>
                    <div className="text-xs text-slate-400">{evaluation.expectedYield} {evaluation.yieldUnit}</div>
                  </div>

                  <div className="p-4 bg-slate-900/70 border border-slate-700/50 rounded-xl">
                    <div className="text-xs text-slate-400">Estimated Net Profit</div>
                    <div
                      className={`text-xl font-bold mt-1 ${
                        (evaluation.estimatedProfit || 0) >= 0 ? 'text-emerald-400' : 'text-red-400'
                      }`}
                    >
                      ₹{evaluation.estimatedProfit?.toLocaleString('en-IN')}
                    </div>
                    <div className="text-xs text-slate-400">After production & selling costs</div>
                  </div>

                  <div className="p-4 bg-slate-900/70 border border-slate-700/50 rounded-xl">
                    <div className="text-xs text-slate-400">Estimated ROI</div>
                    <div
                      className={`text-xl font-bold mt-1 ${
                        (evaluation.roi || 0) >= 0 ? 'text-emerald-400' : 'text-red-400'
                      }`}
                    >
                      {evaluation.roi !== null && evaluation.roi !== undefined ? `${evaluation.roi}%` : 'N/A'}
                    </div>
                    <div className="text-xs text-slate-400">Return on Total Cost</div>
                  </div>
                </div>

                {/* Detailed Profitability Status Card */}
                <div className="p-5 bg-slate-900/80 rounded-xl border border-slate-700/60 space-y-3">
                  <div className="flex items-center justify-between">
                    <span className="text-sm font-semibold text-slate-300">Profitability Classification</span>
                    <span
                      className={`px-3 py-1 rounded-full text-xs font-bold ${
                        evaluation.profitabilityStatus === 'PROFITABLE'
                          ? 'bg-emerald-500/20 text-emerald-300 border border-emerald-500/40'
                          : evaluation.profitabilityStatus === 'BREAK_EVEN'
                          ? 'bg-blue-500/20 text-blue-300 border border-blue-500/40'
                          : 'bg-red-500/20 text-red-300 border border-red-500/40'
                      }`}
                    >
                      {evaluation.profitabilityStatus === 'PROFITABLE' && 'PROFITABLE CROP'}
                      {evaluation.profitabilityStatus === 'BREAK_EVEN' && 'BREAK-EVEN CROP'}
                      {evaluation.profitabilityStatus === 'LOSS' && 'ESTIMATED LOSS'}
                    </span>
                  </div>

                  <div className="grid grid-cols-2 sm:grid-cols-4 gap-4 text-xs pt-2 border-t border-slate-800">
                    <div>
                      <span className="text-slate-400 block">Profit per Unit:</span>
                      <strong className="text-white text-sm">₹{evaluation.profitPerUnit?.toLocaleString('en-IN')} / {evaluation.yieldUnit}</strong>
                    </div>
                    <div>
                      <span className="text-slate-400 block">Production Cost/Unit:</span>
                      <strong className="text-amber-400 text-sm">₹{evaluation.productionCostPerUnit?.toLocaleString('en-IN')}</strong>
                    </div>
                    <div>
                      <span className="text-slate-400 block">Total Cost/Unit:</span>
                      <strong className="text-slate-200 text-sm">₹{evaluation.totalCostPerUnit?.toLocaleString('en-IN')}</strong>
                    </div>
                    <div>
                      <span className="text-slate-400 block">Break-Even Price:</span>
                      <strong className="text-emerald-400 text-sm">₹{evaluation.breakEvenSellingPrice?.toLocaleString('en-IN')}</strong>
                    </div>
                  </div>
                </div>
              </>
            )}
          </div>
        )}
      </div>
    </div>
  );
};
