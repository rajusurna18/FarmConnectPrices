import React from 'react';
import { Link } from 'react-router-dom';
import { APP_NAME, APP_TAGLINE } from '../../constants/app';

export const Footer: React.FC = () => {
  return (
    <footer className="relative z-10 bg-slate-950 border-t border-slate-800/80 pt-12 pb-8 text-slate-400 text-sm">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 grid grid-cols-1 md:grid-cols-4 gap-8 mb-12">
        {/* Brand Column */}
        <div className="md:col-span-2 space-y-4">
          <Link to="/" className="flex items-center space-x-2">
            <div className="w-8 h-8 rounded-lg bg-emerald-500 flex items-center justify-center text-slate-950 font-bold">
              🌾
            </div>
            <span className="text-2xl font-extrabold text-white">
              <span className="text-emerald-400">Farm</span>Connect<span className="text-emerald-400">Prices</span>
            </span>
          </Link>
          <p className="text-slate-400 max-w-sm text-sm leading-relaxed">
            "{APP_TAGLINE}" — The digital bridge connecting agricultural producers, market hubs, buyers, and consumers through real intelligence.
          </p>
        </div>

        {/* Roles Quick Navigation */}
        <div className="space-y-3">
          <h4 className="text-white font-semibold text-base">Ecosystem Roles</h4>
          <ul className="space-y-2 text-xs sm:text-sm">
            <li><a href="#roles" className="hover:text-emerald-400 transition-colors">Farmer Experience</a></li>
            <li><a href="#roles" className="hover:text-emerald-400 transition-colors">Mediator / Buyer Portal</a></li>
            <li><a href="#roles" className="hover:text-emerald-400 transition-colors">Customer Traceability</a></li>
            <li><Link to="/onboarding/role" className="hover:text-emerald-400 transition-colors">Role Selection</Link></li>
          </ul>
        </div>

        {/* Platform & Account */}
        <div className="space-y-3">
          <h4 className="text-white font-semibold text-base">Platform</h4>
          <ul className="space-y-2 text-xs sm:text-sm">
            <li><a href="#market-prices" className="hover:text-emerald-400 transition-colors">Market Prices</a></li>
            <li><a href="#ai-insights" className="hover:text-emerald-400 transition-colors">AI Intelligence</a></li>
            <li><Link to="/login" className="hover:text-emerald-400 transition-colors">Account Login</Link></li>
            <li><Link to="/register" className="hover:text-emerald-400 transition-colors">Register Account</Link></li>
          </ul>
        </div>
      </div>

      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 border-t border-slate-900 pt-6 flex flex-col sm:flex-row items-center justify-between text-xs text-slate-500 gap-3">
        <p>© {new Date().getFullYear()} {APP_NAME}. All rights reserved.</p>
        <p className="text-center sm:text-right">
          Real Prices. Better Markets. Smarter Decisions.
        </p>
      </div>
    </footer>
  );
};
