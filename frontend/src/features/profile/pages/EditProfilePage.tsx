import React, { useState, useEffect } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { useProfile } from '../hooks/useProfile';
import { ROLE_DISPLAY_NAMES } from '../types';

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
      <div className="min-h-screen bg-gray-50 dark:bg-gray-900 flex items-center justify-center p-6">
        <div className="flex items-center space-x-3 text-emerald-600 dark:text-emerald-400">
          <div className="w-6 h-6 border-2 border-current border-t-transparent rounded-full animate-spin"></div>
          <span className="text-sm font-medium">Loading profile for editing...</span>
        </div>
      </div>
    );
  }

  const handleSave = async (e: React.FormEvent) => {
    e.preventDefault();
    setFeedback(null);

    try {
      await updateProfile({
        displayName,
        phoneNumber,
        location: {
          state: stateName,
          district,
          mandal,
          village,
        },
        businessOrganizationName: profile?.role === 'MEDIATOR_BUYER' ? businessOrganizationName : undefined,
        address: profile?.role === 'CUSTOMER' ? address : undefined,
      });

      setFeedback({ type: 'success', message: 'Profile updated successfully!' });
      setTimeout(() => navigate('/profile'), 1200);
    } catch (err: unknown) {
      console.error('Failed to update profile:', err);
      setFeedback({ type: 'error', message: 'Failed to update profile. Please try again.' });
    }
  };

  const roleDisplay = profile?.roleDisplayName || (profile?.role ? ROLE_DISPLAY_NAMES[profile.role] || profile.role : 'User');

  return (
    <div className="min-h-screen bg-gray-50 dark:bg-gray-900 text-gray-900 dark:text-white py-10 px-4 sm:px-6 lg:px-8 transition-colors">
      <div className="max-w-3xl mx-auto space-y-6">
        {/* Navigation Breadcrumb & Title */}
        <div className="flex items-center justify-between">
          <div>
            <Link to="/profile" className="text-xs font-semibold text-emerald-600 dark:text-emerald-400 hover:underline">
              ← Back to Profile
            </Link>
            <h1 className="text-2xl sm:text-3xl font-extrabold text-gray-900 dark:text-white mt-1">
              Edit Foundational Profile
            </h1>
          </div>
        </div>

        {feedback && (
          <div
            role="alert"
            className={`p-4 rounded-xl text-sm font-medium transition-all duration-200 ${
              feedback.type === 'success'
                ? 'bg-emerald-50 dark:bg-emerald-950/60 border-l-4 border-emerald-500 text-emerald-800 dark:text-emerald-200'
                : 'bg-red-50 dark:bg-red-950/60 border-l-4 border-red-500 text-red-800 dark:text-red-200'
            }`}
          >
            {feedback.message}
          </div>
        )}

        <form onSubmit={handleSave} className="bg-white dark:bg-gray-800 rounded-2xl shadow-xl border border-gray-200 dark:border-gray-700 p-6 sm:p-8 space-y-6">
          {/* Read Only System Metadata */}
          <div className="bg-gray-50 dark:bg-gray-750 p-4 rounded-xl space-y-3 border border-gray-200/60 dark:border-gray-700/60">
            <h3 className="text-xs font-bold uppercase tracking-wider text-gray-500 dark:text-gray-400">
              System Accounts Identifiers (Read-Only)
            </h3>
            <div className="grid grid-cols-1 sm:grid-cols-3 gap-3 text-xs">
              <div>
                <span className="text-gray-400 block">UID</span>
                <code className="font-mono text-gray-800 dark:text-gray-200">{profile?.uid}</code>
              </div>
              <div>
                <span className="text-gray-400 block">Email</span>
                <span className="font-medium text-gray-800 dark:text-gray-200">{profile?.email}</span>
              </div>
              <div>
                <span className="text-gray-400 block">Role</span>
                <span className="font-semibold text-emerald-600 dark:text-emerald-400">{roleDisplay}</span>
              </div>
            </div>
          </div>

          {/* Basic Contact Info */}
          <div className="space-y-4">
            <h2 className="text-lg font-bold text-gray-900 dark:text-white border-b pb-2 border-gray-100 dark:border-gray-700">
              Contact & Identity Information
            </h2>

            <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
              <div>
                <label htmlFor="displayName" className="block text-xs font-semibold text-gray-700 dark:text-gray-300 mb-1">
                  Display Name
                </label>
                <input
                  id="displayName"
                  type="text"
                  value={displayName}
                  onChange={(e) => setDisplayName(e.target.value)}
                  placeholder="Enter full name"
                  className="w-full px-4 py-2.5 rounded-xl border border-gray-300 dark:border-gray-600 bg-white dark:bg-gray-900 text-sm focus:ring-2 focus:ring-emerald-500 focus:border-transparent outline-none transition-all"
                />
              </div>

              <div>
                <label htmlFor="phoneNumber" className="block text-xs font-semibold text-gray-700 dark:text-gray-300 mb-1">
                  Phone Number <span className="text-red-500">*</span>
                </label>
                <input
                  id="phoneNumber"
                  type="tel"
                  required
                  value={phoneNumber}
                  onChange={(e) => setPhoneNumber(e.target.value)}
                  placeholder="+91 9876543210"
                  className="w-full px-4 py-2.5 rounded-xl border border-gray-300 dark:border-gray-600 bg-white dark:bg-gray-900 text-sm focus:ring-2 focus:ring-emerald-500 focus:border-transparent outline-none transition-all"
                />
              </div>
            </div>

            {/* Role Specific Foundational Field */}
            {profile?.role === 'MEDIATOR_BUYER' && (
              <div>
                <label htmlFor="businessOrg" className="block text-xs font-semibold text-gray-700 dark:text-gray-300 mb-1">
                  Business / Organization Name <span className="text-red-500">*</span>
                </label>
                <input
                  id="businessOrg"
                  type="text"
                  required
                  value={businessOrganizationName}
                  onChange={(e) => setBusinessOrganizationName(e.target.value)}
                  placeholder="e.g. Annapurna Agro Traders"
                  className="w-full px-4 py-2.5 rounded-xl border border-gray-300 dark:border-gray-600 bg-white dark:bg-gray-900 text-sm focus:ring-2 focus:ring-emerald-500 focus:border-transparent outline-none transition-all"
                />
              </div>
            )}

            {profile?.role === 'CUSTOMER' && (
              <div>
                <label htmlFor="address" className="block text-xs font-semibold text-gray-700 dark:text-gray-300 mb-1">
                  Delivery Address <span className="text-red-500">*</span>
                </label>
                <textarea
                  id="address"
                  required
                  rows={2}
                  value={address}
                  onChange={(e) => setAddress(e.target.value)}
                  placeholder="House No, Street name, Landmark"
                  className="w-full px-4 py-2.5 rounded-xl border border-gray-300 dark:border-gray-600 bg-white dark:bg-gray-900 text-sm focus:ring-2 focus:ring-emerald-500 focus:border-transparent outline-none transition-all"
                />
              </div>
            )}
          </div>

          {/* Location Information */}
          <div className="space-y-4">
            <h2 className="text-lg font-bold text-gray-900 dark:text-white border-b pb-2 border-gray-100 dark:border-gray-700">
              Primary Location Metadata
            </h2>

            <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
              <div>
                <label htmlFor="state" className="block text-xs font-semibold text-gray-700 dark:text-gray-300 mb-1">
                  State <span className="text-red-500">*</span>
                </label>
                <input
                  id="state"
                  type="text"
                  required
                  value={stateName}
                  onChange={(e) => setStateName(e.target.value)}
                  placeholder="State"
                  className="w-full px-4 py-2.5 rounded-xl border border-gray-300 dark:border-gray-600 bg-white dark:bg-gray-900 text-sm focus:ring-2 focus:ring-emerald-500 focus:border-transparent outline-none transition-all"
                />
              </div>

              <div>
                <label htmlFor="district" className="block text-xs font-semibold text-gray-700 dark:text-gray-300 mb-1">
                  District <span className="text-red-500">*</span>
                </label>
                <input
                  id="district"
                  type="text"
                  required
                  value={district}
                  onChange={(e) => setDistrict(e.target.value)}
                  placeholder="District"
                  className="w-full px-4 py-2.5 rounded-xl border border-gray-300 dark:border-gray-600 bg-white dark:bg-gray-900 text-sm focus:ring-2 focus:ring-emerald-500 focus:border-transparent outline-none transition-all"
                />
              </div>

              <div>
                <label htmlFor="mandal" className="block text-xs font-semibold text-gray-700 dark:text-gray-300 mb-1">
                  Mandal / Tehsil <span className="text-red-500">*</span>
                </label>
                <input
                  id="mandal"
                  type="text"
                  required
                  value={mandal}
                  onChange={(e) => setMandal(e.target.value)}
                  placeholder="Mandal"
                  className="w-full px-4 py-2.5 rounded-xl border border-gray-300 dark:border-gray-600 bg-white dark:bg-gray-900 text-sm focus:ring-2 focus:ring-emerald-500 focus:border-transparent outline-none transition-all"
                />
              </div>

              <div>
                <label htmlFor="village" className="block text-xs font-semibold text-gray-700 dark:text-gray-300 mb-1">
                  Village / City <span className="text-red-500">*</span>
                </label>
                <input
                  id="village"
                  type="text"
                  required
                  value={village}
                  onChange={(e) => setVillage(e.target.value)}
                  placeholder="Village or City"
                  className="w-full px-4 py-2.5 rounded-xl border border-gray-300 dark:border-gray-600 bg-white dark:bg-gray-900 text-sm focus:ring-2 focus:ring-emerald-500 focus:border-transparent outline-none transition-all"
                />
              </div>
            </div>
          </div>

          {/* Form Actions */}
          <div className="flex items-center justify-end space-x-4 pt-4 border-t border-gray-200 dark:border-gray-700">
            <Link
              to="/profile"
              className="px-5 py-2.5 rounded-xl border border-gray-300 dark:border-gray-600 text-sm font-medium text-gray-700 dark:text-gray-300 hover:bg-gray-100 dark:hover:bg-gray-700 transition-colors"
            >
              Cancel
            </Link>
            <button
              type="submit"
              disabled={isUpdatingProfile}
              className="px-6 py-2.5 bg-emerald-600 hover:bg-emerald-700 text-white text-sm font-bold rounded-xl shadow-md disabled:opacity-50 transition-all flex items-center space-x-2"
            >
              {isUpdatingProfile ? (
                <>
                  <div className="w-4 h-4 border-2 border-white border-t-transparent rounded-full animate-spin"></div>
                  <span>Saving Profile...</span>
                </>
              ) : (
                <span>Save Profile Details</span>
              )}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
};
