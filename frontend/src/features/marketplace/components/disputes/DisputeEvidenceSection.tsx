import React, { useState } from 'react';
import type { DisputeEvidence, AddDisputeEvidenceRequest } from '../../../../types/dispute';
import {
  useAddDisputeEvidenceMutation,
  extractDisputeApiErrorMessage,
} from '../../api/marketplaceDisputesApi';

interface DisputeEvidenceSectionProps {
  disputeId: string;
  evidence: DisputeEvidence[];
  canAddEvidence?: boolean;
  className?: string;
}

const ALLOWED_CONTENT_TYPES = [
  { value: 'image/jpeg', label: 'JPEG Image (image/jpeg)' },
  { value: 'image/png', label: 'PNG Image (image/png)' },
  { value: 'image/webp', label: 'WebP Image (image/webp)' },
  { value: 'application/pdf', label: 'PDF Document (application/pdf)' },
];

export const DisputeEvidenceSection: React.FC<DisputeEvidenceSectionProps> = ({
  disputeId,
  evidence,
  canAddEvidence = false,
  className = '',
}) => {
  const [showAddForm, setShowAddForm] = useState(false);
  const [fileName, setFileName] = useState('');
  const [contentType, setContentType] = useState('image/jpeg');
  const [fileSizeBytes, setFileSizeBytes] = useState<string>('1024000');
  const [description, setDescription] = useState('');
  const [formError, setFormError] = useState<string | null>(null);

  const mutation = useAddDisputeEvidenceMutation();

  const formatFileSize = (bytes?: number) => {
    if (!bytes && bytes !== 0) return '—';
    if (bytes < 1024) return `${bytes} B`;
    if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`;
    return `${(bytes / (1024 * 1024)).toFixed(2)} MB`;
  };

  const formatDate = (isoString?: string) => {
    if (!isoString) return '—';
    try {
      const d = new Date(isoString);
      return d.toLocaleDateString('en-IN', {
        day: 'numeric',
        month: 'short',
        year: 'numeric',
        hour: '2-digit',
        minute: '2-digit',
      });
    } catch {
      return isoString;
    }
  };

  const handleRegister = async (e: React.FormEvent) => {
    e.preventDefault();
    setFormError(null);

    const trimmedName = fileName.trim();
    const trimmedDesc = description.trim();
    const parsedSize = parseInt(fileSizeBytes, 10);

    if (!trimmedName) {
      setFormError('File name is required.');
      return;
    }
    if (!contentType) {
      setFormError('Content type is required.');
      return;
    }
    if (isNaN(parsedSize) || parsedSize <= 0) {
      setFormError('Valid file size in bytes is required.');
      return;
    }
    if (parsedSize > 10 * 1024 * 1024) {
      setFormError('File size exceeds maximum allowed size (10 MB).');
      return;
    }
    if (!trimmedDesc) {
      setFormError('Evidence description is required.');
      return;
    }

    const payload: AddDisputeEvidenceRequest = {
      fileName: trimmedName,
      contentType,
      fileSize: parsedSize,
      description: trimmedDesc,
    };

    try {
      await mutation.mutateAsync({ disputeId, request: payload });
      setShowAddForm(false);
      setFileName('');
      setDescription('');
    } catch {
      // Error handled via mutation.error
    }
  };

  const errorMessage = formError || (mutation.isError ? extractDisputeApiErrorMessage(mutation.error) : null);

  return (
    <div className={`bg-slate-900/80 border border-slate-800 rounded-2xl p-6 shadow-xl space-y-5 ${className}`}>
      {/* Header */}
      <div className="flex flex-wrap items-center justify-between gap-3 border-b border-slate-800 pb-3">
        <div>
          <h3 className="text-sm font-bold text-slate-200 flex items-center gap-2">
            <span>📎</span> Supporting Evidence ({evidence.length})
          </h3>
          <p className="text-xs text-slate-400 mt-0.5">
            Registered evidentiary metadata recorded for this dispute.
          </p>
        </div>

        {canAddEvidence && !showAddForm && (
          <button
            type="button"
            onClick={() => setShowAddForm(true)}
            className="px-3.5 py-1.5 bg-indigo-950/80 border border-indigo-700/60 text-indigo-300 hover:bg-indigo-900 text-xs font-semibold rounded-xl transition-all"
          >
            + Register Evidence
          </button>
        )}
      </div>

      {/* Honest Storage / Metadata Limitation Notice */}
      <div className="p-3.5 bg-slate-950/80 border border-slate-800 rounded-xl text-xs text-slate-400 flex items-start gap-2.5">
        <span className="text-amber-400 text-base leading-none">ℹ️</span>
        <div className="space-y-0.5">
          <p className="font-semibold text-slate-300">Evidence Registration Notice</p>
          <p className="text-slate-400">
            Secure evidence file upload will be available in a future update. Evidence registration is currently limited to metadata. No raw files are uploaded to public storage.
          </p>
        </div>
      </div>

      {/* Evidence Registration Form Modal/Card */}
      {showAddForm && (
        <form
          onSubmit={handleRegister}
          className="p-4 bg-slate-950 border border-slate-800 rounded-xl space-y-4"
          aria-label="Register evidence metadata"
        >
          <div className="flex items-center justify-between border-b border-slate-800 pb-2">
            <span className="text-xs font-bold text-slate-200">Register Evidence Metadata</span>
            <button
              type="button"
              onClick={() => setShowAddForm(false)}
              className="text-slate-400 hover:text-slate-200 text-xs"
            >
              Cancel
            </button>
          </div>

          <div className="grid grid-cols-1 sm:grid-cols-2 gap-3 text-xs">
            <div className="space-y-1">
              <label htmlFor="evidence-filename" className="font-semibold text-slate-300">
                File Name <span className="text-rose-400">*</span>
              </label>
              <input
                id="evidence-filename"
                type="text"
                value={fileName}
                onChange={(e) => setFileName(e.target.value)}
                placeholder="e.g. weighbridge_slip.pdf"
                className="w-full px-3 py-2 bg-slate-900 border border-slate-800 rounded-lg text-slate-200 placeholder-slate-500 focus:outline-none focus:ring-1 focus:ring-emerald-500"
                required
              />
            </div>

            <div className="space-y-1">
              <label htmlFor="evidence-content-type" className="font-semibold text-slate-300">
                File MIME Type <span className="text-rose-400">*</span>
              </label>
              <select
                id="evidence-content-type"
                value={contentType}
                onChange={(e) => setContentType(e.target.value)}
                className="w-full px-3 py-2 bg-slate-900 border border-slate-800 rounded-lg text-slate-200 focus:outline-none focus:ring-1 focus:ring-emerald-500"
              >
                {ALLOWED_CONTENT_TYPES.map((ct) => (
                  <option key={ct.value} value={ct.value}>
                    {ct.label}
                  </option>
                ))}
              </select>
            </div>

            <div className="space-y-1">
              <label htmlFor="evidence-file-size" className="font-semibold text-slate-300">
                File Size (Bytes) <span className="text-rose-400">*</span>
              </label>
              <input
                id="evidence-file-size"
                type="number"
                min="1"
                max={10 * 1024 * 1024}
                value={fileSizeBytes}
                onChange={(e) => setFileSizeBytes(e.target.value)}
                placeholder="e.g. 2048000"
                className="w-full px-3 py-2 bg-slate-900 border border-slate-800 rounded-lg text-slate-200 placeholder-slate-500 focus:outline-none focus:ring-1 focus:ring-emerald-500 font-mono"
                required
              />
              <span className="text-[10px] text-slate-500">
                Calculates to approx: {formatFileSize(parseInt(fileSizeBytes, 10) || 0)}
              </span>
            </div>

            <div className="space-y-1 sm:col-span-2">
              <label htmlFor="evidence-description" className="font-semibold text-slate-300">
                Description / Context <span className="text-rose-400">*</span>
              </label>
              <input
                id="evidence-description"
                type="text"
                value={description}
                onChange={(e) => setDescription(e.target.value)}
                placeholder="e.g. Photo of mandi weighbridge slip showing 82 quintals"
                className="w-full px-3 py-2 bg-slate-900 border border-slate-800 rounded-lg text-slate-200 placeholder-slate-500 focus:outline-none focus:ring-1 focus:ring-emerald-500"
                required
              />
            </div>
          </div>

          {errorMessage && (
            <div className="p-2.5 bg-rose-950/60 border border-rose-800 text-rose-300 rounded-lg text-xs" role="alert">
              {errorMessage}
            </div>
          )}

          <div className="flex justify-end gap-2 pt-1">
            <button
              type="button"
              onClick={() => setShowAddForm(false)}
              className="px-3 py-1.5 bg-slate-800 hover:bg-slate-700 text-slate-300 text-xs font-medium rounded-lg"
            >
              Cancel
            </button>
            <button
              type="submit"
              disabled={mutation.isPending}
              className="px-4 py-1.5 bg-emerald-600 hover:bg-emerald-500 text-white font-semibold text-xs rounded-lg shadow-md disabled:bg-slate-800 disabled:text-slate-500"
            >
              {mutation.isPending ? 'Registering...' : 'Save Metadata'}
            </button>
          </div>
        </form>
      )}

      {/* Evidence List */}
      {evidence.length === 0 ? (
        <div className="text-center py-6 text-slate-500 text-xs">
          No supporting evidence metadata registered yet.
        </div>
      ) : (
        <div className="grid grid-cols-1 md:grid-cols-2 gap-3" role="list">
          {evidence.map((item) => (
            <div
              key={item.evidenceId}
              className="p-3.5 bg-slate-950/70 border border-slate-800 rounded-xl space-y-2 text-xs"
              role="listitem"
            >
              <div className="flex items-start justify-between gap-2">
                <span className="font-bold text-slate-200 truncate" title={item.fileName}>
                  📄 {item.fileName}
                </span>
                <span className="px-2 py-0.5 rounded text-[10px] font-mono bg-slate-800 text-slate-400">
                  {formatFileSize(item.fileSize)}
                </span>
              </div>

              <p className="text-slate-300 text-[11px] leading-relaxed line-clamp-2">
                {item.description}
              </p>

              <div className="flex flex-wrap items-center justify-between gap-2 text-[10px] text-slate-500 pt-1 border-t border-slate-900">
                <span className="font-mono text-slate-400">Type: {item.contentType}</span>
                <span>
                  Registered by <strong className="text-slate-400">{item.registeredByRole}</strong> on{' '}
                  {formatDate(item.createdAt)}
                </span>
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  );
};
