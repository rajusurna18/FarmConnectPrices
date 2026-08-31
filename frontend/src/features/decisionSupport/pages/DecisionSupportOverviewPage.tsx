import React from 'react';
import { useNavigate } from 'react-router-dom';

export const DecisionSupportOverviewPage: React.FC = () => {
  const navigate = useNavigate();

  return (
    <div className="min-h-screen bg-slate-950 text-slate-100 py-10 px-4 sm:px-6 lg:px-8">
      <div className="max-w-5xl mx-auto space-y-8">
        {/* Header */}
        <div className="text-center max-w-3xl mx-auto space-y-3">
          <span className="inline-block px-3 py-1 bg-emerald-950 text-emerald-400 border border-emerald-800/60 rounded-full text-xs font-bold uppercase tracking-wider">
            Deterministic Decision Support
          </span>
          <h1 className="text-3xl sm:text-4xl font-extrabold text-slate-100 tracking-tight">
            Farmer Profitability & Selling Economics
          </h1>
          <p className="text-slate-400 text-sm sm:text-base leading-relaxed">
            Transform real verified mandi prices into transparent net realization estimates based strictly on your entered selling costs.
          </p>
        </div>

        {/* Action Cards */}
        <div className="grid grid-cols-1 md:grid-cols-2 gap-6 pt-4">
          {/* Single Market Card */}
          <div
            onClick={() => navigate('/decision-support/evaluate')}
            className="bg-slate-900 border border-slate-800 hover:border-emerald-500/50 rounded-2xl p-6 shadow-xl cursor-pointer transform hover:-translate-y-1 transition duration-200 group"
          >
            <div className="w-12 h-12 rounded-xl bg-emerald-950 text-emerald-400 border border-emerald-800 flex items-center justify-center mb-4 group-hover:scale-110 transition">
              <svg className="w-6 h-6" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 7h6m0 10v-3m-3 3h.01M9 17h.01M9 14h.01M12 14h.01M15 11h.01M12 11h.01M9 11h.01M7 21h10a2 2 0 002-2V5a2 2 0 00-2-2H7a2 2 0 00-2 2v14a2 2 0 002 2z" />
              </svg>
            </div>
            <h2 className="text-xl font-bold text-slate-100 group-hover:text-emerald-400 transition">
              Single Market Selling Economics
            </h2>
            <p className="text-slate-400 text-sm mt-2 leading-relaxed">
              Calculate your gross revenue, transport costs, net realization, and break-even selling price for a specific crop and mandi.
            </p>
            <div className="mt-6 flex items-center text-xs font-bold text-emerald-400 space-x-1">
              <span>Evaluate Single Market</span>
              <svg className="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M14 5l7 7m0 0l-7 7m7-7H3" />
              </svg>
            </div>
          </div>

          {/* Multi Market Comparison Card */}
          <div
            onClick={() => navigate('/decision-support/compare')}
            className="bg-slate-900 border border-slate-800 hover:border-emerald-500/50 rounded-2xl p-6 shadow-xl cursor-pointer transform hover:-translate-y-1 transition duration-200 group"
          >
            <div className="w-12 h-12 rounded-xl bg-sky-950 text-sky-400 border border-sky-800 flex items-center justify-center mb-4 group-hover:scale-110 transition">
              <svg className="w-6 h-6" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M3 6l3 1m0 0l-3 9a5 5 0 006.001 0M6 7l3 9M6 7l6-2m6 2l3-1m-3 1l-3 9a5 5 0 006.001 0M18 7l3 9m-3-9l-6-2m0-2v2m0 16V5m0 16H9m3 0h3" />
              </svg>
            </div>
            <h2 className="text-xl font-bold text-slate-100 group-hover:text-sky-400 transition">
              Multi-Market Comparison
            </h2>
            <p className="text-slate-400 text-sm mt-2 leading-relaxed">
              Compare estimated net realizations across multiple mandis after factoring in market-specific transport and handling costs.
            </p>
            <div className="mt-6 flex items-center text-xs font-bold text-sky-400 space-x-1">
              <span>Compare Multiple Markets</span>
              <svg className="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M14 5l7 7m0 0l-7 7m7-7H3" />
              </svg>
            </div>
          </div>
        </div>

        {/* Transparent Principles Notice */}
        <div className="bg-slate-900/60 border border-slate-800 rounded-xl p-6 text-xs text-slate-400 space-y-3">
          <h3 className="text-sm font-bold text-slate-200 uppercase tracking-wider">Transparent Calculation Principles</h3>
          <ul className="list-disc pl-5 space-y-1 text-slate-400">
            <li>Calculations are strictly deterministic (no AI/speculative predictions).</li>
            <li>Prices are sourced directly from verified mandi records with complete date and quality transparency.</li>
            <li>Estimated net realization reflects explicitly entered transportation and handling costs only.</li>
          </ul>
        </div>
      </div>
    </div>
  );
};
