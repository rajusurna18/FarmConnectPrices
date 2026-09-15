import React from 'react';
import type { ReviewResponse } from '../../../types/review';

interface ReviewCardProps {
  review: ReviewResponse;
}

export const ReviewCard: React.FC<ReviewCardProps> = ({ review }) => {
  const getRoleBadge = (role: string) => {
    switch (role) {
      case 'FARMER':
        return <span className="px-2 py-0.5 bg-emerald-950/80 border border-emerald-700/60 text-emerald-300 text-[10px] font-semibold rounded-full">Farmer</span>;
      case 'MEDIATOR_BUYER':
        return <span className="px-2 py-0.5 bg-amber-950/80 border border-amber-700/60 text-amber-300 text-[10px] font-semibold rounded-full">Mediator / Buyer</span>;
      case 'CUSTOMER':
        return <span className="px-2 py-0.5 bg-sky-950/80 border border-sky-700/60 text-sky-300 text-[10px] font-semibold rounded-full">Customer</span>;
      default:
        return <span className="px-2 py-0.5 bg-slate-800 text-slate-300 text-[10px] font-semibold rounded-full">{role}</span>;
    }
  };

  const formatDate = (dateStr: string) => {
    try {
      return new Date(dateStr).toLocaleDateString('en-IN', {
        year: 'numeric',
        month: 'short',
        day: 'numeric',
      });
    } catch {
      return dateStr;
    }
  };

  return (
    <div className="bg-slate-900/60 border border-slate-800 rounded-2xl p-5 shadow-lg hover:border-slate-700 transition-colors space-y-3">
      {/* Top Bar: Reviewer Name, Role, Date */}
      <div className="flex flex-wrap items-center justify-between gap-2 border-b border-slate-800/80 pb-3">
        <div className="flex items-center gap-2">
          <div className="w-8 h-8 rounded-full bg-slate-800 border border-slate-700 flex items-center justify-center font-bold text-xs text-slate-200">
            {review.reviewerDisplayName ? review.reviewerDisplayName.charAt(0).toUpperCase() : 'U'}
          </div>
          <div>
            <span className="text-xs font-bold text-slate-200 block">{review.reviewerDisplayName}</span>
            <div className="flex items-center gap-1.5 mt-0.5">
              {getRoleBadge(review.reviewerRole)}
              <span className="text-[10px] text-slate-400">reviewed</span>
              <span className="text-[10px] font-medium text-slate-300">{review.revieweeDisplayName}</span>
            </div>
          </div>
        </div>

        <div className="flex items-center gap-3">
          {review.verifiedTransaction && (
            <span className="text-[10px] font-medium text-emerald-400 bg-emerald-950/60 border border-emerald-800/60 px-2 py-0.5 rounded-full flex items-center gap-1">
              <span>✓</span> Verified Order
            </span>
          )}
          <span className="text-[11px] font-mono text-slate-400">{formatDate(review.createdAt)}</span>
        </div>
      </div>

      {/* Stars & Title */}
      <div className="space-y-1">
        <div className="flex items-center gap-2">
          <div className="flex text-amber-400 text-sm">
            {[1, 2, 3, 4, 5].map((s) => (
              <span key={s}>{review.rating >= s ? '★' : '☆'}</span>
            ))}
          </div>
          <span className="text-xs font-bold text-slate-200">{review.rating} / 5</span>
        </div>
        {review.title && (
          <h5 className="text-sm font-semibold text-slate-100 mt-1">{review.title}</h5>
        )}
      </div>

      {/* Comment Content (plain text rendering, plain text escaped) */}
      <p className="text-xs text-slate-300 leading-relaxed whitespace-pre-wrap">
        {review.comment}
      </p>

      {/* Crop / Listing reference tag if present */}
      {review.cropName && (
        <div className="pt-2 flex items-center justify-between text-[11px] text-slate-400 border-t border-slate-800/60">
          <span>Crop: <strong className="text-slate-300">{review.cropName}</strong></span>
          <span className="font-mono text-[10px]">Order: {review.orderId}</span>
        </div>
      )}
    </div>
  );
};
