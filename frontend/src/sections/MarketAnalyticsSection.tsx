import React, { useState } from 'react';
import { ResponsiveContainer, AreaChart, Area, XAxis, YAxis, Tooltip, CartesianGrid } from 'recharts';
import { Scale } from 'lucide-react';
import { SectionHeading } from '../components/ui/SectionHeading';
import { GlassCard } from '../components/ui/GlassCard';
import { PriceBadge } from '../components/ui/PriceBadge';
import { MOCK_PRICE_TRENDS } from '../features/market/data/mockMarketData';

export const MarketAnalyticsSection: React.FC = () => {
  const [activeCropKey, setActiveCropKey] = useState<'tomatoPrice' | 'ricePrice' | 'chilliPrice'>('tomatoPrice');

  const cropMeta = {
    tomatoPrice: { name: 'Tomato (Hybrid)', price: 2850, unit: 'Quintal', trend: 7.8, color: '#ef4444' },
    ricePrice: { name: 'Rice (BPT 5204)', price: 3200, unit: 'Quintal', trend: 8.4, color: '#eab308' },
    chilliPrice: { name: 'Teja Chilli', price: 18500, unit: 'Quintal', trend: 14.7, color: '#dc2626' },
  };

  const selectedCrop = cropMeta[activeCropKey];

  return (
    <section id="market-analytics" className="relative py-20 bg-slate-950 text-slate-100 overflow-hidden border-t border-slate-900">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        
        <SectionHeading
          badgeText="Market Price Visualization"
          title="Real Prices. Better Decisions."
          subtitle="Track wholesale market movements, weekly price trajectories, and regional demand dynamics."
        />

        <div className="grid grid-cols-1 lg:grid-cols-3 gap-8">
          
          {/* Main Animated Trend Chart */}
          <div className="lg:col-span-2 space-y-6">
            <GlassCard className="p-6 sm:p-8 border-emerald-500/30">
              
              {/* Crop Selector Tabs */}
              <div className="flex flex-wrap items-center justify-between gap-4 mb-6">
                <div>
                  <h3 className="text-xl font-bold text-white">{selectedCrop.name} Price Trajectory</h3>
                  <p className="text-xs text-slate-400">7-Day Wholesale Market Trend • Hyderabad / Warangal</p>
                </div>

                <div className="flex bg-slate-950 p-1 rounded-xl border border-slate-800 space-x-1">
                  {(Object.keys(cropMeta) as Array<keyof typeof cropMeta>).map((key) => (
                    <button
                      key={key}
                      onClick={() => setActiveCropKey(key)}
                      className={`px-3 py-1.5 rounded-lg text-xs font-semibold transition-all ${
                        activeCropKey === key
                          ? 'bg-emerald-600 text-white shadow-md'
                          : 'text-slate-400 hover:text-white'
                      }`}
                    >
                      {cropMeta[key].name.split(' ')[0]}
                    </button>
                  ))}
                </div>
              </div>

              {/* Chart Canvas */}
              <div className="h-64 sm:h-80 w-full pt-4">
                <ResponsiveContainer width="100%" height="100%">
                  <AreaChart data={MOCK_PRICE_TRENDS}>
                    <defs>
                      <linearGradient id="colorPrice" x1="0" y1="0" x2="0" y2="1">
                        <stop offset="5%" stopColor={selectedCrop.color} stopOpacity={0.4} />
                        <stop offset="95%" stopColor={selectedCrop.color} stopOpacity={0.0} />
                      </linearGradient>
                    </defs>
                    <CartesianGrid strokeDasharray="3 3" stroke="#1e293b" />
                    <XAxis dataKey="date" stroke="#64748b" fontSize={12} />
                    <YAxis stroke="#64748b" fontSize={12} domain={['auto', 'auto']} />
                    <Tooltip
                      contentStyle={{ backgroundColor: '#090d16', borderColor: '#334155', borderRadius: '12px', color: '#fff' }}
                    />
                    <Area
                      type="monotone"
                      dataKey={activeCropKey}
                      stroke={selectedCrop.color}
                      strokeWidth={3}
                      fillOpacity={1}
                      fill="url(#colorPrice)"
                    />
                  </AreaChart>
                </ResponsiveContainer>
              </div>

              <div className="flex items-center justify-between text-xs text-slate-400 border-t border-slate-800/80 pt-4 mt-2">
                <span>Highest Mandi Bidding: <strong className="text-white">Warangal Market</strong></span>
                <span>Demo Analytics Data</span>
              </div>
            </GlassCard>
          </div>

          {/* Market Summary Cards */}
          <div className="space-y-6">
            
            <GlassCard className="p-6 border-slate-800">
              <div className="flex items-center justify-between mb-2">
                <span className="text-xs text-slate-400 font-medium">Selected Commodity</span>
                <PriceBadge percentage={selectedCrop.trend} isUp={true} size="sm" />
              </div>
              <h4 className="text-2xl font-bold text-white">{selectedCrop.name}</h4>
              <div className="flex items-baseline space-x-2 pt-2">
                <span className="text-3xl sm:text-4xl font-extrabold text-white">₹{selectedCrop.price.toLocaleString()}</span>
                <span className="text-xs text-slate-400">/ {selectedCrop.unit}</span>
              </div>
            </GlassCard>

            <GlassCard className="p-6 border-slate-800 space-y-4">
              <div className="flex items-center space-x-2 text-emerald-400 font-semibold text-sm">
                <Scale className="w-4 h-4" />
                <span>Supply vs Demand Index</span>
              </div>

              <div className="space-y-3 text-xs">
                <div>
                  <div className="flex justify-between text-slate-300 font-medium mb-1">
                    <span>Urban Market Demand</span>
                    <span className="text-emerald-400 font-bold">88% (Surge)</span>
                  </div>
                  <div className="w-full bg-slate-800 h-2 rounded-full overflow-hidden">
                    <div className="bg-emerald-500 h-full w-[88%]" />
                  </div>
                </div>

                <div>
                  <div className="flex justify-between text-slate-300 font-medium mb-1">
                    <span>Wholesale Mandi Supply</span>
                    <span className="text-amber-400 font-bold">64% (Moderate)</span>
                  </div>
                  <div className="w-full bg-slate-800 h-2 rounded-full overflow-hidden">
                    <div className="bg-amber-500 h-full w-[64%]" />
                  </div>
                </div>
              </div>
            </GlassCard>

            <div className="p-4 rounded-2xl bg-emerald-500/10 border border-emerald-500/30 text-xs text-emerald-300 leading-relaxed">
              <strong>AI Market Pulse:</strong> Price upward pressure observed across 3 regional markets due to high urban retail demand.
            </div>

          </div>

        </div>

      </div>
    </section>
  );
};
