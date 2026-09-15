import React from 'react';
import type { ReviewResponse } from '../../../types/review';
import { ReviewCard } from './ReviewCard';

interface ReviewListProps {
  reviews: ReviewResponse[];
  isLoading?: boolean;
  emptyTitle?: string;
  emptySubtitle?: string;
}

export const ReviewList: React.FC<ReviewListProps> = ({
  reviews,
  isLoading,
  emptyTitle = 'No Reviews Yet',
  emptySubtitle = 'No commercial transaction reviews have been submitted for this user.',
}) => {
  if (isLoading) {
    return (
      <div className="space-y-4">
        {[1, 2, 3].map((i) => (
          <div key={i} className="bg-slate-900/60 border border-slate-800 p-5 rounded-2xl animate-pulse space-y-3">
            <div className="h-4 bg-slate-800 rounded w-1/4" />
            <div className="h-3 bg-slate-800 rounded w-1/3" />
            <div className="h-10 bg-slate-800 rounded w-full" />
          </div>
        ))}
      </div>
    );
  }

  if (!reviews || reviews.length === 0) {
    return (
      <div className="bg-slate-900/40 border border-slate-800/80 rounded-2xl p-8 text-center space-y-2">
        <span className="text-3xl block">⭐</span>
        <h4 className="text-sm font-bold text-slate-300">{emptyTitle}</h4>
        <p className="text-xs text-slate-400 max-w-md mx-auto">{emptySubtitle}</p>
      </div>
    );
  }

  return (
    <div className="space-y-4">
      {reviews.map((review) => (
        <ReviewCard key={review.reviewId} review={review} />
      ))}
    </div>
  );
};
