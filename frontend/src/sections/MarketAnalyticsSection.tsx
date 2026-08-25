import React, { useState } from 'react';
import { Scale, TrendingUp, BarChart3, ShieldCheck } from 'lucide-react';
import { SectionHeading } from '../components/ui/SectionHeading';
import { GlassCard } from '../components/ui/GlassCard';

export const MarketAnalyticsSection: React.FC = () => {
  const [activeCommodity, setActiveCommodity] = useState<'Tomato' | 'Rice' | 'Chilli'>('Tomato');

  const commodityDetails = {
    Tomato: { name: 'Tomato (Hybrid)', category: 'Vegetables', hub: 'Warangal & Hyderabad Mandis', color: '#ef4444' },
    Rice: { name: 'Rice (BPT 5204)', category: 'Cereals', hub: 'Hyderabad & Nizamabad Mandis', color: '#eab308' },
    Chilli: { name: 'Teja Chilli', category: 'Spices', hub: 'Guntur & Warangal Mandis', color: '#dc2626' },
  };

  const selected = commodityDetails[activeCommodity];

  return (
    <section id="market-analytics" className="relative py-20 bg-slate-950 text-slate-100 overflow-hidden border-t border-slate-900">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        
        <SectionHeading
          badgeText="Platform Analytics Architecture"
          title="Intelligent Market Trajectories"
          subtitle="Track wholesale commodity indexes, Mandi arrival volumes, and inter-state logistics flows across India."
        />

        <div className="grid grid-cols-1 lg:grid-cols-3 gap-8">
          
          {/* Main Trend Overview */}
          <div className="lg:col-span-2 space-y-6">
            <GlassCard className="p-6 sm:p-8 border-emerald-500/30">
              
              {/* Selector Tabs */}
              <div className="flex flex-wrap items-center justify-between gap-4 mb-6">
                <div>
                  <h3 className="text-xl font-bold text-white">{selected.name} Analytics</h3>
                  <p className="text-xs text-slate-400">Wholesale Mandi Index Trajectory • {selected.hub}</p>
                </div>

                <div className="flex bg-slate-950 p-1 rounded-xl border border-slate-800 space-x-1">
                  {(['Tomato', 'Rice', 'Chilli'] as const).map((key) => (
                    <button
                      key={key}
                      onClick={() => setActiveCommodity(key)}
                      className={`px-3 py-1.5 rounded-lg text-xs font-semibold transition-all ${
                        activeCommodity === key
                          ? 'bg-emerald-600 text-white shadow-md'
                          : 'text-slate-400 hover:text-white'
                      }`}
                    >
                      {key}
                    </button>
                  ))}
                </div>
              </div>

              {/* Visual Platform Architecture Canvas */}
              <div className="h-64 sm:h-80 w-full rounded-2xl bg-slate-900/60 border border-slate-800/80 p-6 flex flex-col justify-between relative overflow-hidden">
                <div className="flex items-center justify-between">
                  <div className="flex items-center gap-2">
                    <BarChart3 className="w-5 h-5 text-emerald-400" />
                    <span className="text-sm font-bold text-white">Mandi Pricing Index Engine</span>
                  </div>
                  <span className="text-xs font-semibold px-2.5 py-0.5 rounded-full bg-emerald-500/10 text-emerald-400 border border-emerald-500/30">
                    API Connected
                  </span>
                </div>

                <div className="space-y-4 my-auto">
                  <div className="grid grid-cols-3 gap-4 text-center">
                    <div className="p-3 bg-slate-950/80 rounded-xl border border-slate-800">
                      <span className="text-[11px] text-slate-400 block">Arrival Volume</span>
                      <span className="text-sm sm:text-base font-bold text-slate-200">Wholesale Feeds</span>
                    </div>
                    <div className="p-3 bg-slate-950/80 rounded-xl border border-slate-800">
                      <span className="text-[11px] text-slate-400 block">Quality Grade</span>
                      <span className="text-sm sm:text-base font-bold text-emerald-400">Grade A Standard</span>
                    </div>
                    <div className="p-3 bg-slate-950/80 rounded-xl border border-slate-800">
                      <span className="text-[11px] text-slate-400 block">Bidding Flow</span>
                      <span className="text-sm sm:text-base font-bold text-amber-400">Direct Connect</span>
                    </div>
                  </div>
                </div>

                <div className="flex items-center justify-between text-xs text-slate-400 border-t border-slate-800/80 pt-3">
                  <span>Mandi Network Hub: <strong className="text-white">{selected.hub}</strong></span>
                  <span className="text-emerald-400 font-medium">Real Data Pipeline Active</span>
                </div>
              </div>

            </GlassCard>
          </div>

          {/* Market Summary Cards */}
          <div className="space-y-6">
            
            <GlassCard className="p-6 border-slate-800">
              <div className="flex items-center justify-between mb-2">
                <span className="text-xs text-slate-400 font-medium">Target Commodity</span>
                <span className="text-xs font-semibold px-2 py-0.5 rounded-full bg-emerald-500/10 text-emerald-400 border border-emerald-500/30">
                  {selected.category}
                </span>
              </div>
              <h4 className="text-2xl font-bold text-white">{selected.name}</h4>
              <div className="pt-3">
                <span className="text-xs text-slate-400 block">Primary Distribution Hub</span>
                <span className="text-sm font-semibold text-slate-200">{selected.hub}</span>
              </div>
            </GlassCard>

            <GlassCard className="p-6 border-slate-800 space-y-4">
              <div className="flex items-center space-x-2 text-emerald-400 font-semibold text-sm">
                <Scale className="w-4 h-4" />
                <span>Market Intelligence Capabilities</span>
              </div>

              <div className="space-y-3 text-xs">
                <div className="flex items-center justify-between p-2.5 rounded-xl bg-slate-900 border border-slate-800">
                  <span className="text-slate-300 font-medium">Wholesale Mandi Data Link</span>
                  <ShieldCheck className="w-4 h-4 text-emerald-400" />
                </div>
                <div className="flex items-center justify-between p-2.5 rounded-xl bg-slate-900 border border-slate-800">
                  <span className="text-slate-300 font-medium">Inter-State Transport Logistics</span>
                  <ShieldCheck className="w-4 h-4 text-emerald-400" />
                </div>
                <div className="flex items-center justify-between p-2.5 rounded-xl bg-slate-900 border border-slate-800">
                  <span className="text-slate-300 font-medium">Buyer-Farmer Price Transparency</span>
                  <TrendingUp className="w-4 h-4 text-emerald-400" />
                </div>
              </div>
            </GlassCard>

            <div className="p-4 rounded-2xl bg-emerald-500/10 border border-emerald-500/30 text-xs text-emerald-300 leading-relaxed">
              <strong>Agricultural Market Intelligence:</strong> Direct integration with regional Mandi feeds enables transparent pricing discovery for farmers, mediators, and buyers.
            </div>

          </div>

        </div>

      </div>
    </section>
  );
};

