import React, { useState } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { ArrowLeft, Save, AlertCircle, MapPin, Layers } from 'lucide-react';
import { Navbar } from '../../../components/navigation/Navbar';
import { Footer } from '../../../components/navigation/Footer';
import { GlassCard } from '../../../components/ui/GlassCard';
import { useCreateFarm } from '../hooks/useFarms';
import type { LandAreaUnit } from '../types/farmTypes';

export const CreateFarmPage: React.FC = () => {
  const navigate = useNavigate();
  const createFarmMutation = useCreateFarm();

  const [name, setName] = useState('');
  const [state, setState] = useState('Telangana');
  const [district, setDistrict] = useState('');
  const [mandal, setMandal] = useState('');
  const [village, setVillage] = useState('');
  const [pincode, setPincode] = useState('');
  const [landArea, setLandArea] = useState('');
  const [landAreaUnit, setLandAreaUnit] = useState<LandAreaUnit>('ACRE');
  const [formError, setFormError] = useState<string | null>(null);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
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
      await createFarmMutation.mutateAsync({
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
        status: 'ACTIVE',
      });
      navigate('/farms');
    } catch (err) {
      setFormError(err instanceof Error ? err.message : 'Failed to create farm record.');
    }
  };

  return (
    <div className="min-h-screen bg-slate-950 text-slate-100 flex flex-col justify-between font-sans select-none">
      <Navbar />

      <main className="flex-grow pt-28 pb-20 px-4 sm:px-6 lg:px-8 max-w-4xl mx-auto w-full">
        
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

        {/* Page Title Header */}
        <div className="mb-8">
          <h1 className="text-3xl font-extrabold text-white tracking-tight">
            Register New Farm Holding
          </h1>
          <p className="text-sm text-slate-400 mt-1">
            Provide the location details and total land area for your agricultural farm record.
          </p>
        </div>

        {/* Form Error Banner */}
        {formError && (
          <div className="p-4 rounded-2xl bg-red-500/10 border border-red-500/30 text-red-300 flex items-center space-x-3 mb-6 text-xs sm:text-sm font-medium">
            <AlertCircle className="w-5 h-5 shrink-0 text-red-400" />
            <span>{formError}</span>
          </div>
        )}

        {/* Create Farm Form */}
        <GlassCard className="p-6 sm:p-8 border-slate-800 shadow-2xl">
          <form onSubmit={handleSubmit} className="space-y-6">
            
            {/* Section 1: Farm Basic Info */}
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
                  placeholder="e.g. Green Acres - East Plot"
                  value={name}
                  onChange={(e) => setName(e.target.value)}
                  className="w-full px-4 py-3 rounded-xl bg-slate-950/80 border border-slate-800 text-white placeholder-slate-500 text-sm focus:outline-none focus:border-emerald-500 transition-colors min-h-[48px]"
                />
              </div>

              <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                <div>
                  <label className="block text-xs font-semibold text-slate-300 mb-1.5">
                    Land Area <span className="text-emerald-400">*</span>
                  </label>
                  <input
                    type="number"
                    step="0.01"
                    min="0.01"
                    required
                    placeholder="e.g. 5.5"
                    value={landArea}
                    onChange={(e) => setLandArea(e.target.value)}
                    className="w-full px-4 py-3 rounded-xl bg-slate-950/80 border border-slate-800 text-white placeholder-slate-500 text-sm focus:outline-none focus:border-emerald-500 transition-colors min-h-[48px]"
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
              </div>
            </div>

            {/* Section 2: Location Details */}
            <div className="space-y-4 pt-4">
              <h3 className="text-base font-bold text-white flex items-center space-x-2 border-b border-slate-900 pb-3">
                <MapPin className="w-4 h-4 text-emerald-400" />
                <span>Geographic Location</span>
              </h3>

              <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                <div>
                  <label className="block text-xs font-semibold text-slate-300 mb-1.5">
                    State <span className="text-emerald-400">*</span>
                  </label>
                  <input
                    type="text"
                    required
                    placeholder="e.g. Telangana"
                    value={state}
                    onChange={(e) => setState(e.target.value)}
                    className="w-full px-4 py-3 rounded-xl bg-slate-950/80 border border-slate-800 text-white placeholder-slate-500 text-sm focus:outline-none focus:border-emerald-500 transition-colors min-h-[48px]"
                  />
                </div>

                <div>
                  <label className="block text-xs font-semibold text-slate-300 mb-1.5">
                    District <span className="text-emerald-400">*</span>
                  </label>
                  <input
                    type="text"
                    required
                    placeholder="e.g. Warangal"
                    value={district}
                    onChange={(e) => setDistrict(e.target.value)}
                    className="w-full px-4 py-3 rounded-xl bg-slate-950/80 border border-slate-800 text-white placeholder-slate-500 text-sm focus:outline-none focus:border-emerald-500 transition-colors min-h-[48px]"
                  />
                </div>

                <div>
                  <label className="block text-xs font-semibold text-slate-300 mb-1.5">
                    Mandal / Tehsil <span className="text-emerald-400">*</span>
                  </label>
                  <input
                    type="text"
                    required
                    placeholder="e.g. Enumamula"
                    value={mandal}
                    onChange={(e) => setMandal(e.target.value)}
                    className="w-full px-4 py-3 rounded-xl bg-slate-950/80 border border-slate-800 text-white placeholder-slate-500 text-sm focus:outline-none focus:border-emerald-500 transition-colors min-h-[48px]"
                  />
                </div>

                <div>
                  <label className="block text-xs font-semibold text-slate-300 mb-1.5">
                    Village <span className="text-emerald-400">*</span>
                  </label>
                  <input
                    type="text"
                    required
                    placeholder="e.g. Desrajupalle"
                    value={village}
                    onChange={(e) => setVillage(e.target.value)}
                    className="w-full px-4 py-3 rounded-xl bg-slate-950/80 border border-slate-800 text-white placeholder-slate-500 text-sm focus:outline-none focus:border-emerald-500 transition-colors min-h-[48px]"
                  />
                </div>
              </div>

              <div>
                <label className="block text-xs font-semibold text-slate-300 mb-1.5">
                  Postal Pincode <span className="text-slate-500">(Optional)</span>
                </label>
                <input
                  type="text"
                  maxLength={6}
                  placeholder="e.g. 506002"
                  value={pincode}
                  onChange={(e) => setPincode(e.target.value)}
                  className="w-full px-4 py-3 rounded-xl bg-slate-950/80 border border-slate-800 text-white placeholder-slate-500 text-sm focus:outline-none focus:border-emerald-500 transition-colors min-h-[48px]"
                />
              </div>
            </div>

            {/* Form Action Footer */}
            <div className="pt-6 border-t border-slate-900 flex items-center justify-end space-x-3">
              <Link
                to="/farms"
                className="px-6 py-3 rounded-xl bg-slate-900 hover:bg-slate-800 text-slate-300 text-xs font-semibold border border-slate-800 transition-colors min-h-[48px] flex items-center justify-center"
              >
                Cancel
              </Link>

              <button
                type="submit"
                disabled={createFarmMutation.isPending}
                className="inline-flex items-center space-x-2 px-6 py-3 rounded-xl bg-emerald-500 hover:bg-emerald-400 text-slate-950 font-bold text-sm shadow-lg shadow-emerald-500/20 transition-all min-h-[48px] disabled:opacity-50"
              >
                <Save className="w-4 h-4" />
                <span>{createFarmMutation.isPending ? 'Saving Farm Record...' : 'Save Farm Record'}</span>
              </button>
            </div>

          </form>
        </GlassCard>

      </main>

      <Footer />
    </div>
  );
};
