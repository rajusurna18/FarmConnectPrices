import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useCreateFarmEconomic } from '../hooks/useFarmEconomics';
import { useFarms } from '../../farms/hooks/useFarms';
import { useCrops } from '../../prices/hooks/useCrops';
import type { ProductionCostCategory, ProductionCostItem } from '../types';
import { Plus, Trash2, ArrowLeft, Save, AlertCircle, Sprout, Calculator } from 'lucide-react';

const CATEGORIES: ProductionCostCategory[] = [
  'SEEDS',
  'FERTILIZER',
  'PESTICIDES',
  'LABOR',
  'IRRIGATION',
  'MACHINERY',
  'LAND',
  'OTHER',
];

export const CreateFarmEconomicPage: React.FC = () => {
  const navigate = useNavigate();
  const { data: farms } = useFarms();
  const { data: crops } = useCrops();
  const createMutation = useCreateFarmEconomic();

  const [farmId, setFarmId] = useState('');
  const [cropId, setCropId] = useState('');
  const [season, setSeason] = useState<'KHARIF' | 'RABI' | 'ZAID'>('KHARIF');
  const [cultivatedArea, setCultivatedArea] = useState<number>(1);
  const [cultivatedAreaUnit, setCultivatedAreaUnit] = useState<'ACRE' | 'HECTARE'>('ACRE');
  const [expectedYield, setExpectedYield] = useState<number>(10);
  const [yieldUnit, setYieldUnit] = useState<'KG' | 'QUINTAL' | 'TON'>('QUINTAL');

  const [productionCosts, setProductionCosts] = useState<ProductionCostItem[]>([
    { category: 'SEEDS', description: 'Seed input cost', amount: 0 },
    { category: 'FERTILIZER', description: 'Fertilizers & manure', amount: 0 },
    { category: 'LABOR', description: 'Sowing & harvest labor', amount: 0 },
  ]);

  const [transportationCost, setTransportationCost] = useState<number>(0);
  const [otherSellingCosts, setOtherSellingCosts] = useState<number>(0);

  const [errorMsg, setErrorMsg] = useState('');

  const handleAddCostItem = () => {
    setProductionCosts((prev) => [
      ...prev,
      { category: 'OTHER', description: '', amount: 0 },
    ]);
  };

  const handleRemoveCostItem = (index: number) => {
    setProductionCosts((prev) => prev.filter((_, i) => i !== index));
  };

  const handleCostChange = (index: number, field: keyof ProductionCostItem, value: string | number) => {
    setProductionCosts((prev) => {
      const updated = [...prev];
      updated[index] = { ...updated[index], [field]: value };
      return updated;
    });
  };

  const totalProductionCost = productionCosts.reduce((acc, c) => acc + (Number(c.amount) || 0), 0);
  const totalSellingCost = Number(transportationCost || 0) + Number(otherSellingCosts || 0);
  const totalCost = totalProductionCost + totalSellingCost;
  const breakEvenPrice = expectedYield > 0 ? (totalCost / expectedYield).toFixed(2) : '0.00';

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setErrorMsg('');

    if (!farmId) {
      setErrorMsg('Please select a farm.');
      return;
    }
    if (!cropId) {
      setErrorMsg('Please select a crop.');
      return;
    }
    if (cultivatedArea <= 0) {
      setErrorMsg('Cultivated area must be greater than zero.');
      return;
    }
    if (expectedYield <= 0) {
      setErrorMsg('Expected yield must be greater than zero.');
      return;
    }

    try {
      await createMutation.mutateAsync({
        farmId,
        cropId,
        season,
        cultivatedArea: Number(cultivatedArea),
        cultivatedAreaUnit,
        expectedYield: Number(expectedYield),
        yieldUnit,
        productionCosts: productionCosts.map((c) => ({
          category: c.category,
          description: c.description,
          amount: Number(c.amount) || 0,
        })),
        sellingCosts: {
          transportationCost: Number(transportationCost) || 0,
          otherSellingCosts: Number(otherSellingCosts) || 0,
        },
      });
      navigate('/farm-economics');
    } catch (err: unknown) {
      const errorObj = err as { response?: { data?: string }; message?: string };
      setErrorMsg(errorObj.response?.data || errorObj.message || 'Failed to save economic record.');
    }
  };

  return (
    <div className="max-w-4xl mx-auto space-y-6">
      {/* Top Header */}
      <div className="flex items-center gap-4">
        <button
          onClick={() => navigate('/farm-economics')}
          className="p-2 bg-slate-800 hover:bg-slate-700 text-slate-300 rounded-lg transition-colors"
        >
          <ArrowLeft className="w-5 h-5" />
        </button>
        <div>
          <h1 className="text-2xl font-bold text-white flex items-center gap-2">
            <Sprout className="text-emerald-400 w-6 h-6" />
            New Crop Production Economics
          </h1>
          <p className="text-slate-400 text-sm">Enter production costs and expected yield for your farm crop.</p>
        </div>
      </div>

      {errorMsg && (
        <div className="p-4 bg-red-500/10 border border-red-500/20 rounded-xl text-red-400 flex items-center gap-3">
          <AlertCircle className="w-5 h-5 shrink-0" />
          <span>{errorMsg}</span>
        </div>
      )}

      <form onSubmit={handleSubmit} className="space-y-6">
        {/* Farm & Crop Metadata */}
        <div className="bg-slate-800/60 border border-slate-700/60 rounded-xl p-6 space-y-4 shadow-md">
          <h2 className="text-lg font-semibold text-white border-b border-slate-700/60 pb-3 flex items-center gap-2">
            <Sprout className="w-5 h-5 text-emerald-400" />
            Crop & Allocation Details
          </h2>

          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
            <div>
              <label className="block text-xs font-medium text-slate-300 mb-1">Select Farm *</label>
              <select
                value={farmId}
                onChange={(e) => setFarmId(e.target.value)}
                required
                className="w-full px-4 py-2.5 bg-slate-900/80 border border-slate-700 text-white rounded-lg text-sm focus:outline-none focus:border-emerald-500"
              >
                <option value="">-- Choose Farm --</option>
                {farms?.map((f) => (
                  <option key={f.id} value={f.id}>
                    {f.name} ({f.landArea} {f.landAreaUnit})
                  </option>
                ))}
              </select>
            </div>

            <div>
              <label className="block text-xs font-medium text-slate-300 mb-1">Select Crop *</label>
              <select
                value={cropId}
                onChange={(e) => setCropId(e.target.value)}
                required
                className="w-full px-4 py-2.5 bg-slate-900/80 border border-slate-700 text-white rounded-lg text-sm focus:outline-none focus:border-emerald-500"
              >
                <option value="">-- Choose Crop --</option>
                {crops?.map((c) => (
                  <option key={c.id} value={c.id}>
                    {c.name} ({c.category})
                  </option>
                ))}
              </select>
            </div>

            <div>
              <label className="block text-xs font-medium text-slate-300 mb-1">Crop Season *</label>
              <select
                value={season}
                onChange={(e) => setSeason(e.target.value as 'KHARIF' | 'RABI' | 'ZAID')}
                className="w-full px-4 py-2.5 bg-slate-900/80 border border-slate-700 text-white rounded-lg text-sm focus:outline-none focus:border-emerald-500"
              >
                <option value="KHARIF">KHARIF (Monsoon)</option>
                <option value="RABI">RABI (Winter)</option>
                <option value="ZAID">ZAID (Summer)</option>
              </select>
            </div>

            <div className="grid grid-cols-2 gap-2">
              <div>
                <label className="block text-xs font-medium text-slate-300 mb-1">Cultivated Area *</label>
                <input
                  type="number"
                  step="0.1"
                  min="0.1"
                  value={cultivatedArea}
                  onChange={(e) => setCultivatedArea(parseFloat(e.target.value) || 0)}
                  required
                  className="w-full px-4 py-2.5 bg-slate-900/80 border border-slate-700 text-white rounded-lg text-sm focus:outline-none focus:border-emerald-500"
                />
              </div>
              <div>
                <label className="block text-xs font-medium text-slate-300 mb-1">Area Unit</label>
                <select
                  value={cultivatedAreaUnit}
                  onChange={(e) => setCultivatedAreaUnit(e.target.value as 'ACRE' | 'HECTARE')}
                  className="w-full px-4 py-2.5 bg-slate-900/80 border border-slate-700 text-white rounded-lg text-sm focus:outline-none focus:border-emerald-500"
                >
                  <option value="ACRE">ACRE</option>
                  <option value="HECTARE">HECTARE</option>
                </select>
              </div>
            </div>

            <div className="grid grid-cols-2 gap-2 sm:col-span-2">
              <div>
                <label className="block text-xs font-medium text-slate-300 mb-1">Expected Yield *</label>
                <input
                  type="number"
                  step="0.1"
                  min="0.1"
                  value={expectedYield}
                  onChange={(e) => setExpectedYield(parseFloat(e.target.value) || 0)}
                  required
                  className="w-full px-4 py-2.5 bg-slate-900/80 border border-slate-700 text-white rounded-lg text-sm focus:outline-none focus:border-emerald-500"
                />
              </div>
              <div>
                <label className="block text-xs font-medium text-slate-300 mb-1">Yield Unit *</label>
                <select
                  value={yieldUnit}
                  onChange={(e) => setYieldUnit(e.target.value as 'QUINTAL' | 'KG' | 'TON')}
                  className="w-full px-4 py-2.5 bg-slate-900/80 border border-slate-700 text-white rounded-lg text-sm focus:outline-none focus:border-emerald-500"
                >
                  <option value="QUINTAL">QUINTAL (100 kg)</option>
                  <option value="KG">KG</option>
                  <option value="TON">TON (1000 kg)</option>
                </select>
              </div>
            </div>
          </div>
        </div>

        {/* Production Costs Section */}
        <div className="bg-slate-800/60 border border-slate-700/60 rounded-xl p-6 space-y-4 shadow-md">
          <div className="flex items-center justify-between border-b border-slate-700/60 pb-3">
            <h2 className="text-lg font-semibold text-white flex items-center gap-2">
              <Calculator className="w-5 h-5 text-amber-400" />
              Production Cost Items (Inputs & Operations)
            </h2>
            <button
              type="button"
              onClick={handleAddCostItem}
              className="inline-flex items-center gap-1.5 px-3 py-1.5 bg-slate-700 hover:bg-slate-600 text-slate-200 text-xs font-medium rounded-lg transition-colors"
            >
              <Plus className="w-4 h-4" />
              Add Cost Category
            </button>
          </div>

          <div className="space-y-3">
            {productionCosts.map((item, index) => (
              <div key={index} className="grid grid-cols-1 sm:grid-cols-12 gap-3 items-center bg-slate-900/50 p-3 rounded-lg border border-slate-700/40">
                <div className="sm:col-span-4">
                  <select
                    value={item.category}
                    onChange={(e) => handleCostChange(index, 'category', e.target.value as ProductionCostCategory)}
                    className="w-full px-3 py-2 bg-slate-900 border border-slate-700 text-white rounded-lg text-xs focus:outline-none focus:border-emerald-500"
                  >
                    {CATEGORIES.map((cat) => (
                      <option key={cat} value={cat}>
                        {cat}
                      </option>
                    ))}
                  </select>
                </div>

                <div className="sm:col-span-4">
                  <input
                    type="text"
                    placeholder="Description (e.g. Rice seeds 20kg)"
                    value={item.description || ''}
                    onChange={(e) => handleCostChange(index, 'description', e.target.value)}
                    className="w-full px-3 py-2 bg-slate-900 border border-slate-700 text-white rounded-lg text-xs focus:outline-none focus:border-emerald-500 placeholder-slate-500"
                  />
                </div>

                <div className="sm:col-span-3">
                  <div className="relative">
                    <span className="absolute left-3 top-2 text-xs text-slate-400">₹</span>
                    <input
                      type="number"
                      min="0"
                      step="1"
                      value={item.amount}
                      onChange={(e) => handleCostChange(index, 'amount', parseFloat(e.target.value) || 0)}
                      className="w-full pl-7 pr-3 py-2 bg-slate-900 border border-slate-700 text-white rounded-lg text-xs focus:outline-none focus:border-emerald-500 font-semibold text-amber-400"
                    />
                  </div>
                </div>

                <div className="sm:col-span-1 text-right">
                  <button
                    type="button"
                    onClick={() => handleRemoveCostItem(index)}
                    className="p-1.5 text-slate-500 hover:text-red-400 transition-colors"
                  >
                    <Trash2 className="w-4 h-4" />
                  </button>
                </div>
              </div>
            ))}
          </div>

          <div className="flex justify-between items-center pt-2 text-sm text-slate-300 font-medium">
            <span>Subtotal Production Cost:</span>
            <span className="text-amber-400 font-bold text-base">₹{totalProductionCost.toLocaleString('en-IN')}</span>
          </div>
        </div>

        {/* Selling Costs Section */}
        <div className="bg-slate-800/60 border border-slate-700/60 rounded-xl p-6 space-y-4 shadow-md">
          <h2 className="text-lg font-semibold text-white border-b border-slate-700/60 pb-3">
            Estimated Selling Expenses
          </h2>

          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
            <div>
              <label className="block text-xs font-medium text-slate-300 mb-1">Transportation Cost (₹)</label>
              <input
                type="number"
                min="0"
                value={transportationCost}
                onChange={(e) => setTransportationCost(parseFloat(e.target.value) || 0)}
                className="w-full px-4 py-2.5 bg-slate-900/80 border border-slate-700 text-white rounded-lg text-sm focus:outline-none focus:border-emerald-500"
              />
            </div>

            <div>
              <label className="block text-xs font-medium text-slate-300 mb-1">Other Selling Costs (Loading, Mandi Fees ₹)</label>
              <input
                type="number"
                min="0"
                value={otherSellingCosts}
                onChange={(e) => setOtherSellingCosts(parseFloat(e.target.value) || 0)}
                className="w-full px-4 py-2.5 bg-slate-900/80 border border-slate-700 text-white rounded-lg text-sm focus:outline-none focus:border-emerald-500"
              />
            </div>
          </div>
        </div>

        {/* Calculation Summary Bar */}
        <div className="bg-emerald-950/40 border border-emerald-500/30 rounded-xl p-5 flex flex-col sm:flex-row items-center justify-between gap-4">
          <div>
            <div className="text-xs text-emerald-400 font-medium">Calculated Break-Even Selling Price</div>
            <div className="text-2xl font-extrabold text-white">
              ₹{breakEvenPrice} <span className="text-sm font-normal text-emerald-300">/ {yieldUnit}</span>
            </div>
            <div className="text-xs text-slate-400 mt-0.5">Price per unit required to recover production + selling costs</div>
          </div>

          <div className="text-right">
            <div className="text-xs text-slate-400">Total Crop Expenses</div>
            <div className="text-xl font-bold text-amber-400">₹{totalCost.toLocaleString('en-IN')}</div>
          </div>
        </div>

        {/* Form Controls */}
        <div className="flex items-center justify-end gap-3 pt-2">
          <button
            type="button"
            onClick={() => navigate('/farm-economics')}
            className="px-5 py-2.5 bg-slate-700 hover:bg-slate-600 text-slate-200 text-sm font-medium rounded-lg transition-colors"
          >
            Cancel
          </button>
          <button
            type="submit"
            disabled={createMutation.isPending}
            className="inline-flex items-center gap-2 px-6 py-2.5 bg-emerald-600 hover:bg-emerald-500 text-white font-semibold text-sm rounded-lg transition-colors shadow-lg shadow-emerald-900/30 disabled:opacity-50"
          >
            <Save className="w-4 h-4" />
            {createMutation.isPending ? 'Saving...' : 'Save Economic Record'}
          </button>
        </div>
      </form>
    </div>
  );
};
