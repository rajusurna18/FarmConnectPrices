import React from 'react';
import { Link } from 'react-router-dom';
import { motion } from 'framer-motion';
import {
  User,
  ShieldCheck,
  MapPin,
  Edit,
  CheckCircle2,
  AlertTriangle,
  Mail,
  Key,
  Phone,
  Building2,
  Home,
  RefreshCw,
} from 'lucide-react';
import { useProfile } from '../hooks/useProfile';
import { ROLE_DISPLAY_NAMES } from '../types';
import { GlassCard } from '../../../components/ui/GlassCard';
import { Navbar } from '../../../components/navigation/Navbar';
import { Footer } from '../../../components/navigation/Footer';

// Helper to compute initials from display name or email
function getInitials(displayName?: string | null, email?: string | null): string {
  if (displayName && displayName.trim()) {
    const parts = displayName.trim().split(/\s+/);
    if (parts.length >= 2) {
      return (parts[0][0] + parts[1][0]).toUpperCase();
    }
    return parts[0].substring(0, 2).toUpperCase();
  }
  if (email && email.trim()) {
    return email.trim().substring(0, 2).toUpperCase();
  }
  return 'FC';
}

export const ProfilePage: React.FC = () => {
  const { profile, isLoadingProfile, profileError, refetchProfile } = useProfile();

  // Skeleton Loading State
  if (isLoadingProfile) {
    return (
      <div className="min-h-screen bg-slate-950 text-slate-100 font-sans flex flex-col justify-between select-none">
        <Navbar />
        <main className="max-w-6xl mx-auto px-4 sm:px-6 lg:px-8 pt-28 pb-16 space-y-6 w-full flex-grow">
          {/* Skeleton Header */}
          <div className="h-16 bg-slate-900/60 rounded-2xl animate-pulse border border-slate-800" />
          
          {/* Skeleton Grid */}
          <div className="grid grid-cols-1 lg:grid-cols-[320px_1fr] gap-6">
            <div className="h-96 bg-slate-900/60 rounded-2xl animate-pulse border border-slate-800" />
            <div className="space-y-6">
              <div className="h-44 bg-slate-900/60 rounded-2xl animate-pulse border border-slate-800" />
              <div className="h-56 bg-slate-900/60 rounded-2xl animate-pulse border border-slate-800" />
            </div>
          </div>
        </main>
        <Footer />
      </div>
    );
  }

  // Error State
  if (profileError || !profile) {
    return (
      <div className="min-h-screen bg-slate-950 text-slate-100 font-sans flex flex-col justify-between">
        <Navbar />
        <main className="flex-grow flex items-center justify-center p-6">
          <GlassCard className="max-w-md w-full p-8 text-center border-rose-500/30 shadow-2xl">
            <div className="w-12 h-12 rounded-full bg-rose-500/10 border border-rose-500/30 flex items-center justify-center mx-auto mb-4 text-rose-400">
              <AlertTriangle className="w-6 h-6" />
            </div>
            <h2 className="text-xl font-bold text-white mb-2">Unable to load profile</h2>
            <p className="text-xs text-slate-400 mb-6">
              Could not fetch your profile parameters. Please verify your authentication state.
            </p>
            <div className="flex items-center justify-center space-x-3">
              <button
                onClick={() => refetchProfile()}
                className="px-4 py-2.5 bg-slate-900 hover:bg-slate-800 border border-slate-800 text-slate-200 text-xs font-semibold rounded-xl transition-colors flex items-center space-x-1.5 min-h-[44px]"
              >
                <RefreshCw className="w-3.5 h-3.5" />
                <span>Try Again</span>
              </button>
              <Link
                to="/login"
                className="px-5 py-2.5 bg-emerald-500 hover:bg-emerald-400 text-slate-950 text-xs font-bold rounded-xl shadow transition-colors min-h-[44px] flex items-center"
              >
                Go to Login
              </Link>
            </div>
          </GlassCard>
        </main>
        <Footer />
      </div>
    );
  }

  const isRoleAssigned = profile.role && profile.role !== 'USER';
  const roleDisplay = profile.roleDisplayName || (profile.role ? ROLE_DISPLAY_NAMES[profile.role] || profile.role : 'User');
  const initials = getInitials(profile.displayName, profile.email);

  // Role Badge Visual Icon
  let roleBadge = { icon: '🌾', label: roleDisplay, color: 'emerald' };
  if (profile.role === 'MEDIATOR_BUYER') {
    roleBadge = { icon: '🏪', label: 'Mediator / Buyer', color: 'sky' };
  } else if (profile.role === 'CUSTOMER') {
    roleBadge = { icon: '🛒', label: 'Customer', color: 'amber' };
  } else if (profile.role === 'FARMER') {
    roleBadge = { icon: '🌾', label: 'Farmer', color: 'emerald' };
  }

  // Real profile completion checklist
  const hasPhone = Boolean(profile.phoneNumber && profile.phoneNumber.trim().length > 0);
  const hasLocation = Boolean(
    profile.location?.state?.trim() &&
    profile.location?.district?.trim() &&
    profile.location?.mandal?.trim() &&
    profile.location?.village?.trim()
  );
  const hasRoleField =
    profile.role === 'MEDIATOR_BUYER'
      ? Boolean(profile.businessOrganizationName && profile.businessOrganizationName.trim().length > 0)
      : profile.role === 'CUSTOMER'
      ? Boolean(profile.address && profile.address.trim().length > 0)
      : true;

  const completedSteps = [hasPhone, hasLocation, hasRoleField].filter(Boolean).length;
  const totalSteps = 3;
  const completionPercentage = Math.round((completedSteps / totalSteps) * 100);

  return (
    <div className="min-h-screen w-full bg-slate-950 text-slate-100 font-sans flex flex-col justify-between overflow-x-hidden select-none relative">
      {/* Soft Ambient Radial Background Accent (No heavy 3D objects) */}
      <div className="pointer-events-none fixed inset-0 bg-[radial-gradient(ellipse_at_top,_var(--tw-gradient-stops))] from-emerald-950/20 via-slate-950 to-slate-950 z-0" />

      <Navbar />

      <main className="max-w-6xl mx-auto px-4 sm:px-6 lg:px-8 pt-28 pb-20 space-y-8 w-full flex-grow relative z-10">
        
        {/* TOP PROFILE HEADER */}
        <motion.div
          initial={{ opacity: 0, y: -10 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.3 }}
          className="flex flex-col md:flex-row md:items-center justify-between gap-4 pb-4 border-b border-slate-900"
        >
          <div>
            <div className="inline-flex items-center space-x-2 px-3 py-1 rounded-full bg-emerald-500/10 border border-emerald-500/30 text-emerald-400 text-xs font-mono mb-2">
              <User className="w-3.5 h-3.5" />
              <span>Foundational Identity Profile</span>
            </div>
            <h1 className="text-3xl sm:text-4xl font-extrabold text-white tracking-tight">
              User Profile
            </h1>
            <p className="text-sm text-slate-400 mt-1">
              Manage your FarmConnectPrices account credentials and foundational information.
            </p>
          </div>

          <Link
            to="/profile/edit"
            className="inline-flex items-center justify-center space-x-2 px-6 py-3 rounded-xl bg-emerald-500 hover:bg-emerald-400 text-slate-950 font-bold text-sm shadow-lg shadow-emerald-500/20 transition-all min-h-[48px] self-start md:self-auto"
          >
            <Edit className="w-4 h-4" />
            <span>Edit Profile</span>
          </Link>
        </motion.div>

        {/* ROLE SELECTION WARNING BANNER */}
        {!isRoleAssigned && (
          <motion.div
            initial={{ opacity: 0, scale: 0.95 }}
            animate={{ opacity: 1, scale: 1 }}
            className="p-5 bg-amber-500/10 border border-amber-500/40 rounded-2xl flex flex-col sm:flex-row sm:items-center justify-between gap-4 text-amber-300 shadow-xl"
          >
            <div className="flex items-start space-x-3">
              <AlertTriangle className="w-5 h-5 text-amber-400 shrink-0 mt-0.5" />
              <div>
                <h3 className="text-sm font-bold">Action Required: Ecosystem Role Pending</h3>
                <p className="text-xs text-amber-200/80 mt-0.5">
                  Select your primary role (Farmer, Mediator / Buyer, or Customer) to configure tailored platform views.
                </p>
              </div>
            </div>
            <Link
              to="/onboarding/role"
              className="px-5 py-2.5 bg-amber-500 hover:bg-amber-400 text-slate-950 text-xs font-extrabold rounded-xl whitespace-nowrap shadow transition-colors min-h-[44px] flex items-center justify-center"
            >
              Select Role Now →
            </Link>
          </motion.div>
        )}

        {/* MAIN DESKTOP 2-COLUMN / MOBILE 1-COLUMN GRID */}
        <div className="grid grid-cols-1 lg:grid-cols-[320px_1fr] gap-8 items-start">
          
          {/* LEFT SIDEBAR: AVATAR IDENTITY & COMPLETION CARD */}
          <motion.div
            initial={{ opacity: 0, x: -10 }}
            animate={{ opacity: 1, x: 0 }}
            transition={{ duration: 0.3, delay: 0.1 }}
          >
            <GlassCard className="p-6 border-slate-800 shadow-2xl space-y-6 text-center flex flex-col items-center">
              {/* Avatar Initial Treatment */}
              <div className="relative">
                <div className="w-20 h-20 rounded-2xl bg-gradient-to-br from-slate-900 to-slate-950 border-2 border-emerald-500/40 text-emerald-400 text-2xl font-extrabold shadow-xl shadow-emerald-500/10 flex items-center justify-center tracking-wider">
                  {initials}
                </div>
                <div className="absolute -bottom-1 -right-1 w-5 h-5 bg-emerald-500 rounded-full border-2 border-slate-950 flex items-center justify-center">
                  <CheckCircle2 className="w-3 h-3 text-slate-950" />
                </div>
              </div>

              {/* User Name & Email */}
              <div className="space-y-1 w-full">
                <h2 className="text-xl font-extrabold text-white truncate px-2">
                  {profile.displayName || 'Unnamed User'}
                </h2>
                <p className="text-xs text-slate-400 font-mono truncate px-2 break-all">
                  {profile.email}
                </p>
              </div>

              {/* Role Badge */}
              <div className="inline-flex items-center space-x-1.5 px-3.5 py-1.5 rounded-full bg-emerald-500/10 border border-emerald-500/30 text-emerald-400 text-xs font-bold shadow-sm">
                <span>{roleBadge.icon}</span>
                <span>{roleBadge.label}</span>
              </div>

              <div className="w-full border-t border-slate-900 pt-4 space-y-3">
                <div className="flex items-center justify-between text-xs font-semibold">
                  <span className="text-slate-400">Profile Completion</span>
                  <span className="text-emerald-400 font-mono">{completionPercentage}%</span>
                </div>
                
                {/* Progress Bar */}
                <div className="w-full bg-slate-950 h-2 rounded-full overflow-hidden border border-slate-900">
                  <div
                    className="bg-gradient-to-r from-emerald-500 to-teal-400 h-full transition-all duration-500 rounded-full"
                    style={{ width: `${completionPercentage}%` }}
                  />
                </div>

                <p className="text-[11px] text-slate-500 text-left">
                  {completionPercentage === 100
                    ? '✓ All foundational parameters configured.'
                    : 'Complete phone, location, and role details.'}
                </p>
              </div>

              {/* Action Button */}
              <Link
                to="/profile/edit"
                className="w-full inline-flex items-center justify-center space-x-2 px-5 py-3 rounded-xl bg-slate-900 hover:bg-slate-800 text-slate-200 border border-slate-800 text-xs font-semibold transition-colors min-h-[48px]"
              >
                <Edit className="w-4 h-4 text-emerald-400" />
                <span>Edit Profile Details</span>
              </Link>
            </GlassCard>
          </motion.div>

          {/* RIGHT SIDEBAR: DETAILED INFORMATION PANELS */}
          <motion.div
            initial={{ opacity: 0, x: 10 }}
            animate={{ opacity: 1, x: 0 }}
            transition={{ duration: 0.3, delay: 0.2 }}
            className="space-y-6"
          >
            {/* 1. ACCOUNT INFORMATION */}
            <GlassCard className="p-6 border-slate-800 shadow-2xl space-y-4">
              <h2 className="text-base font-bold text-white border-b border-slate-900 pb-3 flex items-center space-x-2">
                <ShieldCheck className="w-4.5 h-4.5 text-emerald-400" />
                <span>Account Information</span>
              </h2>

              <div className="grid grid-cols-1 sm:grid-cols-2 gap-4 text-xs">
                <div className="p-3.5 rounded-xl bg-slate-950/80 border border-slate-900 space-y-1">
                  <span className="text-slate-400 font-medium flex items-center">
                    <Mail className="w-3.5 h-3.5 text-sky-400 mr-1.5" />
                    Email Address
                  </span>
                  <div className="flex items-center space-x-2 pt-1">
                    <span className="font-semibold text-slate-200 break-all">{profile.email}</span>
                    {profile.emailVerified && (
                      <span className="text-[10px] font-bold text-emerald-400 shrink-0">✓ Verified</span>
                    )}
                  </div>
                </div>

                <div className="p-3.5 rounded-xl bg-slate-950/80 border border-slate-900 space-y-1">
                  <span className="text-slate-400 font-medium flex items-center">
                    <Key className="w-3.5 h-3.5 text-emerald-400 mr-1.5" />
                    Firebase UID
                  </span>
                  <code className="font-mono text-slate-300 block text-xs break-all pt-1 font-semibold">
                    {profile.uid}
                  </code>
                </div>

                <div className="p-3.5 rounded-xl bg-slate-950/80 border border-slate-900 space-y-1 sm:col-span-2">
                  <span className="text-slate-400 font-medium">Account Role</span>
                  <div className="pt-1 flex items-center space-x-2">
                    <span className="font-extrabold text-white">{roleBadge.icon} {roleBadge.label}</span>
                    <span className="text-[10px] font-mono text-slate-500">({profile.role || 'UNASSIGNED'})</span>
                  </div>
                </div>
              </div>
            </GlassCard>

            {/* 2. CONTACT & LOCATION */}
            <GlassCard className="p-6 border-slate-800 shadow-2xl space-y-4">
              <h2 className="text-base font-bold text-white border-b border-slate-900 pb-3 flex items-center space-x-2">
                <MapPin className="w-4.5 h-4.5 text-emerald-400" />
                <span>Contact & Regional Location</span>
              </h2>

              <div className="grid grid-cols-1 sm:grid-cols-2 gap-4 text-xs">
                <div className="p-3.5 rounded-xl bg-slate-950/80 border border-slate-900 space-y-1 sm:col-span-2">
                  <span className="text-slate-400 font-medium flex items-center">
                    <Phone className="w-3.5 h-3.5 text-slate-400 mr-1.5" />
                    Contact Phone Number
                  </span>
                  <span className={`font-semibold block pt-1 ${profile.phoneNumber ? 'text-slate-200' : 'text-slate-500 font-normal italic'}`}>
                    {profile.phoneNumber || 'Not provided'}
                  </span>
                </div>

                <div className="p-3 rounded-xl bg-slate-950/80 border border-slate-900">
                  <span className="text-[10px] text-slate-500 block">State</span>
                  <span className={`font-semibold block text-xs ${profile.location?.state ? 'text-white' : 'text-slate-500 font-normal italic'}`}>
                    {profile.location?.state || 'Not provided'}
                  </span>
                </div>

                <div className="p-3 rounded-xl bg-slate-950/80 border border-slate-900">
                  <span className="text-[10px] text-slate-500 block">District</span>
                  <span className={`font-semibold block text-xs ${profile.location?.district ? 'text-white' : 'text-slate-500 font-normal italic'}`}>
                    {profile.location?.district || 'Not provided'}
                  </span>
                </div>

                <div className="p-3 rounded-xl bg-slate-950/80 border border-slate-900">
                  <span className="text-[10px] text-slate-500 block">Mandal / Tehsil</span>
                  <span className={`font-semibold block text-xs ${profile.location?.mandal ? 'text-white' : 'text-slate-500 font-normal italic'}`}>
                    {profile.location?.mandal || 'Not provided'}
                  </span>
                </div>

                <div className="p-3 rounded-xl bg-slate-950/80 border border-slate-900">
                  <span className="text-[10px] text-slate-500 block">Village / City</span>
                  <span className={`font-semibold block text-xs ${profile.location?.village ? 'text-white' : 'text-slate-500 font-normal italic'}`}>
                    {profile.location?.village || 'Not provided'}
                  </span>
                </div>
              </div>
            </GlassCard>

            {/* 3. ROLE-SPECIFIC INFORMATION */}
            <GlassCard className="p-6 border-slate-800 shadow-2xl space-y-4">
              <h2 className="text-base font-bold text-white border-b border-slate-900 pb-3 flex items-center space-x-2">
                <span>Role-Specific Details</span>
              </h2>

              <div className="text-xs space-y-3">
                {profile.role === 'FARMER' && (
                  <div className="p-3.5 rounded-xl bg-slate-950/80 border border-slate-900 space-y-1">
                    <span className="text-slate-400 font-medium">Agricultural Profile</span>
                    <p className="text-slate-300 pt-0.5 leading-relaxed">
                      Primary agricultural profile configured for farm management and crop tracking across cultivation seasons.
                    </p>
                  </div>
                )}

                {profile.role === 'MEDIATOR_BUYER' && (
                  <div className="p-3.5 rounded-xl bg-slate-950/80 border border-slate-900 space-y-1">
                    <span className="text-slate-400 font-medium flex items-center">
                      <Building2 className="w-3.5 h-3.5 text-slate-400 mr-1.5" />
                      Business / Organization Name
                    </span>
                    <span className={`font-semibold block pt-1 ${profile.businessOrganizationName ? 'text-white' : 'text-slate-500 font-normal italic'}`}>
                      {profile.businessOrganizationName || 'Not provided'}
                    </span>
                  </div>
                )}

                {profile.role === 'CUSTOMER' && (
                  <div className="p-3.5 rounded-xl bg-slate-950/80 border border-slate-900 space-y-1">
                    <span className="text-slate-400 font-medium flex items-center">
                      <Home className="w-3.5 h-3.5 text-slate-400 mr-1.5" />
                      Delivery Address
                    </span>
                    <span className={`font-semibold block pt-1 ${profile.address ? 'text-white' : 'text-slate-500 font-normal italic'}`}>
                      {profile.address || 'Not provided'}
                    </span>
                  </div>
                )}
              </div>
            </GlassCard>

            {/* 4. PROFILE STATUS CHECKLIST */}
            <GlassCard className="p-6 border-slate-800 shadow-2xl space-y-4">
              <h2 className="text-base font-bold text-white border-b border-slate-900 pb-3 flex items-center space-x-2">
                <CheckCircle2 className="w-4.5 h-4.5 text-emerald-400" />
                <span>Profile Parameter Status</span>
              </h2>

              <div className="space-y-2 text-xs">
                <div className="flex items-center justify-between p-2.5 rounded-xl bg-slate-950/80 border border-slate-900">
                  <span className="text-slate-300">Account Credentials & UID</span>
                  <span className="text-emerald-400 font-bold flex items-center">
                    <CheckCircle2 className="w-3.5 h-3.5 mr-1" /> Configured
                  </span>
                </div>

                <div className="flex items-center justify-between p-2.5 rounded-xl bg-slate-950/80 border border-slate-900">
                  <span className="text-slate-300">Contact Phone Number</span>
                  {hasPhone ? (
                    <span className="text-emerald-400 font-bold flex items-center">
                      <CheckCircle2 className="w-3.5 h-3.5 mr-1" /> Configured
                    </span>
                  ) : (
                    <span className="text-amber-400 font-semibold">Action Needed</span>
                  )}
                </div>

                <div className="flex items-center justify-between p-2.5 rounded-xl bg-slate-950/80 border border-slate-900">
                  <span className="text-slate-300">Regional Location Metadata</span>
                  {hasLocation ? (
                    <span className="text-emerald-400 font-bold flex items-center">
                      <CheckCircle2 className="w-3.5 h-3.5 mr-1" /> Configured
                    </span>
                  ) : (
                    <span className="text-amber-400 font-semibold">Action Needed</span>
                  )}
                </div>
              </div>
            </GlassCard>

            {/* BOTTOM ACTION AREA */}
            <div className="pt-2">
              <Link
                to="/profile/edit"
                className="w-full inline-flex items-center justify-center space-x-2 px-6 py-3.5 rounded-xl bg-emerald-500 hover:bg-emerald-400 text-slate-950 font-bold text-sm shadow-lg shadow-emerald-500/20 transition-all min-h-[48px]"
              >
                <Edit className="w-4 h-4" />
                <span>Edit Profile Information</span>
              </Link>
            </div>

          </motion.div>

        </div>

      </main>

      <Footer />
    </div>
  );
};
