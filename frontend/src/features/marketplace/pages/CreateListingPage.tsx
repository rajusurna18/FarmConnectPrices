import React, { useState } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { motion } from 'framer-motion';
import {
  PlusCircle,
  ArrowLeft,
  ShoppingBag,
  Tag,
  MapPin,
  Calendar,
  Info,
  AlertCircle,
} from 'lucide-react';
import { Navbar } from '../../../components/navigation/Navbar';
import { Footer } from '../../../components/navigation/Footer';
import { useCrops } from '../../prices/hooks/useCrops';
import { useLocationCascade } from '../../markets/hooks/useLocationCascade';
import { useCreateListingMutation } from '../api/marketplaceApi';
import type { CreateListingRequest } from '../../../types/marketplace';

export const CreateListingPage: React.FC = () => {
  const navigate = useNavigate();
  const { data: cropList = [] } = useCrops();

  const [formData, setFormData] = useState<Partial<CreateListingRequest>>({
    cropId: '',
    quantity: undefined,
    availableQuantity: undefined,
    unit: 'QUINTAL',
    askingPrice: undefined,
    priceUnit: 'QUINTAL',
    qualityGrade: 'GRADE_A',
    status: 'ACTIVE',
    location: {
      state: '',
      district: '',
      mandal: '',
      village: '',
    },
    description: '',
    harvestDate: '',
    availableFrom: '',
  });

  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  const { states = [], districts = [] } = useLocationCascade(
    formData.location?.state,
    formData.location?.district
  );

  const createMutation = useCreateListingMutation();

  const handleChange = (field: string, value: string | number | undefined) => {
    setFormData((prev) => ({
      ...prev,
      [field]: value,
    }));
  };

  const handleLocationChange = (locField: string, value: string) => {
    setFormData((prev) => ({
      ...prev,
      location: {
        ...prev.location,
        [locField]: value,
      },
    }));
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setErrorMessage(null);

    // Form Validations
    if (!formData.cropId) {
      setErrorMessage('Please select a crop commodity.');
      return;
    }

    if (!formData.quantity || formData.quantity <= 0) {
      setErrorMessage('Quantity must be greater than zero.');
      return;
    }

    if (!formData.askingPrice || formData.askingPrice <= 0) {
      setErrorMessage('Asking price must be greater than zero.');
      return;
    }

    const qty = Number(formData.quantity);
    const availQty = formData.availableQuantity !== undefined && formData.availableQuantity !== null && String(formData.availableQuantity).trim() !== ''
      ? Number(formData.availableQuantity)
      : qty;

    if (availQty < 0 || availQty > qty) {
      setErrorMessage('Available quantity cannot exceed total quantity and must be non-negative.');
      return;
    }

    try {
      const payload: CreateListingRequest = {
        cropId: formData.cropId,
        quantity: qty,
        availableQuantity: availQty,
        unit: formData.unit || 'QUINTAL',
        askingPrice: Number(formData.askingPrice),
        priceUnit: formData.priceUnit || formData.unit || 'QUINTAL',
        location: formData.location,
        qualityGrade: formData.qualityGrade,
        harvestDate: formData.harvestDate || undefined,
        availableFrom: formData.availableFrom || undefined,
        status: formData.status,
        description: formData.description || undefined,
      };

      await createMutation.mutateAsync(payload);
      navigate('/marketplace/my-listings');
    } catch (err: unknown) {
      const errorObj = err as { response?: { data?: { message?: string } | string }; message?: string };
      const serverMsg = typeof errorObj.response?.data === 'object' ? errorObj.response?.data?.message : errorObj.response?.data;
      setErrorMessage(
        serverMsg || errorObj.message || 'Failed to create produce listing.'
      );
    }
  };


  return (
    <div className="min-h-screen flex flex-col bg-slate-950 text-slate-100 font-sans antialiased select-none">
      <Navbar />

      <main className="flex-1 max-w-3xl w-full mx-auto px-4 sm:px-6 py-8">
        {/* Back link */}
        <div className="mb-6">
          <Link
            to="/marketplace/my-listings"
            className="inline-flex items-center space-x-2 text-xs font-semibold text-slate-400 hover:text-emerald-400 transition-colors"
          >
            <ArrowLeft className="w-4 h-4" />
            <span>Back to My Listings</span>
          </Link>
        </div>

        {/* Page Title */}
        <div className="flex items-center space-x-3 mb-8">
          <span className="p-2.5 rounded-2xl bg-emerald-500/10 border border-emerald-500/20 text-emerald-400">
            <PlusCircle className="w-7 h-7" />
          </span>
          <div>
            <h1 className="text-2xl sm:text-3xl font-extrabold text-white">
              Create Produce Listing
            </h1>
            <p className="text-xs sm:text-sm text-slate-400 mt-0.5">
              List your harvested or upcoming agricultural produce for marketplace discovery.
            </p>
          </div>
        </div>

        {/* Form Container */}
        <motion.form
          initial={{ opacity: 0, y: 12 }}
          animate={{ opacity: 1, y: 0 }}
          onSubmit={handleSubmit}
          className="bg-slate-900/80 border border-slate-800 rounded-3xl p-6 sm:p-8 shadow-2xl space-y-6 backdrop-blur-md"
        >
          {errorMessage && (
            <div className="p-4 rounded-xl bg-red-500/10 border border-red-500/20 text-red-400 text-xs font-semibold flex items-center space-x-2">
              <AlertCircle className="w-4 h-4 flex-shrink-0" />
              <span>{errorMessage}</span>
            </div>
          )}

          {/* Section 1: Crop & Quantity */}
          <div className="space-y-4 pt-2">
            <h3 className="text-xs font-bold uppercase tracking-wider text-emerald-400 flex items-center space-x-2">
              <ShoppingBag className="w-4 h-4" />
              <span>Produce Details</span>
            </h3>

            <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
              {/* Crop Select */}
              <div className="sm:col-span-2">
                <label className="block text-xs font-medium text-slate-300 mb-1.5">
                  Select Crop Commodity <span className="text-emerald-400">*</span>
                </label>
                <select
                  required
                  value={formData.cropId}
                  onChange={(e) => handleChange('cropId', e.target.value)}
                  className="w-full bg-slate-950 border border-slate-800 rounded-xl px-4 py-2.5 text-sm text-slate-100 focus:outline-none focus:border-emerald-500/50"
                >
                  <option value="">-- Choose Crop --</option>
                  {cropList.map((c) => (
                    <option key={c.id} value={c.id}>
                      {c.name} ({c.category})
                    </option>
                  ))}
                </select>
              </div>

              {/* Total Quantity */}
              <div>
                <label className="block text-xs font-medium text-slate-300 mb-1.5">
                  Total Quantity <span className="text-emerald-400">*</span>
                </label>
                <input
                  type="number"
                  step="any"
                  min="0.1"
                  required
                  placeholder="e.g. 500"
                  value={formData.quantity || ''}
                  onChange={(e) => handleChange('quantity', e.target.value)}
                  className="w-full bg-slate-950 border border-slate-800 rounded-xl px-4 py-2.5 text-sm text-slate-100 focus:outline-none focus:border-emerald-500/50"
                />
              </div>

              {/* Quantity Unit */}
              <div>
                <label className="block text-xs font-medium text-slate-300 mb-1.5">
                  Unit <span className="text-emerald-400">*</span>
                </label>
                <select
                  value={formData.unit}
                  onChange={(e) => {
                    handleChange('unit', e.target.value);
                    if (!formData.priceUnit) handleChange('priceUnit', e.target.value);
                  }}
                  className="w-full bg-slate-950 border border-slate-800 rounded-xl px-4 py-2.5 text-sm text-slate-100 focus:outline-none focus:border-emerald-500/50"
                >
                  <option value="QUINTAL">QUINTAL (100 kg)</option>
                  <option value="KG">KG (Kilogram)</option>
                  <option value="TON">TON / TONNE</option>
                  <option value="BAG">BAG</option>
                  <option value="PIECE">PIECE</option>
                  <option value="LITRE">LITRE</option>
                </select>
              </div>

              {/* Available Quantity */}
              <div className="sm:col-span-2">
                <label className="block text-xs font-medium text-slate-300 mb-1.5">
                  Available Quantity (Optional)
                </label>
                <input
                  type="number"
                  step="any"
                  min="0"
                  placeholder="Defaults to total quantity if left empty"
                  value={formData.availableQuantity ?? ''}
                  onChange={(e) => handleChange('availableQuantity', e.target.value)}
                  className="w-full bg-slate-950 border border-slate-800 rounded-xl px-4 py-2.5 text-sm text-slate-100 focus:outline-none focus:border-emerald-500/50"
                />
              </div>
            </div>
          </div>

          {/* Section 2: Pricing */}
          <div className="space-y-4 pt-4 border-t border-slate-800/80">
            <h3 className="text-xs font-bold uppercase tracking-wider text-emerald-400 flex items-center space-x-2">
              <Tag className="w-4 h-4" />
              <span>Seller Asking Price</span>
            </h3>

            <div className="bg-slate-950/90 border border-emerald-500/30 rounded-2xl p-4 space-y-3">
              <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                <div>
                  <label className="block text-xs font-medium text-slate-300 mb-1.5">
                    Asking Price (₹) <span className="text-emerald-400">*</span>
                  </label>
                  <input
                    type="number"
                    step="any"
                    min="1"
                    required
                    placeholder="e.g. 2400"
                    value={formData.askingPrice || ''}
                    onChange={(e) => handleChange('askingPrice', e.target.value)}
                    className="w-full bg-slate-900 border border-slate-700 rounded-xl px-4 py-2.5 text-sm text-slate-100 font-bold focus:outline-none focus:border-emerald-500"
                  />
                </div>

                <div>
                  <label className="block text-xs font-medium text-slate-300 mb-1.5">
                    Per Unit
                  </label>
                  <select
                    value={formData.priceUnit || formData.unit}
                    onChange={(e) => handleChange('priceUnit', e.target.value)}
                    className="w-full bg-slate-900 border border-slate-700 rounded-xl px-4 py-2.5 text-sm text-slate-100 focus:outline-none focus:border-emerald-500"
                  >
                    <option value="QUINTAL">Per QUINTAL</option>
                    <option value="KG">Per KG</option>
                    <option value="TON">Per TON</option>
                    <option value="BAG">Per BAG</option>
                    <option value="PIECE">Per PIECE</option>
                  </select>
                </div>
              </div>

              <div className="flex items-center space-x-2 text-[11px] text-slate-400 pt-1">
                <Info className="w-4 h-4 text-emerald-400 flex-shrink-0" />
                <span>
                  Your asking price is set directly by you. Verified market prices are displayed separately as reference data.
                </span>
              </div>
            </div>
          </div>

          {/* Section 3: Region & Quality */}
          <div className="space-y-4 pt-4 border-t border-slate-800/80">
            <h3 className="text-xs font-bold uppercase tracking-wider text-emerald-400 flex items-center space-x-2">
              <MapPin className="w-4 h-4" />
              <span>Region & Quality Grade</span>
            </h3>

            <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
              <div>
                <label className="block text-xs font-medium text-slate-300 mb-1.5">State</label>
                <select
                  value={formData.location?.state || ''}
                  onChange={(e) => handleLocationChange('state', e.target.value)}
                  className="w-full bg-slate-950 border border-slate-800 rounded-xl px-4 py-2.5 text-sm text-slate-100 focus:outline-none focus:border-emerald-500/50"
                >
                  <option value="">Select State</option>
                  {states.map((s) => (
                    <option key={s} value={s}>{s}</option>
                  ))}
                </select>
              </div>

              <div>
                <label className="block text-xs font-medium text-slate-300 mb-1.5">District</label>
                <select
                  value={formData.location?.district || ''}
                  onChange={(e) => handleLocationChange('district', e.target.value)}
                  className="w-full bg-slate-950 border border-slate-800 rounded-xl px-4 py-2.5 text-sm text-slate-100 focus:outline-none focus:border-emerald-500/50"
                >
                  <option value="">Select District</option>
                  {districts.map((d) => (
                    <option key={d} value={d}>{d}</option>
                  ))}
                </select>
              </div>

              <div>
                <label className="block text-xs font-medium text-slate-300 mb-1.5">Quality Grade</label>
                <select
                  value={formData.qualityGrade}
                  onChange={(e) => handleChange('qualityGrade', e.target.value)}
                  className="w-full bg-slate-950 border border-slate-800 rounded-xl px-4 py-2.5 text-sm text-slate-100 focus:outline-none focus:border-emerald-500/50"
                >
                  <option value="PREMIUM">PREMIUM</option>
                  <option value="GRADE_A">GRADE A</option>
                  <option value="GRADE_B">GRADE B</option>
                  <option value="STANDARD">STANDARD</option>
                  <option value="UNSPECIFIED">UNSPECIFIED</option>
                </select>
              </div>
            </div>
          </div>

          {/* Section 4: Harvest & Description */}
          <div className="space-y-4 pt-4 border-t border-slate-800/80">
            <h3 className="text-xs font-bold uppercase tracking-wider text-emerald-400 flex items-center space-x-2">
              <Calendar className="w-4 h-4" />
              <span>Availability & Description</span>
            </h3>

            <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
              <div>
                <label className="block text-xs font-medium text-slate-300 mb-1.5">Harvest Date</label>
                <input
                  type="date"
                  value={formData.harvestDate}
                  onChange={(e) => handleChange('harvestDate', e.target.value)}
                  className="w-full bg-slate-950 border border-slate-800 rounded-xl px-4 py-2.5 text-sm text-slate-100 focus:outline-none focus:border-emerald-500/50"
                />
              </div>

              <div>
                <label className="block text-xs font-medium text-slate-300 mb-1.5">Available From</label>
                <input
                  type="date"
                  value={formData.availableFrom}
                  onChange={(e) => handleChange('availableFrom', e.target.value)}
                  className="w-full bg-slate-950 border border-slate-800 rounded-xl px-4 py-2.5 text-sm text-slate-100 focus:outline-none focus:border-emerald-500/50"
                />
              </div>
            </div>

            <div>
              <label className="block text-xs font-medium text-slate-300 mb-1.5">Description / Seller Notes</label>
              <textarea
                rows={3}
                placeholder="Provide details such as variety, organic status, packaging, or harvest condition..."
                value={formData.description}
                onChange={(e) => handleChange('description', e.target.value)}
                className="w-full bg-slate-950 border border-slate-800 rounded-xl p-3.5 text-sm text-slate-100 focus:outline-none focus:border-emerald-500/50"
              />
            </div>
          </div>

          {/* Actions */}
          <div className="pt-6 border-t border-slate-800 flex items-center justify-end space-x-3">
            <button
              type="button"
              onClick={() => navigate('/marketplace/my-listings')}
              className="px-5 py-2.5 rounded-xl bg-slate-800 hover:bg-slate-700 text-slate-300 font-semibold text-xs transition-all"
            >
              Cancel
            </button>

            <button
              type="submit"
              disabled={createMutation.isPending}
              className="px-6 py-2.5 rounded-xl bg-emerald-500 hover:bg-emerald-400 text-slate-950 font-bold text-xs transition-all shadow-lg shadow-emerald-500/20 disabled:opacity-50"
            >
              {createMutation.isPending ? 'Creating Listing...' : 'Publish Produce Listing'}
            </button>
          </div>
        </motion.form>
      </main>

      <Footer />
    </div>
  );
};
