import React from 'react';
import { motion } from 'framer-motion';
import { Database, ShieldCheck, Cpu, ArrowRight, Layers, Network, Activity } from 'lucide-react';
import { Link } from 'react-router-dom';
import { SectionHeading } from '../components/ui/SectionHeading';
import { GlassCard } from '../components/ui/GlassCard';
import { useAuth } from '../features/auth/hooks/useAuth';

export const MarketIntelligencePreview: React.FC = () => {
  const { isAuthenticated } = useAuth();

  const platformPillars = [
    {
      icon: <Layers className="w-5 h-5 text-emerald-400" />,
      title: 'Commodity Standardisation',
      category: 'Data Taxonomy',
      description: 'Structured classification across Cereals, Vegetables, Spices, and Cash Crops for nationwide comparability.',
      status: 'Engine Ready'
    },
    {
      icon: <Network className="w-5 h-5 text-amber-400" />,
      title: 'Mandi Telemetry Link',
      category: 'Wholesale Feeds',
      description: 'Direct API pipelines connecting APMC wholesale market feeds, arrival volumes, and auction signals.',
      status: 'API Standing By'
    },
    {
      icon: <Activity className="w-5 h-5 text-sky-400" />,
      title: 'Price Transparency Engine',
      category: 'Market Fairness',
      description: 'Eliminates predatory middleman price distortion by establishing transparent baseline market rates.',
      status: 'Architecture Active'
    },
    {
      icon: <Cpu className="w-5 h-5 text-purple-400" />,
      title: 'AI Seasonal Forecasting',
      category: 'Predictive Models',
      description: 'Harvest trajectory modeling and demand surge forecasting tailored for farmers, buyers, and consumers.',
      status: 'Pipeline Standing By'
    }
  ];

  return (
    <section id="market-prices" className="relative py-20 bg-slate-950 text-slate-100 overflow-hidden border-t border-slate-900 select-none">
      {/* Soft Ambient Background Glow */}
      <div className="absolute inset-0 bg-[radial-gradient(ellipse_at_center,rgba(16,185,129,0.05)_0%,transparent_70%)] pointer-events-none" />

      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 relative z-10">
        
        <SectionHeading
          badgeText="Market Intelligence Architecture"
          title="Live Agricultural Market Data Engine"
          subtitle="Direct integration with regional Mandis, wholesale arrival volumes, and price transparency will link live data in upcoming market intelligence modules."
        />

        {/* Central Platform Architecture Visualization Panel */}
        <GlassCard className="p-6 sm:p-10 border-emerald-500/30 mb-12 shadow-2xl">
          <div className="flex flex-col lg:flex-row items-center justify-between gap-8">
            
            {/* Left Conceptual Summary */}
            <div className="space-y-4 max-w-xl text-center lg:text-left">
              <div className="inline-flex items-center space-x-2 px-3 py-1 rounded-full bg-slate-900 border border-slate-800 text-xs text-slate-400 font-mono">
                <Database className="w-3.5 h-3.5 text-emerald-400" />
                <span>Production Data Integrity Policy</span>
              </div>

              <h3 className="text-2xl sm:text-3xl font-extrabold text-white leading-tight">
                Authentic Market Data. Zero Fabricated Metrics.
              </h3>

              <p className="text-sm text-slate-300 leading-relaxed">
                FarmConnectPrices enforces strict production data integrity. Live market pricing, regional Mandi bidding, and AI forecasts will stream directly from verified agricultural APIs once the market engine is connected.
              </p>

              <div className="pt-2 flex flex-wrap items-center justify-center lg:justify-start gap-3">
                <div className="px-3.5 py-2 rounded-xl bg-slate-950/80 border border-slate-800 text-xs text-slate-300 flex items-center space-x-2">
                  <ShieldCheck className="w-4 h-4 text-emerald-400" />
                  <span>Verified Mandi Feeds</span>
                </div>
                <div className="px-3.5 py-2 rounded-xl bg-slate-950/80 border border-slate-800 text-xs text-slate-300 flex items-center space-x-2">
                  <ShieldCheck className="w-4 h-4 text-emerald-400" />
                  <span>Zero Static Placeholders</span>
                </div>
              </div>
            </div>

            {/* Right Status Indicator Card */}
            <div className="w-full lg:w-80 p-6 rounded-2xl bg-slate-950/90 border border-slate-800 text-center space-y-4 shadow-xl shrink-0">
              <div className="w-12 h-12 rounded-2xl bg-emerald-500/10 border border-emerald-500/30 flex items-center justify-center mx-auto text-emerald-400">
                <Activity className="w-6 h-6 animate-pulse" />
              </div>

              <div>
                <span className="text-xs font-semibold text-emerald-400 uppercase tracking-wider block">Engine Status</span>
                <h4 className="text-base font-bold text-white mt-1">Live Mandi Data Integration</h4>
              </div>

              <p className="text-xs text-slate-400 leading-relaxed">
                Live market data integration will appear here in the upcoming Market Intelligence Module.
              </p>

              <div className="pt-1">
                <span className="text-[11px] font-semibold text-slate-300 bg-slate-900 px-3.5 py-1.5 rounded-full border border-slate-800 block">
                  Integration Standing By
                </span>
              </div>
            </div>

          </div>
        </GlassCard>

        {/* 4 Architectural Pillar Cards Grid */}
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-6 mb-12">
          {platformPillars.map((pillar, idx) => (
            <motion.div
              key={pillar.title}
              initial={{ opacity: 0, y: 20 }}
              whileInView={{ opacity: 1, y: 0 }}
              viewport={{ once: true }}
              transition={{ duration: 0.4, delay: idx * 0.1 }}
            >
              <GlassCard className="p-6 h-full flex flex-col justify-between border-slate-800/90 hover:border-emerald-500/40 transition-colors">
                <div className="space-y-3">
                  <div className="flex items-center justify-between">
                    <div className="p-2.5 rounded-xl bg-slate-900 border border-slate-800">
                      {pillar.icon}
                    </div>
                    <span className="text-[10px] font-semibold px-2 py-0.5 rounded-full bg-slate-900 text-slate-400 border border-slate-800">
                      {pillar.category}
                    </span>
                  </div>

                  <h4 className="text-base font-bold text-white">{pillar.title}</h4>
                  <p className="text-xs text-slate-300 leading-relaxed">{pillar.description}</p>
                </div>

                <div className="pt-4 border-t border-slate-900 mt-4 flex items-center justify-between text-[11px]">
                  <span className="text-slate-500 font-medium">Status</span>
                  <span className="text-emerald-400 font-semibold">{pillar.status}</span>
                </div>
              </GlassCard>
            </motion.div>
          ))}
        </div>

        {/* Action Link Banner */}
        <div className="text-center">
          <Link
            to={isAuthenticated ? '/dashboard' : '/register'}
            className="inline-flex items-center space-x-2 px-6 py-3 rounded-xl bg-slate-900 hover:bg-slate-800 text-slate-200 hover:text-white font-semibold text-xs sm:text-sm border border-slate-800 hover:border-slate-700 transition-all group"
          >
            <span>Get Notified When Live Market Data Launches</span>
            <ArrowRight className="w-4 h-4 text-emerald-400 group-hover:translate-x-1 transition-transform" />
          </Link>
        </div>

      </div>
    </section>
  );
};
