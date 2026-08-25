import React from 'react';
import { Link } from 'react-router-dom';
import { User, ShieldCheck, MapPin, Edit, CheckCircle, AlertTriangle } from 'lucide-react';
import { useProfile } from '../hooks/useProfile';
import { ROLE_DISPLAY_NAMES } from '../types';
import { GlassCard } from '../../../components/ui/GlassCard';
import { Navbar } from '../../../components/navigation/Navbar';

export const ProfilePage: React.FC = () => {
  const { profile, isLoadingProfile, profileError } = useProfile();

  if (isLoadingProfile) {
    return (
      <div className="min-h-screen bg-slate-950 flex items-center justify-center p-6 text-slate-100">
        <div className="flex items-center space-x-3 text-emerald-400">
          <div className="w-6 h-6 border-2 border-current border-t-transparent rounded-full animate-spin"></div>
          <span className="text-sm font-medium">Loading profile details...</span>
        </div>
      </div>
    );
  }

  if (profileError || !profile) {
    return (
      <div className="min-h-screen bg-slate-950 flex items-center justify-center p-6 text-slate-100">
        <GlassCard className="max-w-md w-full p-6 text-center border-rose-500/30">
          <h2 className="text-xl font-bold text-rose-400 mb-2">Failed to load profile</h2>
          <p className="text-sm text-slate-300 mb-4">
            Could not fetch profile information. Please verify your authentication.
          </p>
          <Link
            to="/login"
            className="inline-block px-5 py-2.5 bg-emerald-600 hover:bg-emerald-500 text-white rounded-xl text-sm font-semibold shadow"
          >
            Go to Login
          </Link>
        </GlassCard>
      </div>
    );
  }

  const isRoleAssigned = profile.role && profile.role !== 'USER';
  const roleDisplay = profile.roleDisplayName || ROLE_DISPLAY_NAMES[profile.role] || profile.role;

  return (
    <div className="min-h-screen w-full bg-slate-950 text-slate-100 font-sans flex flex-col justify-between">
      <Navbar />

      <main className="max-w-4xl mx-auto px-4 sm:px-6 lg:px-8 pt-28 pb-16 space-y-6 w-full">
        {/* Header Bar */}
        <GlassCard className="p-6 border-slate-800 flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4">
          <div>
            <div className="inline-flex items-center space-x-2 text-xs font-bold uppercase tracking-wider text-emerald-400 mb-1">
              <User className="w-3.5 h-3.5" />
              <span>User Profile Identity</span>
            </div>
            <h1 className="text-2xl sm:text-3xl font-extrabold text-white">
              {profile.displayName || 'User Profile'}
            </h1>
            <p className="text-xs text-slate-400 mt-1 font-mono">
              UID: <code className="bg-slate-900 px-2 py-0.5 rounded text-emerald-400">{profile.uid}</code>
            </p>
          </div>

          <div className="flex items-center space-x-3">
            <Link
              to="/profile/edit"
              className="px-4 py-2 bg-emerald-600 hover:bg-emerald-500 text-white rounded-xl text-sm font-semibold shadow-lg shadow-emerald-950/50 transition-all flex items-center space-x-1.5"
            >
              <Edit className="w-4 h-4" />
              <span>Edit Profile</span>
            </Link>
            <Link
              to="/dashboard"
              className="px-4 py-2 bg-slate-900 border border-slate-700 hover:bg-slate-800 text-slate-200 rounded-xl text-sm font-medium transition-colors"
            >
              Dashboard
            </Link>
          </div>
        </GlassCard>

        {/* Role Warning Banner if role is unassigned */}
        {!isRoleAssigned && (
          <div className="p-4 bg-amber-500/10 border border-amber-500/40 rounded-2xl flex flex-col sm:flex-row sm:items-center justify-between gap-4 text-amber-300">
            <div className="flex items-start space-x-3">
              <AlertTriangle className="w-5 h-5 text-amber-400 shrink-0 mt-0.5" />
              <div>
                <h3 className="text-sm font-bold">Action Required: Role Selection Pending</h3>
                <p className="text-xs text-amber-200 mt-0.5">
                  You have not selected an application role yet. Please choose your primary role (Farmer, Mediator / Buyer, or Customer).
                </p>
              </div>
            </div>
            <Link
              to="/onboarding/role"
              className="px-4 py-2 bg-amber-600 hover:bg-amber-500 text-white text-xs font-bold rounded-xl whitespace-nowrap shadow transition-colors"
            >
              Select Role Now →
            </Link>
          </div>
        )}

        {/* Profile Details Main Grid */}
        <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
          {/* Identity & Contact Card */}
          <GlassCard className="p-6 border-slate-800 space-y-4">
            <h2 className="text-lg font-bold text-white border-b border-slate-800 pb-2 flex items-center">
              <ShieldCheck className="w-4 h-4 text-emerald-400 mr-2" />
              Account Parameters
            </h2>

            <div className="space-y-3 text-sm">
              <div>
                <span className="text-xs text-slate-400 block">Display Name</span>
                <span className="font-semibold text-white">{profile.displayName || 'N/A'}</span>
              </div>

              <div>
                <span className="text-xs text-slate-400 block">Email Address</span>
                <div className="flex items-center space-x-2 mt-0.5">
                  <span className="font-semibold text-white">{profile.email}</span>
                  {profile.emailVerified ? (
                    <span className="px-2 py-0.5 text-[10px] font-bold bg-emerald-500/20 text-emerald-400 rounded-full border border-emerald-500/30">
                      ✓ Verified
                    </span>
                  ) : (
                    <span className="px-2 py-0.5 text-[10px] font-bold bg-amber-500/20 text-amber-400 rounded-full border border-amber-500/30">
                      Unverified
                    </span>
                  )}
                </div>
              </div>

              <div>
                <span className="text-xs text-slate-400 block">Primary Ecosystem Role</span>
                <span className="inline-block px-3 py-1 mt-1 text-xs font-bold bg-emerald-500/10 text-emerald-400 border border-emerald-500/30 rounded-lg">
                  {roleDisplay}
                </span>
              </div>

              <div>
                <span className="text-xs text-slate-400 block">Phone Number</span>
                <span className="font-semibold text-white">
                  {profile.phoneNumber || 'Not provided'}
                </span>
              </div>
            </div>
          </GlassCard>

          {/* Foundational Details Card */}
          <GlassCard className="p-6 border-slate-800 space-y-4">
            <h2 className="text-lg font-bold text-white border-b border-slate-800 pb-2 flex items-center">
              <MapPin className="w-4 h-4 text-emerald-400 mr-2" />
              Location & Details
            </h2>

            <div className="space-y-3 text-sm">
              <div>
                <span className="text-xs text-slate-400 block">Profile Completion Status</span>
                <div className="mt-1">
                  {profile.profileCompleted ? (
                    <span className="inline-flex items-center px-3 py-1 rounded-full text-xs font-bold bg-emerald-500/10 text-emerald-400 border border-emerald-500/30">
                      <CheckCircle className="w-3.5 h-3.5 mr-1" />
                      Profile Complete
                    </span>
                  ) : (
                    <span className="inline-flex items-center px-3 py-1 rounded-full text-xs font-bold bg-amber-500/10 text-amber-400 border border-amber-500/30">
                      Incomplete • Details Needed
                    </span>
                  )}
                </div>
              </div>

              {profile.role === 'MEDIATOR_BUYER' && (
                <div>
                  <span className="text-xs text-slate-400 block">Business / Organization Name</span>
                  <span className="font-semibold text-white">
                    {profile.businessOrganizationName || 'Not provided'}
                  </span>
                </div>
              )}

              {profile.role === 'CUSTOMER' && (
                <div>
                  <span className="text-xs text-slate-400 block">Delivery Address</span>
                  <span className="font-semibold text-white">
                    {profile.address || 'Not provided'}
                  </span>
                </div>
              )}

              <div className="pt-2 border-t border-slate-800">
                <span className="text-xs text-slate-400 block mb-1">Primary Regional Location</span>
                <div className="grid grid-cols-2 gap-2 text-xs">
                  <div className="bg-slate-900 p-2 rounded-xl border border-slate-800">
                    <span className="text-slate-500 block text-[10px]">State</span>
                    <span className="font-semibold text-white">{profile.location?.state || '—'}</span>
                  </div>
                  <div className="bg-slate-900 p-2 rounded-xl border border-slate-800">
                    <span className="text-slate-500 block text-[10px]">District</span>
                    <span className="font-semibold text-white">{profile.location?.district || '—'}</span>
                  </div>
                  <div className="bg-slate-900 p-2 rounded-xl border border-slate-800">
                    <span className="text-slate-500 block text-[10px]">Mandal</span>
                    <span className="font-semibold text-white">{profile.location?.mandal || '—'}</span>
                  </div>
                  <div className="bg-slate-900 p-2 rounded-xl border border-slate-800">
                    <span className="text-slate-500 block text-[10px]">Village</span>
                    <span className="font-semibold text-white">{profile.location?.village || '—'}</span>
                  </div>
                </div>
              </div>
            </div>
          </GlassCard>
        </div>
      </main>
    </div>
  );
};
