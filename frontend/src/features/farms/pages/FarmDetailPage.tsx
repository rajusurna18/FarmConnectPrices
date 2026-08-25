import React from 'react';
import { useParams, Link } from 'react-router-dom';
import { ArrowLeft, MapPin, Layers, Plus, Calendar, Edit3, Sprout, AlertCircle } from 'lucide-react';
import { Navbar } from '../../../components/navigation/Navbar';
import { Footer } from '../../../components/navigation/Footer';
import { GlassCard } from '../../../components/ui/GlassCard';
import { useFarm, useFarmCrops, useDeleteFarmCrop } from '../hooks/useFarms';

export const FarmDetailPage: React.FC = () => {
  const { farmId } = useParams<{ farmId: string }>();

  const { data: farm, isLoading: farmLoading, error: farmError } = useFarm(farmId);
  const { data: farmCrops, isLoading: cropsLoading } = useFarmCrops(farmId);
  const deleteCropMutation = useDeleteFarmCrop();

  const handleDeleteCrop = async (farmCropId: string, cropName?: string) => {
    if (!farmId) return;
    if (window.confirm(`Are you sure you want to remove ${cropName || 'this crop association'} from your farm?`)) {
      try {
        await deleteCropMutation.mutateAsync({ farmId, farmCropId });
      } catch (err) {
        alert(err instanceof Error ? err.message : 'Failed to delete crop relationship.');
      }
    }
  };

  return (
    <div className="min-h-screen bg-slate-950 text-slate-100 flex flex-col justify-between font-sans select-none">
      <Navbar />

      <main className="flex-grow pt-28 pb-20 px-4 sm:px-6 lg:px-8 max-w-5xl mx-auto w-full">
        
        {/* Navigation Back Link */}
        <div className="mb-6">
          <Link
            to="/farms"
            className="inline-flex items-center space-x-2 text-xs font-semibold text-slate-400 hover:text-emerald-400 transition-colors"
          >
            <ArrowLeft className="w-4 h-4" />
            <span>Back to My Farms</span>
          </Link>
        </div>

        {/* Loading State */}
        {farmLoading && (
          <div className="py-20 text-center">
            <div className="w-12 h-12 border-4 border-emerald-500 border-t-transparent rounded-full animate-spin mx-auto mb-4" />
            <p className="text-slate-400 text-sm font-medium">Loading farm details...</p>
          </div>
        )}

        {/* Error State */}
        {farmError && (
          <div className="p-4 rounded-2xl bg-red-500/10 border border-red-500/30 text-red-300 flex items-center space-x-3 mb-6 text-xs sm:text-sm font-medium">
            <AlertCircle className="w-5 h-5 shrink-0 text-red-400" />
            <span>Farm record not found or access denied.</span>
          </div>
        )}

        {!farmLoading && farm && (
          <div className="space-y-8">
            
            {/* Header Farm Detail Card */}
            <GlassCard className="p-6 sm:p-8 border-slate-800 shadow-2xl">
              <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 pb-6 border-b border-slate-900">
                <div>
                  <div className="flex items-center space-x-2 mb-2">
                    <span className="text-xs font-semibold px-2.5 py-0.5 rounded-full bg-slate-900 text-slate-400 border border-slate-800 font-mono">
                      {farm.landAreaUnit}
                    </span>
                    <span className={`text-[10px] font-bold uppercase px-2.5 py-0.5 rounded-full border ${
                      farm.status === 'ACTIVE'
                        ? 'bg-emerald-500/10 text-emerald-400 border-emerald-500/30'
                        : 'bg-slate-800 text-slate-400 border-slate-700'
                    }`}>
                      {farm.status}
                    </span>
                  </div>
                  <h1 className="text-3xl font-extrabold text-white">{farm.name}</h1>
                </div>

                <Link
                  to={`/farms/${farm.id}/edit`}
                  className="inline-flex items-center justify-center space-x-2 px-4 py-2.5 rounded-xl bg-slate-900 hover:bg-slate-800 text-slate-200 font-semibold text-xs border border-slate-800 transition-colors min-h-[44px]"
                >
                  <Edit3 className="w-4 h-4" />
                  <span>Edit Details</span>
                </Link>
              </div>

              {/* Specs Summary Grid */}
              <div className="grid grid-cols-1 sm:grid-cols-2 gap-6 pt-6 text-xs sm:text-sm">
                <div className="space-y-2 bg-slate-950/80 p-4 rounded-2xl border border-slate-900">
                  <span className="text-slate-400 font-medium block flex items-center">
                    <MapPin className="w-4 h-4 text-emerald-400 mr-1.5" />
                    Location
                  </span>
                  <p className="text-white font-bold text-base">
                    {farm.location.village}, {farm.location.mandal}
                  </p>
                  <p className="text-slate-400">
                    {farm.location.district}, {farm.location.state} {farm.location.pincode ? `- ${farm.location.pincode}` : ''}
                  </p>
                </div>

                <div className="space-y-2 bg-slate-950/80 p-4 rounded-2xl border border-slate-900">
                  <span className="text-slate-400 font-medium block flex items-center">
                    <Layers className="w-4 h-4 text-emerald-400 mr-1.5" />
                    Total Land Area
                  </span>
                  <p className="text-white font-bold text-2xl">
                    {farm.landArea} <span className="text-xs text-slate-400 font-normal">{farm.landAreaUnit}s</span>
                  </p>
                  <p className="text-slate-500 text-[11px]">
                    Registered on {farm.createdAt ? new Date(farm.createdAt).toLocaleDateString() : 'N/A'}
                  </p>
                </div>
              </div>
            </GlassCard>

            {/* Farm Crops Section */}
            <div className="space-y-4">
              <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
                <div>
                  <h2 className="text-2xl font-bold text-white flex items-center space-x-2">
                    <Sprout className="w-5 h-5 text-emerald-400" />
                    <span>Cultivated Crops & Seasons</span>
                  </h2>
                  <p className="text-xs text-slate-400 mt-1">
                    Crops associated with this farm holding across Kharif, Rabi, and Zaid seasons.
                  </p>
                </div>

                <Link
                  to={`/farms/${farm.id}/crops`}
                  className="inline-flex items-center justify-center space-x-2 px-4 py-2.5 rounded-xl bg-emerald-500 hover:bg-emerald-400 text-slate-950 font-bold text-xs shadow-lg shadow-emerald-500/20 transition-all min-h-[44px]"
                >
                  <Plus className="w-4 h-4" />
                  <span>Add Crop Association</span>
                </Link>
              </div>

              {/* Crops List */}
              {cropsLoading && (
                <div className="py-12 text-center text-slate-400 text-xs">
                  Loading associated crops...
                </div>
              )}

              {!cropsLoading && farmCrops && farmCrops.length === 0 && (
                <GlassCard className="p-8 text-center border-slate-800">
                  <p className="text-slate-400 text-xs sm:text-sm mb-4">
                    No crops associated with this farm yet. Click below to select from Crop Master.
                  </p>
                  <Link
                    to={`/farms/${farm.id}/crops`}
                    className="inline-flex items-center space-x-2 px-4 py-2 rounded-xl bg-slate-900 hover:bg-slate-800 text-emerald-400 font-semibold text-xs border border-slate-800 transition-colors"
                  >
                    <Plus className="w-4 h-4" />
                    <span>Associate Crop</span>
                  </Link>
                </GlassCard>
              )}

              {!cropsLoading && farmCrops && farmCrops.length > 0 && (
                <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4">
                  {farmCrops.map((fc) => (
                    <GlassCard key={fc.id} className="p-5 border-slate-800/80 hover:border-emerald-500/40 transition-all">
                      <div className="space-y-3">
                        <div className="flex items-center justify-between">
                          <span className="text-[10px] font-bold px-2.5 py-0.5 rounded-full bg-emerald-500/10 text-emerald-400 border border-emerald-500/30 flex items-center space-x-1">
                            <Calendar className="w-3 h-3 mr-1" />
                            {fc.season}
                          </span>

                          <button
                            onClick={() => handleDeleteCrop(fc.id, fc.crop?.name)}
                            className="text-slate-500 hover:text-red-400 text-xs font-semibold p-1 hover:bg-red-500/10 rounded transition-colors"
                            title="Remove Crop Association"
                          >
                            Remove
                          </button>
                        </div>

                        <div>
                          <h4 className="text-lg font-bold text-white">
                            {fc.crop?.name || 'Crop'}
                          </h4>
                          {fc.crop?.scientificName && (
                            <p className="text-[11px] text-slate-400 italic font-mono">
                              {fc.crop.scientificName}
                            </p>
                          )}
                        </div>

                        <div className="pt-3 border-t border-slate-900 flex items-center justify-between text-[11px]">
                          <span className="text-slate-500">Category</span>
                          <span className="text-slate-300 font-semibold">{fc.crop?.category || 'N/A'}</span>
                        </div>
                      </div>
                    </GlassCard>
                  ))}
                </div>
              )}
            </div>

          </div>
        )}

      </main>

      <Footer />
    </div>
  );
};
