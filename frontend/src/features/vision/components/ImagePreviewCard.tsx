import React from 'react';
import { X, Image as ImageIcon, Sparkles } from 'lucide-react';
import type { ImagePreviewCardProps } from '../types';

export const ImagePreviewCard: React.FC<ImagePreviewCardProps> = ({
  image,
  onRemove,
  disabled = false,
}) => {
  const formatBytes = (bytes: number): string => {
    if (bytes < 1024) return `${bytes} B`;
    if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`;
    return `${(bytes / (1024 * 1024)).toFixed(1)} MB`;
  };

  const getFormatLabel = (mimeType: string): string => {
    if (mimeType.includes('webp')) return 'WebP';
    if (mimeType.includes('png')) return 'PNG';
    return 'JPEG';
  };

  return (
    <div
      role="group"
      aria-label={`Image preview: ${image.originalFile.name}`}
      className="relative flex flex-col bg-slate-950 border border-slate-800 rounded-2xl overflow-hidden shadow-lg group transition-all duration-200 hover:border-slate-700"
    >
      {/* Thumbnail Aspect Container */}
      <div className="relative w-full aspect-square bg-slate-900 flex items-center justify-center overflow-hidden">
        {image.previewUrl ? (
          <img
            src={image.previewUrl}
            alt={image.originalFile.name}
            className="w-full h-full object-cover transition-transform duration-300 group-hover:scale-105"
            loading="lazy"
          />
        ) : (
          <div className="flex flex-col items-center justify-center text-slate-500 gap-1">
            <ImageIcon className="w-8 h-8" />
            <span className="text-[10px]">No Preview</span>
          </div>
        )}

        {/* Format Badge Overlay */}
        <span className="absolute top-2 left-2 px-2 py-0.5 bg-slate-950/80 backdrop-blur border border-slate-700 text-slate-300 text-[10px] font-mono font-semibold rounded-md uppercase">
          {getFormatLabel(image.displayDerivative.mimeType)}
        </span>

        {/* Remove Button */}
        <button
          type="button"
          onClick={() => onRemove(image.id)}
          disabled={disabled}
          aria-label={`Remove image ${image.originalFile.name}`}
          className="absolute top-2 right-2 p-1.5 rounded-full bg-slate-950/80 hover:bg-rose-950 text-slate-300 hover:text-rose-400 border border-slate-700 hover:border-rose-500/50 transition-all focus:outline-none focus:ring-2 focus:ring-rose-500/50 disabled:opacity-40 disabled:cursor-not-allowed"
        >
          <X className="w-3.5 h-3.5" />
        </button>
      </div>

      {/* Metadata Footer */}
      <div className="p-3 space-y-1.5">
        <p className="text-xs font-semibold text-slate-200 truncate" title={image.originalFile.name}>
          {image.originalFile.name}
        </p>

        <div className="flex items-center justify-between text-[11px] text-slate-400">
          <span>{formatBytes(image.originalFile.size)}</span>
          <span className="font-mono text-[10px]">
            {`${image.originalDimensions.width}×${image.originalDimensions.height}`}
          </span>
        </div>

        {/* Diagnostic Preservation Notice */}
        {image.diagnosticSource && (
          <div className="flex items-center gap-1 text-[10px] text-emerald-400/90 font-medium pt-0.5">
            <Sparkles className="w-3 h-3 text-emerald-400" />
            <span>High-Res Diagnostic Preserved</span>
          </div>
        )}
      </div>
    </div>
  );
};
