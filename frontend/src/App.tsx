import React, { lazy, Suspense } from 'react';
import { BrowserRouter, Routes, Route } from 'react-router-dom';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { AuthProvider } from './features/auth/context/AuthProvider';
import { ProtectedRoute } from './components/ProtectedRoute';
import { FarmerRoute } from './components/FarmerRoute';
import { HomePage } from './pages/HomePage';
import { LoginPage } from './features/auth/pages/LoginPage';
import { RegisterPage } from './features/auth/pages/RegisterPage';
import { ForgotPasswordPage } from './features/auth/pages/ForgotPasswordPage';
import { VerifyEmailPage } from './features/auth/pages/VerifyEmailPage';

// Lazy-loaded protected & feature routes
const DashboardPage = lazy(() => import('./pages/DashboardPage').then((m) => ({ default: m.DashboardPage })));
const ProfilePage = lazy(() => import('./features/profile/pages/ProfilePage').then((m) => ({ default: m.ProfilePage })));
const EditProfilePage = lazy(() => import('./features/profile/pages/EditProfilePage').then((m) => ({ default: m.EditProfilePage })));
const RoleSelectionPage = lazy(() => import('./features/profile/pages/RoleSelectionPage').then((m) => ({ default: m.RoleSelectionPage })));
const FarmListPage = lazy(() => import('./features/farms/pages/FarmListPage').then((m) => ({ default: m.FarmListPage })));
const CreateFarmPage = lazy(() => import('./features/farms/pages/CreateFarmPage').then((m) => ({ default: m.CreateFarmPage })));
const FarmDetailPage = lazy(() => import('./features/farms/pages/FarmDetailPage').then((m) => ({ default: m.FarmDetailPage })));
const EditFarmPage = lazy(() => import('./features/farms/pages/EditFarmPage').then((m) => ({ default: m.EditFarmPage })));
const FarmCropsPage = lazy(() => import('./features/farms/pages/FarmCropsPage').then((m) => ({ default: m.FarmCropsPage })));
const MarketListPage = lazy(() => import('./features/markets/pages/MarketListPage').then((m) => ({ default: m.MarketListPage })));
const MarketDetailPage = lazy(() => import('./features/markets/pages/MarketDetailPage').then((m) => ({ default: m.MarketDetailPage })));

const PageFallback: React.FC = () => (
  <div className="min-h-screen flex items-center justify-center bg-slate-950 text-slate-100 select-none">
    <div className="flex flex-col items-center space-y-4">
      <div className="w-10 h-10 border-3 border-emerald-500 border-t-transparent rounded-full animate-spin" />
      <p className="text-slate-400 font-medium text-xs sm:text-sm font-sans tracking-wide">Loading portal view...</p>
    </div>
  </div>
);

const queryClient = new QueryClient();

export const App: React.FC = () => {
  return (
    <QueryClientProvider client={queryClient}>
      <AuthProvider>
        <BrowserRouter>
          <Suspense fallback={<PageFallback />}>
            <Routes>
              {/* Public Routes */}
              <Route path="/" element={<HomePage />} />
              <Route path="/login" element={<LoginPage />} />
              <Route path="/register" element={<RegisterPage />} />
              <Route path="/forgot-password" element={<ForgotPasswordPage />} />
              <Route path="/verify-email" element={<VerifyEmailPage />} />

              {/* Protected Routes */}
              <Route element={<ProtectedRoute />}>
                <Route path="/dashboard" element={<DashboardPage />} />
                <Route path="/profile" element={<ProfilePage />} />
                <Route path="/profile/edit" element={<EditProfilePage />} />
                <Route path="/onboarding/role" element={<RoleSelectionPage />} />
                <Route path="/markets" element={<MarketListPage />} />
                <Route path="/markets/:marketId" element={<MarketDetailPage />} />

                {/* FARMER Role Restricted Routes */}
                <Route element={<FarmerRoute />}>
                  <Route path="/farms" element={<FarmListPage />} />
                  <Route path="/farms/new" element={<CreateFarmPage />} />
                  <Route path="/farms/:farmId" element={<FarmDetailPage />} />
                  <Route path="/farms/:farmId/edit" element={<EditFarmPage />} />
                  <Route path="/farms/:farmId/crops" element={<FarmCropsPage />} />
                </Route>
              </Route>
            </Routes>
          </Suspense>
        </BrowserRouter>
      </AuthProvider>
    </QueryClientProvider>
  );
};

export default App;

