import React, { useState } from 'react';
import { useCrops } from '../../prices/hooks/useCrops';
import { useMarkets } from '../../markets/hooks/useMarkets';
import { useFarmEconomics } from '../../farmEconomics/hooks/useFarmEconomics';
import { useAiDecision } from '../hooks/useAiDecision';
import type { AiDecisionType, AiDecisionResponse } from '../types';
import {
  Brain,
  ShoppingBag,
  TrendingUp,
  Scale,
  Lightbulb,
  CheckCircle2,
  AlertTriangle,
  ArrowRight,
  Info,
  DollarSign,
  ShieldCheck,
  RotateCcw,
} from 'lucide-react';

export const AiDecisionPage: React.FC = () => {
  const { data: crops = [] } = useCrops();
  const { data: markets = [] } = useMarkets();
  const { data: economicRecords = [] } = useFarmEconomics();
  const decisionMutation = useAiDecision();

  const [activeType, setActiveType] = useState<AiDecisionType>('MARKET_SELECTION');
  const [selectedCropId, setSelectedCropId] = useState<string>('');
  const [selectedEconomicRecordId, setSelectedEconomicRecordId] = useState<string>('');
  const [selectedMarketIds, setSelectedMarketIds] = useState<string[]>([]);
  const [quantity, setQuantity] = useState<number>(10);
  const [quantityUnit, setQuantityUnit] = useState<string>('QUINTAL');
  const [transportationCost, setTransportationCost] = useState<number>(300);
  const [otherSellingCosts, setOtherSellingCosts] = useState<number>(100);

  const [result, setResult] = useState<AiDecisionResponse | null>(null);
  const [formError, setFormError] = useState<string | null>(null);

  const toggleMarketSelection = (id: string) => {
    setSelectedMarketIds((prev) =>
      prev.includes(id) ? prev.filter((m) => m !== id) : [...prev, id]
    );
  };

  const handleRunDecision = async (e: React.FormEvent) => {
    e.preventDefault();
    setFormError(null);
    setResult(null);

    if (activeType === 'PROFITABILITY_EXPLANATION') {
      if (!selectedEconomicRecordId) {
        setFormError('Please select a saved farm economic record.');
        return;
      }
    } else {
      if (!selectedCropId && !selectedEconomicRecordId) {
        setFormError('Please select a crop or economic record.');
        return;
      }
      if ((activeType === 'MARKET_SELECTION' || activeType === 'MARKET_COMPARISON_EXPLANATION') && selectedMarketIds.length === 0) {
        setFormError('Please select at least one target market.');
        return;
      }
    }

    try {
      const res = await decisionMutation.mutateAsync({
        decisionType: activeType,
        cropId: selectedCropId || undefined,
        economicRecordId: selectedEconomicRecordId || undefined,
        marketIds: selectedMarketIds,
        quantity: quantity,
        quantityUnit: quantityUnit,
        transportationCost: transportationCost,
        otherSellingCosts: otherSellingCosts,
      });
      setResult(res);
    } catch (err: unknown) {
      const errorObj = err as { response?: { data?: { message?: string } } };
      setFormError(errorObj.response?.data?.message || 'Failed to process AI decision support. Please try again.');
    }
  };

  return (
    <div className="max-w-6xl mx-auto px-4 py-8 space-y-8">
      {/* Header Banner */}
      <div className="bg-gradient-to-r from-emerald-950 via-slate-900 to-teal-950 p-6 rounded-2xl border border-emerald-500/20 shadow-xl relative overflow-hidden">
        <div className="absolute right-0 top-0 w-96 h-96 bg-emerald-500/10 rounded-full blur-3xl -z-0"></div>
        <div className="relative z-10 space-y-2">
          <div className="inline-flex items-center gap-2 px-3 py-1 bg-emerald-500/10 border border-emerald-500/30 rounded-full text-emerald-400 text-xs font-semibold uppercase tracking-wider">
            <Brain className="w-3.5 h-3.5" /> AI Decision Intelligence Foundation
          </div>
          <h1 className="text-2xl sm:text-3xl font-extrabold text-white">
            Farmer AI Decision Assistant 🤖
          </h1>
          <p className="text-slate-300 text-sm max-w-3xl leading-relaxed">
            Data-driven reasoning and explanation layer built on top of verified market prices, net realization, and your production costs.
          </p>
        </div>
      </div>

      {/* Decision Type Tabs */}
      <div className="grid grid-cols-2 md:grid-cols-4 gap-3">
        <button
          type="button"
          onClick={() => { setActiveType('MARKET_SELECTION'); setResult(null); }}
          className={`p-4 rounded-xl border font-medium text-left transition-all duration-200 flex flex-col gap-2 ${
            activeType === 'MARKET_SELECTION'
              ? 'bg-emerald-500/15 border-emerald-500 text-emerald-300 shadow-lg shadow-emerald-950/40'
              : 'bg-slate-900/60 border-slate-800 text-slate-400 hover:border-slate-700 hover:text-slate-200'
          }`}
        >
          <div className="flex items-center gap-2 font-bold text-sm">
            <ShoppingBag className="w-4 h-4 text-emerald-400" /> Market Selection
          </div>
          <p className="text-xs text-slate-400">Which market appears best for selling my crop?</p>
        </button>

        <button
          type="button"
          onClick={() => { setActiveType('PROFITABILITY_EXPLANATION'); setResult(null); }}
          className={`p-4 rounded-xl border font-medium text-left transition-all duration-200 flex flex-col gap-2 ${
            activeType === 'PROFITABILITY_EXPLANATION'
              ? 'bg-emerald-500/15 border-emerald-500 text-emerald-300 shadow-lg shadow-emerald-950/40'
              : 'bg-slate-900/60 border-slate-800 text-slate-400 hover:border-slate-700 hover:text-slate-200'
          }`}
        >
          <div className="flex items-center gap-2 font-bold text-sm">
            <TrendingUp className="w-4 h-4 text-emerald-400" /> Profitability Explanation
          </div>
          <p className="text-xs text-slate-400">Explain my costs, break-even price, and estimated ROI.</p>
        </button>

        <button
          type="button"
          onClick={() => { setActiveType('MARKET_COMPARISON_EXPLANATION'); setResult(null); }}
          className={`p-4 rounded-xl border font-medium text-left transition-all duration-200 flex flex-col gap-2 ${
            activeType === 'MARKET_COMPARISON_EXPLANATION'
              ? 'bg-emerald-500/15 border-emerald-500 text-emerald-300 shadow-lg shadow-emerald-950/40'
              : 'bg-slate-900/60 border-slate-800 text-slate-400 hover:border-slate-700 hover:text-slate-200'
          }`}
        >
          <div className="flex items-center gap-2 font-bold text-sm">
            <Scale className="w-4 h-4 text-emerald-400" /> Market Comparison
          </div>
          <p className="text-xs text-slate-400">Why is Market A better/worse than Market B?</p>
        </button>

        <button
          type="button"
          onClick={() => { setActiveType('SELLING_DECISION_SUPPORT'); setResult(null); }}
          className={`p-4 rounded-xl border font-medium text-left transition-all duration-200 flex flex-col gap-2 ${
            activeType === 'SELLING_DECISION_SUPPORT'
              ? 'bg-emerald-500/15 border-emerald-500 text-emerald-300 shadow-lg shadow-emerald-950/40'
              : 'bg-slate-900/60 border-slate-800 text-slate-400 hover:border-slate-700 hover:text-slate-200'
          }`}
        >
          <div className="flex items-center gap-2 font-bold text-sm">
            <Lightbulb className="w-4 h-4 text-emerald-400" /> Pre-Sale Support
          </div>
          <p className="text-xs text-slate-400">What factors should I check before selling today?</p>
        </button>
      </div>

      {/* Input Form */}
      <form onSubmit={handleRunDecision} className="bg-slate-900/80 rounded-2xl border border-slate-800 p-6 space-y-6">
        {formError && (
          <div className="p-3.5 bg-rose-500/10 border border-rose-500/30 rounded-xl text-rose-300 text-xs flex items-center gap-2">
            <AlertTriangle className="w-4 h-4 shrink-0" />
            <span>{formError}</span>
          </div>
        )}

        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-5">
          {/* Crop Dropdown */}
          {activeType !== 'PROFITABILITY_EXPLANATION' && (
            <div>
              <label className="block text-xs font-semibold text-slate-300 mb-1.5">Select Crop</label>
              <select
                value={selectedCropId}
                onChange={(e) => setSelectedCropId(e.target.value)}
                className="w-full px-3.5 py-2.5 bg-slate-950 border border-slate-700 rounded-xl text-slate-200 text-sm focus:outline-none focus:border-emerald-500"
              >
                <option value="">-- Choose Crop --</option>
                {crops.map((c) => (
                  <option key={c.id} value={c.id}>{c.name} ({c.category})</option>
                ))}
              </select>
            </div>
          )}

          {/* Saved Economic Record */}
          {economicRecords.length > 0 && (
            <div>
              <label className="block text-xs font-semibold text-slate-300 mb-1.5">Saved Production Cost Record</label>
              <select
                value={selectedEconomicRecordId}
                onChange={(e) => setSelectedEconomicRecordId(e.target.value)}
                className="w-full px-3.5 py-2.5 bg-slate-950 border border-slate-700 rounded-xl text-slate-200 text-sm focus:outline-none focus:border-emerald-500"
              >
                <option value="">-- Optional Saved Record --</option>
                {economicRecords.map((r) => (
                  <option key={r.id} value={r.id}>
                    {r.cropName} ({r.farmName} - {r.season})
                  </option>
                ))}
              </select>
            </div>
          )}

          {/* Quantity & Unit */}
          <div>
            <label className="block text-xs font-semibold text-slate-300 mb-1.5">Target Quantity & Unit</label>
            <div className="flex gap-2">
              <input
                type="number"
                min="0.1"
                step="0.1"
                value={quantity}
                onChange={(e) => setQuantity(parseFloat(e.target.value) || 0)}
                className="w-full px-3.5 py-2.5 bg-slate-950 border border-slate-700 rounded-xl text-slate-200 text-sm focus:outline-none focus:border-emerald-500"
              />
              <select
                value={quantityUnit}
                onChange={(e) => setQuantityUnit(e.target.value)}
                className="px-3 py-2.5 bg-slate-950 border border-slate-700 rounded-xl text-slate-200 text-xs focus:outline-none focus:border-emerald-500"
              >
                <option value="QUINTAL">QUINTAL</option>
                <option value="TONNE">TONNE</option>
                <option value="KG">KG</option>
              </select>
            </div>
          </div>

          {/* Transportation Cost */}
          {activeType !== 'PROFITABILITY_EXPLANATION' && (
            <div>
              <label className="block text-xs font-semibold text-slate-300 mb-1.5">Est. Transport Cost (₹)</label>
              <input
                type="number"
                min="0"
                value={transportationCost}
                onChange={(e) => setTransportationCost(parseFloat(e.target.value) || 0)}
                className="w-full px-3.5 py-2.5 bg-slate-950 border border-slate-700 rounded-xl text-slate-200 text-sm focus:outline-none focus:border-emerald-500"
              />
            </div>
          )}

          {/* Other Selling Costs */}
          {activeType !== 'PROFITABILITY_EXPLANATION' && (
            <div>
              <label className="block text-xs font-semibold text-slate-300 mb-1.5">Other Selling Costs (₹)</label>
              <input
                type="number"
                min="0"
                value={otherSellingCosts}
                onChange={(e) => setOtherSellingCosts(parseFloat(e.target.value) || 0)}
                className="w-full px-3.5 py-2.5 bg-slate-950 border border-slate-700 rounded-xl text-slate-200 text-sm focus:outline-none focus:border-emerald-500"
              />
            </div>
          )}
        </div>

        {/* Candidate Markets Selector */}
        {activeType !== 'PROFITABILITY_EXPLANATION' && (
          <div className="space-y-2">
            <label className="block text-xs font-semibold text-slate-300">
              Select Target Markets to Evaluate ({selectedMarketIds.length} selected):
            </label>
            <div className="grid grid-cols-2 sm:grid-cols-4 gap-2 max-h-40 overflow-y-auto p-3 bg-slate-950 rounded-xl border border-slate-800">
              {markets.map((m) => {
                const isSelected = selectedMarketIds.includes(m.id);
                return (
                  <button
                    key={m.id}
                    type="button"
                    onClick={() => toggleMarketSelection(m.id)}
                    className={`px-3 py-2 rounded-lg text-xs font-medium text-left truncate transition-all ${
                      isSelected
                        ? 'bg-emerald-500/20 border border-emerald-500 text-emerald-300'
                        : 'bg-slate-900 border border-slate-800 text-slate-400 hover:text-slate-200'
                    }`}
                  >
                    {m.name}
                  </button>
                );
              })}
            </div>
          </div>
        )}

        <button
          type="submit"
          disabled={decisionMutation.isPending}
          className="w-full py-3 bg-emerald-600 hover:bg-emerald-500 text-white font-semibold text-sm rounded-xl shadow-lg transition-all duration-200 flex items-center justify-center gap-2 disabled:opacity-50"
        >
          {decisionMutation.isPending ? (
            <>
              <RotateCcw className="w-4 h-4 animate-spin" /> Evaluating Decision Intelligence...
            </>
          ) : (
            <>
              <Brain className="w-4 h-4" /> Generate AI Decision Support
            </>
          )}
        </button>
      </form>

      {/* Result Display */}
      {result && (
        <div className="bg-slate-900/90 rounded-2xl border border-emerald-500/30 p-6 space-y-6 shadow-2xl animate-fade-in">
          {/* Header Metadata */}
          <div className="flex flex-wrap items-center justify-between gap-4 pb-4 border-b border-slate-800">
            <div className="flex items-center gap-3">
              <span className="text-xs font-semibold text-slate-400 uppercase tracking-wider">Confidence Level:</span>
              <span
                className={`px-3 py-1 rounded-full text-xs font-bold ${
                  result.confidence === 'HIGH'
                    ? 'bg-emerald-500/20 text-emerald-400 border border-emerald-500/40'
                    : result.confidence === 'MEDIUM'
                    ? 'bg-amber-500/20 text-amber-300 border border-amber-500/40'
                    : 'bg-rose-500/20 text-rose-300 border border-rose-500/40'
                }`}
              >
                {result.confidence} CONFIDENCE
              </span>
            </div>

            <div className="flex items-center gap-3 text-xs text-slate-400">
              <span className="px-2.5 py-0.5 bg-slate-800 rounded-md font-mono text-emerald-400">{result.engine}</span>
              {result.dataFreshness?.marketPriceDate && (
                <span>Price Date: <strong className="text-slate-200">{result.dataFreshness.marketPriceDate}</strong></span>
              )}
            </div>
          </div>

          {/* AI Decision Summary Banner */}
          <div className="bg-emerald-950/40 border border-emerald-500/30 p-5 rounded-xl space-y-2">
            <h3 className="text-sm font-bold text-emerald-400 uppercase tracking-wider flex items-center gap-2">
              <ShieldCheck className="w-4 h-4" /> AI Recommendation Summary
            </h3>
            <p className="text-base font-medium text-white">{result.summary}</p>
            {result.recommendation && (
              <div className="pt-2 text-xs text-emerald-300">
                Recommended Action: <strong className="text-white text-sm">{result.recommendation}</strong>
              </div>
            )}
          </div>

          {/* Authoritative Calculated Metrics */}
          {result.calculatedMetrics && (
            <div className="space-y-3">
              <h4 className="text-xs font-bold text-slate-300 uppercase tracking-wider flex items-center gap-1.5">
                <DollarSign className="w-4 h-4 text-emerald-400" /> Authoritative Calculated Metrics (Modules 09/10)
              </h4>
              <div className="grid grid-cols-2 sm:grid-cols-4 gap-3">
                {result.calculatedMetrics.selectedPrice && (
                  <div className="bg-slate-950 p-3.5 rounded-xl border border-slate-800">
                    <span className="text-xs text-slate-400 block">Verified Price</span>
                    <span className="text-lg font-bold text-white">₹{result.calculatedMetrics.selectedPrice}</span>
                  </div>
                )}
                {result.calculatedMetrics.estimatedNetRealization && (
                  <div className="bg-slate-950 p-3.5 rounded-xl border border-slate-800">
                    <span className="text-xs text-slate-400 block">Est. Net Realization</span>
                    <span className="text-lg font-bold text-emerald-400">₹{result.calculatedMetrics.estimatedNetRealization}</span>
                  </div>
                )}
                {result.calculatedMetrics.breakEvenSellingPrice && (
                  <div className="bg-slate-950 p-3.5 rounded-xl border border-slate-800">
                    <span className="text-xs text-slate-400 block">Break-even Price</span>
                    <span className="text-lg font-bold text-amber-300">₹{result.calculatedMetrics.breakEvenSellingPrice}</span>
                  </div>
                )}
                {result.calculatedMetrics.estimatedProfit && (
                  <div className="bg-slate-950 p-3.5 rounded-xl border border-slate-800">
                    <span className="text-xs text-slate-400 block">Est. Profit</span>
                    <span className="text-lg font-bold text-emerald-300">₹{result.calculatedMetrics.estimatedProfit}</span>
                  </div>
                )}
              </div>
            </div>
          )}

          {/* Verified Facts & Reasoning */}
          <div className="grid grid-cols-1 md:grid-cols-2 gap-5">
            {/* Verified Facts */}
            <div className="bg-slate-950 p-4 rounded-xl border border-slate-800 space-y-2.5">
              <h4 className="text-xs font-bold text-slate-300 uppercase tracking-wider flex items-center gap-1.5">
                <CheckCircle2 className="w-4 h-4 text-emerald-400" /> Verified Market Facts
              </h4>
              <ul className="space-y-1.5 text-xs text-slate-300">
                {result.verifiedFacts.map((fact, idx) => (
                  <li key={idx} className="flex items-start gap-2">
                    <span className="text-emerald-400 shrink-0">•</span>
                    <span>{fact}</span>
                  </li>
                ))}
              </ul>
            </div>

            {/* AI Reasoning */}
            <div className="bg-slate-950 p-4 rounded-xl border border-slate-800 space-y-2.5">
              <h4 className="text-xs font-bold text-slate-300 uppercase tracking-wider flex items-center gap-1.5">
                <Brain className="w-4 h-4 text-emerald-400" /> AI Decision Reasoning
              </h4>
              <ul className="space-y-1.5 text-xs text-slate-300">
                {result.reasoning.map((reason, idx) => (
                  <li key={idx} className="flex items-start gap-2">
                    <span className="text-emerald-400 shrink-0">•</span>
                    <span>{reason}</span>
                  </li>
                ))}
              </ul>
            </div>
          </div>

          {/* Risks & Next Steps */}
          <div className="grid grid-cols-1 md:grid-cols-2 gap-5">
            {/* Risks */}
            <div className="bg-amber-950/20 p-4 rounded-xl border border-amber-500/20 space-y-2.5">
              <h4 className="text-xs font-bold text-amber-300 uppercase tracking-wider flex items-center gap-1.5">
                <AlertTriangle className="w-4 h-4 text-amber-400" /> Risks & Factors to Watch
              </h4>
              <ul className="space-y-1.5 text-xs text-amber-200/80">
                {result.risks.map((risk, idx) => (
                  <li key={idx} className="flex items-start gap-2">
                    <span className="text-amber-400 shrink-0">•</span>
                    <span>{risk}</span>
                  </li>
                ))}
              </ul>
            </div>

            {/* Next Steps */}
            <div className="bg-slate-950 p-4 rounded-xl border border-slate-800 space-y-2.5">
              <h4 className="text-xs font-bold text-slate-300 uppercase tracking-wider flex items-center gap-1.5">
                <ArrowRight className="w-4 h-4 text-emerald-400" /> Recommended Next Steps
              </h4>
              <ul className="space-y-1.5 text-xs text-slate-300">
                {result.nextSteps.map((step, idx) => (
                  <li key={idx} className="flex items-start gap-2">
                    <span className="text-emerald-400 shrink-0">•</span>
                    <span>{step}</span>
                  </li>
                ))}
              </ul>
            </div>
          </div>

          {/* Data Freshness & Limitations Footer */}
          <div className="p-4 bg-slate-950/60 rounded-xl border border-slate-800 text-xs text-slate-400 space-y-1">
            <div className="flex items-center gap-1.5 text-slate-300 font-semibold">
              <Info className="w-3.5 h-3.5 text-emerald-400" /> Safety & Data Freshness Statement
            </div>
            <p>{result.dataFreshness?.staleMessage || 'Based on current verified data.'}</p>
            {result.limitations.map((lim, idx) => (
              <p key={idx} className="text-slate-400 italic">• {lim}</p>
            ))}
          </div>
        </div>
      )}
    </div>
  );
};
