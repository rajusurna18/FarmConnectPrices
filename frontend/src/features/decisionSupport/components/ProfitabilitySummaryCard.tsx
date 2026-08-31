import React from 'react';
import type { MarketEvaluationResponse } from '../types/decisionSupport';

interface ProfitabilitySummaryCardProps {
  evaluation: MarketEvaluationResponse;
}

export const ProfitabilitySummaryCard: React.FC<ProfitabilitySummaryCardProps> = ({ evaluation }) => {
  if (evaluation.status !== 'SUCCESS') {
    return (
      <div className="bg-slate-900 border border-slate-800 rounded-xl p-6 shadow-md text-slate-300">
        <div className="flex items-center space-x-3 text-amber-400 font-semibold mb-2">
          <svg className="w-6 h-6" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z" />
          </svg>
          <span>{evaluation.status === 'NO_VERIFIED_PRICE' ? 'No Verified Price Available' : evaluation.status === 'UNIT_MISMATCH' ? 'Unit Mismatch Error' : 'Evaluation Notice'}</span>
        </div>
        <p className="text-slate-400 text-sm">{evaluation.message}</p>
      </div>
    );
  }

  const isLoss = evaluation.isLoss || (evaluation.estimatedNetRealization ?? 0) < 0;

  return (
    <div className="bg-slate-900 border border-slate-800 rounded-xl p-6 shadow-xl space-y-6 text-slate-100">
      {/* Header Info */}
      <div className="flex flex-col md:flex-row md:items-center justify-between pb-4 border-b border-slate-800 gap-4">
        <div>
          <div className="flex items-center space-x-2">
            <h2 className="text-xl font-bold text-emerald-400">{evaluation.crop?.name || 'Crop'}</h2>
            <span className="text-slate-500 font-medium">@</span>
            <h3 className="text-lg font-semibold text-slate-200">{evaluation.market?.name || 'Market'}</h3>
          </div>
          <p className="text-xs text-slate-400 mt-1">
            Verified Price: <span className="font-semibold text-emerald-300">₹{evaluation.selectedPrice} / {evaluation.priceUnit}</span> ({evaluation.priceBasis} Price)
          </p>
        </div>

        <div className="flex flex-wrap items-center gap-2">
          <span className="px-2.5 py-1 text-xs font-semibold rounded-full bg-emerald-950 text-emerald-400 border border-emerald-800/50">
            {evaluation.qualityStatus || 'VERIFIED'}
          </span>
          <span className="px-2.5 py-1 text-xs font-medium rounded-full bg-slate-800 text-slate-300">
            Source: {evaluation.source?.name || evaluation.source?.type || 'Official Mandi'}
          </span>
        </div>
      </div>

      {/* Stale Price Warning Banner */}
      {evaluation.isStalePrice && evaluation.staleMessage && (
        <div className="bg-amber-950/40 border border-amber-800/60 text-amber-300 px-4 py-3 rounded-lg text-xs flex items-center space-x-3">
          <svg className="w-5 h-5 flex-shrink-0 text-amber-400" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z" />
          </svg>
          <span>{evaluation.staleMessage}</span>
        </div>
      )}

      {/* Main Calculated Metrics Cards */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4">
        {/* Estimated Net Realization */}
        <div className={`p-4 rounded-xl border ${isLoss ? 'bg-rose-950/30 border-rose-800/60' : 'bg-emerald-950/30 border-emerald-800/60'} col-span-1 sm:col-span-2 lg:col-span-1`}>
          <p className="text-xs font-medium uppercase tracking-wider text-slate-400">Estimated Net Realization</p>
          <div className="mt-2 flex items-baseline justify-between">
            <p className={`text-2xl font-extrabold ${isLoss ? 'text-rose-400' : 'text-emerald-400'}`}>
              ₹{evaluation.estimatedNetRealization?.toLocaleString('en-IN')}
            </p>
            {isLoss && (
              <span className="text-xs px-2 py-0.5 rounded bg-rose-900/60 text-rose-300 font-semibold border border-rose-700/50">
                Estimated Loss
              </span>
            )}
          </div>
          <p className="text-[11px] text-slate-400 mt-1">
            After ₹{evaluation.totalSellingCosts?.toLocaleString('en-IN')} entered costs
          </p>
        </div>

        {/* Net Realization Per Unit */}
        <div className="p-4 rounded-xl bg-slate-950/60 border border-slate-800">
          <p className="text-xs font-medium uppercase tracking-wider text-slate-400">Net Realization / Unit</p>
          <p className="text-xl font-bold text-slate-100 mt-2">
            ₹{evaluation.netRealizationPerUnit?.toLocaleString('en-IN')}
            <span className="text-xs text-slate-400 font-normal"> / {evaluation.quantityUnit}</span>
          </p>
          <p className="text-[11px] text-slate-400 mt-1">Realized value per unit</p>
        </div>

        {/* Selling-Cost Break-Even Price */}
        <div className="p-4 rounded-xl bg-slate-950/60 border border-slate-800">
          <p className="text-xs font-medium uppercase tracking-wider text-slate-400">Selling-Cost Break-Even</p>
          <p className="text-xl font-bold text-sky-400 mt-2">
            ₹{evaluation.sellingCostBreakEvenPrice?.toLocaleString('en-IN')}
            <span className="text-xs text-slate-400 font-normal"> / {evaluation.quantityUnit}</span>
          </p>
          <p className="text-[11px] text-slate-400 mt-1">Price required to recover selling costs</p>
        </div>
      </div>

      {/* Detailed Cost Breakdown Table */}
      <div className="bg-slate-950/50 rounded-xl p-4 border border-slate-800/80 space-y-3">
        <h4 className="text-xs font-semibold text-slate-400 uppercase tracking-wider">Calculation Breakdown</h4>
        <div className="space-y-2 text-sm">
          <div className="flex justify-between text-slate-300">
            <span>Expected Sale Quantity</span>
            <span className="font-semibold">{evaluation.quantity} {evaluation.quantityUnit}</span>
          </div>
          <div className="flex justify-between text-slate-300">
            <span>Verified Market Price</span>
            <span className="font-semibold">₹{evaluation.selectedPrice} / {evaluation.priceUnit}</span>
          </div>
          <div className="flex justify-between text-emerald-300 pt-2 border-t border-slate-800">
            <span>Gross Revenue (Price × Quantity)</span>
            <span className="font-bold">₹{evaluation.grossRevenue?.toLocaleString('en-IN')}</span>
          </div>
          <div className="flex justify-between text-slate-400 text-xs pl-3">
            <span>• Transportation Cost</span>
            <span>- ₹{evaluation.transportationCost?.toLocaleString('en-IN')}</span>
          </div>
          <div className="flex justify-between text-slate-400 text-xs pl-3">
            <span>• Other Selling Costs (Handling/Fees)</span>
            <span>- ₹{evaluation.otherSellingCosts?.toLocaleString('en-IN')}</span>
          </div>
          <div className="flex justify-between text-rose-300 font-medium text-xs pt-1 border-t border-slate-800/50">
            <span>Total Selling Costs</span>
            <span>₹{evaluation.totalSellingCosts?.toLocaleString('en-IN')}</span>
          </div>
        </div>
      </div>

      {/* Transparent Disclaimer */}
      <div className="text-[11px] text-slate-500 italic bg-slate-950/40 p-3 rounded-lg border border-slate-800/40">
        Note: Estimated net realization includes explicitly entered transportation and handling costs. Unentered costs such as seeds, fertilizer, labor, irrigation, quality deductions, spoilage, or market waiting risks are not included.
      </div>
    </div>
  );
};
