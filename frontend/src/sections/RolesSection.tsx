import React from 'react';
import { motion } from 'framer-motion';
import { Link } from 'react-router-dom';
import { CheckCircle2, ArrowRight } from 'lucide-react';
import { SectionHeading } from '../components/ui/SectionHeading';
import { GlassCard } from '../components/ui/GlassCard';
import { PRIMARY_ROLE_CARDS } from '../features/market/data/roleData';
import { useAuth } from '../features/auth/hooks/useAuth';

export const RolesSection: React.FC = () => {
  const { isAuthenticated, userDocument } = useAuth();

  return (
    <section id="roles" className="relative py-20 bg-slate-950 text-slate-100 overflow-hidden border-t border-slate-900/80">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        
        <SectionHeading
          badgeText="Core Architecture"
          title="Tailored Experience for Three Primary Profiles"
          subtitle="FarmConnectPrices connects the three key pillars of the agricultural economy through role-specific market intelligence and tools."
        />

        {/* 3 Role Cards Grid */}
        <div className="grid grid-cols-1 md:grid-cols-3 gap-8">
          {PRIMARY_ROLE_CARDS.map((card, idx) => {
            const isUserRole = (userDocument?.role as string) === card.roleKey;

            return (
              <motion.div
                key={card.roleKey}
                initial={{ opacity: 0, y: 30 }}
                whileInView={{ opacity: 1, y: 0 }}
                viewport={{ once: true }}
                transition={{ duration: 0.5, delay: idx * 0.15 }}
              >
                <GlassCard
                  glowColor={
                    card.roleKey === 'FARMER'
                      ? 'rgba(52, 211, 153, 0.2)'
                      : card.roleKey === 'MEDIATOR_BUYER'
                      ? 'rgba(245, 158, 11, 0.2)'
                      : 'rgba(236, 72, 153, 0.2)'
                  }
                  className={`p-8 h-full flex flex-col justify-between border-slate-800 ${
                    isUserRole ? 'ring-2 ring-emerald-500 bg-slate-900/90' : ''
                  }`}
                >
                  <div className="space-y-6">
                    {/* Header Icon & Badge */}
                    <div className="flex items-center justify-between">
                      <div className="w-14 h-14 rounded-2xl bg-slate-800/80 border border-slate-700/80 flex items-center justify-center text-3xl shadow-inner">
                        {card.icon}
                      </div>
                      <span className="text-xs font-semibold px-3 py-1 rounded-full bg-slate-800/80 border border-slate-700 text-slate-300">
                        {card.badgeText}
                      </span>
                    </div>

                    {/* Role Title & Tagline */}
                    <div>
                      <div className="flex items-center space-x-2">
                        <h3 className="text-2xl font-bold text-white">{card.title}</h3>
                        {isUserRole && (
                          <span className="text-xs px-2 py-0.5 rounded-full bg-emerald-500/20 text-emerald-400 font-bold border border-emerald-500/40">
                            Your Role
                          </span>
                        )}
                      </div>
                      <p className="text-sm font-medium text-emerald-400 mt-1">
                        {card.tagline}
                      </p>
                    </div>

                    {/* Features List */}
                    <ul className="space-y-3 pt-2">
                      {card.features.map((feat, i) => (
                        <li key={i} className="flex items-start space-x-2.5 text-xs sm:text-sm text-slate-300">
                          <CheckCircle2 className="w-4 h-4 text-emerald-400 shrink-0 mt-0.5" />
                          <span>{feat}</span>
                        </li>
                      ))}
                    </ul>
                  </div>

                  {/* CTA Action */}
                  <div className="pt-8">
                    <Link
                      to={isAuthenticated ? '/dashboard' : '/register'}
                      className="w-full py-3 px-4 rounded-xl bg-slate-800 hover:bg-emerald-600 text-slate-200 hover:text-white font-semibold text-sm transition-all duration-200 flex items-center justify-center space-x-2 group"
                    >
                      <span>{card.ctaText}</span>
                      <ArrowRight className="w-4 h-4 group-hover:translate-x-1 transition-transform" />
                    </Link>
                  </div>
                </GlassCard>
              </motion.div>
            );
          })}
        </div>

      </div>
    </section>
  );
};
