import React, { useState } from 'react';
import {
  useAddDisputeResponseMutation,
  extractDisputeApiErrorMessage,
} from '../../api/marketplaceDisputesApi';

interface DisputeResponseFormProps {
  disputeId: string;
  onSuccess?: () => void;
  className?: string;
}

const MAX_MESSAGE_LENGTH = 2000;

export const DisputeResponseForm: React.FC<DisputeResponseFormProps> = ({
  disputeId,
  onSuccess,
  className = '',
}) => {
  const [message, setMessage] = useState('');
  const [validationError, setValidationError] = useState<string | null>(null);

  const mutation = useAddDisputeResponseMutation();

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setValidationError(null);

    const trimmed = message.trim();
    if (!trimmed) {
      setValidationError('Response message cannot be blank.');
      return;
    }

    if (trimmed.length > MAX_MESSAGE_LENGTH) {
      setValidationError(`Response message cannot exceed ${MAX_MESSAGE_LENGTH} characters.`);
      return;
    }

    try {
      await mutation.mutateAsync({ disputeId, message: trimmed });
      setMessage('');
      if (onSuccess) onSuccess();
    } catch {
      // Error handled via mutation.error or extractDisputeApiErrorMessage
    }
  };

  const errorMessage = validationError || (mutation.isError ? extractDisputeApiErrorMessage(mutation.error) : null);

  return (
    <form
      onSubmit={handleSubmit}
      className={`bg-slate-900/80 border border-slate-800 rounded-2xl p-5 space-y-4 shadow-xl ${className}`}
      aria-label="Submit dispute response"
    >
      <div className="flex items-center justify-between border-b border-slate-800/80 pb-2">
        <h4 className="text-sm font-bold text-slate-200 flex items-center gap-2">
          <span>💬</span> Submit Response
        </h4>
        <span
          className={`text-[11px] font-mono ${
            message.length > MAX_MESSAGE_LENGTH ? 'text-rose-400 font-bold' : 'text-slate-500'
          }`}
          aria-live="polite"
        >
          {message.length} / {MAX_MESSAGE_LENGTH}
        </span>
      </div>

      <div className="space-y-1">
        <label htmlFor={`dispute-response-${disputeId}`} className="block text-xs font-semibold text-slate-300">
          Your Message <span className="text-rose-400">*</span>
        </label>
        <textarea
          id={`dispute-response-${disputeId}`}
          name="message"
          rows={3}
          value={message}
          onChange={(e) => {
            setMessage(e.target.value);
            if (validationError) setValidationError(null);
          }}
          placeholder="State your position, address counterparty concerns, or provide clarification..."
          className="w-full px-3.5 py-2.5 bg-slate-950 border border-slate-800 rounded-xl text-slate-100 placeholder-slate-500 text-xs focus:outline-none focus:ring-2 focus:ring-emerald-500 focus:border-transparent transition-all resize-y"
          disabled={mutation.isPending}
          required
        />
      </div>

      {errorMessage && (
        <div
          className="p-3 bg-rose-950/60 border border-rose-800/80 rounded-xl text-xs text-rose-300"
          role="alert"
        >
          {errorMessage}
        </div>
      )}

      {mutation.isSuccess && (
        <div className="p-3 bg-emerald-950/60 border border-emerald-800/80 rounded-xl text-xs text-emerald-300 flex items-center gap-1.5" role="status">
          <span>✓</span> Response submitted successfully.
        </div>
      )}

      <div className="flex justify-end">
        <button
          type="submit"
          disabled={mutation.isPending || !message.trim()}
          className="px-4 py-2 bg-emerald-600 hover:bg-emerald-500 disabled:bg-slate-800 disabled:text-slate-500 text-white font-semibold text-xs rounded-xl shadow-lg transition-all focus:outline-none focus-visible:ring-2 focus-visible:ring-emerald-400 flex items-center gap-2"
        >
          {mutation.isPending ? (
            <>
              <span className="w-3.5 h-3.5 border-2 border-white border-t-transparent rounded-full animate-spin" />
              <span>Submitting...</span>
            </>
          ) : (
            <span>Send Response</span>
          )}
        </button>
      </div>
    </form>
  );
};
