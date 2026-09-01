import React, { useState } from 'react';
import { Link } from 'react-router-dom';
import { useFarmEconomics, useDeleteFarmEconomic } from '../hooks/useFarmEconomics';
import {
  TrendingUp,
  DollarSign,
  Plus,
  Trash2,
  Edit,
  Eye,
  BarChart2,
  AlertCircle,
  Sprout,
  ShieldAlert,
} from 'lucide-react';

export const FarmEconomicsDashboardPage: React.FC = () => {
  const { data: records, isLoading, error } = useFarmEconomics();
  const deleteMutation = useDeleteFarmEconomic();
  const [searchTerm, setSearchTerm] = useState('');

  const handleDelete = async (id: string, name?: string) => {
    if (confirm(`Are you sure you want to delete economic record for ${name || 'this crop'}?`)) {
      await deleteMutation.mutateAsync(id);
    }
  };

  const filteredRecords = (records || []).filter(
    (r) =>
      r.cropName?.toLowerCase().includes(searchTerm.toLowerCase()) ||
      r.farmName?.toLowerCase().includes(searchTerm.toLowerCase()) ||
      r.season.toLowerCase().includes(searchTerm.toLowerCase())
  );

  const totalProductionCostSum = (records || []).reduce((acc, r) => acc + (r.totalProductionCost || 0), 0);
  const totalCostSum = (records || []).reduce((acc, r) => acc + (r.totalCost || 0), 0);

  if (isLoading) {
    return (
      <div className="flex items-center justify-center min-h-[400px]">
        <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-emerald-500"></div>
      </div>
    );
  }

  if (error) {
    return (
      <div className="p-6 bg-red-500/10 border border-red-500/20 rounded-xl text-red-400 flex items-center gap-3">
        <AlertCircle className="w-6 h-6" />
        <span>Failed to load farm economics records. Please try again later.</span>
      </div>
    );
  }

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold text-white flex items-center gap-2">
            <Sprout className="text-emerald-400 w-7 h-7" />
            Farm Economics & Production Cost
          </h1>
          <p className="text-slate-400 text-sm mt-1">
            Track crop production costs, selling expenses, break-even selling prices, and estimated net profitability.
          </p>
        </div>
        <Link
          to="/farm-economics/new"
          className="inline-flex items-center justify-center gap-2 px-4 py-2.5 bg-emerald-600 hover:bg-emerald-500 text-white font-medium rounded-lg transition-colors shadow-lg shadow-emerald-900/30"
        >
          <Plus className="w-5 h-5" />
          Add Crop Economics
        </Link>
      </div>

      {/* Overview Cards */}
      <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
        <div className="p-5 bg-slate-800/60 border border-slate-700/50 rounded-xl">
          <div className="flex items-center justify-between text-slate-400 mb-2">
            <span className="text-sm font-medium">Active Economic Records</span>
            <Sprout className="w-5 h-5 text-emerald-400" />
          </div>
          <p className="text-3xl font-bold text-white">{records?.length || 0}</p>
          <p className="text-xs text-slate-400 mt-1">Crop production budgets tracked</p>
        </div>

        <div className="p-5 bg-slate-800/60 border border-slate-700/50 rounded-xl">
          <div className="flex items-center justify-between text-slate-400 mb-2">
            <span className="text-sm font-medium">Total Production Costs</span>
            <DollarSign className="w-5 h-5 text-amber-400" />
          </div>
          <p className="text-3xl font-bold text-amber-400">₹{totalProductionCostSum.toLocaleString('en-IN')}</p>
          <p className="text-xs text-slate-400 mt-1">Input, labor, land & machinery costs</p>
        </div>

        <div className="p-5 bg-slate-800/60 border border-slate-700/50 rounded-xl">
          <div className="flex items-center justify-between text-slate-400 mb-2">
            <span className="text-sm font-medium">Total Farm Expenses</span>
            <TrendingUp className="w-5 h-5 text-blue-400" />
          </div>
          <p className="text-3xl font-bold text-blue-400">₹{totalCostSum.toLocaleString('en-IN')}</p>
          <p className="text-xs text-slate-400 mt-1">Production + selling costs</p>
        </div>
      </div>

      {/* Filter / Search Bar */}
      <div className="flex items-center justify-between bg-slate-800/40 p-4 rounded-xl border border-slate-700/40">
        <input
          type="text"
          placeholder="Filter by farm name, crop, or season..."
          value={searchTerm}
          onChange={(e) => setSearchTerm(e.target.value)}
          className="w-full sm:w-80 px-4 py-2 bg-slate-900/60 border border-slate-700 text-white rounded-lg text-sm focus:outline-none focus:border-emerald-500 placeholder-slate-500"
        />
        <span className="text-xs text-slate-400 hidden sm:block">
          Showing {filteredRecords.length} of {records?.length || 0} records
        </span>
      </div>

      {/* Records Table / List */}
      {filteredRecords.length === 0 ? (
        <div className="text-center py-12 bg-slate-800/30 rounded-xl border border-slate-700/30">
          <Sprout className="w-12 h-12 text-slate-600 mx-auto mb-3" />
          <h3 className="text-lg font-semibold text-slate-300">No Economic Records Found</h3>
          <p className="text-slate-500 text-sm mt-1 mb-4">
            Start by creating a farm production cost record to evaluate crop profitability.
          </p>
          <Link
            to="/farm-economics/new"
            className="inline-flex items-center gap-2 px-4 py-2 bg-emerald-600 hover:bg-emerald-500 text-white text-sm font-medium rounded-lg transition-colors"
          >
            <Plus className="w-4 h-4" />
            Create First Record
          </Link>
        </div>
      ) : (
        <div className="grid grid-cols-1 gap-4">
          {filteredRecords.map((r) => (
            <div
              key={r.id}
              className="bg-slate-800/60 border border-slate-700/60 hover:border-slate-600 rounded-xl p-5 transition-all shadow-md"
            >
              <div className="flex flex-col md:flex-row md:items-center justify-between gap-4">
                {/* Left Info */}
                <div className="space-y-1">
                  <div className="flex items-center gap-3">
                    <h3 className="text-lg font-bold text-white">{r.cropName || 'Crop Record'}</h3>
                    <span className="px-2.5 py-0.5 rounded-full text-xs font-semibold bg-emerald-500/20 text-emerald-300 border border-emerald-500/30">
                      {r.season}
                    </span>
                    <span className="text-xs text-slate-400 font-medium">Farm: {r.farmName || r.farmId}</span>
                  </div>
                  <div className="flex flex-wrap gap-4 text-xs text-slate-400 pt-1">
                    <span>
                      Cultivated: <strong className="text-slate-200">{r.cultivatedArea} {r.cultivatedAreaUnit}</strong>
                    </span>
                    <span>
                      Expected Yield: <strong className="text-slate-200">{r.expectedYield} {r.yieldUnit}</strong>
                    </span>
                  </div>
                </div>

                {/* Right Costs & Metrics */}
                <div className="flex flex-wrap items-center gap-6">
                  <div className="text-left md:text-right">
                    <div className="text-xs text-slate-400">Total Production Cost</div>
                    <div className="text-base font-bold text-amber-400">₹{(r.totalProductionCost || 0).toLocaleString('en-IN')}</div>
                  </div>

                  <div className="text-left md:text-right">
                    <div className="text-xs text-slate-400">Total Selling Cost</div>
                    <div className="text-base font-semibold text-blue-400">₹{(r.totalSellingCost || 0).toLocaleString('en-IN')}</div>
                  </div>

                  <div className="text-left md:text-right">
                    <div className="text-xs text-slate-400">Break-Even Selling Price</div>
                    <div className="text-base font-bold text-emerald-400">
                      ₹{(r.breakEvenSellingPrice || 0).toLocaleString('en-IN')} / {r.yieldUnit}
                    </div>
                  </div>

                  {/* Actions */}
                  <div className="flex items-center gap-2 border-t md:border-t-0 border-slate-700/50 pt-3 md:pt-0 w-full md:w-auto justify-end">
                    <Link
                      to={`/farm-economics/${r.id}`}
                      className="p-2 bg-emerald-600/20 hover:bg-emerald-600/40 text-emerald-300 rounded-lg transition-colors title='View Detail & Evaluate'"
                    >
                      <Eye className="w-4 h-4" />
                    </Link>
                    <Link
                      to={`/farm-economics/compare?recordId=${r.id}`}
                      className="p-2 bg-blue-600/20 hover:bg-blue-600/40 text-blue-300 rounded-lg transition-colors"
                      title="Compare Markets"
                    >
                      <BarChart2 className="w-4 h-4" />
                    </Link>
                    <Link
                      to={`/farm-economics/${r.id}/edit`}
                      className="p-2 bg-slate-700/50 hover:bg-slate-700 text-slate-300 rounded-lg transition-colors"
                      title="Edit Record"
                    >
                      <Edit className="w-4 h-4" />
                    </Link>
                    <button
                      onClick={() => handleDelete(r.id, r.cropName)}
                      className="p-2 bg-red-500/20 hover:bg-red-500/40 text-red-400 rounded-lg transition-colors"
                      title="Delete Record"
                    >
                      <Trash2 className="w-4 h-4" />
                    </button>
                  </div>
                </div>
              </div>
            </div>
          ))}
        </div>
      )}

      {/* Disclaimers & Notes */}
      <div className="p-4 bg-slate-900/60 border border-slate-800 rounded-xl text-xs text-slate-400 flex items-start gap-2.5">
        <ShieldAlert className="w-4 h-4 text-amber-400 shrink-0 mt-0.5" />
        <div>
          <strong className="text-slate-300">Estimates Disclaimer:</strong> Profitability, break-even price, and ROI metrics are calculated estimates based on entered cost data and verified market prices. They do not constitute guaranteed financial returns.
        </div>
      </div>
    </div>
  );
};
