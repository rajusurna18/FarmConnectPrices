import React from 'react';
import { Navbar } from '../components/navigation/Navbar';
import { HeroSection } from '../sections/HeroSection';
import { EcosystemSection } from '../sections/EcosystemSection';
import { RolesSection } from '../sections/RolesSection';
import { CropMarketSection } from '../sections/CropMarketSection';
import { IndiaNetworkSection } from '../sections/IndiaNetworkSection';
import { MarketAnalyticsSection } from '../sections/MarketAnalyticsSection';
import { AISection } from '../sections/AISection';
import { RoleExperiencesSection } from '../sections/RoleExperiencesSection';
import { CTASection } from '../sections/CTASection';
import { Footer } from '../components/navigation/Footer';

export const HomePage: React.FC = () => {
  return (
    <div className="relative min-h-screen w-full bg-slate-950 text-slate-100 flex flex-col justify-between overflow-x-hidden select-none font-sans">
      {/* Floating Glassmorphic Top Navbar */}
      <Navbar />

      {/* Main Landing Sections */}
      <main className="w-full flex-grow">
        <HeroSection />
        <EcosystemSection />
        <RolesSection />
        <CropMarketSection />
        <IndiaNetworkSection />
        <MarketAnalyticsSection />
        <AISection />
        <RoleExperiencesSection />
        <CTASection />
      </main>

      {/* Global Footer */}
      <Footer />
    </div>
  );
};

export default HomePage;
