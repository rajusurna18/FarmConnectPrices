import React from 'react';
import { Link } from 'react-router-dom';
import { useProfile } from '../hooks/useProfile';
import { ROLE_DISPLAY_NAMES } from '../types';

export const ProfilePage: React.FC = () => {
  const { profile, isLoadingProfile, profileError } = useProfile();

  if (isLoadingProfile) {
    return (
      <div className="min-h-screen bg-gray-50 dark:bg-gray-900 flex items-center justify-center p-6">
        <div className="flex items-center space-x-3 text-emerald-600 dark:text-emerald-400">
          <div className="w-6 h-6 border-2 border-current border-t-transparent rounded-full animate-spin"></div>
          <span className="text-sm font-medium">Loading profile details...</span>
        </div>
      </div>
    );
  }

  if (profileError || !profile) {
    return (
      <div className="min-h-screen bg-gray-50 dark:bg-gray-900 flex items-center justify-center p-6">
        <div className="max-w-md w-full bg-white dark:bg-gray-800 rounded-2xl p-6 shadow-xl border border-red-200 text-center">
          <h2 className="text-xl font-bold text-red-600 dark:text-red-400 mb-2">Failed to load profile</h2>
          <p className="text-sm text-gray-600 dark:text-gray-300 mb-4">
            Could not fetch profile information. Please verify your authentication.
          </p>
          <Link
            to="/login"
            className="inline-block px-5 py-2.5 bg-emerald-600 text-white rounded-xl text-sm font-medium"
          >
            Go to Login
          </Link>
        </div>
      </div>
    );
  }

  const isRoleAssigned = profile.role && profile.role !== 'USER';
  const roleDisplay = profile.roleDisplayName || ROLE_DISPLAY_NAMES[profile.role] || profile.role;

  return (
    <div className="min-h-screen bg-gray-50 dark:bg-gray-900 text-gray-900 dark:text-white py-10 px-4 sm:px-6 lg:px-8 transition-colors">
      <div className="max-w-4xl mx-auto space-y-6">
        {/* Header Bar */}
        <div className="flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4 bg-white dark:bg-gray-800 p-6 rounded-2xl shadow-md border border-gray-200 dark:border-gray-700">
          <div>
            <span className="text-xs font-bold uppercase tracking-wider text-emerald-600 dark:text-emerald-400">
              User Profile
            </span>
            <h1 className="text-2xl sm:text-3xl font-extrabold text-gray-900 dark:text-white mt-1">
              {profile.displayName || 'User Profile'}
            </h1>
            <p className="text-xs sm:text-sm text-gray-500 dark:text-gray-400">
              UID: <code className="bg-gray-100 dark:bg-gray-700 px-2 py-0.5 rounded text-gray-800 dark:text-gray-200 font-mono">{profile.uid}</code>
            </p>
          </div>

          <div className="flex items-center space-x-3">
            <Link
              to="/profile/edit"
              className="min-h-[40px] px-4 py-2 bg-emerald-600 hover:bg-emerald-700 text-white rounded-xl text-sm font-medium shadow transition-colors flex items-center space-x-2"
            >
              <span>Edit Profile</span>
            </Link>
            <Link
              to="/dashboard"
              className="min-h-[40px] px-4 py-2 bg-gray-200 dark:bg-gray-700 hover:bg-gray-300 dark:hover:bg-gray-600 text-gray-800 dark:text-gray-200 rounded-xl text-sm font-medium transition-colors"
            >
              Dashboard
            </Link>
          </div>
        </div>

        {/* Role Warning Banner if role is unassigned */}
        {!isRoleAssigned && (
          <div className="p-4 bg-amber-50 dark:bg-amber-900/30 border-l-4 border-amber-500 rounded-r-2xl flex flex-col sm:flex-row sm:items-center justify-between gap-4">
            <div>
              <h3 className="text-sm font-bold text-amber-800 dark:text-amber-300">
                Action Required: Role Selection Pending
              </h3>
              <p className="text-xs text-amber-700 dark:text-amber-400 mt-0.5">
                You have not selected an application role yet. Please choose your profile role to unlock features.
              </p>
            </div>
            <Link
              to="/onboarding/role"
              className="px-4 py-2 bg-amber-600 hover:bg-amber-700 text-white text-xs font-semibold rounded-xl whitespace-nowrap shadow transition-colors"
            >
              Select Role Now →
            </Link>
          </div>
        )}

        {/* Profile Card Main Grid */}
        <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
          {/* Identity & Contact Card */}
          <div className="bg-white dark:bg-gray-800 p-6 rounded-2xl shadow-md border border-gray-200 dark:border-gray-700 space-y-4">
            <h2 className="text-lg font-bold text-gray-900 dark:text-white border-b pb-2 border-gray-100 dark:border-gray-700">
              Account Identity
            </h2>

            <div className="space-y-3 text-sm">
              <div>
                <span className="text-xs text-gray-500 dark:text-gray-400 block">Display Name</span>
                <span className="font-semibold text-gray-800 dark:text-gray-200">{profile.displayName || 'N/A'}</span>
              </div>

              <div>
                <span className="text-xs text-gray-500 dark:text-gray-400 block">Email Address</span>
                <div className="flex items-center space-x-2">
                  <span className="font-semibold text-gray-800 dark:text-gray-200">{profile.email}</span>
                  {profile.emailVerified ? (
                    <span className="px-2 py-0.5 text-[10px] font-bold bg-green-100 text-green-700 dark:bg-green-950 dark:text-green-300 rounded-full">
                      ✓ Verified
                    </span>
                  ) : (
                    <span className="px-2 py-0.5 text-[10px] font-bold bg-amber-100 text-amber-700 dark:bg-amber-950 dark:text-amber-300 rounded-full">
                      Unverified
                    </span>
                  )}
                </div>
              </div>

              <div>
                <span className="text-xs text-gray-500 dark:text-gray-400 block">Application Role</span>
                <span className="inline-block px-3 py-1 mt-1 text-xs font-bold bg-emerald-100 text-emerald-800 dark:bg-emerald-950 dark:text-emerald-300 rounded-lg">
                  {roleDisplay}
                </span>
              </div>

              <div>
                <span className="text-xs text-gray-500 dark:text-gray-400 block">Phone Number</span>
                <span className="font-semibold text-gray-800 dark:text-gray-200">
                  {profile.phoneNumber || 'Not provided'}
                </span>
              </div>
            </div>
          </div>

          {/* Location & Foundational Metadata Card */}
          <div className="bg-white dark:bg-gray-800 p-6 rounded-2xl shadow-md border border-gray-200 dark:border-gray-700 space-y-4">
            <h2 className="text-lg font-bold text-gray-900 dark:text-white border-b pb-2 border-gray-100 dark:border-gray-700">
              Foundational Profile Details
            </h2>

            <div className="space-y-3 text-sm">
              <div>
                <span className="text-xs text-gray-500 dark:text-gray-400 block">Profile Completion Status</span>
                <div className="mt-1">
                  {profile.profileCompleted ? (
                    <span className="inline-flex items-center px-3 py-1 rounded-full text-xs font-bold bg-emerald-100 text-emerald-700 dark:bg-emerald-950 dark:text-emerald-300">
                      ✓ Profile Complete
                    </span>
                  ) : (
                    <span className="inline-flex items-center px-3 py-1 rounded-full text-xs font-bold bg-amber-100 text-amber-700 dark:bg-amber-950 dark:text-amber-300">
                      Incomplete • Details Needed
                    </span>
                  )}
                </div>
              </div>

              {/* Role Specific Fields */}
              {profile.role === 'MEDIATOR_BUYER' && (
                <div>
                  <span className="text-xs text-gray-500 dark:text-gray-400 block">Business / Organization Name</span>
                  <span className="font-semibold text-gray-800 dark:text-gray-200">
                    {profile.businessOrganizationName || 'Not provided'}
                  </span>
                </div>
              )}

              {profile.role === 'CUSTOMER' && (
                <div>
                  <span className="text-xs text-gray-500 dark:text-gray-400 block">Delivery Address</span>
                  <span className="font-semibold text-gray-800 dark:text-gray-200">
                    {profile.address || 'Not provided'}
                  </span>
                </div>
              )}

              {/* Location Fields */}
              <div className="pt-2 border-t border-gray-100 dark:border-gray-700">
                <span className="text-xs text-gray-500 dark:text-gray-400 block mb-1">Primary Location</span>
                <div className="grid grid-cols-2 gap-2 text-xs">
                  <div className="bg-gray-50 dark:bg-gray-750 p-2 rounded-lg">
                    <span className="text-gray-400 block">State</span>
                    <span className="font-medium">{profile.location?.state || '—'}</span>
                  </div>
                  <div className="bg-gray-50 dark:bg-gray-750 p-2 rounded-lg">
                    <span className="text-gray-400 block">District</span>
                    <span className="font-medium">{profile.location?.district || '—'}</span>
                  </div>
                  <div className="bg-gray-50 dark:bg-gray-750 p-2 rounded-lg">
                    <span className="text-gray-400 block">Mandal</span>
                    <span className="font-medium">{profile.location?.mandal || '—'}</span>
                  </div>
                  <div className="bg-gray-50 dark:bg-gray-750 p-2 rounded-lg">
                    <span className="text-gray-400 block">Village</span>
                    <span className="font-medium">{profile.location?.village || '—'}</span>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};
