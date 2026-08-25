import React, { useState } from 'react';

interface VideoBackgroundProps {
  videoSrc: string;
  posterSrc?: string;
}

export const VideoBackground: React.FC<VideoBackgroundProps> = ({ videoSrc, posterSrc }) => {
  const [videoError, setVideoError] = useState(false);
  const [videoLoaded, setVideoLoaded] = useState(false);

  return (
    <div className="absolute inset-0 w-full h-full min-h-[100svh] overflow-hidden bg-slate-950 z-0">
      {/* Ambient background glow while loading or if video fails */}
      <div className="absolute inset-0 bg-gradient-to-br from-emerald-950/50 via-slate-950 to-slate-950 z-0 pointer-events-none" />

      {/* Primary Video Background with Mobile-Specific Focal Framing */}
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
          className={`absolute inset-0 w-full h-full object-cover object-[center_35%] sm:object-center transition-opacity duration-1000 motion-reduce:transition-none ${
            videoLoaded ? 'opacity-70 sm:opacity-80' : 'opacity-35'
          }`}
        >
          <source src={videoSrc} type="video/mp4" />
          Your browser does not support the video tag.
        </video>
      )}

      {/* Top Navbar Contrast Gradient */}
      <div className="absolute top-0 inset-x-0 h-32 bg-gradient-to-b from-slate-950/90 via-slate-950/50 to-transparent pointer-events-none z-[1]" />

      {/* Center Readability Contrast Gradient */}
      <div className="absolute inset-0 bg-gradient-to-b from-transparent via-slate-950/45 to-slate-950/85 pointer-events-none z-[1]" />

      {/* Soft Center Radial Glow */}
      <div className="absolute inset-0 bg-[radial-gradient(ellipse_at_center,rgba(16,185,129,0.08)_0%,transparent_75%)] pointer-events-none z-[1]" />
    </div>
  );
};


