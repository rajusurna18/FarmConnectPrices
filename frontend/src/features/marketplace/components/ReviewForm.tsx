import React, { useState } from 'react';
import { useCreateReviewMutation } from '../api/marketplaceReviewsApi';

interface ReviewFormProps {
  orderId: string;
  revieweeDisplayName?: string | null;
  revieweeRole?: string | null;
  onSuccess?: () => void;
  onCancel?: () => void;
}

export const ReviewForm: React.FC<ReviewFormProps> = ({
  orderId,
  revieweeDisplayName,
  revieweeRole,
  onSuccess,
  onCancel,
}) => {
  const [rating, setRating] = useState<number>(5);
  const [hoverRating, setHoverRating] = useState<number>(0);
  const [title, setTitle] = useState<string>('');
  const [comment, setComment] = useState<string>('');
  const [errorMsg, setErrorMsg] = useState<string | null>(null);

  const createReviewMutation = useCreateReviewMutation();

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setErrorMsg(null);

    if (rating < 1 || rating > 5) {
      setErrorMsg('Please select a rating between 1 and 5 stars.');
      return;
    }
    if (!comment.trim()) {
      setErrorMsg('Please enter a review comment.');
      return;
    }
    if (comment.length > 1000) {
      setErrorMsg('Comment exceeds the maximum length of 1000 characters.');
      return;
    }

    try {
      await createReviewMutation.mutateAsync({
        orderId,
        rating,
        title: title.trim() || undefined,
        comment: comment.trim(),
      });
      if (onSuccess) onSuccess();
    } catch (err: unknown) {
      const errorObj = err as { response?: { data?: string }; message?: string };
      const msg = errorObj.response?.data || errorObj.message || 'Failed to submit review.';
      setErrorMsg(typeof msg === 'string' ? msg : JSON.stringify(msg));
    }
  };

  return (
    <div className="bg-slate-900/90 border border-slate-800 rounded-2xl p-6 shadow-2xl space-y-5">
      <div className="flex items-center justify-between border-b border-slate-800 pb-4">
        <div>
          <h3 className="text-lg font-bold text-slate-100 flex items-center gap-2">
            <span>⭐</span> Rate Commercial Transaction
          </h3>
          <p className="text-xs text-slate-400 mt-1">
            Reviewing {revieweeDisplayName || 'Counterparty'} {revieweeRole ? `(${revieweeRole})` : ''}
          </p>
        </div>
        <span className="text-xs font-mono text-emerald-400 bg-emerald-950/60 border border-emerald-800/60 px-2.5 py-1 rounded-full">
          Verified Order
        </span>
      </div>

      {errorMsg && (
        <div className="p-3 bg-rose-950/80 border border-rose-800 rounded-xl text-xs text-rose-300">
          {errorMsg}
        </div>
      )}

      <form onSubmit={handleSubmit} className="space-y-4">
        {/* Rating Stars Selection */}
        <div>
          <label className="block text-xs font-semibold text-slate-300 mb-2">Overall Rating *</label>
          <div className="flex items-center gap-2">
            {[1, 2, 3, 4, 5].map((star) => (
              <button
                type="button"
                key={star}
                onClick={() => setRating(star)}
                onMouseEnter={() => setHoverRating(star)}
                onMouseLeave={() => setHoverRating(0)}
                className="text-2xl transition-transform hover:scale-110 focus:outline-none"
              >
                <span className={(hoverRating || rating) >= star ? 'text-amber-400' : 'text-slate-700'}>
                  ★
                </span>
              </button>
            ))}
            <span className="ml-2 text-xs font-bold text-amber-300">
              {hoverRating || rating} / 5 Stars
            </span>
          </div>
        </div>

        {/* Title Input */}
        <div>
          <div className="flex justify-between items-center mb-1">
            <label className="block text-xs font-semibold text-slate-300">Review Headline (Optional)</label>
            <span className="text-[10px] text-slate-500">{title.length} / 100</span>
          </div>
          <input
            type="text"
            maxLength={100}
            value={title}
            onChange={(e) => setTitle(e.target.value)}
            placeholder="e.g. Excellent produce quality and fast fulfillment"
            className="w-full bg-slate-950 border border-slate-800 rounded-xl px-3.5 py-2 text-xs text-slate-100 placeholder-slate-600 focus:outline-none focus:border-emerald-500"
          />
        </div>

        {/* Comment Textarea */}
        <div>
          <div className="flex justify-between items-center mb-1">
            <label className="block text-xs font-semibold text-slate-300">Detailed Feedback *</label>
            <span className="text-[10px] text-slate-500">{comment.length} / 1000</span>
          </div>
          <textarea
            required
            rows={4}
            maxLength={1000}
            value={comment}
            onChange={(e) => setComment(e.target.value)}
            placeholder="Share your experience regarding produce quality, packaging, delivery, or communication..."
            className="w-full bg-slate-950 border border-slate-800 rounded-xl px-3.5 py-2 text-xs text-slate-100 placeholder-slate-600 focus:outline-none focus:border-emerald-500 resize-none"
          />
        </div>

        {/* Action Buttons */}
        <div className="flex items-center justify-end gap-3 pt-2">
          {onCancel && (
            <button
              type="button"
              onClick={onCancel}
              className="px-4 py-2 bg-slate-800 hover:bg-slate-700 text-slate-300 text-xs font-medium rounded-xl transition-colors"
            >
              Cancel
            </button>
          )}
          <button
            type="submit"
            disabled={createReviewMutation.isPending}
            className="px-5 py-2 bg-emerald-600 hover:bg-emerald-500 text-slate-950 text-xs font-bold rounded-xl shadow-lg transition-colors disabled:opacity-50"
          >
            {createReviewMutation.isPending ? 'Submitting...' : 'Submit Review'}
          </button>
        </div>
      </form>
    </div>
  );
};
