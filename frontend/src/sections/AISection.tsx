import React from 'react';
import { Sparkles, BrainCircuit } from 'lucide-react';
import { SectionHeading } from '../components/ui/SectionHeading';
import { GlassCard } from '../components/ui/GlassCard';
import { AIDataSphere } from '../components/3d/AIDataSphere';

export const AISection: React.FC = () => {
  const steps = [
    { label: 'DATA INGESTION', icon: '📡', text: 'Multi-mandi wholesale APMC feeds & crop arrival telemetry' },
    { label: 'PATTERN ANALYSIS', icon: '⚙️', text: 'Algorithmic price variance & multi-year seasonal trends' },
    { label: 'PREDICTIVE CORE', icon: '🔮', text: 'AI price trajectory forecasting for upcoming harvest cycles' },
    { label: 'DECISION SUPPORT', icon: '🎯', text: 'Actionable sell/buy recommendations tailored for each role' },
  ];

  const aiCapabilities = [
    { label: 'Wholesale Mandi Link', category: 'Data Pipeline', status: 'Architecture Active', desc: 'Direct APMC pricing feed integration' },
    { label: 'Demand Forecasting', category: 'Predictive', status: 'Model Ready', desc: 'Urban consumer & retail demand trajectories' },
    { label: 'Supply Logistics Flow', category: 'Logistics', status: 'Telemetry Ready', desc: 'Inter-state volume arrival tracking' },
    { label: 'Seasonal Pattern Analysis', category: 'Analytics', status: 'Engine Ready', desc: 'Multi-year harvest cycle modeling' },
  ];

  return (
    <section id="ai-insights" className="relative py-20 bg-slate-950 text-slate-100 overflow-hidden border-t border-slate-900">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        
        <SectionHeading
          badgeText="Predictive Intelligence"
          title="AI Market Intelligence Engine"
          subtitle="Transforming raw agricultural data into real-time decision intelligence for farmers, mediators, and customers."
        />

        <div className="grid grid-cols-1 lg:grid-cols-12 gap-8 items-center">
          
          {/* Left Feature Column */}
          <div className="lg:col-span-4 space-y-4">
            <h3 className="text-2xl font-bold text-white mb-2 flex items-center">
              <BrainCircuit className="w-6 h-6 text-emerald-400 mr-2" />
              Intelligence Pipeline
            </h3>

            <div className="space-y-3">
              {steps.map((item) => (
                <GlassCard key={item.label} className="p-4 border-slate-800/80 hover:border-emerald-500/40">
                  <div className="flex items-center space-x-3">
                    <span className="text-xl">{item.icon}</span>
                    <div>
                      <span className="text-xs font-mono font-bold text-emerald-400 block">{item.label}</span>
                      <p className="text-xs text-slate-300 font-medium leading-tight">{item.text}</p>
                    </div>
                  </div>
                </GlassCard>
              ))}
            </div>
          </div>

          {/* Center 3D AI Data Sphere */}
          <div className="lg:col-span-5 relative flex items-center justify-center">
            <div className="w-full h-80 sm:h-96 rounded-3xl bg-slate-900/30 border border-emerald-500/20 p-2 backdrop-blur-xl shadow-2xl relative">
              <div className="absolute top-3 left-4 z-10 text-xs font-mono text-emerald-400 flex items-center space-x-1.5">
                <Sparkles className="w-3.5 h-3.5 animate-pulse text-amber-400" />
                <span>AI Neural Core Active</span>
              </div>
              
              <AIDataSphere />
            </div>
          </div>

          {/* Right Orbit Nodes Details */}
          <div className="lg:col-span-3 space-y-3">
            <h4 className="text-sm font-semibold text-slate-400 uppercase tracking-wider mb-3">
              Core AI Modules
            </h4>

            {aiCapabilities.map((node) => (
              <GlassCard key={node.label} className="p-3.5 border-slate-800">
                <div className="flex items-center justify-between">
                  <span className="text-xs font-bold text-white">{node.label}</span>
                  <span className="text-[10px] font-mono px-2 py-0.5 rounded-full bg-emerald-500/10 text-emerald-400 border border-emerald-500/30">
                    {node.status}
                  </span>
                </div>
                <p className="text-xs text-slate-400 mt-1">{node.desc}</p>
              </GlassCard>
            ))}
          </div>

        </div>

      </div>
    </section>
  );
};

