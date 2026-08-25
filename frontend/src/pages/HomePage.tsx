import React, { lazy, Suspense } from 'react';
import { Navbar } from '../components/navigation/Navbar';
import { HeroSection } from '../sections/HeroSection';
import { RolesSection } from '../sections/RolesSection';
import { MarketIntelligencePreview } from '../sections/MarketIntelligencePreview';
import { RoleExperiencesSection } from '../sections/RoleExperiencesSection';
import { CTASection } from '../sections/CTASection';
import { Footer } from '../components/navigation/Footer';

const EcosystemSectionLazy = lazy(() =>
  import('../sections/EcosystemSection').then((m) => ({ default: m.EcosystemSection }))
);

const IndiaNetworkSectionLazy = lazy(() =>
  import('../sections/IndiaNetworkSection').then((m) => ({ default: m.IndiaNetworkSection }))
);

const AISectionLazy = lazy(() =>
  import('../sections/AISection').then((m) => ({ default: m.AISection }))
);

const SectionFallback: React.FC = () => (
  <div className="w-full py-20 bg-slate-950 flex items-center justify-center border-t border-slate-900 select-none min-h-[350px]">
    <div className="flex flex-col items-center space-y-3">
      <div className="w-8 h-8 border-2 border-emerald-500 border-t-transparent rounded-full animate-spin" />
      <span className="text-xs text-slate-400 font-medium">Loading visualization...</span>
    </div>
  </div>
);

export const HomePage: React.FC = () => {
  return (
    <div className="relative min-h-screen w-full bg-slate-950 text-slate-100 flex flex-col justify-between overflow-x-hidden select-none font-sans">
      {/* Floating Glassmorphic Top Navbar */}
      <Navbar />

      {/* Main Landing Sections */}
      <main className="w-full flex-grow">
        <HeroSection />

        <Suspense fallback={<SectionFallback />}>
          <EcosystemSectionLazy />
        </Suspense>

        <RolesSection />
        <MarketIntelligencePreview />

        <Suspense fallback={<SectionFallback />}>
          <IndiaNetworkSectionLazy />
        </Suspense>

        <Suspense fallback={<SectionFallback />}>
          <AISectionLazy />
        </Suspense>

        <RoleExperiencesSection />
        <CTASection />
      </main>

      {/* Global Footer */}
      <Footer />
    </div>
  );
};

export default HomePage;

