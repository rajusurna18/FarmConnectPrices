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
const MarketPriceListPage = lazy(() => import('./features/prices/pages/MarketPriceListPage').then((m) => ({ default: m.MarketPriceListPage })));
const MarketPriceDetailPage = lazy(() => import('./features/prices/pages/MarketPriceDetailPage').then((m) => ({ default: m.MarketPriceDetailPage })));
const MarketIntelligencePage = lazy(() => import('./features/intelligence/pages/MarketIntelligencePage').then((m) => ({ default: m.MarketIntelligencePage })));
const MarketTrendsPage = lazy(() => import('./features/intelligence/pages/MarketTrendsPage').then((m) => ({ default: m.MarketTrendsPage })));
const DecisionSupportOverviewPage = lazy(() => import('./features/decisionSupport/pages/DecisionSupportOverviewPage').then((m) => ({ default: m.DecisionSupportOverviewPage })));
const SingleMarketEvaluationPage = lazy(() => import('./features/decisionSupport/pages/SingleMarketEvaluationPage').then((m) => ({ default: m.SingleMarketEvaluationPage })));
const MarketComparisonPage = lazy(() => import('./features/decisionSupport/pages/MarketComparisonPage').then((m) => ({ default: m.MarketComparisonPage })));

// Module 10 — Farm Economics routes
const FarmEconomicsDashboardPage = lazy(() => import('./features/farmEconomics/pages/FarmEconomicsDashboardPage').then((m) => ({ default: m.FarmEconomicsDashboardPage })));
const CreateFarmEconomicPage = lazy(() => import('./features/farmEconomics/pages/CreateFarmEconomicPage').then((m) => ({ default: m.CreateFarmEconomicPage })));
const EditFarmEconomicPage = lazy(() => import('./features/farmEconomics/pages/EditFarmEconomicPage').then((m) => ({ default: m.EditFarmEconomicPage })));
const FarmEconomicDetailPage = lazy(() => import('./features/farmEconomics/pages/FarmEconomicDetailPage').then((m) => ({ default: m.FarmEconomicDetailPage })));
const FarmMarketComparisonPage = lazy(() => import('./features/farmEconomics/pages/FarmMarketComparisonPage').then((m) => ({ default: m.FarmMarketComparisonPage })));

// Module 11 — AI Decision Intelligence routes
const AiDecisionPage = lazy(() => import('./features/aiDecision/pages/AiDecisionPage').then((m) => ({ default: m.AiDecisionPage })));

// Module 13 — Price Prediction & Forecasting Foundation routes
const PriceForecastPage = lazy(() => import('./features/forecast/pages/PriceForecastPage').then((m) => ({ default: m.PriceForecastPage })));

// Module 14 — Smart Selling Decision Engine route
const SmartSellingPage = lazy(() => import('./pages/SmartSellingPage').then((m) => ({ default: m.SmartSellingPage })));

// Module 15 — Marketplace Product Catalog & Listing Foundation routes
const MarketplaceBrowsePage = lazy(() => import('./features/marketplace/pages/MarketplaceBrowsePage').then((m) => ({ default: m.MarketplaceBrowsePage })));
const MarketplaceDetailPage = lazy(() => import('./features/marketplace/pages/MarketplaceDetailPage').then((m) => ({ default: m.MarketplaceDetailPage })));
const MyListingsPage = lazy(() => import('./features/marketplace/pages/MyListingsPage').then((m) => ({ default: m.MyListingsPage })));
const CreateListingPage = lazy(() => import('./features/marketplace/pages/CreateListingPage').then((m) => ({ default: m.CreateListingPage })));
const EditListingPage = lazy(() => import('./features/marketplace/pages/EditListingPage').then((m) => ({ default: m.EditListingPage })));

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
                <Route path="/market-prices" element={<MarketPriceListPage />} />
                <Route path="/market-prices/:priceId" element={<MarketPriceDetailPage />} />
                <Route path="/market-intelligence" element={<MarketIntelligencePage />} />
                <Route path="/market-trends" element={<MarketTrendsPage />} />
                <Route path="/marketplace" element={<MarketplaceBrowsePage />} />
                <Route path="/marketplace/listings/:id" element={<MarketplaceDetailPage />} />

                {/* FARMER Role Restricted Routes */}
                <Route element={<FarmerRoute />}>
                  <Route path="/farms" element={<FarmListPage />} />
                  <Route path="/farms/new" element={<CreateFarmPage />} />
                  <Route path="/farms/:farmId" element={<FarmDetailPage />} />
                  <Route path="/farms/:farmId/edit" element={<EditFarmPage />} />
                  <Route path="/farms/:farmId/crops" element={<FarmCropsPage />} />
                  <Route path="/decision-support" element={<DecisionSupportOverviewPage />} />
                  <Route path="/decision-support/evaluate" element={<SingleMarketEvaluationPage />} />
                  <Route path="/decision-support/compare" element={<MarketComparisonPage />} />
                  <Route path="/farm-economics" element={<FarmEconomicsDashboardPage />} />
                  <Route path="/farm-economics/new" element={<CreateFarmEconomicPage />} />
                  <Route path="/farm-economics/compare" element={<FarmMarketComparisonPage />} />
                  <Route path="/farm-economics/:id" element={<FarmEconomicDetailPage />} />
                  <Route path="/farm-economics/:id/edit" element={<EditFarmEconomicPage />} />
                  <Route path="/ai-decisions" element={<AiDecisionPage />} />
                  <Route path="/price-forecast" element={<PriceForecastPage />} />
                  <Route path="/smart-selling" element={<SmartSellingPage />} />
                  <Route path="/marketplace/my-listings" element={<MyListingsPage />} />
                  <Route path="/marketplace/listings/new" element={<CreateListingPage />} />
                  <Route path="/marketplace/listings/:id/edit" element={<EditListingPage />} />
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

