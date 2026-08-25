import React, { useState } from 'react';

interface VideoBackgroundProps {
  videoSrc: string;
  posterSrc?: string;
}

export const VideoBackground: React.FC<VideoBackgroundProps> = ({ videoSrc, posterSrc }) => {
  const [videoError, setVideoError] = useState(false);
  const [videoLoaded, setVideoLoaded] = useState(false);

  return (
    <div className="absolute inset-0 w-full h-full overflow-hidden bg-slate-950 z-0">
      {/* Fallback ambient visual background if video fails or delays */}
      <div className="absolute inset-0 bg-gradient-to-br from-emerald-950/40 via-slate-950 to-slate-950 z-0 pointer-events-none" />

      {/* Primary Video Background */}
      {!videoError && (
        <video
          autoPlay
          muted
          loop
          playsInline
          preload="metadata"
          aria-hidden="true"
          poster={posterSrc}
          onLoadedData={() => setVideoLoaded(true)}
          onCanPlay={() => setVideoLoaded(true)}
          onError={() => setVideoError(true)}
          className={`absolute inset-0 w-full h-full object-cover object-center transition-opacity duration-1000 motion-reduce:transition-none ${
            videoLoaded ? 'opacity-75 sm:opacity-80' : 'opacity-40'
          }`}
        >
          <source src={videoSrc} type="video/mp4" />
          Your browser does not support the video tag.
        </video>
      )}

      {/* Cinematic Vignette & Atmospheric Contrast Gradient */}
      <div className="absolute inset-0 bg-gradient-to-b from-slate-950/75 via-slate-950/40 to-slate-950/90 pointer-events-none z-[1]" />

      {/* Soft Radial Center Highlight */}
      <div className="absolute inset-0 bg-[radial-gradient(ellipse_at_center,rgba(16,185,129,0.06)_0%,transparent_70%)] pointer-events-none z-[1]" />
    </div>
  );
};

