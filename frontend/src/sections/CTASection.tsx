import React from 'react';
import { Link } from 'react-router-dom';
import { ArrowRight, Sparkles } from 'lucide-react';
import { useAuth } from '../features/auth/hooks/useAuth';

export const CTASection: React.FC = () => {
  const { isAuthenticated } = useAuth();

  return (
    <section className="relative py-24 bg-slate-950 text-slate-100 overflow-hidden border-t border-slate-900">
      
      {/* Background Radial Glow */}
      <div className="absolute inset-0 bg-[radial-gradient(circle_at_center,rgba(16,185,129,0.15)_0%,transparent_70%)] pointer-events-none" />

      <div className="relative z-10 max-w-5xl mx-auto px-4 sm:px-6 lg:px-8 text-center space-y-8">
        
        <div className="inline-flex items-center space-x-2 px-4 py-1.5 rounded-full bg-slate-900/90 border border-emerald-500/30 text-emerald-300 text-xs sm:text-sm font-medium backdrop-blur-xl">
          <Sparkles className="w-4 h-4 text-emerald-400 animate-pulse" />
          <span>Real Prices • Better Markets • Smarter Decisions</span>
        </div>

        <h2 className="text-3xl sm:text-5xl md:text-6xl font-extrabold tracking-tight text-white leading-tight">
          From Farm to Market,<br /> Make Every Decision Smarter.
        </h2>

        <p className="text-base sm:text-xl text-slate-300 max-w-2xl mx-auto leading-relaxed">
          Join thousands of agricultural producers, market aggregators, and customers building a transparent digital ecosystem today.
        </p>

        <div className="pt-4 flex flex-col sm:flex-row items-center justify-center gap-4 max-w-md mx-auto">
          <Link
            to={isAuthenticated ? '/dashboard' : '/register'}
            className="w-full sm:w-auto min-h-[50px] px-8 py-3.5 bg-emerald-600 hover:bg-emerald-500 text-white font-bold rounded-2xl shadow-xl shadow-emerald-950/60 hover:shadow-emerald-600/30 transition-all flex items-center justify-center space-x-2 text-base group"
          >
            <span>{isAuthenticated ? 'Go to Dashboard' : 'Get Started Now'}</span>
            <ArrowRight className="w-5 h-5 group-hover:translate-x-1 transition-transform" />
          </Link>

          {!isAuthenticated && (
            <Link
              to="/login"
              className="w-full sm:w-auto min-h-[50px] px-8 py-3.5 bg-slate-900 hover:bg-slate-800 text-slate-200 font-medium rounded-2xl border border-slate-700 backdrop-blur-xl transition-all flex items-center justify-center text-base"
            >
              <span>Sign In to Account</span>
            </Link>
          )}
        </div>

      </div>
    </section>
  );
};
