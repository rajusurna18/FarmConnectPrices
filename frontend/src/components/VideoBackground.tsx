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
      <div className="absolute inset-0 bg-gradient-to-br from-emerald-950/60 via-slate-950 to-slate-950 z-0 pointer-events-none" />

      {/* Primary Video Background with Mobile-Specific Intentional Framing */}
      {!videoError && (
        <div className="video-mobile-frame absolute inset-0 w-full h-full overflow-hidden">
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
            className={`absolute inset-0 w-full h-full object-cover transition-opacity duration-1000 motion-reduce:transition-none ${
              // Focal framing across mobile breakpoint ranges (preserves main agricultural subject)
              // <=374px: center 25%, 375-389px: center 30%, 390-429px: center 35%, >=430px: center 35%, desktop: center
              'object-[center_28%] xs:object-[center_32%] sm:object-center'
            } ${videoLoaded ? 'opacity-70 sm:opacity-80' : 'opacity-35'}`}
          >
            <source src={videoSrc} type="video/mp4" />
            Your browser does not support the video tag.
          </video>
        </div>
      )}

      {/* Top Navbar Readability Overlay (Z-10) */}
      <div className="absolute top-0 inset-x-0 h-32 sm:h-36 bg-gradient-to-b from-slate-950/95 via-slate-950/60 to-transparent pointer-events-none z-[10]" />

      {/* Center Readability Contrast Gradient Overlay (Z-10) */}
      <div className="absolute inset-0 bg-gradient-to-b from-slate-950/30 via-slate-950/40 to-slate-950/90 pointer-events-none z-[10]" />

      {/* Soft Center Atmospheric Emerald Radial Glow (Z-10) */}
      <div className="absolute inset-0 bg-[radial-gradient(ellipse_at_center,rgba(16,185,129,0.08)_0%,transparent_75%)] pointer-events-none z-[10]" />

      {/* Bottom Hero-to-Content Transition Gradient (Z-10) */}
      <div className="absolute bottom-0 inset-x-0 h-32 sm:h-40 bg-gradient-to-t from-slate-950 via-slate-950/80 to-transparent pointer-events-none z-[10]" />
    </div>
  );
};
