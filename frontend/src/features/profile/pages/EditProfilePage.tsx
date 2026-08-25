import React, { useState, useEffect } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { motion } from 'framer-motion';
import {
  ArrowLeft,
  ShieldCheck,
  User,
  Mail,
  Phone,
  Building2,
  Home,
  MapPin,
  Save,
  CheckCircle2,
  AlertCircle,
  Key,
  Award,
} from 'lucide-react';
import { useProfile } from '../hooks/useProfile';
import { ROLE_DISPLAY_NAMES } from '../types';
import { GlassCard } from '../../../components/ui/GlassCard';
import { Navbar } from '../../../components/navigation/Navbar';
import { Footer } from '../../../components/navigation/Footer';

export const EditProfilePage: React.FC = () => {
  const { profile, isLoadingProfile, updateProfile, isUpdatingProfile } = useProfile();
  const navigate = useNavigate();

  const [displayName, setDisplayName] = useState('');
  const [phoneNumber, setPhoneNumber] = useState('');
  const [stateName, setStateName] = useState('');
  const [district, setDistrict] = useState('');
  const [mandal, setMandal] = useState('');
  const [village, setVillage] = useState('');
  const [businessOrganizationName, setBusinessOrganizationName] = useState('');
  const [address, setAddress] = useState('');

  const [feedback, setFeedback] = useState<{ type: 'success' | 'error'; message: string } | null>(null);

  useEffect(() => {
    if (profile) {
      setDisplayName(profile.displayName || '');
      setPhoneNumber(profile.phoneNumber || '');
      setStateName(profile.location?.state || '');
      setDistrict(profile.location?.district || '');
      setMandal(profile.location?.mandal || '');
      setVillage(profile.location?.village || '');
      setBusinessOrganizationName(profile.businessOrganizationName || '');
      setAddress(profile.address || '');
    }
  }, [profile]);

  if (isLoadingProfile) {
    return (
      <div className="min-h-screen bg-slate-950 flex items-center justify-center p-6 text-slate-100 font-sans select-none">
        <div className="flex flex-col items-center space-y-4">
          <div className="w-12 h-12 border-4 border-emerald-500 border-t-transparent rounded-full animate-spin"></div>
          <p className="text-slate-400 font-medium text-sm">Loading profile parameters...</p>
        </div>
      </div>
    );
  }

  const handleSave = async (e: React.FormEvent) => {
    e.preventDefault();
    setFeedback(null);

    try {
      await updateProfile({
        displayName: displayName.trim(),
        phoneNumber: phoneNumber.trim(),
        location: {
          state: stateName.trim(),
          district: district.trim(),
          mandal: mandal.trim(),
          village: village.trim(),
        },
        businessOrganizationName: profile?.role === 'MEDIATOR_BUYER' ? businessOrganizationName.trim() : undefined,
        address: profile?.role === 'CUSTOMER' ? address.trim() : undefined,
      });

      setFeedback({ type: 'success', message: 'Profile updated successfully!' });
      setTimeout(() => navigate('/profile'), 1200);
    } catch (err: unknown) {
      console.error('Failed to update profile:', err);
      const resData = (err as { response?: { data?: string | { message?: string } } })?.response?.data;
      const errorMsg =
        typeof resData === 'string'
          ? resData
          : typeof resData === 'object' && resData?.message
          ? resData.message
          : 'Failed to update profile. Please verify fields and try again.';
      setFeedback({ type: 'error', message: errorMsg });
    }
  };

  const roleDisplay = profile?.roleDisplayName || (profile?.role ? ROLE_DISPLAY_NAMES[profile.role] || profile.role : 'User');

  // Real profile completion checklist
  const hasPhone = Boolean(phoneNumber && phoneNumber.trim().length > 0);
  const hasLocation = Boolean(stateName.trim() && district.trim() && mandal.trim() && village.trim());
  const hasRoleField =
    profile?.role === 'MEDIATOR_BUYER'
      ? Boolean(businessOrganizationName && businessOrganizationName.trim().length > 0)
      : profile?.role === 'CUSTOMER'
      ? Boolean(address && address.trim().length > 0)
      : true;

  const completedStepsCount = [hasPhone, hasLocation, hasRoleField].filter(Boolean).length;
  const totalStepsCount = 3;
  const completionPercentage = Math.round((completedStepsCount / totalStepsCount) * 100);

  return (
    <div className="min-h-screen w-full bg-slate-950 text-slate-100 font-sans flex flex-col justify-between overflow-x-hidden select-none">
      <Navbar />

      <main className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 pt-28 pb-20 space-y-8 w-full flex-grow">
        
        {/* TOP HEADER */}
        <motion.div
          initial={{ opacity: 0, y: -10 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.3 }}
          className="flex flex-col md:flex-row md:items-center justify-between gap-4 pb-4 border-b border-slate-900"
        >
          <div>
            <Link
              to="/profile"
              className="inline-flex items-center space-x-2 text-xs font-semibold text-slate-400 hover:text-emerald-400 transition-colors mb-2"
            >
              <ArrowLeft className="w-4 h-4" />
              <span>Back to Profile</span>
            </Link>

            <h1 className="text-3xl sm:text-4xl font-extrabold text-white tracking-tight">
              Edit Foundational Profile
            </h1>
            <p className="text-sm text-slate-400 mt-1">
              Update your core identity, primary contact metrics, and regional location metadata.
            </p>
          </div>

          <div className="inline-flex items-center space-x-2 px-3.5 py-2 rounded-xl bg-slate-900/90 border border-slate-800 text-xs text-slate-300 shrink-0 self-start md:self-auto shadow-md">
            <ShieldCheck className="w-4 h-4 text-emerald-400" />
            <span className="font-medium">Profile Security: <strong className="text-emerald-400 font-semibold">Protected</strong></span>
          </div>
        </motion.div>

        {/* FEEDBACK ALERT */}
        {feedback && (
          <motion.div
            initial={{ opacity: 0, scale: 0.95 }}
            animate={{ opacity: 1, scale: 1 }}
            role="alert"
            className={`p-4 rounded-2xl text-sm font-medium flex items-center space-x-3 shadow-lg border ${
              feedback.type === 'success'
                ? 'bg-emerald-500/10 border-emerald-500/30 text-emerald-300'
                : 'bg-red-500/10 border-red-500/30 text-red-300'
            }`}
          >
            {feedback.type === 'success' ? (
              <CheckCircle2 className="w-5 h-5 text-emerald-400 shrink-0" />
            ) : (
              <AlertCircle className="w-5 h-5 text-red-400 shrink-0" />
            )}
            <span>{feedback.message}</span>
          </motion.div>
        )}

        {/* SYSTEM ACCOUNT IDENTIFIERS CARD (READ-ONLY) */}
        <motion.div
          initial={{ opacity: 0, y: 10 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.3, delay: 0.1 }}
        >
          <GlassCard className="p-6 border-emerald-500/20 shadow-2xl relative overflow-hidden">
            {/* Subtle glow accent */}
            <div className="absolute top-0 right-0 w-64 h-64 bg-emerald-500/5 rounded-full blur-3xl pointer-events-none" />

            <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-2 mb-4 border-b border-slate-900 pb-3">
              <div>
                <h3 className="text-base font-bold text-white flex items-center space-x-2">
                  <Key className="w-4 h-4 text-emerald-400" />
                  <span>System Account Identifiers</span>
                </h3>
                <p className="text-xs text-slate-400 mt-0.5">
                  These core account parameters are managed by the authentication system and remain read-only.
                </p>
              </div>

              <span className="text-[10px] font-mono font-semibold uppercase px-2.5 py-1 rounded-full bg-slate-950 text-slate-400 border border-slate-800 self-start sm:self-auto">
                Read-Only System Data
              </span>
            </div>

            <div className="grid grid-cols-1 sm:grid-cols-3 gap-4 text-xs">
              {/* UID */}
              <div className="p-3.5 rounded-xl bg-slate-950/80 border border-slate-900 space-y-1">
                <div className="flex items-center justify-between">
                  <span className="text-slate-400 font-medium flex items-center">
                    <Key className="w-3.5 h-3.5 text-emerald-400 mr-1.5 shrink-0" />
                    UID
                  </span>
                  <span className="text-[10px] font-semibold px-2 py-0.5 rounded-full bg-slate-900 text-slate-400 border border-slate-800">
                    System ID
                  </span>
                </div>
                <code className="font-mono text-slate-200 block text-xs break-all font-semibold pt-1">
                  {profile?.uid || '—'}
                </code>
              </div>

              {/* EMAIL */}
              <div className="p-3.5 rounded-xl bg-slate-950/80 border border-slate-900 space-y-1">
                <div className="flex items-center justify-between">
                  <span className="text-slate-400 font-medium flex items-center">
                    <Mail className="w-3.5 h-3.5 text-sky-400 mr-1.5 shrink-0" />
                    Email
                  </span>
                  <span className={`text-[10px] font-semibold px-2 py-0.5 rounded-full border ${
                    profile?.emailVerified
                      ? 'bg-emerald-500/10 text-emerald-400 border-emerald-500/30'
                      : 'bg-amber-500/10 text-amber-400 border-amber-500/30'
                  }`}>
                    {profile?.emailVerified ? 'Verified' : 'Unverified'}
                  </span>
                </div>
                <span className="font-semibold text-slate-200 block text-xs break-all pt-1">
                  {profile?.email || '—'}
                </span>
              </div>

              {/* ROLE */}
              <div className="p-3.5 rounded-xl bg-slate-950/80 border border-slate-900 space-y-1">
                <div className="flex items-center justify-between">
                  <span className="text-slate-400 font-medium flex items-center">
                    <Award className="w-3.5 h-3.5 text-amber-400 mr-1.5 shrink-0" />
                    Role
                  </span>
                  <span className="text-[10px] font-semibold px-2 py-0.5 rounded-full bg-emerald-500/10 text-emerald-400 border border-emerald-500/30">
                    Primary Role
                  </span>
                </div>
                <span className="font-extrabold text-white block text-sm pt-1">
                  {roleDisplay}
                </span>
              </div>
            </div>
          </GlassCard>
        </motion.div>

        {/* MAIN CONTENT GRID */}
        <form onSubmit={handleSave}>
          <div className="grid grid-cols-1 lg:grid-cols-[1fr_340px] gap-8 items-start">
            
            {/* LEFT COLUMN: CONTACT & IDENTITY FORM */}
            <motion.div
              initial={{ opacity: 0, x: -10 }}
              animate={{ opacity: 1, x: 0 }}
              transition={{ duration: 0.3, delay: 0.2 }}
              className="space-y-6"
            >
              <GlassCard className="p-6 sm:p-8 border-slate-800 shadow-2xl">
                <div className="flex items-center justify-between mb-6 pb-4 border-b border-slate-900">
                  <div>
                    <h2 className="text-xl font-extrabold text-white flex items-center space-x-2">
                      <User className="w-5 h-5 text-emerald-400" />
                      <span>Contact & Identity Information</span>
                    </h2>
                    <p className="text-xs text-slate-400 mt-0.5">
                      Manage the core information used across your FarmConnectPrices experience.
                    </p>
                  </div>
                  <span className="text-xs text-emerald-400 font-medium hidden sm:inline">* Required fields</span>
                </div>

                <div className="space-y-6">
                  {/* DISPLAY NAME & PHONE */}
                  <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                    <div>
                      <label htmlFor="displayName" className="block text-xs font-semibold text-slate-300 mb-1.5 flex items-center">
                        <User className="w-3.5 h-3.5 text-slate-400 mr-1.5" />
                        Display Name
                      </label>
                      <input
                        id="displayName"
                        type="text"
                        value={displayName}
                        onChange={(e) => setDisplayName(e.target.value)}
                        placeholder="Enter full name"
                        className="w-full px-4 py-3 rounded-xl bg-slate-950/80 border border-slate-800 text-white placeholder-slate-500 text-sm focus:outline-none focus:border-emerald-500 focus:ring-1 focus:ring-emerald-500 transition-all min-h-[48px]"
                      />
                      <p className="text-[11px] text-slate-500 mt-1">Your public profile name.</p>
                    </div>

                    <div>
                      <label htmlFor="phoneNumber" className="block text-xs font-semibold text-slate-300 mb-1.5 flex items-center">
                        <Phone className="w-3.5 h-3.5 text-slate-400 mr-1.5" />
                        Phone Number <span className="text-emerald-400 ml-0.5">*</span>
                      </label>
                      <input
                        id="phoneNumber"
                        type="tel"
                        required
                        value={phoneNumber}
                        onChange={(e) => setPhoneNumber(e.target.value)}
                        placeholder="+91 9876543210"
                        className="w-full px-4 py-3 rounded-xl bg-slate-950/80 border border-slate-800 text-white placeholder-slate-500 text-sm focus:outline-none focus:border-emerald-500 focus:ring-1 focus:ring-emerald-500 transition-all min-h-[48px]"
                      />
                      <p className="text-[11px] text-slate-500 mt-1">Primary contact number.</p>
                    </div>
                  </div>

                  {/* ROLE SPECIFIC FIELDS */}
                  {profile?.role === 'MEDIATOR_BUYER' && (
                    <div>
                      <label htmlFor="businessOrg" className="block text-xs font-semibold text-slate-300 mb-1.5 flex items-center">
                        <Building2 className="w-3.5 h-3.5 text-slate-400 mr-1.5" />
                        Business / Organization Name <span className="text-emerald-400 ml-0.5">*</span>
                      </label>
                      <input
                        id="businessOrg"
                        type="text"
                        required
                        value={businessOrganizationName}
                        onChange={(e) => setBusinessOrganizationName(e.target.value)}
                        placeholder="e.g. Annapurna Agro Traders"
                        className="w-full px-4 py-3 rounded-xl bg-slate-950/80 border border-slate-800 text-white placeholder-slate-500 text-sm focus:outline-none focus:border-emerald-500 focus:ring-1 focus:ring-emerald-500 transition-all min-h-[48px]"
                      />
                      <p className="text-[11px] text-slate-500 mt-1">Registered trade or firm name for mediator buyers.</p>
                    </div>
                  )}

                  {profile?.role === 'CUSTOMER' && (
                    <div>
                      <label htmlFor="address" className="block text-xs font-semibold text-slate-300 mb-1.5 flex items-center">
                        <Home className="w-3.5 h-3.5 text-slate-400 mr-1.5" />
                        Delivery Address <span className="text-emerald-400 ml-0.5">*</span>
                      </label>
                      <textarea
                        id="address"
                        required
                        rows={3}
                        value={address}
                        onChange={(e) => setAddress(e.target.value)}
                        placeholder="House No, Street name, Landmark, City"
                        className="w-full px-4 py-3 rounded-xl bg-slate-950/80 border border-slate-800 text-white placeholder-slate-500 text-sm focus:outline-none focus:border-emerald-500 focus:ring-1 focus:ring-emerald-500 transition-all"
                      />
                      <p className="text-[11px] text-slate-500 mt-1">Primary delivery destination address.</p>
                    </div>
                  )}

                  {/* LOCATION METADATA */}
                  <div className="pt-4 border-t border-slate-900 space-y-4">
                    <h3 className="text-base font-bold text-white flex items-center space-x-2">
                      <MapPin className="w-4 h-4 text-emerald-400" />
                      <span>Primary Regional Location Metadata</span>
                    </h3>

                    <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                      <div>
                        <label htmlFor="state" className="block text-xs font-semibold text-slate-300 mb-1.5">
                          State <span className="text-emerald-400">*</span>
                        </label>
                        <input
                          id="state"
                          type="text"
                          required
                          value={stateName}
                          onChange={(e) => setStateName(e.target.value)}
                          placeholder="e.g. Telangana"
                          className="w-full px-4 py-3 rounded-xl bg-slate-950/80 border border-slate-800 text-white placeholder-slate-500 text-sm focus:outline-none focus:border-emerald-500 focus:ring-1 focus:ring-emerald-500 transition-all min-h-[48px]"
                        />
                      </div>

                      <div>
                        <label htmlFor="district" className="block text-xs font-semibold text-slate-300 mb-1.5">
                          District <span className="text-emerald-400">*</span>
                        </label>
                        <input
                          id="district"
                          type="text"
                          required
                          value={district}
                          onChange={(e) => setDistrict(e.target.value)}
                          placeholder="e.g. Warangal"
                          className="w-full px-4 py-3 rounded-xl bg-slate-950/80 border border-slate-800 text-white placeholder-slate-500 text-sm focus:outline-none focus:border-emerald-500 focus:ring-1 focus:ring-emerald-500 transition-all min-h-[48px]"
                        />
                      </div>

                      <div>
                        <label htmlFor="mandal" className="block text-xs font-semibold text-slate-300 mb-1.5">
                          Mandal / Tehsil <span className="text-emerald-400">*</span>
                        </label>
                        <input
                          id="mandal"
                          type="text"
                          required
                          value={mandal}
                          onChange={(e) => setMandal(e.target.value)}
                          placeholder="e.g. Enumamula"
                          className="w-full px-4 py-3 rounded-xl bg-slate-950/80 border border-slate-800 text-white placeholder-slate-500 text-sm focus:outline-none focus:border-emerald-500 focus:ring-1 focus:ring-emerald-500 transition-all min-h-[48px]"
                        />
                      </div>

                      <div>
                        <label htmlFor="village" className="block text-xs font-semibold text-slate-300 mb-1.5">
                          Village / City <span className="text-emerald-400">*</span>
                        </label>
                        <input
                          id="village"
                          type="text"
                          required
                          value={village}
                          onChange={(e) => setVillage(e.target.value)}
                          placeholder="e.g. Desrajupalle"
                          className="w-full px-4 py-3 rounded-xl bg-slate-950/80 border border-slate-800 text-white placeholder-slate-500 text-sm focus:outline-none focus:border-emerald-500 focus:ring-1 focus:ring-emerald-500 transition-all min-h-[48px]"
                        />
                      </div>
                    </div>
                  </div>

                </div>
              </GlassCard>
            </motion.div>

            {/* RIGHT COLUMN: PROFILE COMPLETION & SUBTLE 3D CARD */}
            <motion.div
              initial={{ opacity: 0, x: 10 }}
              animate={{ opacity: 1, x: 0 }}
              transition={{ duration: 0.3, delay: 0.3 }}
              className="space-y-6"
            >
              {/* PROFILE COMPLETION CARD */}
              <GlassCard className="p-6 border-slate-800/90 shadow-2xl relative overflow-hidden">
                <h3 className="text-lg font-bold text-white mb-1 flex items-center justify-between">
                  <span>Profile Completion</span>
                  <span className="text-xs font-mono text-emerald-400 bg-emerald-500/10 px-2.5 py-0.5 rounded-full border border-emerald-500/30">
                    {completionPercentage}%
                  </span>
                </h3>
                <p className="text-xs text-slate-400 mb-4">
                  Complete your profile to unlock full platform capabilities.
                </p>

                {/* Progress Bar */}
                <div className="w-full bg-slate-950 h-2.5 rounded-full overflow-hidden border border-slate-900 mb-6">
                  <div
                    className="bg-gradient-to-r from-emerald-500 to-teal-400 h-full transition-all duration-500 rounded-full"
                    style={{ width: `${completionPercentage}%` }}
                  />
                </div>

                {/* Checklist */}
                <div className="space-y-3 text-xs">
                  <div className="flex items-center justify-between p-2.5 rounded-xl bg-slate-950/80 border border-slate-900">
                    <span className="text-slate-300 flex items-center">
                      <Phone className="w-3.5 h-3.5 text-slate-400 mr-2" />
                      Contact Phone
                    </span>
                    {hasPhone ? (
                      <span className="text-emerald-400 font-bold flex items-center">
                        <CheckCircle2 className="w-3.5 h-3.5 mr-1" /> Done
                      </span>
                    ) : (
                      <span className="text-amber-400 font-semibold">Required</span>
                    )}
                  </div>

                  <div className="flex items-center justify-between p-2.5 rounded-xl bg-slate-950/80 border border-slate-900">
                    <span className="text-slate-300 flex items-center">
                      <MapPin className="w-3.5 h-3.5 text-slate-400 mr-2" />
                      Regional Location
                    </span>
                    {hasLocation ? (
                      <span className="text-emerald-400 font-bold flex items-center">
                        <CheckCircle2 className="w-3.5 h-3.5 mr-1" /> Done
                      </span>
                    ) : (
                      <span className="text-amber-400 font-semibold">Required</span>
                    )}
                  </div>

                  {profile?.role === 'MEDIATOR_BUYER' && (
                    <div className="flex items-center justify-between p-2.5 rounded-xl bg-slate-950/80 border border-slate-900">
                      <span className="text-slate-300 flex items-center">
                        <Building2 className="w-3.5 h-3.5 text-slate-400 mr-2" />
                        Business Name
                      </span>
                      {hasRoleField ? (
                        <span className="text-emerald-400 font-bold flex items-center">
                          <CheckCircle2 className="w-3.5 h-3.5 mr-1" /> Done
                        </span>
                      ) : (
                        <span className="text-amber-400 font-semibold">Required</span>
                      )}
                    </div>
                  )}

                  {profile?.role === 'CUSTOMER' && (
                    <div className="flex items-center justify-between p-2.5 rounded-xl bg-slate-950/80 border border-slate-900">
                      <span className="text-slate-300 flex items-center">
                        <Home className="w-3.5 h-3.5 text-slate-400 mr-2" />
                        Delivery Address
                      </span>
                      {hasRoleField ? (
                        <span className="text-emerald-400 font-bold flex items-center">
                          <CheckCircle2 className="w-3.5 h-3.5 mr-1" /> Done
                        </span>
                      ) : (
                        <span className="text-amber-400 font-semibold">Required</span>
                      )}
                    </div>
                  )}
                </div>
              </GlassCard>

              {/* AMBIENT VISUAL STATUS BADGE (LIGHTWEIGHT CSS-ONLY / ZERO WEBGL) */}
              <GlassCard className="p-5 border-emerald-500/20 shadow-xl relative overflow-hidden hidden md:block">
                <div className="absolute -top-12 -right-12 w-32 h-32 bg-emerald-500/10 rounded-full blur-2xl pointer-events-none" />
                <div className="flex items-center space-x-3 mb-2">
                  <div className="w-8 h-8 rounded-lg bg-emerald-500/10 border border-emerald-500/30 flex items-center justify-center shrink-0">
                    <ShieldCheck className="w-4 h-4 text-emerald-400" />
                  </div>
                  <div>
                    <h4 className="text-xs font-bold text-white">Direct Verification Gateway</h4>
                    <p className="text-[11px] text-slate-400">Authenticated & Role Verified</p>
                  </div>
                </div>
                <p className="text-xs text-slate-400 leading-relaxed">
                  Profile metadata updates are validated and securely saved to your primary identity record.
                </p>
              </GlassCard>

              {/* ACTION BUTTONS (DESKTOP & MOBILE STICKY FOOTER) */}
              <GlassCard className="p-5 border-slate-800 space-y-3">
                <button
                  type="submit"
                  disabled={isUpdatingProfile}
                  className="w-full inline-flex items-center justify-center space-x-2 px-6 py-3.5 rounded-xl bg-emerald-500 hover:bg-emerald-400 text-slate-950 font-bold text-sm shadow-lg shadow-emerald-500/20 transition-all min-h-[48px] disabled:opacity-50 active:scale-95"
                >
                  <Save className="w-4 h-4" />
                  <span>{isUpdatingProfile ? 'Saving Changes...' : 'Save Profile Changes'}</span>
                </button>

                <Link
                  to="/profile"
                  className="w-full inline-flex items-center justify-center px-6 py-3 rounded-xl bg-slate-900 hover:bg-slate-800 text-slate-300 text-xs font-semibold border border-slate-800 transition-colors min-h-[44px]"
                >
                  Cancel
                </Link>
              </GlassCard>

            </motion.div>

          </div>
        </form>

      </main>

      <Footer />
    </div>
  );
};
