import React from 'react';
import { UploadCloud, AlertCircle, RefreshCw } from 'lucide-react';
import type { ImageUploadZoneProps } from '../types';
import { MAX_IMAGE_SIZE_BYTES } from '../types';
import { useImageUploadZone } from '../hooks/useImageUploadZone';
import { ImagePreviewCard } from './ImagePreviewCard';

export const ImageUploadZone: React.FC<ImageUploadZoneProps> = ({
  onImagesChange,
  onError,
  maxFiles = 5,
  maxSizeBytes = MAX_IMAGE_SIZE_BYTES,
  disabled = false,
  className = '',
  label = 'Upload Crop Images',
  description = 'Drag & drop high-resolution photos of crops, leaves, or produce for diagnosis and quality analysis.',
  generateDiagnosticDerivative = true,
}) => {
  const {
    images,
    isDragging,
    isProcessing,
    errorMessage,
    fileInputRef,
    handleFiles,
    handleRemoveImage,
    handleDrop,
    handleDragOver,
    handleDragLeave,
    handleZoneClick,
    handleKeyDown,
    isAtCapacity,
  } = useImageUploadZone({
    onImagesChange,
    onError,
    maxFiles,
    maxSizeBytes,
    disabled,
    generateDiagnosticDerivative,
  });

  return (
    <div className={`space-y-4 ${className}`}>
      {/* Hidden File Input */}
      <input
        ref={fileInputRef}
        type="file"
        accept="image/jpeg,image/png,image/webp"
        multiple={maxFiles > 1}
        disabled={disabled || isAtCapacity || isProcessing}
        onChange={(e) => {
          if (e.target.files) {
            handleFiles(e.target.files);
          }
        }}
        className="hidden"
        aria-hidden="true"
        tabIndex={-1}
      />

      {/* Drag & Drop Target Zone */}
      {!isAtCapacity && (
        <div
          role="button"
          tabIndex={disabled || isProcessing ? -1 : 0}
          aria-label={label}
          aria-disabled={disabled || isProcessing}
          onClick={handleZoneClick}
          onKeyDown={handleKeyDown}
          onDragOver={handleDragOver}
          onDragEnter={handleDragOver}
          onDragLeave={handleDragLeave}
          onDrop={handleDrop}
          className={`relative border-2 border-dashed rounded-2xl p-6 sm:p-8 text-center cursor-pointer transition-all duration-200 select-none focus:outline-none focus:ring-2 focus:ring-emerald-500/50 ${
            disabled
              ? 'opacity-40 cursor-not-allowed border-slate-800 bg-slate-950/40'
              : isDragging
                ? 'border-emerald-400 bg-emerald-950/20 shadow-xl shadow-emerald-950/30 scale-[1.01]'
                : 'border-slate-800 bg-slate-900/60 hover:border-slate-700 hover:bg-slate-900'
          }`}
        >
          <div className="flex flex-col items-center justify-center gap-3">
            <div
              className={`w-12 h-12 rounded-2xl flex items-center justify-center transition-colors ${
                isDragging ? 'bg-emerald-500/20 text-emerald-400' : 'bg-slate-800 text-slate-400'
              }`}
            >
              {isProcessing ? (
                <RefreshCw className="w-6 h-6 animate-spin text-emerald-400" />
              ) : (
                <UploadCloud className="w-6 h-6" />
              )}
            </div>

            <div className="space-y-1">
              <p className="text-sm font-semibold text-slate-200">
                {isProcessing ? 'Preprocessing image...' : label}
              </p>
              <p className="text-xs text-slate-400 max-w-sm mx-auto leading-relaxed">
                {description}
              </p>
            </div>

            <div className="flex flex-wrap items-center justify-center gap-2 pt-1">
              <span className="px-2 py-0.5 bg-slate-800 border border-slate-700 rounded text-[11px] font-mono text-slate-300">
                JPEG, PNG, WebP
              </span>
              <span className="text-[11px] text-slate-500">•</span>
              <span className="text-[11px] text-slate-400">
                Max {(maxSizeBytes / (1024 * 1024)).toFixed(0)} MB
              </span>
              <span className="text-[11px] text-slate-500">•</span>
              <span className="text-[11px] text-slate-400 font-medium">
                {`${images.length} / ${maxFiles} images`}
              </span>
            </div>
          </div>
        </div>
      )}

      {/* Error Banner */}
      {errorMessage && (
        <div
          role="alert"
          className="flex items-center gap-2 p-3 bg-rose-950/70 border border-rose-500/30 rounded-xl text-rose-300 text-xs animate-fade-in"
        >
          <AlertCircle className="w-4 h-4 shrink-0 text-rose-400" />
          <span>{errorMessage}</span>
        </div>
      )}

      {/* Image Previews Grid */}
      {images.length > 0 && (
        <div className="space-y-2">
          <div className="flex items-center justify-between text-xs font-semibold text-slate-400 px-1">
            <span>Selected Images ({images.length} of {maxFiles})</span>
            <span className="text-[11px] text-slate-500">Untouched originals preserved in memory</span>
          </div>

          <div className="grid grid-cols-2 sm:grid-cols-3 md:grid-cols-4 lg:grid-cols-5 gap-3">
            {images.map((image) => (
              <ImagePreviewCard
                key={image.id}
                image={image}
                onRemove={handleRemoveImage}
                disabled={disabled}
              />
            ))}
          </div>
        </div>
      )}
    </div>
  );
};
