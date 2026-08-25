import React, { useState } from 'react';
import { useParams, useNavigate, Link } from 'react-router-dom';
import { ArrowLeft, Save, AlertCircle, Sprout } from 'lucide-react';
import { Navbar } from '../../../components/navigation/Navbar';
import { Footer } from '../../../components/navigation/Footer';
import { GlassCard } from '../../../components/ui/GlassCard';
import { useFarm, useCrops, useAddFarmCrop } from '../hooks/useFarms';
import type { CropSeason } from '../types/farmTypes';

export const FarmCropsPage: React.FC = () => {
  const { farmId } = useParams<{ farmId: string }>();
  const navigate = useNavigate();

  const { data: farm, isLoading: farmLoading } = useFarm(farmId);
  const { data: cropsMaster, isLoading: cropsLoading } = useCrops();
  const addFarmCropMutation = useAddFarmCrop();

  const [selectedCropId, setSelectedCropId] = useState('');
  const [season, setSeason] = useState<CropSeason>('KHARIF');
  const [formError, setFormError] = useState<string | null>(null);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!farmId) return;
    setFormError(null);

    if (!selectedCropId) {
      setFormError('Please select a crop from Crop Master.');
      return;
    }

    try {
      await addFarmCropMutation.mutateAsync({
        farmId,
        input: {
          cropId: selectedCropId,
          season,
          status: 'ACTIVE',
        },
      });
      navigate(`/farms/${farmId}`);
    } catch (err) {
      setFormError(err instanceof Error ? err.message : 'Failed to add crop association.');
    }
  };

  return (
    <div className="min-h-screen bg-slate-950 text-slate-100 flex flex-col justify-between font-sans select-none">
      <Navbar />

      <main className="flex-grow pt-28 pb-20 px-4 sm:px-6 lg:px-8 max-w-3xl mx-auto w-full">
        
        {/* Navigation Back Link */}
        <div className="mb-6">
          <Link
            to={farmId ? `/farms/${farmId}` : '/farms'}
            className="inline-flex items-center space-x-2 text-xs font-semibold text-slate-400 hover:text-emerald-400 transition-colors"
          >
            <ArrowLeft className="w-4 h-4" />
            <span>Back to Farm Details</span>
          </Link>
        </div>

        {/* Page Title Header */}
        <div className="mb-8">
          <div className="inline-flex items-center space-x-2 px-3 py-1 rounded-full bg-emerald-500/10 border border-emerald-500/30 text-emerald-400 text-xs font-mono mb-2">
            <Sprout className="w-3.5 h-3.5" />
            <span>Crop Association Master</span>
          </div>
          <h1 className="text-3xl font-extrabold text-white tracking-tight">
            Add Crop Association
          </h1>
          <p className="text-sm text-slate-400 mt-1">
            Associate a standardized crop from Crop Master with {farm?.name || 'your farm'} and select the cultivation season.
          </p>
        </div>

        {/* Form Error Banner */}
        {formError && (
          <div className="p-4 rounded-2xl bg-red-500/10 border border-red-500/30 text-red-300 flex items-center space-x-3 mb-6 text-xs sm:text-sm font-medium">
            <AlertCircle className="w-5 h-5 shrink-0 text-red-400" />
            <span>{formError}</span>
          </div>
        )}

        {/* Add Crop Form */}
        <GlassCard className="p-6 sm:p-8 border-slate-800 shadow-2xl">
          <form onSubmit={handleSubmit} className="space-y-6">
            
            {/* Select Crop Dropdown */}
            <div>
              <label className="block text-xs font-semibold text-slate-300 mb-1.5">
                Crop Master <span className="text-emerald-400">*</span>
              </label>

              {cropsLoading ? (
                <div className="p-3 text-xs text-slate-400">Loading Crop Master...</div>
              ) : (
                <select
                  required
                  value={selectedCropId}
                  onChange={(e) => setSelectedCropId(e.target.value)}
                  className="w-full px-4 py-3 rounded-xl bg-slate-950/80 border border-slate-800 text-white text-sm focus:outline-none focus:border-emerald-500 transition-colors min-h-[48px]"
                >
                  <option value="">-- Select Crop from Master List --</option>
                  {cropsMaster?.map((crop) => (
                    <option key={crop.id} value={crop.id}>
                      {crop.name} ({crop.category}) {crop.scientificName ? `- ${crop.scientificName}` : ''}
                    </option>
                  ))}
                </select>
              )}
              <p className="text-[11px] text-slate-500 mt-1">
                Only standardized crops from the Crop Master index may be selected. Free-text crop entry is disabled for data consistency.
              </p>
            </div>

            {/* Select Season Dropdown */}
            <div>
              <label className="block text-xs font-semibold text-slate-300 mb-1.5">
                Cultivation Season <span className="text-emerald-400">*</span>
              </label>
              <select
                value={season}
                onChange={(e) => setSeason(e.target.value as CropSeason)}
                className="w-full px-4 py-3 rounded-xl bg-slate-950/80 border border-slate-800 text-white text-sm focus:outline-none focus:border-emerald-500 transition-colors min-h-[48px]"
              >
                <option value="KHARIF">KHARIF (Monsoon Crop: June - October)</option>
                <option value="RABI">RABI (Winter Crop: October - March)</option>
                <option value="ZAID">ZAID (Summer Crop: March - June)</option>
              </select>
            </div>

            {/* Form Actions Footer */}
            <div className="pt-6 border-t border-slate-900 flex items-center justify-end space-x-3">
              <Link
                to={farmId ? `/farms/${farmId}` : '/farms'}
                className="px-6 py-3 rounded-xl bg-slate-900 hover:bg-slate-800 text-slate-300 text-xs font-semibold border border-slate-800 transition-colors min-h-[48px] flex items-center justify-center"
              >
                Cancel
              </Link>

              <button
                type="submit"
                disabled={addFarmCropMutation.isPending || farmLoading}
                className="inline-flex items-center space-x-2 px-6 py-3 rounded-xl bg-emerald-500 hover:bg-emerald-400 text-slate-950 font-bold text-sm shadow-lg shadow-emerald-500/20 transition-all min-h-[48px] disabled:opacity-50"
              >
                <Save className="w-4 h-4" />
                <span>{addFarmCropMutation.isPending ? 'Associating Crop...' : 'Associate Crop'}</span>
              </button>
            </div>

          </form>
        </GlassCard>

      </main>

      <Footer />
    </div>
  );
};
