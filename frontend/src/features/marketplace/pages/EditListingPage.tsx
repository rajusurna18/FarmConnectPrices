import React, { useState, useEffect } from 'react';
import { useParams, useNavigate, Link } from 'react-router-dom';
import { motion } from 'framer-motion';
import {
  Edit,
  ArrowLeft,
  Tag,
  Layers,
  AlertCircle,
} from 'lucide-react';
import { Navbar } from '../../../components/navigation/Navbar';
import { Footer } from '../../../components/navigation/Footer';
import {
  useListingDetailQuery,
  useUpdateListingMutation,
} from '../api/marketplaceApi';
import type { UpdateListingRequest } from '../../../types/marketplace';

export const EditListingPage: React.FC = () => {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();

  const { data: listing, isLoading, isError } = useListingDetailQuery(id || '');
  const updateMutation = useUpdateListingMutation();

  const [formData, setFormData] = useState<Partial<UpdateListingRequest>>({
    askingPrice: undefined,
    quantity: undefined,
    availableQuantity: undefined,
    unit: 'QUINTAL',
    priceUnit: 'QUINTAL',
    qualityGrade: 'GRADE_A',
    description: '',
    harvestDate: '',
    availableFrom: '',
    location: {},
  });

  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  useEffect(() => {
    if (listing) {
      setFormData({
        askingPrice: listing.askingPrice,
        quantity: listing.quantity,
        availableQuantity: listing.availableQuantity,
        unit: listing.unit || 'QUINTAL',
        priceUnit: listing.priceUnit || listing.unit || 'QUINTAL',
        qualityGrade: listing.qualityGrade || 'UNSPECIFIED',
        description: listing.description || '',
        harvestDate: listing.harvestDate || '',
        availableFrom: listing.availableFrom || '',
        location: listing.location || {},
      });
    }
  }, [listing]);

  const handleChange = (field: string, value: string | number | undefined) => {
    setFormData((prev) => ({
      ...prev,
      [field]: value,
    }));
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!id) return;
    setErrorMessage(null);

    if (formData.quantity && Number(formData.quantity) <= 0) {
      setErrorMessage('Quantity must be greater than zero.');
      return;
    }

    if (formData.askingPrice && Number(formData.askingPrice) <= 0) {
      setErrorMessage('Asking price must be greater than zero.');
      return;
    }

    try {
      const payload: UpdateListingRequest = {
        askingPrice: formData.askingPrice ? Number(formData.askingPrice) : undefined,
        quantity: formData.quantity ? Number(formData.quantity) : undefined,
        availableQuantity: formData.availableQuantity !== undefined && String(formData.availableQuantity).trim() !== ''
          ? Number(formData.availableQuantity)
          : undefined,
        unit: formData.unit,
        priceUnit: formData.priceUnit,
        qualityGrade: formData.qualityGrade,
        description: formData.description,
        harvestDate: formData.harvestDate,
        availableFrom: formData.availableFrom,
        location: formData.location,
      };

      await updateMutation.mutateAsync({ listingId: id, payload });
      navigate(`/marketplace/listings/${id}`);
    } catch (err: unknown) {
      const errorObj = err as { response?: { data?: { message?: string } | string }; message?: string };
      const serverMsg = typeof errorObj.response?.data === 'object' ? errorObj.response?.data?.message : errorObj.response?.data;
      setErrorMessage(
        serverMsg || errorObj.message || 'Failed to update produce listing.'
      );
    }
  };


  if (isLoading) {
    return (
      <div className="min-h-screen flex flex-col bg-slate-950 text-slate-100 font-sans">
        <Navbar />
        <main className="flex-1 max-w-3xl w-full mx-auto px-4 py-12 flex justify-center items-center">
          <div className="w-8 h-8 border-3 border-emerald-500 border-t-transparent rounded-full animate-spin" />
        </main>
        <Footer />
      </div>
    );
  }

  if (isError || !listing) {
    return (
      <div className="min-h-screen flex flex-col bg-slate-950 text-slate-100 font-sans">
        <Navbar />
        <main className="flex-1 max-w-3xl w-full mx-auto px-4 py-12 text-center space-y-4">
          <AlertCircle className="w-12 h-12 text-red-400 mx-auto" />
          <h2 className="text-xl font-bold text-slate-200">Listing Not Found</h2>
          <Link to="/marketplace/my-listings" className="text-xs text-emerald-400 underline">
            Back to My Listings
          </Link>
        </main>
        <Footer />
      </div>
    );
  }

  return (
    <div className="min-h-screen flex flex-col bg-slate-950 text-slate-100 font-sans antialiased select-none">
      <Navbar />

      <main className="flex-1 max-w-3xl w-full mx-auto px-4 sm:px-6 py-8">
        <div className="mb-6">
          <Link
            to={`/marketplace/listings/${id}`}
            className="inline-flex items-center space-x-2 text-xs font-semibold text-slate-400 hover:text-emerald-400 transition-colors"
          >
            <ArrowLeft className="w-4 h-4" />
            <span>Cancel and View Listing</span>
          </Link>
        </div>

        <div className="flex items-center space-x-3 mb-8">
          <span className="p-2.5 rounded-2xl bg-emerald-500/10 border border-emerald-500/20 text-emerald-400">
            <Edit className="w-7 h-7" />
          </span>
          <div>
            <h1 className="text-2xl sm:text-3xl font-extrabold text-white">
              Edit Produce Listing
            </h1>
            <p className="text-xs sm:text-sm text-slate-400 mt-0.5">
              Update pricing, available quantity, quality, or description for {listing.cropName}.
            </p>
          </div>
        </div>

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

          {/* Commodity indicator */}
          <div className="bg-slate-950/70 border border-slate-800 rounded-xl p-4 flex items-center justify-between text-xs">
            <span className="text-slate-400">Crop Commodity:</span>
            <span className="font-bold text-white text-sm">{listing.cropName}</span>
          </div>

          {/* Pricing */}
          <div className="space-y-4 pt-2">
            <h3 className="text-xs font-bold uppercase tracking-wider text-emerald-400 flex items-center space-x-2">
              <Tag className="w-4 h-4" />
              <span>Seller Asking Price</span>
            </h3>

            <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
              <div>
                <label className="block text-xs font-medium text-slate-300 mb-1.5">Asking Price (₹)</label>
                <input
                  type="number"
                  step="any"
                  min="1"
                  required
                  value={formData.askingPrice || ''}
                  onChange={(e) => handleChange('askingPrice', e.target.value)}
                  className="w-full bg-slate-950 border border-slate-800 rounded-xl px-4 py-2.5 text-sm text-slate-100 font-bold focus:outline-none focus:border-emerald-500"
                />
              </div>

              <div>
                <label className="block text-xs font-medium text-slate-300 mb-1.5">Price Unit</label>
                <select
                  value={formData.priceUnit || formData.unit}
                  onChange={(e) => handleChange('priceUnit', e.target.value)}
                  className="w-full bg-slate-950 border border-slate-800 rounded-xl px-4 py-2.5 text-sm text-slate-100 focus:outline-none focus:border-emerald-500"
                >
                  <option value="QUINTAL">Per QUINTAL</option>
                  <option value="KG">Per KG</option>
                  <option value="TON">Per TON</option>
                  <option value="BAG">Per BAG</option>
                  <option value="PIECE">Per PIECE</option>
                </select>
              </div>
            </div>
          </div>

          {/* Quantity */}
          <div className="space-y-4 pt-4 border-t border-slate-800/80">
            <h3 className="text-xs font-bold uppercase tracking-wider text-emerald-400 flex items-center space-x-2">
              <Layers className="w-4 h-4" />
              <span>Quantity & Stock</span>
            </h3>

            <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
              <div>
                <label className="block text-xs font-medium text-slate-300 mb-1.5">Total Quantity</label>
                <input
                  type="number"
                  step="any"
                  min="0.1"
                  required
                  value={formData.quantity || ''}
                  onChange={(e) => handleChange('quantity', e.target.value)}
                  className="w-full bg-slate-950 border border-slate-800 rounded-xl px-4 py-2.5 text-sm text-slate-100 focus:outline-none focus:border-emerald-500/50"
                />
              </div>

              <div>
                <label className="block text-xs font-medium text-slate-300 mb-1.5">Available Quantity</label>
                <input
                  type="number"
                  step="any"
                  min="0"
                  value={formData.availableQuantity ?? ''}
                  onChange={(e) => handleChange('availableQuantity', e.target.value)}
                  className="w-full bg-slate-950 border border-slate-800 rounded-xl px-4 py-2.5 text-sm text-slate-100 focus:outline-none focus:border-emerald-500/50"
                />
              </div>
            </div>
          </div>

          {/* Description */}
          <div className="space-y-2 pt-4 border-t border-slate-800/80">
            <label className="block text-xs font-medium text-slate-300 mb-1.5">Description</label>
            <textarea
              rows={3}
              value={formData.description || ''}
              onChange={(e) => handleChange('description', e.target.value)}
              className="w-full bg-slate-950 border border-slate-800 rounded-xl p-3.5 text-sm text-slate-100 focus:outline-none focus:border-emerald-500/50"
            />
          </div>

          {/* Actions */}
          <div className="pt-6 border-t border-slate-800 flex items-center justify-end space-x-3">
            <button
              type="button"
              onClick={() => navigate(`/marketplace/listings/${id}`)}
              className="px-5 py-2.5 rounded-xl bg-slate-800 hover:bg-slate-700 text-slate-300 font-semibold text-xs transition-all"
            >
              Cancel
            </button>

            <button
              type="submit"
              disabled={updateMutation.isPending}
              className="px-6 py-2.5 rounded-xl bg-emerald-500 hover:bg-emerald-400 text-slate-950 font-bold text-xs transition-all shadow-lg shadow-emerald-500/20 disabled:opacity-50"
            >
              {updateMutation.isPending ? 'Saving Changes...' : 'Save Changes'}
            </button>
          </div>
        </motion.form>
      </main>

      <Footer />
    </div>
  );
};
