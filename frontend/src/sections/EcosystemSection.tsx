import React, { useState } from 'react';
import { EcosystemCanvas } from '../components/3d/EcosystemCanvas';
import { SectionHeading } from '../components/ui/SectionHeading';
import { GlassCard } from '../components/ui/GlassCard';

export const EcosystemSection: React.FC = () => {
  const [activeStep, setActiveStep] = useState(4);

  const steps = [
    {
      step: '01',
      title: 'Farm Production',
      symbol: '🌾',
      description: 'Farmers register harvest yields, location, and crop quality metrics digitally.',
      color: 'border-emerald-500/40 text-emerald-400',
    },
    {
      step: '02',
      title: 'Market Intelligence',
      symbol: '📊',
      description: 'Real prices, supply volumes, and historical market trends are aggregated.',
      color: 'border-blue-500/40 text-blue-400',
    },
    {
      step: '03',
      title: 'Mediator / Buyer Node',
      symbol: '🏪',
      description: 'Aggregators and buyers discover available crops with full price transparency.',
      color: 'border-amber-500/40 text-amber-400',
    },
    {
      step: '04',
      title: 'Customer Quality',
      symbol: '🛒',
      description: 'End consumers enjoy farm-to-table origin clarity and fair market prices.',
      color: 'border-pink-500/40 text-pink-400',
    },
  ];

  return (
    <section id="ecosystem" className="relative py-20 bg-slate-950 text-slate-100 overflow-hidden border-t border-slate-900">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        
        <SectionHeading
          badgeText="Visual Storytelling"
          title="One Connected Agricultural Ecosystem"
          subtitle="Discover how FarmConnectPrices creates a seamless digital bridge connecting every stage of the agricultural value chain."
        />

        {/* 3D Ecosystem Canvas Container */}
        <div className="relative rounded-3xl bg-slate-900/40 border border-slate-800 p-4 sm:p-8 backdrop-blur-xl mb-12 shadow-2xl">
          <EcosystemCanvas activeStep={activeStep} />
        </div>

        {/* Interactive Step Cards */}
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-6">
          {steps.map((item, index) => (
            <GlassCard
              key={item.step}
              onClick={() => setActiveStep(index + 1)}
              className={`p-6 transition-all duration-300 ${
                activeStep >= index + 1 ? 'border-emerald-500/40 shadow-emerald-950/20' : 'opacity-70'
              }`}
            >
              <div className="flex items-center justify-between mb-4">
                <span className="text-3xl">{item.symbol}</span>
                <span className={`text-xs font-mono font-bold px-2.5 py-1 rounded-full border ${item.color}`}>
                  STAGE {item.step}
                </span>
              </div>
              <h3 className="text-lg font-bold text-white mb-2">{item.title}</h3>
              <p className="text-xs sm:text-sm text-slate-400 leading-relaxed">{item.description}</p>
            </GlassCard>
          ))}
        </div>

      </div>
    </section>
  );
};
