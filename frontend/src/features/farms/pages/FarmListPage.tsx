import React, { useState } from 'react';
import { Link } from 'react-router-dom';
import { Plus, MapPin, Layers, Trash2, Edit3, ArrowRight, Sprout, AlertCircle } from 'lucide-react';
import { Navbar } from '../../../components/navigation/Navbar';
import { Footer } from '../../../components/navigation/Footer';
import { GlassCard } from '../../../components/ui/GlassCard';
import { useFarms, useDeleteFarm } from '../hooks/useFarms';

export const FarmListPage: React.FC = () => {
  const { data: farms, isLoading, error } = useFarms();
  const deleteFarmMutation = useDeleteFarm();
  const [deletingId, setDeletingId] = useState<string | null>(null);

  const handleDelete = async (farmId: string, name: string) => {
    if (window.confirm(`Are you sure you want to delete "${name}"? This action cannot be undone.`)) {
      setDeletingId(farmId);
      try {
        await deleteFarmMutation.mutateAsync(farmId);
      } catch (err) {
        alert(err instanceof Error ? err.message : 'Failed to delete farm.');
      } finally {
        setDeletingId(null);
      }
    }
  };

  return (
    <div className="min-h-screen bg-slate-950 text-slate-100 flex flex-col justify-between font-sans select-none">
      <Navbar />

      <main className="flex-grow pt-28 pb-20 px-4 sm:px-6 lg:px-8 max-w-7xl mx-auto w-full">
        
        {/* Page Header */}
        <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 mb-8">
          <div>
            <div className="inline-flex items-center space-x-2 px-3 py-1 rounded-full bg-emerald-500/10 border border-emerald-500/30 text-emerald-400 text-xs font-mono mb-2">
              <Sprout className="w-3.5 h-3.5" />
              <span>Farmer Domain • Land Ownership</span>
            </div>
            <h1 className="text-3xl sm:text-4xl font-extrabold text-white tracking-tight">
              My Farm Records
            </h1>
            <p className="text-sm text-slate-400 mt-1">
              Manage your agricultural land holdings, locations, and crop associations.
            </p>
          </div>

          <Link
            to="/farms/new"
            className="inline-flex items-center justify-center space-x-2 min-h-[48px] px-6 py-3 rounded-xl bg-emerald-500 hover:bg-emerald-400 text-slate-950 font-bold text-sm shadow-lg shadow-emerald-500/20 transition-all shrink-0 active:scale-95"
          >
            <Plus className="w-5 h-5" />
            <span>Add New Farm</span>
          </Link>
        </div>

        {/* Loading State */}
        {isLoading && (
          <div className="py-20 text-center">
            <div className="w-12 h-12 border-4 border-emerald-500 border-t-transparent rounded-full animate-spin mx-auto mb-4" />
            <p className="text-slate-400 text-sm font-medium">Loading your farm records...</p>
          </div>
        )}

        {/* Error State */}
        {error && (
          <div className="p-6 rounded-2xl bg-red-500/10 border border-red-500/30 text-red-300 flex items-center space-x-3 mb-8">
            <AlertCircle className="w-6 h-6 shrink-0 text-red-400" />
            <p className="text-sm font-medium">
              Failed to load farm records. Please verify your authentication and try again.
            </p>
          </div>
        )}

        {/* Empty State */}
        {!isLoading && !error && farms && farms.length === 0 && (
          <GlassCard className="p-12 text-center max-w-xl mx-auto my-12 border-slate-800">
            <div className="w-16 h-16 rounded-2xl bg-slate-900 border border-slate-800 flex items-center justify-center mx-auto mb-4 text-emerald-400">
              <Sprout className="w-8 h-8" />
            </div>
            <h3 className="text-xl font-bold text-white mb-2">No Farms Registered Yet</h3>
            <p className="text-xs sm:text-sm text-slate-400 mb-6 leading-relaxed">
              You have not added any farm holdings to your profile. Register your first farm to start associating crops and seasons.
            </p>
            <Link
              to="/farms/new"
              className="inline-flex items-center justify-center space-x-2 min-h-[48px] px-6 py-3 rounded-xl bg-emerald-500 hover:bg-emerald-400 text-slate-950 font-bold text-sm shadow-lg shadow-emerald-500/20 transition-all"
            >
              <Plus className="w-5 h-5" />
              <span>Register Your First Farm</span>
            </Link>
          </GlassCard>
        )}

        {/* Farms Grid */}
        {!isLoading && !error && farms && farms.length > 0 && (
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
            {farms.map((farm) => (
              <GlassCard key={farm.id} className="p-6 flex flex-col justify-between border-slate-800/80 hover:border-emerald-500/40 transition-all shadow-xl">
                <div className="space-y-4">
                  {/* Top Status Badge & Title */}
                  <div className="flex items-start justify-between gap-2">
                    <div>
                      <span className="text-xs font-semibold px-2.5 py-0.5 rounded-full bg-slate-900 text-slate-400 border border-slate-800 font-mono">
                        {farm.landAreaUnit}
                      </span>
                      <h3 className="text-xl font-extrabold text-white mt-1 leading-snug">{farm.name}</h3>
                    </div>

                    <span className={`text-[10px] font-bold uppercase px-2.5 py-1 rounded-full border ${
                      farm.status === 'ACTIVE'
                        ? 'bg-emerald-500/10 text-emerald-400 border-emerald-500/30'
                        : 'bg-slate-800 text-slate-400 border-slate-700'
                    }`}>
                      {farm.status}
                    </span>
                  </div>

                  {/* Location Info */}
                  <div className="bg-slate-950/80 p-3.5 rounded-xl border border-slate-900 space-y-1.5 text-xs text-slate-300">
                    <div className="flex items-center space-x-2 text-slate-400">
                      <MapPin className="w-3.5 h-3.5 text-emerald-400 shrink-0" />
                      <span className="truncate">{farm.location.village}, {farm.location.mandal}</span>
                    </div>
                    <div className="pl-5 text-slate-400 text-[11px]">
                      {farm.location.district}, {farm.location.state} {farm.location.pincode ? ` - ${farm.location.pincode}` : ''}
                    </div>
                  </div>

                  {/* Land Area Metric */}
                  <div className="flex items-center justify-between p-3 rounded-xl bg-slate-900/60 border border-slate-800">
                    <span className="text-xs text-slate-400 font-medium flex items-center">
                      <Layers className="w-3.5 h-3.5 text-emerald-400 mr-1.5" />
                      Land Area
                    </span>
                    <span className="text-base font-extrabold text-white">
                      {farm.landArea} <span className="text-xs font-normal text-slate-400">{farm.landAreaUnit}s</span>
                    </span>
                  </div>
                </div>

                {/* Card Actions Footer */}
                <div className="pt-5 border-t border-slate-900 mt-6 flex items-center justify-between gap-2">
                  <div className="flex items-center space-x-2">
                    <Link
                      to={`/farms/${farm.id}/edit`}
                      className="p-2.5 rounded-xl bg-slate-900 hover:bg-slate-800 text-slate-300 hover:text-white border border-slate-800 transition-colors min-h-[44px] min-w-[44px] flex items-center justify-center"
                      title="Edit Farm"
                    >
                      <Edit3 className="w-4 h-4" />
                    </Link>

                    <button
                      onClick={() => handleDelete(farm.id, farm.name)}
                      disabled={deletingId === farm.id}
                      className="p-2.5 rounded-xl bg-slate-900 hover:bg-red-500/20 text-slate-400 hover:text-red-400 border border-slate-800 hover:border-red-500/30 transition-colors min-h-[44px] min-w-[44px] flex items-center justify-center disabled:opacity-50"
                      title="Delete Farm"
                    >
                      <Trash2 className="w-4 h-4" />
                    </button>
                  </div>

                  <Link
                    to={`/farms/${farm.id}`}
                    className="inline-flex items-center space-x-1.5 px-4 py-2.5 rounded-xl bg-emerald-500/10 hover:bg-emerald-500/20 text-emerald-400 text-xs font-bold border border-emerald-500/30 transition-all min-h-[44px]"
                  >
                    <span>View Crops</span>
                    <ArrowRight className="w-3.5 h-3.5" />
                  </Link>
                </div>
              </GlassCard>
            ))}
          </div>
        )}

      </main>

      <Footer />
    </div>
  );
};
