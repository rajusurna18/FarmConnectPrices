import React, { useState } from 'react';
import { AnimatePresence } from 'framer-motion';
import { MapPin, ShieldCheck, Activity } from 'lucide-react';
import { SectionHeading } from '../components/ui/SectionHeading';
import { GlassCard } from '../components/ui/GlassCard';
import { Crop3DViewer } from '../components/3d/Crop3DViewer';
import { MOCK_CROPS, type CropMarketData } from '../features/market/data/mockMarketData';

export const CropMarketSection: React.FC = () => {
  const [selectedCrop, setSelectedCrop] = useState<CropMarketData>(MOCK_CROPS[0]);
  const [hoveredCropId, setHoveredCropId] = useState<string | null>(null);

  return (
    <section id="market-prices" className="relative py-20 bg-slate-950 text-slate-100 overflow-hidden border-t border-slate-900">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        
        <SectionHeading
          badgeText="Market Intelligence Architecture"
          title="Explore Agricultural Commodities"
          subtitle="Discover commodity classifications, regional Mandi hubs, and quality grading standards across India."
        />

        {/* Crops Grid */}
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-6 mb-12">
          {MOCK_CROPS.map((crop) => {
            const isSelected = selectedCrop.id === crop.id;
            const isHovered = hoveredCropId === crop.id;

            return (
              <GlassCard
                key={crop.id}
                onClick={() => setSelectedCrop(crop)}
                onMouseEnter={() => setHoveredCropId(crop.id)}
                onMouseLeave={() => setHoveredCropId(null)}
                glowColor={`${crop.color}25`}
                className={`p-6 transition-all duration-300 cursor-pointer ${
                  isSelected ? 'ring-2 ring-emerald-500/80 bg-slate-900/90' : ''
                }`}
              >
                {/* 3D Interactive Model Canvas */}
                <Crop3DViewer color={crop.color} category={crop.category} isHovered={isHovered || isSelected} />

                {/* Crop Info */}
                <div className="mt-4 space-y-2">
                  <div className="flex items-center justify-between">
                    <span className="text-xs font-semibold px-2.5 py-0.5 rounded-full bg-slate-800 text-slate-300 border border-slate-700">
                      {crop.category}
                    </span>
                    <span className="text-[11px] font-semibold text-emerald-400 flex items-center gap-1">
                      <Activity className="w-3 h-3" /> Tracked
                    </span>
                  </div>

                  <h3 className="text-lg font-bold text-white leading-snug">{crop.name}</h3>

                  <div className="flex items-center text-xs text-slate-400 pt-1">
                    <MapPin className="w-3.5 h-3.5 text-emerald-400 mr-1 shrink-0" />
                    <span>{crop.marketLocation}</span>
                  </div>

                  <div className="pt-2">
                    <span className="text-[11px] font-medium text-slate-400 bg-slate-900/80 px-2.5 py-1 rounded-lg border border-slate-800 block text-center">
                      Live Feed Connecting Soon
                    </span>
                  </div>
                </div>
              </GlassCard>
            );
          })}
        </div>

        {/* Selected Crop Detail Panel */}
        <AnimatePresence mode="wait">
          {selectedCrop && (
            <div className="space-y-4">
              <GlassCard className="p-6 sm:p-8 border-emerald-500/30">
                <div className="grid grid-cols-1 md:grid-cols-3 gap-6 items-center">
                  <div className="space-y-3 md:col-span-2">
                    <div className="flex items-center space-x-3">
                      <span className="text-xs font-semibold px-3 py-1 rounded-full bg-emerald-500/10 text-emerald-400 border border-emerald-500/30">
                        {selectedCrop.category}
                      </span>
                      <span className="text-xs font-medium text-slate-400 flex items-center">
                        <ShieldCheck className="w-4 h-4 text-emerald-400 mr-1" />
                        {selectedCrop.qualityGrade}
                      </span>
                    </div>

                    <h3 className="text-2xl sm:text-3xl font-extrabold text-white">
                      {selectedCrop.name}
                    </h3>

                    <p className="text-sm text-slate-300 leading-relaxed max-w-2xl">
                      {selectedCrop.description}
                    </p>

                    <div className="flex flex-wrap items-center gap-4 pt-2 text-xs sm:text-sm">
                      <div className="bg-slate-800/80 px-3 py-2 rounded-xl border border-slate-700">
                        <span className="text-slate-400 block text-xs">Primary Market Hub</span>
                        <span className="font-bold text-white">{selectedCrop.marketLocation}, {selectedCrop.state}</span>
                      </div>
                      <div className="bg-slate-800/80 px-3 py-2 rounded-xl border border-slate-700">
                        <span className="text-slate-400 block text-xs">Market Readiness</span>
                        <span className="font-bold text-emerald-400">API Integration Ready</span>
                      </div>
                    </div>
                  </div>

                  <div className="bg-slate-950/80 p-6 rounded-2xl border border-slate-800 flex flex-col items-center justify-center text-center space-y-3">
                    <span className="text-xs font-semibold text-emerald-400 uppercase tracking-wider">Market Intelligence Feed</span>
                    <div className="text-xl font-bold text-white">
                      Live Mandi Rates
                    </div>
                    <p className="text-xs text-slate-400 leading-relaxed">
                      Real-time market price integration will link direct Mandi pricing data in upcoming releases.
                    </p>
                    <span className="text-[11px] font-semibold text-slate-400 bg-slate-900 px-3 py-1 rounded-full border border-slate-800">
                      API Pipeline Standing By
                    </span>
                  </div>
                </div>
              </GlassCard>
            </div>
          )}
        </AnimatePresence>

      </div>
    </section>
  );
};

