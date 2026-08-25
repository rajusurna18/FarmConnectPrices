import React, { useState, useEffect } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { motion, AnimatePresence } from 'framer-motion';
import { Menu, X, ChevronRight, User, LayoutDashboard, LogOut } from 'lucide-react';
import { useAuth } from '../../features/auth/hooks/useAuth';

export const Navbar: React.FC = () => {
  const [isScrolled, setIsScrolled] = useState(false);
  const [mobileMenuOpen, setMobileMenuOpen] = useState(false);
  const { isAuthenticated, userDocument, logout } = useAuth();
  const navigate = useNavigate();

  useEffect(() => {
    const handleScroll = () => {
      if (window.scrollY > 20) {
        setIsScrolled(true);
      } else {
        setIsScrolled(false);
      }
    };
    window.addEventListener('scroll', handleScroll);
    return () => window.removeEventListener('scroll', handleScroll);
  }, []);

  const handleLogout = async () => {
    await logout();
    navigate('/');
    setMobileMenuOpen(false);
  };

  const navLinks = [
    { label: 'Home', href: '/' },
    { label: 'Markets', href: '/markets' },
    { label: 'Market Prices', href: '/#market-prices' },
    { label: 'AI Insights', href: '/#ai-insights' },
    { label: 'Marketplace', href: '/#role-previews' },
    { label: 'About', href: '/#ecosystem' },
  ];

  return (
    <header
      className={`fixed top-0 left-0 right-0 z-50 transition-all duration-300 ${
        isScrolled
          ? 'bg-slate-950/80 backdrop-blur-xl border-b border-emerald-500/20 shadow-xl shadow-slate-950/50 py-3'
          : 'bg-transparent py-5'
      }`}
    >
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 flex items-center justify-between">
        {/* Brand Logo */}
        <Link to="/" className="flex items-center space-x-2 group">
          <div className="w-9 h-9 rounded-xl bg-gradient-to-tr from-emerald-600 to-emerald-400 flex items-center justify-center text-slate-950 font-bold shadow-lg shadow-emerald-500/20 group-hover:scale-105 transition-transform">
            🌾
          </div>
          <span className="text-xl sm:text-2xl font-extrabold tracking-tight text-white">
            <span className="text-emerald-400">Farm</span>Connect<span className="text-emerald-400">Prices</span>
          </span>
        </Link>

        {/* Desktop Links */}
        <nav className="hidden md:flex items-center space-x-6 text-sm font-medium text-slate-300">
          {navLinks.map((link) =>
            link.href.startsWith('/') && !link.href.includes('#') ? (
              <Link
                key={link.label}
                to={link.href}
                className="hover:text-emerald-400 transition-colors py-1"
              >
                {link.label}
              </Link>
            ) : (
              <a
                key={link.label}
                href={link.href}
                className="hover:text-emerald-400 transition-colors py-1"
              >
                {link.label}
              </a>
            )
          )}
        </nav>

        {/* Desktop Auth CTAs */}
        <div className="hidden md:flex items-center space-x-3">
          {isAuthenticated ? (
            <div className="flex items-center space-x-3">
              <Link
                to="/dashboard"
                className="px-4 py-2 text-xs sm:text-sm font-semibold rounded-xl bg-emerald-600 hover:bg-emerald-500 text-white shadow-lg shadow-emerald-950/50 transition-all flex items-center space-x-1.5"
              >
                <LayoutDashboard className="w-4 h-4" />
                <span>Dashboard</span>
              </Link>
              <Link
                to="/profile"
                className="px-3.5 py-2 text-xs sm:text-sm font-medium rounded-xl bg-slate-900/80 hover:bg-slate-800 text-slate-200 border border-slate-700/60 backdrop-blur-md transition-colors flex items-center space-x-1.5"
              >
                <User className="w-4 h-4" />
                <span>{userDocument?.displayName || 'Profile'}</span>
              </Link>
              <button
                onClick={handleLogout}
                className="p-2 text-slate-400 hover:text-rose-400 transition-colors"
                title="Sign Out"
              >
                <LogOut className="w-4 h-4" />
              </button>
            </div>
          ) : (
            <div className="flex items-center space-x-3">
              <Link
                to="/login"
                className="px-4 py-2 text-xs sm:text-sm font-medium text-slate-200 hover:text-white bg-slate-900/60 hover:bg-slate-800/80 border border-slate-700/50 rounded-xl backdrop-blur-md transition-all"
              >
                Sign In
              </Link>
              <Link
                to="/register"
                className="px-4.5 py-2 text-xs sm:text-sm font-semibold rounded-xl bg-emerald-600 hover:bg-emerald-500 text-white shadow-lg shadow-emerald-900/30 transition-all flex items-center space-x-1"
              >
                <span>Get Started</span>
                <ChevronRight className="w-4 h-4" />
              </Link>
            </div>
          )}
        </div>

        {/* Mobile Hamburger Button */}
        <div className="md:hidden flex items-center">
          <button
            onClick={() => setMobileMenuOpen(!mobileMenuOpen)}
            className="w-12 h-12 min-w-[48px] min-h-[48px] rounded-xl bg-slate-900/85 border border-slate-800 text-slate-200 hover:text-white flex items-center justify-center focus:outline-none focus-visible:ring-2 focus-visible:ring-emerald-400"
            aria-label="Toggle navigation menu"
          >
            {mobileMenuOpen ? <X className="w-6 h-6" /> : <Menu className="w-6 h-6" />}
          </button>
        </div>
      </div>

      {/* Mobile Glass Drawer */}
      <AnimatePresence>
        {mobileMenuOpen && (
          <motion.div
            initial={{ opacity: 0, height: 0 }}
            animate={{ opacity: 1, height: 'auto' }}
            exit={{ opacity: 0, height: 0 }}
            transition={{ duration: 0.25 }}
            className="md:hidden bg-slate-950/95 backdrop-blur-2xl border-b border-emerald-500/20 px-5 pt-3 pb-6 space-y-4 shadow-2xl"
          >
            <div className="flex flex-col space-y-2">
              {navLinks.map((link) => (
                <a
                  key={link.label}
                  href={link.href}
                  onClick={() => setMobileMenuOpen(false)}
                  className="px-4 py-3 min-h-[48px] text-base font-medium text-slate-200 hover:text-emerald-400 hover:bg-slate-900/70 rounded-xl transition-all flex items-center"
                >
                  {link.label}
                </a>
              ))}
            </div>

            <div className="pt-4 border-t border-slate-800/80 flex flex-col space-y-3">
              {isAuthenticated ? (
                <>
                  <Link
                    to="/dashboard"
                    onClick={() => setMobileMenuOpen(false)}
                    className="w-full min-h-[48px] py-3 bg-emerald-600 hover:bg-emerald-500 text-white font-semibold rounded-xl text-center flex items-center justify-center space-x-2"
                  >
                    <LayoutDashboard className="w-5 h-5" />
                    <span>Go to Dashboard</span>
                  </Link>
                  <Link
                    to="/profile"
                    onClick={() => setMobileMenuOpen(false)}
                    className="w-full min-h-[48px] py-3 bg-slate-900 border border-slate-700 text-slate-200 font-medium rounded-xl text-center flex items-center justify-center space-x-2"
                  >
                    <User className="w-5 h-5" />
                    <span>View Profile</span>
                  </Link>
                  <button
                    onClick={handleLogout}
                    className="w-full min-h-[48px] py-3 text-rose-400 hover:bg-rose-500/10 rounded-xl font-medium text-center flex items-center justify-center space-x-1.5"
                  >
                    <LogOut className="w-4 h-4" />
                    <span>Sign Out</span>
                  </button>
                </>
              ) : (
                <>
                  <Link
                    to="/register"
                    onClick={() => setMobileMenuOpen(false)}
                    className="w-full min-h-[48px] py-3 bg-emerald-600 hover:bg-emerald-500 text-white font-semibold rounded-xl text-center shadow-lg shadow-emerald-950/50 flex items-center justify-center"
                  >
                    Get Started
                  </Link>
                  <Link
                    to="/login"
                    onClick={() => setMobileMenuOpen(false)}
                    className="w-full min-h-[48px] py-3 bg-slate-900 border border-slate-700 text-slate-200 font-medium rounded-xl text-center flex items-center justify-center"
                  >
                    Sign In
                  </Link>
                </>
              )}
            </div>
          </motion.div>
        )}
      </AnimatePresence>
    </header>
  );
};
