import React from 'react';
import { ArrowRight, MapPin, Sparkles, ShoppingBag, Store, Sprout, Activity, ShieldCheck, Layers } from 'lucide-react';
import { SectionHeading } from '../components/ui/SectionHeading';
import { GlassCard } from '../components/ui/GlassCard';
import { Link } from 'react-router-dom';

export const RoleExperiencesSection: React.FC = () => {
  return (
    <section id="role-previews" className="relative py-20 bg-slate-950 text-slate-100 overflow-hidden border-t border-slate-900 select-none">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 space-y-20">
        
        {/* Section Header */}
        <SectionHeading
          badgeText="Role-Tailored Portals • Architectural Previews"
          title="Designed for Every Role in the Agricultural Ecosystem"
          subtitle="Specialized operational interfaces built specifically for Farmers, Mediators / Buyers, and Customers."
        />

        {/* 1. FARMER EXPERIENCE */}
        <div id="farmer-experience" className="grid grid-cols-1 lg:grid-cols-12 gap-8 items-center">
          <div className="lg:col-span-5 space-y-4">
            <div className="inline-flex items-center space-x-2 px-3 py-1 rounded-full bg-emerald-500/10 border border-emerald-500/30 text-emerald-400 text-xs font-semibold">
              <Sprout className="w-4 h-4" />
              <span>Farmer Experience</span>
            </div>
            <h3 className="text-3xl font-extrabold text-white leading-tight">Sell Smarter with Mandi Intelligence</h3>
            <p className="text-sm text-slate-300 leading-relaxed">
              Farmers register their farms, map seasonal crop cultivation, and receive direct insights on nearby Mandi arrival demand and harvest windows.
            </p>
            <div className="pt-2">
              <Link
                to="/register"
                className="inline-flex items-center space-x-2 px-6 py-3 rounded-xl bg-emerald-600 hover:bg-emerald-500 text-white font-bold text-sm shadow-lg shadow-emerald-950/50 transition-all"
              >
                <span>Register as Farmer</span>
                <ArrowRight className="w-4 h-4" />
              </Link>
            </div>
          </div>

          <div className="lg:col-span-7">
            <GlassCard className="p-6 sm:p-8 border-emerald-500/30">
              <div className="flex items-center justify-between border-b border-slate-800 pb-4 mb-6">
                <div>
                  <span className="text-xs text-slate-400 font-mono">PORTAL PREVIEW</span>
                  <h4 className="text-lg font-bold text-white">Farmer Telemetry & Crop Management</h4>
                </div>
                <span className="text-xs font-semibold px-3 py-1 rounded-full bg-emerald-500/10 text-emerald-400 border border-emerald-500/30">
                  Farmer Portal Active
                </span>
              </div>

              <div className="grid grid-cols-1 sm:grid-cols-2 gap-4 mb-6">
                <div className="bg-slate-950/80 p-4 rounded-2xl border border-slate-800 space-y-1">
                  <span className="text-xs text-slate-400 block font-medium">Farm Registration</span>
                  <p className="text-base font-bold text-white">Land & Location Mapping</p>
                  <span className="text-xs text-emerald-400 font-semibold flex items-center gap-1">
                    <ShieldCheck className="w-3.5 h-3.5" /> Module 05 Operational
                  </span>
                </div>
                <div className="bg-slate-950/80 p-4 rounded-2xl border border-slate-800 space-y-1">
                  <span className="text-xs text-slate-400 block font-medium">Crop Management</span>
                  <p className="text-base font-bold text-white">Season & Variety Tracking</p>
                  <span className="text-xs text-emerald-400 font-semibold flex items-center gap-1">
                    <Layers className="w-3.5 h-3.5" /> Kharif, Rabi & Zaid Support
                  </span>
                </div>
              </div>

              <div className="p-4 rounded-2xl bg-emerald-500/10 border border-emerald-500/30 text-xs text-emerald-200 flex items-start space-x-2.5">
                <Activity className="w-4 h-4 text-emerald-400 shrink-0 mt-0.5 animate-pulse" />
                <div>
                  <strong>Live Mandi Feed Integration:</strong> Wholesale APMC price streaming and harvest window telemetry will connect in the upcoming Market Intelligence module.
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
                  <h4 className="text-lg font-bold text-white">Regional Harvest Visibility</h4>
                </div>
                <span className="text-xs font-semibold px-3 py-1 rounded-full bg-amber-500/10 text-amber-400 border border-amber-500/30">
                  Mediator / Buyer Portal
                </span>
              </div>

              <div className="grid grid-cols-1 sm:grid-cols-2 gap-4 mb-4">
                <div className="bg-slate-950/80 p-4 rounded-2xl border border-slate-800 space-y-1.5">
                  <div className="flex items-center justify-between">
                    <span className="font-bold text-white text-base">Verified Farmer Supply</span>
                    <span className="text-[11px] px-2 py-0.5 rounded bg-emerald-500/20 text-emerald-400 font-bold">Standardized</span>
                  </div>
                  <p className="text-xs text-slate-400">Direct sourcing from registered regional farms</p>
                  <p className="text-xs text-amber-400 flex items-center pt-1 font-medium">
                    <MapPin className="w-3.5 h-3.5 text-amber-400 mr-1" /> State & District Telemetry
                  </p>
                </div>

                <div className="bg-slate-950/80 p-4 rounded-2xl border border-slate-800 space-y-1.5">
                  <div className="flex items-center justify-between">
                    <span className="font-bold text-white text-base">Wholesale Alignment</span>
                    <span className="text-[11px] px-2 py-0.5 rounded bg-amber-500/20 text-amber-400 font-bold">Transparent</span>
                  </div>
                  <p className="text-xs text-slate-400">Fair price discovery eliminating middleman markup</p>
                  <p className="text-xs text-emerald-400 flex items-center pt-1 font-medium">
                    <Sparkles className="w-3.5 h-3.5 text-emerald-400 mr-1" /> APMC Telemetry Standby
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
            <h3 className="text-3xl font-extrabold text-white leading-tight">Connect Directly with Regional Supply</h3>
            <p className="text-sm text-slate-300 leading-relaxed">
              Mediators and Buyers gain aggregated visibility over regional harvests, verified quality classifications, and transparent market signals.
            </p>
            <div className="pt-2">
              <Link
                to="/register"
                className="inline-flex items-center space-x-2 px-6 py-3 rounded-xl bg-amber-600 hover:bg-amber-500 text-white font-bold text-sm shadow-lg shadow-amber-950/50 transition-all"
              >
                <span>Register as Mediator / Buyer</span>
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
            <h3 className="text-3xl font-extrabold text-white leading-tight">Know Where Your Food Comes From</h3>
            <p className="text-sm text-slate-300 leading-relaxed">
              End consumers access farm-to-table origin transparency, regional harvest tracking, and fair retail price baselines.
            </p>
            <div className="pt-2">
              <Link
                to="/register"
                className="inline-flex items-center space-x-2 px-6 py-3 rounded-xl bg-pink-600 hover:bg-pink-500 text-white font-bold text-sm shadow-lg shadow-pink-950/50 transition-all"
              >
                <span>Register as Customer</span>
                <ArrowRight className="w-4 h-4" />
              </Link>
            </div>
          </div>

          <div className="lg:col-span-7">
            <GlassCard className="p-6 sm:p-8 border-pink-500/30">
              <div className="flex items-center justify-between border-b border-slate-800 pb-4 mb-6">
                <div>
                  <span className="text-xs text-slate-400 font-mono">TRACEABILITY PREVIEW</span>
                  <h4 className="text-lg font-bold text-white">Farm Origin & Price Transparency</h4>
                </div>
                <span className="text-xs font-semibold px-3 py-1 rounded-full bg-pink-500/10 text-pink-400 border border-pink-500/30">
                  Customer View
                </span>
              </div>

              <div className="bg-slate-950/80 p-6 rounded-2xl border border-slate-800 grid grid-cols-2 sm:grid-cols-3 gap-4 text-center">
                <div>
                  <span className="text-xs text-slate-400 block font-medium">Origin Traceability</span>
                  <strong className="text-white text-sm sm:text-base">Verified Regional Farms</strong>
                </div>
                <div>
                  <span className="text-xs text-slate-400 block font-medium">Commodity Baseline</span>
                  <strong className="text-emerald-400 text-sm sm:text-base">Standardized Data</strong>
                </div>
                <div className="col-span-2 sm:col-span-1">
                  <span className="text-xs text-slate-400 block font-medium">Price Fairness</span>
                  <strong className="text-pink-400 text-sm sm:text-base">Transparent Benchmark</strong>
                </div>
              </div>
            </GlassCard>
          </div>
        </div>

      </div>
    </section>
  );
};

