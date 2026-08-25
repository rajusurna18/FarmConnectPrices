import React, { useState, useEffect } from 'react';

interface VideoBackgroundProps {
  videoSrc?: string;
  posterSrc?: string;
}

export const VideoBackground: React.FC<VideoBackgroundProps> = ({ videoSrc, posterSrc }) => {
  const [videoError, setVideoError] = useState(false);
  const [videoLoaded, setVideoLoaded] = useState(false);
  const [prefersReducedMotion, setPrefersReducedMotion] = useState(false);

  useEffect(() => {
    if (typeof window !== 'undefined') {
      const mediaQuery = window.matchMedia('(prefers-reduced-motion: reduce)');
      setPrefersReducedMotion(mediaQuery.matches);

      const handleChange = (e: MediaQueryListEvent) => setPrefersReducedMotion(e.matches);
      mediaQuery.addEventListener('change', handleChange);
      return () => mediaQuery.removeEventListener('change', handleChange);
    }
  }, []);

  const effectiveVideoSrc = videoSrc || (typeof import.meta !== 'undefined' ? import.meta.env.VITE_HERO_VIDEO_URL : '');
  const shouldRenderVideo = Boolean(effectiveVideoSrc) && !videoError && !prefersReducedMotion;

  return (
    <div className="absolute inset-0 w-full h-full min-h-[100svh] overflow-hidden bg-slate-950 z-0 select-none">
      {/* Ambient background glow & atmospheric depth */}
      <div className="absolute inset-0 bg-gradient-to-br from-emerald-950/50 via-slate-950 to-slate-950 z-0 pointer-events-none" />

      {/* Primary Video Background (Rendered only when a valid video URL is explicitly configured) */}
      {shouldRenderVideo && (
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
          <source src={effectiveVideoSrc} type="video/mp4" />
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



