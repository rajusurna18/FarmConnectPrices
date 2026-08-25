import React, { useState, useEffect } from 'react';
import { useNavigate, useParams, Link } from 'react-router-dom';
import { ArrowLeft, Save, AlertCircle, MapPin, Layers } from 'lucide-react';
import { Navbar } from '../../../components/navigation/Navbar';
import { Footer } from '../../../components/navigation/Footer';
import { GlassCard } from '../../../components/ui/GlassCard';
import { useFarm, useUpdateFarm } from '../hooks/useFarms';
import type { LandAreaUnit, FarmStatus } from '../types/farmTypes';

export const EditFarmPage: React.FC = () => {
  const { farmId } = useParams<{ farmId: string }>();
  const navigate = useNavigate();

  const { data: farm, isLoading, error } = useFarm(farmId);
  const updateFarmMutation = useUpdateFarm();

  const [name, setName] = useState('');
  const [state, setState] = useState('');
  const [district, setDistrict] = useState('');
  const [mandal, setMandal] = useState('');
  const [village, setVillage] = useState('');
  const [pincode, setPincode] = useState('');
  const [landArea, setLandArea] = useState('');
  const [landAreaUnit, setLandAreaUnit] = useState<LandAreaUnit>('ACRE');
  const [status, setStatus] = useState<FarmStatus>('ACTIVE');
  const [formError, setFormError] = useState<string | null>(null);

  useEffect(() => {
    if (farm) {
      setName(farm.name || '');
      setState(farm.location?.state || '');
      setDistrict(farm.location?.district || '');
      setMandal(farm.location?.mandal || '');
      setVillage(farm.location?.village || '');
      setPincode(farm.location?.pincode || '');
      setLandArea(farm.landArea ? String(farm.landArea) : '');
      setLandAreaUnit(farm.landAreaUnit || 'ACRE');
      setStatus(farm.status || 'ACTIVE');
    }
  }, [farm]);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!farmId) return;
    setFormError(null);

    if (!name.trim()) {
      setFormError('Farm name is required.');
      return;
    }
    if (!state.trim() || !district.trim() || !mandal.trim() || !village.trim()) {
      setFormError('Location requires State, District, Mandal, and Village.');
      return;
    }
    if (pincode.trim() && !/^[1-9][0-9]{5}$/.test(pincode.trim())) {
      setFormError('Pincode must be a valid 6-digit postal code.');
      return;
    }
    const areaNum = parseFloat(landArea);
    if (isNaN(areaNum) || areaNum <= 0) {
      setFormError('Land area must be a positive number greater than zero.');
      return;
    }

    try {
      await updateFarmMutation.mutateAsync({
        farmId,
        input: {
          name: name.trim(),
          location: {
            state: state.trim(),
            district: district.trim(),
            mandal: mandal.trim(),
            village: village.trim(),
            pincode: pincode.trim() || undefined,
          },
          landArea: areaNum,
          landAreaUnit,
          status,
        },
      });
      navigate('/farms');
    } catch (err) {
      setFormError(err instanceof Error ? err.message : 'Failed to update farm record.');
    }
  };

  return (
    <div className="min-h-screen bg-slate-950 text-slate-100 flex flex-col justify-between font-sans select-none">
      <Navbar />

      <main className="flex-grow pt-28 pb-20 px-4 sm:px-6 lg:px-8 max-w-4xl mx-auto w-full">
        
        <div className="mb-6">
          <Link
            to="/farms"
            className="inline-flex items-center space-x-2 text-xs font-semibold text-slate-400 hover:text-emerald-400 transition-colors"
          >
            <ArrowLeft className="w-4 h-4" />
            <span>Back to My Farms</span>
          </Link>
        </div>

        <div className="mb-8">
          <h1 className="text-3xl font-extrabold text-white tracking-tight">
            Edit Farm Record
          </h1>
          <p className="text-sm text-slate-400 mt-1">
            Update location parameters, acreage, and operational status.
          </p>
        </div>

        {isLoading && (
          <div className="py-20 text-center">
            <div className="w-12 h-12 border-4 border-emerald-500 border-t-transparent rounded-full animate-spin mx-auto mb-4" />
            <p className="text-slate-400 text-sm font-medium">Loading farm record...</p>
          </div>
        )}

        {error && (
          <div className="p-4 rounded-2xl bg-red-500/10 border border-red-500/30 text-red-300 flex items-center space-x-3 mb-6 text-xs sm:text-sm font-medium">
            <AlertCircle className="w-5 h-5 shrink-0 text-red-400" />
            <span>Failed to load farm. Make sure you own this farm.</span>
          </div>
        )}

        {formError && (
          <div className="p-4 rounded-2xl bg-red-500/10 border border-red-500/30 text-red-300 flex items-center space-x-3 mb-6 text-xs sm:text-sm font-medium">
            <AlertCircle className="w-5 h-5 shrink-0 text-red-400" />
            <span>{formError}</span>
          </div>
        )}

        {!isLoading && farm && (
          <GlassCard className="p-6 sm:p-8 border-slate-800 shadow-2xl">
            <form onSubmit={handleSubmit} className="space-y-6">
              
              <div className="space-y-4">
                <h3 className="text-base font-bold text-white flex items-center space-x-2 border-b border-slate-900 pb-3">
                  <Layers className="w-4 h-4 text-emerald-400" />
                  <span>Farm Identity & Metrics</span>
                </h3>

                <div>
                  <label className="block text-xs font-semibold text-slate-300 mb-1.5">
                    Farm Name <span className="text-emerald-400">*</span>
                  </label>
                  <input
                    type="text"
                    required
                    value={name}
                    onChange={(e) => setName(e.target.value)}
                    className="w-full px-4 py-3 rounded-xl bg-slate-950/80 border border-slate-800 text-white text-sm focus:outline-none focus:border-emerald-500 transition-colors min-h-[48px]"
                  />
                </div>

                <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
                  <div>
                    <label className="block text-xs font-semibold text-slate-300 mb-1.5">
                      Land Area <span className="text-emerald-400">*</span>
                    </label>
                    <input
                      type="number"
                      step="0.01"
                      min="0.01"
                      required
                      value={landArea}
                      onChange={(e) => setLandArea(e.target.value)}
                      className="w-full px-4 py-3 rounded-xl bg-slate-950/80 border border-slate-800 text-white text-sm focus:outline-none focus:border-emerald-500 transition-colors min-h-[48px]"
                    />
                  </div>

                  <div>
                    <label className="block text-xs font-semibold text-slate-300 mb-1.5">
                      Land Area Unit <span className="text-emerald-400">*</span>
                    </label>
                    <select
                      value={landAreaUnit}
                      onChange={(e) => setLandAreaUnit(e.target.value as LandAreaUnit)}
                      className="w-full px-4 py-3 rounded-xl bg-slate-950/80 border border-slate-800 text-white text-sm focus:outline-none focus:border-emerald-500 transition-colors min-h-[48px]"
                    >
                      <option value="ACRE">ACRE (Acres)</option>
                      <option value="HECTARE">HECTARE (Hectares)</option>
                    </select>
                  </div>

                  <div>
                    <label className="block text-xs font-semibold text-slate-300 mb-1.5">
                      Operational Status <span className="text-emerald-400">*</span>
                    </label>
                    <select
                      value={status}
                      onChange={(e) => setStatus(e.target.value as FarmStatus)}
                      className="w-full px-4 py-3 rounded-xl bg-slate-950/80 border border-slate-800 text-white text-sm focus:outline-none focus:border-emerald-500 transition-colors min-h-[48px]"
                    >
                      <option value="ACTIVE">ACTIVE</option>
                      <option value="INACTIVE">INACTIVE</option>
                    </select>
                  </div>
                </div>
              </div>

              <div className="space-y-4 pt-4">
                <h3 className="text-base font-bold text-white flex items-center space-x-2 border-b border-slate-900 pb-3">
                  <MapPin className="w-4 h-4 text-emerald-400" />
                  <span>Geographic Location</span>
                </h3>

                <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                  <div>
                    <label className="block text-xs font-semibold text-slate-300 mb-1.5">State</label>
                    <input
                      type="text"
                      required
                      value={state}
                      onChange={(e) => setState(e.target.value)}
                      className="w-full px-4 py-3 rounded-xl bg-slate-950/80 border border-slate-800 text-white text-sm focus:outline-none focus:border-emerald-500 transition-colors min-h-[48px]"
                    />
                  </div>

                  <div>
                    <label className="block text-xs font-semibold text-slate-300 mb-1.5">District</label>
                    <input
                      type="text"
                      required
                      value={district}
                      onChange={(e) => setDistrict(e.target.value)}
                      className="w-full px-4 py-3 rounded-xl bg-slate-950/80 border border-slate-800 text-white text-sm focus:outline-none focus:border-emerald-500 transition-colors min-h-[48px]"
                    />
                  </div>

                  <div>
                    <label className="block text-xs font-semibold text-slate-300 mb-1.5">Mandal / Tehsil</label>
                    <input
                      type="text"
                      required
                      value={mandal}
                      onChange={(e) => setMandal(e.target.value)}
                      className="w-full px-4 py-3 rounded-xl bg-slate-950/80 border border-slate-800 text-white text-sm focus:outline-none focus:border-emerald-500 transition-colors min-h-[48px]"
                    />
                  </div>

                  <div>
                    <label className="block text-xs font-semibold text-slate-300 mb-1.5">Village</label>
                    <input
                      type="text"
                      required
                      value={village}
                      onChange={(e) => setVillage(e.target.value)}
                      className="w-full px-4 py-3 rounded-xl bg-slate-950/80 border border-slate-800 text-white text-sm focus:outline-none focus:border-emerald-500 transition-colors min-h-[48px]"
                    />
                  </div>
                </div>

                <div>
                  <label className="block text-xs font-semibold text-slate-300 mb-1.5">Postal Pincode</label>
                  <input
                    type="text"
                    maxLength={6}
                    value={pincode}
                    onChange={(e) => setPincode(e.target.value)}
                    className="w-full px-4 py-3 rounded-xl bg-slate-950/80 border border-slate-800 text-white text-sm focus:outline-none focus:border-emerald-500 transition-colors min-h-[48px]"
                  />
                </div>
              </div>

              <div className="pt-6 border-t border-slate-900 flex items-center justify-end space-x-3">
                <Link
                  to="/farms"
                  className="px-6 py-3 rounded-xl bg-slate-900 hover:bg-slate-800 text-slate-300 text-xs font-semibold border border-slate-800 transition-colors min-h-[48px] flex items-center justify-center"
                >
                  Cancel
                </Link>

                <button
                  type="submit"
                  disabled={updateFarmMutation.isPending}
                  className="inline-flex items-center space-x-2 px-6 py-3 rounded-xl bg-emerald-500 hover:bg-emerald-400 text-slate-950 font-bold text-sm shadow-lg shadow-emerald-500/20 transition-all min-h-[48px] disabled:opacity-50"
                >
                  <Save className="w-4 h-4" />
                  <span>{updateFarmMutation.isPending ? 'Updating...' : 'Update Farm Record'}</span>
                </button>
              </div>

            </form>
          </GlassCard>
        )}

      </main>

      <Footer />
    </div>
  );
};
