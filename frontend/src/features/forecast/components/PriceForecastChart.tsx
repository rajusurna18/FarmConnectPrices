import React from 'react';
import {
  ComposedChart,
  Line,
  Area,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  ResponsiveContainer,
} from 'recharts';
import type { ForecastResponse } from '../services/forecastApi';

interface PriceForecastChartProps {
  forecast: ForecastResponse;
  historicalPrices?: { priceDate: string; modalPrice: number }[];
}

export const PriceForecastChart: React.FC<PriceForecastChartProps> = ({ forecast, historicalPrices = [] }) => {
  if (!forecast || forecast.forecastPrice == null) {
    return (
      <div className="h-64 flex items-center justify-center bg-slate-900/60 rounded-xl border border-slate-800 text-slate-400 text-sm">
        Insufficient data for visualization chart
      </div>
    );
  }

  // Combine historical trend points and target forecast point into single dataset
  const chartData: {
    date: string;
    historical: number | null;
    forecast: number | null;
    lowerBound: number | null;
    upperBound: number | null;
    range: [number, number] | null;
  }[] = historicalPrices.map((p) => ({
    date: p.priceDate,
    historical: p.modalPrice,
    forecast: null,
    lowerBound: null,
    upperBound: null,
    range: null,
  }));

  const forecastLabel = `Forecast (${forecast.horizon})`;

  // Ensure latest observation connects to forecast point
  if (chartData.length > 0) {
    const lastItem = chartData[chartData.length - 1];
    lastItem.forecast = lastItem.historical;
    if (forecast.forecastLowerBound != null && forecast.forecastUpperBound != null && lastItem.historical != null) {
      lastItem.lowerBound = lastItem.historical;
      lastItem.upperBound = lastItem.historical;
      lastItem.range = [lastItem.historical, lastItem.historical];
    }
  }

  // Target forecast point
  chartData.push({
    date: forecastLabel,
    historical: null,
    forecast: forecast.forecastPrice,
    lowerBound: forecast.forecastLowerBound,
    upperBound: forecast.forecastUpperBound,
    range:
      forecast.forecastLowerBound != null && forecast.forecastUpperBound != null
        ? [forecast.forecastLowerBound, forecast.forecastUpperBound]
        : null,
  });

  return (
    <div className="w-full bg-slate-900/70 p-4 sm:p-6 rounded-2xl border border-slate-800 shadow-xl space-y-4">
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-2 border-b border-slate-800 pb-3">
        <div>
          <h3 className="text-base font-semibold text-slate-100 flex items-center gap-2">
            <span>📈 Price Forecast & Historical Trend</span>
          </h3>
          <p className="text-xs text-slate-400">
            Historical verified market prices ({forecast.unit}) & empirical uncertainty range
          </p>
        </div>
        <div className="flex items-center gap-3 text-xs text-slate-300">
          <span className="flex items-center gap-1.5 font-medium">
            <span className="w-2.5 h-2.5 rounded-full bg-emerald-400 inline-block" /> Historical
          </span>
          <span className="flex items-center gap-1.5 font-medium">
            <span className="w-2.5 h-2.5 rounded-full bg-amber-400 inline-block" /> Forecast
          </span>
          {forecast.forecastLowerBound != null && (
            <span className="flex items-center gap-1.5 font-medium">
              <span className="w-2.5 h-2.5 rounded bg-amber-500/30 border border-amber-500/50 inline-block" /> Uncertainty Bounds
            </span>
          )}
        </div>
      </div>

      <div className="h-72 w-full pt-2">
        <ResponsiveContainer width="100%" height="100%">
          <ComposedChart data={chartData} margin={{ top: 10, right: 20, left: 10, bottom: 5 }}>
            <defs>
              <linearGradient id="forecastAreaGrad" x1="0" y1="0" x2="0" y2="1">
                <stop offset="5%" stopColor="#f59e0b" stopOpacity={0.25} />
                <stop offset="95%" stopColor="#f59e0b" stopOpacity={0.05} />
              </linearGradient>
            </defs>
            <CartesianGrid strokeDasharray="3 3" stroke="#334155" opacity={0.5} />
            <XAxis dataKey="date" stroke="#94a3b8" fontSize={11} tickLine={false} />
            <YAxis
              stroke="#94a3b8"
              fontSize={11}
              domain={['auto', 'auto']}
              tickFormatter={(v) => `₹${v}`}
              tickLine={false}
            />
            <Tooltip
              contentStyle={{
                backgroundColor: '#0f172a',
                borderColor: '#334155',
                borderRadius: '0.75rem',
                color: '#f8fafc',
                fontSize: '12px',
              }}
              formatter={(value: unknown, name: string) => {
                if (value == null) return ['-', name];
                if (Array.isArray(value)) {
                  return [`₹${value[0]} – ₹${value[1]}`, 'Empirical Bounds'];
                }
                return [`₹${Number(value).toLocaleString('en-IN')}`, name];
              }}
            />
            {forecast.forecastLowerBound != null && (
              <Area
                type="monotone"
                dataKey="range"
                stroke="transparent"
                fill="url(#forecastAreaGrad)"
                name="Uncertainty Range"
              />
            )}
            <Line
              type="monotone"
              dataKey="historical"
              stroke="#10b981"
              strokeWidth={2.5}
              dot={{ r: 4, fill: '#10b981' }}
              name="Historical Price"
              connectNulls
            />
            <Line
              type="monotone"
              dataKey="forecast"
              stroke="#f59e0b"
              strokeWidth={2.5}
              strokeDasharray="4 4"
              dot={{ r: 6, fill: '#f59e0b', stroke: '#78350f', strokeWidth: 2 }}
              name="Forecast Price"
              connectNulls
            />
          </ComposedChart>
        </ResponsiveContainer>
      </div>
    </div>
  );
};
