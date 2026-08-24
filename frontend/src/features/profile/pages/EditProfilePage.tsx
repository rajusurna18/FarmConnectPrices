import React, { useState, useEffect } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { useProfile } from '../hooks/useProfile';

export const EditProfilePage: React.FC = () => {
  const { profile, isLoading, updateProfile, isUpdating } = useProfile();
  const navigate = useNavigate();

  const [displayName, setDisplayName] = useState('');
  const [phoneNumber, setPhoneNumber] = useState('');
  const [state, setState] = useState('');
  const [district, setDistrict] = useState('');
  const [mandal, setMandal] = useState('');
  const [village, setVillage] = useState('');

  const [formError, setFormError] = useState<string | null>(null);
  const [successMsg, setSuccessMsg] = useState<string | null>(null);

  useEffect(() => {
    if (profile) {
      setDisplayName(profile.displayName || '');
      setPhoneNumber(profile.phoneNumber || '');
      setState(profile.location?.state || '');
      setDistrict(profile.location?.district || '');
      setMandal(profile.location?.mandal || '');
      setVillage(profile.location?.village || '');
    }
  }, [profile]);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setFormError(null);
    setSuccessMsg(null);

    if (!displayName.trim()) {
      setFormError('Display Name is required.');
      return;
    }

    try {
      await updateProfile({
        displayName: displayName.trim(),
        phoneNumber: phoneNumber.trim() || undefined,
        location: {
          state: state.trim() || undefined,
          district: district.trim() || undefined,
          mandal: mandal.trim() || undefined,
          village: village.trim() || undefined,
        },
      });

      setSuccessMsg('Profile updated successfully!');
      setTimeout(() => {
        navigate('/profile');
      }, 1000);
    } catch (err: unknown) {
      console.error('Update profile error:', err);
      setFormError('Failed to update profile. Please try again.');
    }
  };

  if (isLoading) {
    return (
      <div className="min-h-screen flex items-center justify-center bg-gray-50 dark:bg-gray-900">
        <div className="flex flex-col items-center space-y-4">
          <div className="w-12 h-12 border-4 border-emerald-500 border-t-transparent rounded-full animate-spin"></div>
          <p className="text-gray-600 dark:text-gray-300 font-medium text-sm">Loading profile editor...</p>
        </div>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-gray-50 dark:bg-gray-900 text-gray-900 dark:text-white transition-colors py-8 px-4 sm:px-6 lg:px-8">
      <main className="max-w-3xl mx-auto bg-white dark:bg-gray-800 rounded-2xl shadow-xl border border-gray-200 dark:border-gray-700 p-6 sm:p-10">
        {/* Header */}
        <div className="flex items-center justify-between border-b border-gray-200 dark:border-gray-700 pb-4 mb-6">
          <div>
            <h1 className="text-2xl sm:text-3xl font-extrabold text-gray-900 dark:text-white">Edit Profile</h1>
            <p className="text-sm text-gray-500 dark:text-gray-400 mt-1">
              Update your basic details and location metadata.
            </p>
          </div>
          <Link
            to="/profile"
            className="text-sm text-emerald-600 dark:text-emerald-400 hover:underline font-medium"
          >
            ← Back to Profile
          </Link>
        </div>

        {/* Banners */}
        {formError && (
          <div role="alert" className="mb-6 p-4 bg-red-50 dark:bg-red-900/30 border-l-4 border-red-500 text-red-700 dark:text-red-300 text-sm rounded-r-lg">
            {formError}
          </div>
        )}
        {successMsg && (
          <div role="status" className="mb-6 p-4 bg-green-50 dark:bg-green-900/30 border-l-4 border-green-500 text-green-700 dark:text-green-300 text-sm rounded-r-lg">
            {successMsg}
          </div>
        )}

        <form onSubmit={handleSubmit} className="space-y-6">
          {/* Read-Only Account Information */}
          <div className="bg-gray-50 dark:bg-gray-900/50 p-4 rounded-xl border border-gray-200 dark:border-gray-700 space-y-4">
            <h2 className="text-xs font-bold uppercase tracking-wider text-gray-500 dark:text-gray-400">
              System Protected Fields (Read-Only)
            </h2>
            <div className="grid grid-cols-1 sm:grid-cols-2 gap-4 text-xs">
              <div>
                <span className="text-gray-400 block">UID</span>
                <span className="font-mono text-gray-600 dark:text-gray-300 break-all">{profile?.uid}</span>
              </div>
              <div>
                <span className="text-gray-400 block">Email</span>
                <span className="font-semibold text-gray-700 dark:text-gray-300">{profile?.email}</span>
              </div>
              <div>
                <span className="text-gray-400 block">Role</span>
                <span className="font-semibold text-emerald-600 dark:text-emerald-400">{profile?.role}</span>
              </div>
              <div>
                <span className="text-gray-400 block">Status</span>
                <span className="font-semibold text-blue-600 dark:text-blue-400">{profile?.status}</span>
              </div>
            </div>
          </div>

          {/* Basic Info Inputs */}
          <div className="space-y-4">
            <h2 className="text-base font-bold text-gray-900 dark:text-white border-b border-gray-200 dark:border-gray-700 pb-2">
              Basic Information
            </h2>

            <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
              <div>
                <label htmlFor="displayName" className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">
                  Display Name <span className="text-red-500">*</span>
                </label>
                <input
                  type="text"
                  id="displayName"
                  value={displayName}
                  onChange={(e) => setDisplayName(e.target.value)}
                  placeholder="Enter full name"
                  className="w-full min-h-[44px] px-4 py-2 bg-white dark:bg-gray-900 border border-gray-300 dark:border-gray-700 rounded-xl focus:ring-2 focus:ring-emerald-500 focus:outline-none text-sm"
                  required
                />
              </div>

              <div>
                <label htmlFor="phoneNumber" className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">
                  Phone Number
                </label>
                <input
                  type="tel"
                  id="phoneNumber"
                  value={phoneNumber}
                  onChange={(e) => setPhoneNumber(e.target.value)}
                  placeholder="+91 98765 43210"
                  className="w-full min-h-[44px] px-4 py-2 bg-white dark:bg-gray-900 border border-gray-300 dark:border-gray-700 rounded-xl focus:ring-2 focus:ring-emerald-500 focus:outline-none text-sm"
                />
              </div>
            </div>
          </div>

          {/* Location Inputs */}
          <div className="space-y-4">
            <h2 className="text-base font-bold text-gray-900 dark:text-white border-b border-gray-200 dark:border-gray-700 pb-2">
              Location Foundation (State, District, Mandal, Village)
            </h2>

            <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
              <div>
                <label htmlFor="state" className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">
                  State
                </label>
                <input
                  type="text"
                  id="state"
                  value={state}
                  onChange={(e) => setState(e.target.value)}
                  placeholder="e.g. Telangana"
                  className="w-full min-h-[44px] px-4 py-2 bg-white dark:bg-gray-900 border border-gray-300 dark:border-gray-700 rounded-xl focus:ring-2 focus:ring-emerald-500 focus:outline-none text-sm"
                />
              </div>

              <div>
                <label htmlFor="district" className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">
                  District
                </label>
                <input
                  type="text"
                  id="district"
                  value={district}
                  onChange={(e) => setDistrict(e.target.value)}
                  placeholder="e.g. Ranga Reddy"
                  className="w-full min-h-[44px] px-4 py-2 bg-white dark:bg-gray-900 border border-gray-300 dark:border-gray-700 rounded-xl focus:ring-2 focus:ring-emerald-500 focus:outline-none text-sm"
                />
              </div>

              <div>
                <label htmlFor="mandal" className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">
                  Mandal
                </label>
                <input
                  type="text"
                  id="mandal"
                  value={mandal}
                  onChange={(e) => setMandal(e.target.value)}
                  placeholder="e.g. Rajendranagar"
                  className="w-full min-h-[44px] px-4 py-2 bg-white dark:bg-gray-900 border border-gray-300 dark:border-gray-700 rounded-xl focus:ring-2 focus:ring-emerald-500 focus:outline-none text-sm"
                />
              </div>

              <div>
                <label htmlFor="village" className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">
                  Village
                </label>
                <input
                  type="text"
                  id="village"
                  value={village}
                  onChange={(e) => setVillage(e.target.value)}
                  placeholder="e.g. Budvel"
                  className="w-full min-h-[44px] px-4 py-2 bg-white dark:bg-gray-900 border border-gray-300 dark:border-gray-700 rounded-xl focus:ring-2 focus:ring-emerald-500 focus:outline-none text-sm"
                />
              </div>
            </div>
          </div>

          {/* Form Action Buttons */}
          <div className="flex items-center justify-end space-x-4 pt-6 border-t border-gray-200 dark:border-gray-700">
            <Link
              to="/profile"
              className="px-6 py-2.5 min-h-[44px] flex items-center justify-center bg-gray-100 dark:bg-gray-700 hover:bg-gray-200 dark:hover:bg-gray-600 text-gray-800 dark:text-gray-200 rounded-xl text-sm font-medium transition-colors"
            >
              Cancel
            </Link>
            <button
              type="submit"
              disabled={isUpdating}
              className="px-8 py-2.5 min-h-[44px] bg-emerald-600 hover:bg-emerald-700 text-white font-medium rounded-xl shadow disabled:opacity-50 transition-all flex items-center justify-center space-x-2 text-sm"
            >
              {isUpdating ? (
                <>
                  <div className="w-4 h-4 border-2 border-white border-t-transparent rounded-full animate-spin"></div>
                  <span>Saving...</span>
                </>
              ) : (
                <span>Save Profile Changes</span>
              )}
            </button>
          </div>
        </form>
      </main>
    </div>
  );
};
