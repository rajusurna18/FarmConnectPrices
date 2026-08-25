import React from 'react';

interface PriceBadgeProps {
  percentage: number;
  isUp: boolean;
  size?: 'sm' | 'md' | 'lg';
}

export const PriceBadge: React.FC<PriceBadgeProps> = ({ percentage, isUp, size = 'md' }) => {
  const isPositive = isUp || percentage > 0;

  const sizeClasses = {
    sm: 'px-2 py-0.5 text-xs',
    md: 'px-2.5 py-1 text-xs sm:text-sm font-semibold',
    lg: 'px-3 py-1.5 text-sm sm:text-base font-bold',
  }[size];

  return (
    <span
      className={`inline-flex items-center space-x-1 rounded-full border backdrop-blur-md transition-all ${sizeClasses} ${
        isPositive
          ? 'bg-emerald-500/10 border-emerald-500/30 text-emerald-400'
          : 'bg-rose-500/10 border-rose-500/30 text-rose-400'
      }`}
    >
      <span>{isPositive ? '▲' : '▼'}</span>
      <span>{Math.abs(percentage)}%</span>
    </span>
  );
};
