import React, { useState } from 'react';
import {
  evaluateSmartSelling,
  SmartSellingDecisionResponse,
  MarketCard,
  TradeOffItem,
  ForecastScenarioItem,
} from '../services/smartSellingApi';

export const SmartSellingPage: React.FC = () => {
  const [cropId, setCropId] = useState('crop-1');
  const [quantity, setQuantity] = useState<number>(10);
  const [quantityUnit, setQuantityUnit] = useState('QUINTAL');
  const [candidateMarketIdsText, setCandidateMarketIdsText] = useState('market-1, market-2');
  const [transportCost, setTransportCost] = useState<number>(200);
  const [otherSellingCost, setOtherSellingCost] = useState<number>(0);
  const [priceBasis, setPriceBasis] = useState('MODAL');
  const [forecastHorizon, setForecastHorizon] = useState('7_DAYS');

  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [result, setResult] = useState<SmartSellingDecisionResponse | null>(null);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setLoading(true);
    setError(null);

    const marketIds = candidateMarketIdsText
      .split(',')
      .map((s) => s.trim())
      .filter((s) => s.length > 0);

    if (marketIds.length === 0) {
      setError('Please enter at least one candidate market ID.');
      setLoading(false);
      return;
    }

    try {
      const res = await evaluateSmartSelling({
        cropId,
        quantity,
        quantityUnit,
        candidateMarketIds: marketIds,
        customSellingCosts: {
          transportationCost: transportCost,
          otherSellingCosts: otherSellingCost,
        },
        priceBasis,
        forecastHorizon,
      });
      setResult(res);
    } catch (err: any) {
      console.error(err);
      setError(err?.response?.data || err?.message || 'Failed to evaluate Smart Selling decision.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="min-h-screen bg-slate-950 text-slate-100 p-4 md:p-8 font-sans">
      <div className="max-w-6xl mx-auto space-y-8">
        {/* Header */}
        <header className="border-b border-slate-800 pb-5">
          <div className="flex items-center space-y-1">
            <span className="px-3 py-1 bg-emerald-500/10 text-emerald-400 text-xs font-semibold rounded-full border border-emerald-500/20">
              Module 14
            </span>
            <h1 className="text-3xl font-extrabold tracking-tight text-white ml-3">
              Smart Selling Decision Engine
            </h1>
          </div>
          <p className="text-slate-400 text-sm mt-2">
            Multi-market price discovery, selling cost evaluation, net realization ranking, trade-off analysis & forecast scenario intelligence.
          </p>
        </header>

        {/* Form Container */}
        <form onSubmit={handleSubmit} className="bg-slate-900 border border-slate-800 rounded-xl p-6 space-y-6 shadow-xl">
          <h2 className="text-lg font-semibold text-white border-b border-slate-800 pb-2">
            Decision Input Parameters
          </h2>

          <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
            <div>
              <label className="block text-xs font-medium text-slate-400 mb-1">Crop ID</label>
              <input
                type="text"
                value={cropId}
                onChange={(e) => setCropId(e.target.value)}
                className="w-full bg-slate-950 border border-slate-800 rounded-lg px-3 py-2 text-sm text-white focus:outline-none focus:border-emerald-500"
                required
              />
            </div>

            <div>
              <label className="block text-xs font-medium text-slate-400 mb-1">Quantity</label>
              <input
                type="number"
                value={quantity}
                onChange={(e) => setQuantity(Number(e.target.value))}
                className="w-full bg-slate-950 border border-slate-800 rounded-lg px-3 py-2 text-sm text-white focus:outline-none focus:border-emerald-500"
                min="0.1"
                step="any"
                required
              />
            </div>

            <div>
              <label className="block text-xs font-medium text-slate-400 mb-1">Quantity Unit</label>
              <select
                value={quantityUnit}
                onChange={(e) => setQuantityUnit(e.target.value)}
                className="w-full bg-slate-950 border border-slate-800 rounded-lg px-3 py-2 text-sm text-white focus:outline-none focus:border-emerald-500"
              >
                <option value="QUINTAL">QUINTAL</option>
                <option value="KG">KG</option>
                <option value="TON">TON</option>
              </select>
            </div>
          </div>

          <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
            <div>
              <label className="block text-xs font-medium text-slate-400 mb-1">
                Candidate Market IDs (Comma Separated, max 10)
              </label>
              <input
                type="text"
                value={candidateMarketIdsText}
                onChange={(e) => setCandidateMarketIdsText(e.target.value)}
                className="w-full bg-slate-950 border border-slate-800 rounded-lg px-3 py-2 text-sm text-white focus:outline-none focus:border-emerald-500"
                placeholder="market-1, market-2"
                required
              />
            </div>

            <div className="grid grid-cols-2 gap-4">
              <div>
                <label className="block text-xs font-medium text-slate-400 mb-1">Price Basis</label>
                <select
                  value={priceBasis}
                  onChange={(e) => setPriceBasis(e.target.value)}
                  className="w-full bg-slate-950 border border-slate-800 rounded-lg px-3 py-2 text-sm text-white focus:outline-none focus:border-emerald-500"
                >
                  <option value="MODAL">MODAL</option>
                  <option value="MIN">MIN</option>
                  <option value="MAX">MAX</option>
                </select>
              </div>

              <div>
                <label className="block text-xs font-medium text-slate-400 mb-1">Forecast Horizon</label>
                <select
                  value={forecastHorizon}
                  onChange={(e) => setForecastHorizon(e.target.value)}
                  className="w-full bg-slate-950 border border-slate-800 rounded-lg px-3 py-2 text-sm text-white focus:outline-none focus:border-emerald-500"
                >
                  <option value="1_DAY">1 Day</option>
                  <option value="3_DAYS">3 Days</option>
                  <option value="7_DAYS">7 Days (Default)</option>
                  <option value="14_DAYS">14 Days</option>
                </select>
              </div>
            </div>
          </div>

          <div className="grid grid-cols-1 md:grid-cols-2 gap-6 pt-2 border-t border-slate-800">
            <div>
              <label className="block text-xs font-medium text-slate-400 mb-1">
                Custom Transportation Cost (₹)
              </label>
              <input
                type="number"
                value={transportCost}
                onChange={(e) => setTransportCost(Number(e.target.value))}
                className="w-full bg-slate-950 border border-slate-800 rounded-lg px-3 py-2 text-sm text-white focus:outline-none focus:border-emerald-500"
                min="0"
              />
            </div>

            <div>
              <label className="block text-xs font-medium text-slate-400 mb-1">
                Custom Other Selling Cost (₹)
              </label>
              <input
                type="number"
                value={otherSellingCost}
                onChange={(e) => setOtherSellingCost(Number(e.target.value))}
                className="w-full bg-slate-950 border border-slate-800 rounded-lg px-3 py-2 text-sm text-white focus:outline-none focus:border-emerald-500"
                min="0"
              />
            </div>
          </div>

          <button
            type="submit"
            disabled={loading}
            className="w-full md:w-auto px-6 py-2.5 bg-emerald-600 hover:bg-emerald-500 text-white font-medium text-sm rounded-lg transition-colors duration-200 disabled:opacity-50"
          >
            {loading ? 'Evaluating Smart Selling Decision...' : 'Evaluate Smart Selling Decision'}
          </button>
        </form>

        {/* Error Alert */}
        {error && (
          <div className="bg-rose-500/10 border border-rose-500/20 text-rose-300 p-4 rounded-xl text-sm">
            {error}
          </div>
        )}

        {/* Results Presentation */}
        {result && (
          <div className="space-y-8 animate-fadeIn">
            {/* Primary Recommendation Banner */}
            {result.primaryRecommendation && (
              <div className="bg-gradient-to-r from-emerald-900/40 via-slate-900 to-slate-900 border border-emerald-500/30 rounded-xl p-6 space-y-4 shadow-2xl">
                <div className="flex flex-wrap items-center justify-between gap-4">
                  <div>
                    <span className="px-3 py-1 bg-emerald-500/20 text-emerald-300 text-xs font-bold rounded-full border border-emerald-500/30 uppercase tracking-wider">
                      ★ Primary Recommendation
                    </span>
                    <h2 className="text-2xl font-black text-white mt-2">
                      {result.primaryRecommendation.market.name} ({result.primaryRecommendation.market.district}, {result.primaryRecommendation.market.state})
                    </h2>
                  </div>

                  <div className="text-right">
                    <p className="text-xs text-slate-400 uppercase font-semibold">Estimated Net Realization</p>
                    <p className="text-3xl font-extrabold text-emerald-400">
                      ₹{result.primaryRecommendation.estimatedNetRealization?.toLocaleString()}
                    </p>
                  </div>
                </div>

                <div className="grid grid-cols-2 md:grid-cols-4 gap-4 pt-4 border-t border-slate-800/80 text-xs">
                  <div>
                    <span className="text-slate-400 block">Selected Price</span>
                    <span className="font-semibold text-white">₹{result.primaryRecommendation.selectedPrice} / {result.primaryRecommendation.priceUnit}</span>
                  </div>
                  <div>
                    <span className="text-slate-400 block">Total Selling Cost</span>
                    <span className="font-semibold text-white">₹{result.primaryRecommendation.totalSellingCost}</span>
                  </div>
                  <div>
                    <span className="text-slate-400 block font-sans">Overall Decision Confidence</span>
                    <span className={`font-bold ${result.overallDecisionConfidence === 'HIGH' ? 'text-emerald-400' : 'text-amber-400'}`}>
                      {result.overallDecisionConfidence}
                    </span>
                  </div>
                  <div>
                    <span className="text-slate-400 block">Decision Basis</span>
                    <span className="font-medium text-slate-300">{result.decisionBasis}</span>
                  </div>
                </div>
              </div>
            )}

            {/* Ranked Markets List */}
            <div className="space-y-4">
              <h3 className="text-xl font-bold text-white">Ranked Candidate Markets</h3>
              <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                {result.rankedMarkets.map((card: MarketCard) => (
                  <div
                    key={card.market.id}
                    className={`bg-slate-900 border ${
                      card.rank === 1 ? 'border-emerald-500/40' : 'border-slate-800'
                    } rounded-xl p-5 space-y-3 relative shadow-lg`}
                  >
                    <div className="flex justify-between items-start">
                      <div>
                        <span className="text-xs font-bold px-2.5 py-0.5 rounded bg-slate-800 text-slate-300 border border-slate-700">
                          Rank #{card.rank}
                        </span>
                        <h4 className="text-lg font-bold text-white mt-1">{card.market.name}</h4>
                        <p className="text-xs text-slate-400">{card.market.district}, {card.market.state}</p>
                      </div>

                      <div className="text-right">
                        <span className="text-xs text-slate-400 block">Net Realization</span>
                        <span className="text-xl font-bold text-emerald-400">
                          ₹{card.estimatedNetRealization?.toLocaleString()}
                        </span>
                      </div>
                    </div>

                    <div className="grid grid-cols-3 gap-2 text-xs pt-3 border-t border-slate-800/60">
                      <div>
                        <span className="text-slate-400 block">Raw Price</span>
                        <span className="font-semibold text-slate-200">₹{card.selectedPrice}</span>
                      </div>
                      <div>
                        <span className="text-slate-400 block">Selling Cost</span>
                        <span className="font-semibold text-slate-200">₹{card.totalSellingCost}</span>
                      </div>
                      <div>
                        <span className="text-slate-400 block">30D Trend</span>
                        <span className={`font-semibold ${card.trendDirection === 'RISING' ? 'text-emerald-400' : 'text-slate-300'}`}>
                          {card.trendDirection}
                        </span>
                      </div>
                    </div>
                  </div>
                ))}
              </div>
            </div>

            {/* Trade-Off Analysis */}
            {result.tradeOffs && result.tradeOffs.length > 0 && (
              <div className="space-y-4">
                <h3 className="text-xl font-bold text-white">Consequence Trade-Off Analysis</h3>
                <div className="space-y-3">
                  {result.tradeOffs.map((trade: TradeOffItem, idx: number) => (
                    <div key={idx} className="bg-slate-900 border border-slate-800 rounded-xl p-4 space-y-2">
                      <div className="flex items-center space-x-2">
                        <span className="px-2 py-0.5 bg-amber-500/10 text-amber-400 text-xs font-semibold rounded border border-amber-500/20">
                          {trade.title}
                        </span>
                      </div>
                      <p className="text-sm text-slate-300">{trade.description}</p>
                    </div>
                  ))}
                </div>
              </div>
            )}

            {/* Forecast Scenarios Section */}
            {result.forecastScenarios && result.forecastScenarios.length > 0 && (
              <div className="space-y-4">
                <div className="flex items-center justify-between border-b border-slate-800 pb-2">
                  <h3 className="text-xl font-bold text-white">Price Forecast Scenarios ({result.forecastScenarios[0]?.horizon})</h3>
                  <span className="text-xs text-amber-400 bg-amber-500/10 px-2.5 py-1 rounded-full border border-amber-500/20">
                    isScenario = true (Context Only)
                  </span>
                </div>

                <div className="overflow-x-auto">
                  <table className="w-full text-left text-xs border-collapse bg-slate-900 border border-slate-800 rounded-xl">
                    <thead>
                      <tr className="border-b border-slate-800 text-slate-400 uppercase">
                        <th className="p-3">Market</th>
                        <th className="p-3">Direction</th>
                        <th className="p-3">Bear Price (Lower)</th>
                        <th className="p-3">Expected Price</th>
                        <th className="p-3">Bull Price (Upper)</th>
                        <th className="p-3">Expected Net Realization</th>
                      </tr>
                    </thead>
                    <tbody className="divide-y divide-slate-800/60 text-slate-200">
                      {result.forecastScenarios.map((sc: ForecastScenarioItem, idx: number) => (
                        <tr key={idx} className="hover:bg-slate-800/40">
                          <td className="p-3 font-semibold text-white">{sc.marketName}</td>
                          <td className="p-3 font-medium">{sc.direction}</td>
                          <td className="p-3 text-rose-400">₹{sc.bearPrice ?? '-'}</td>
                          <td className="p-3 font-bold text-emerald-400">₹{sc.expectedPrice ?? '-'}</td>
                          <td className="p-3 text-emerald-300">₹{sc.bullPrice ?? '-'}</td>
                          <td className="p-3 font-semibold text-emerald-400">
                            {sc.expectedNetRealization ? `₹${sc.expectedNetRealization.toLocaleString()}` : '-'}
                          </td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
                <p className="text-xs text-slate-500 italic">
                  * Forecast scenarios are probabilistic estimates derived from historical moving average models and do not guarantee future prices.
                </p>
              </div>
            )}

            {/* AI Explanation & Recommendations */}
            {result.aiExplanation && (
              <div className="bg-slate-900 border border-slate-800 rounded-xl p-6 space-y-4">
                <h3 className="text-lg font-bold text-white border-b border-slate-800 pb-2">
                  AI Decision Intelligence & Context
                </h3>
                <p className="text-sm text-slate-300 leading-relaxed">{result.aiExplanation.summary}</p>

                {result.aiExplanation.reasoning && result.aiExplanation.reasoning.length > 0 && (
                  <div>
                    <h4 className="text-xs font-bold text-slate-400 uppercase mb-2">Reasoning Audit Trail</h4>
                    <ul className="list-disc list-inside text-xs text-slate-300 space-y-1">
                      {result.aiExplanation.reasoning.map((r: string, i: number) => (
                        <li key={i}>{r}</li>
                      ))}
                    </ul>
                  </div>
                )}
              </div>
            )}
          </div>
        )}
      </div>
    </div>
  );
};
