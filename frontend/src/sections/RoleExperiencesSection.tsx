import React from 'react';
import { ArrowRight, MapPin, Sparkles, ShoppingBag, Store, Sprout } from 'lucide-react';
import { SectionHeading } from '../components/ui/SectionHeading';
import { GlassCard } from '../components/ui/GlassCard';
import { PriceBadge } from '../components/ui/PriceBadge';
import { Link } from 'react-router-dom';

export const RoleExperiencesSection: React.FC = () => {
  return (
    <section id="role-previews" className="relative py-20 bg-slate-950 text-slate-100 overflow-hidden border-t border-slate-900">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 space-y-20">
        
        {/* Section Header */}
        <SectionHeading
          badgeText="Role-Tailored Portals • UI Previews"
          title="Designed for Every Role in the Agriculture Journey"
          subtitle="Explore mock UI previews tailored specifically for Farmers, Mediators / Buyers, and Customers."
        />

        {/* 1. FARMER EXPERIENCE */}
        <div id="farmer-experience" className="grid grid-cols-1 lg:grid-cols-12 gap-8 items-center">
          <div className="lg:col-span-5 space-y-4">
            <div className="inline-flex items-center space-x-2 px-3 py-1 rounded-full bg-emerald-500/10 border border-emerald-500/30 text-emerald-400 text-xs font-semibold">
              <Sprout className="w-4 h-4" />
              <span>Farmer Experience</span>
            </div>
            <h3 className="text-3xl font-extrabold text-white">Sell Smarter with Mandi Intelligence</h3>
            <p className="text-sm text-slate-300 leading-relaxed">
              Farmers gain direct insights into nearby market price surges, buyer demand levels, and AI recommendations for optimal harvest timing.
            </p>
            <div className="pt-2">
              <Link
                to="/register"
                className="inline-flex items-center space-x-2 px-6 py-3 rounded-xl bg-emerald-600 hover:bg-emerald-500 text-white font-bold text-sm shadow-lg shadow-emerald-950/50 transition-all"
              >
                <span>Explore Opportunities</span>
                <ArrowRight className="w-4 h-4" />
              </Link>
            </div>
          </div>

          <div className="lg:col-span-7">
            <GlassCard className="p-6 sm:p-8 border-emerald-500/30">
              <div className="flex items-center justify-between border-b border-slate-800 pb-4 mb-6">
                <div>
                  <span className="text-xs text-slate-400 font-mono">LIVE PREVIEW</span>
                  <h4 className="text-lg font-bold text-white">Today's Market Intelligence</h4>
                </div>
                <span className="text-xs font-semibold px-3 py-1 rounded-full bg-emerald-500/10 text-emerald-400 border border-emerald-500/30">
                  Farmer Portal
                </span>
              </div>

              <div className="grid grid-cols-1 sm:grid-cols-2 gap-4 mb-6">
                <div className="bg-slate-950/80 p-4 rounded-2xl border border-slate-800">
                  <span className="text-xs text-slate-400">Best Opportunity</span>
                  <p className="text-lg font-bold text-white">Tomato (Hybrid)</p>
                  <span className="text-xs text-emerald-400 font-semibold">Highest Market: Warangal</span>
                </div>
                <div className="bg-slate-950/80 p-4 rounded-2xl border border-slate-800">
                  <span className="text-xs text-slate-400">Current Mandi Price</span>
                  <p className="text-lg font-bold text-white">₹2,850 / Quintal</p>
                  <PriceBadge percentage={7.8} isUp={true} size="sm" />
                </div>
              </div>

              <div className="p-4 rounded-2xl bg-emerald-500/10 border border-emerald-500/30 text-xs text-emerald-200 flex items-start space-x-2">
                <Sparkles className="w-4 h-4 text-emerald-400 shrink-0 mt-0.5" />
                <div>
                  <strong>AI Recommendation:</strong> "Tomato prices are trending upward in nearby markets (+7.8%). Recommended harvest window: next 48 hours."
                </div>
              </div>
            </GlassCard>
          </div>
        </div>

        {/* 2. MEDIATOR / BUYER EXPERIENCE */}
        <div id="mediator-buyer-experience" className="grid grid-cols-1 lg:grid-cols-12 gap-8 items-center">
          <div className="lg:col-span-7 order-2 lg:order-1">
            <GlassCard className="p-6 sm:p-8 border-amber-500/30">
              <div className="flex items-center justify-between border-b border-slate-800 pb-4 mb-6">
                <div>
                  <span className="text-xs text-slate-400 font-mono">SUPPLY MATCHING</span>
                  <h4 className="text-lg font-bold text-white">Available Regional Produce</h4>
                </div>
                <span className="text-xs font-semibold px-3 py-1 rounded-full bg-amber-500/10 text-amber-400 border border-amber-500/30">
                  Mediator / Buyer Portal
                </span>
              </div>

              <div className="grid grid-cols-1 sm:grid-cols-2 gap-4 mb-4">
                <div className="bg-slate-950/80 p-4 rounded-2xl border border-slate-800">
                  <div className="flex items-center justify-between mb-1">
                    <span className="font-bold text-white text-base">Tomato (Hybrid)</span>
                    <span className="text-xs px-2 py-0.5 rounded bg-emerald-500/20 text-emerald-400 font-bold">Grade A</span>
                  </div>
                  <p className="text-xs text-slate-400">Available Quantity: <strong className="text-white">2,500 kg</strong></p>
                  <p className="text-xs text-slate-400">Expected Price: <strong className="text-amber-400">₹2,850 / Quintal</strong></p>
                  <p className="text-xs text-slate-400 flex items-center mt-1">
                    <MapPin className="w-3 h-3 text-emerald-400 mr-1" /> Telangana
                  </p>
                </div>

                <div className="bg-slate-950/80 p-4 rounded-2xl border border-slate-800">
                  <div className="flex items-center justify-between mb-1">
                    <span className="font-bold text-white text-base">Teja Chilli</span>
                    <span className="text-xs px-2 py-0.5 rounded bg-emerald-500/20 text-emerald-400 font-bold">Premium</span>
                  </div>
                  <p className="text-xs text-slate-400">Available Quantity: <strong className="text-white">1,200 kg</strong></p>
                  <p className="text-xs text-slate-400">Expected Price: <strong className="text-amber-400">₹18,500 / Quintal</strong></p>
                  <p className="text-xs text-slate-400 flex items-center mt-1">
                    <MapPin className="w-3 h-3 text-emerald-400 mr-1" /> Warangal
                  </p>
                </div>
              </div>
            </GlassCard>
          </div>

          <div className="lg:col-span-5 space-y-4 order-1 lg:order-2">
            <div className="inline-flex items-center space-x-2 px-3 py-1 rounded-full bg-amber-500/10 border border-amber-500/30 text-amber-400 text-xs font-semibold">
              <Store className="w-4 h-4" />
              <span>Mediator / Buyer Experience</span>
            </div>
            <h3 className="text-3xl font-extrabold text-white">Connect Directly with Regional Supply</h3>
            <p className="text-sm text-slate-300 leading-relaxed">
              Mediators and Buyers gain aggregated visibility over fresh harvests, quality grades, and transparent wholesale pricing.
            </p>
            <div className="pt-2">
              <Link
                to="/register"
                className="inline-flex items-center space-x-2 px-6 py-3 rounded-xl bg-amber-600 hover:bg-amber-500 text-white font-bold text-sm shadow-lg shadow-amber-950/50 transition-all"
              >
                <span>Explore Supply</span>
                <ArrowRight className="w-4 h-4" />
              </Link>
            </div>
          </div>
        </div>

        {/* 3. CUSTOMER EXPERIENCE */}
        <div id="customer-experience" className="grid grid-cols-1 lg:grid-cols-12 gap-8 items-center">
          <div className="lg:col-span-5 space-y-4">
            <div className="inline-flex items-center space-x-2 px-3 py-1 rounded-full bg-pink-500/10 border border-pink-500/30 text-pink-400 text-xs font-semibold">
              <ShoppingBag className="w-4 h-4" />
              <span>Customer Experience</span>
            </div>
            <h3 className="text-3xl font-extrabold text-white">Know Where Your Food Comes From</h3>
            <p className="text-sm text-slate-300 leading-relaxed">
              End consumers access farm-to-table transparency, verified farm origin details, and fair pricing baselines.
            </p>
            <div className="pt-2">
              <Link
                to="/register"
                className="inline-flex items-center space-x-2 px-6 py-3 rounded-xl bg-pink-600 hover:bg-pink-500 text-white font-bold text-sm shadow-lg shadow-pink-950/50 transition-all"
              >
                <span>Explore Products</span>
                <ArrowRight className="w-4 h-4" />
              </Link>
            </div>
          </div>

          <div className="lg:col-span-7">
            <GlassCard className="p-6 sm:p-8 border-pink-500/30">
              <div className="flex items-center justify-between border-b border-slate-800 pb-4 mb-6">
                <div>
                  <span className="text-xs text-slate-400 font-mono">TRACEABILITY PREVIEW</span>
                  <h4 className="text-lg font-bold text-white">Farm Origin Transparency</h4>
                </div>
                <span className="text-xs font-semibold px-3 py-1 rounded-full bg-pink-500/10 text-pink-400 border border-pink-500/30">
                  Customer View
                </span>
              </div>

              <div className="bg-slate-950/80 p-6 rounded-2xl border border-slate-800 grid grid-cols-2 sm:grid-cols-4 gap-4 text-center">
                <div>
                  <span className="text-xs text-slate-400 block">Produce</span>
                  <strong className="text-white text-base">Tomato</strong>
                </div>
                <div>
                  <span className="text-xs text-slate-400 block">Source</span>
                  <strong className="text-emerald-400 text-base">Telangana</strong>
                </div>
                <div>
                  <span className="text-xs text-slate-400 block">Farmer</span>
                  <strong className="text-white text-base">Demo Farmer</strong>
                </div>
                <div>
                  <span className="text-xs text-slate-400 block">Retail Rate</span>
                  <strong className="text-pink-400 text-base">₹120 / kg</strong>
                </div>
              </div>
            </GlassCard>
          </div>
        </div>

      </div>
    </section>
  );
};
