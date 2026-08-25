import React from 'react';
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
import { DashboardPage } from './pages/DashboardPage';
import { ProfilePage } from './features/profile/pages/ProfilePage';
import { EditProfilePage } from './features/profile/pages/EditProfilePage';
import { RoleSelectionPage } from './features/profile/pages/RoleSelectionPage';
import { FarmListPage } from './features/farms/pages/FarmListPage';
import { CreateFarmPage } from './features/farms/pages/CreateFarmPage';
import { FarmDetailPage } from './features/farms/pages/FarmDetailPage';
import { EditFarmPage } from './features/farms/pages/EditFarmPage';
import { FarmCropsPage } from './features/farms/pages/FarmCropsPage';

const queryClient = new QueryClient();

export const App: React.FC = () => {
  return (
    <QueryClientProvider client={queryClient}>
      <AuthProvider>
        <BrowserRouter>
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
        </BrowserRouter>
      </AuthProvider>
    </QueryClientProvider>
  );
};

export default App;
