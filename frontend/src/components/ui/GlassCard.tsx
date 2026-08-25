import React, { useState } from 'react';
import { motion } from 'framer-motion';

export interface GlassCardProps {
  children: React.ReactNode;
  className?: string;
  glowColor?: string;
  onClick?: () => void;
  onMouseEnter?: () => void;
  onMouseLeave?: () => void;
  enableTilt?: boolean;
}

export const GlassCard: React.FC<GlassCardProps> = ({
  children,
  className = '',
  glowColor = 'rgba(16, 185, 129, 0.15)',
  onClick,
  onMouseEnter,
  onMouseLeave,
  enableTilt = true,
}) => {
  const [rotateX, setRotateX] = useState(0);
  const [rotateY, setRotateY] = useState(0);

  const handleMouseMove = (e: React.MouseEvent<HTMLDivElement>) => {
    if (!enableTilt) return;
    const card = e.currentTarget;
    const rect = card.getBoundingClientRect();
    const x = e.clientX - rect.left;
    const y = e.clientY - rect.top;
    const centerX = rect.width / 2;
    const centerY = rect.height / 2;

    const rotX = (y - centerY) / 25;
    const rotY = (centerX - x) / 25;

    setRotateX(rotX);
    setRotateY(rotY);
  };

  const handleMouseLeaveCombined = () => {
    setRotateX(0);
    setRotateY(0);
    if (onMouseLeave) onMouseLeave();
  };

  const handleMouseEnterCombined = () => {
    if (onMouseEnter) onMouseEnter();
  };

  return (
    <motion.div
      onMouseMove={handleMouseMove}
      onMouseEnter={handleMouseEnterCombined}
      onMouseLeave={handleMouseLeaveCombined}
      onClick={onClick}
      style={{
        transformStyle: 'preserve-3d',
        transform: `perspective(1000px) rotateX(${rotateX}deg) rotateY(${rotateY}deg)`,
      }}
      className={`relative rounded-3xl bg-slate-900/60 border border-slate-800/80 backdrop-blur-xl transition-shadow duration-300 hover:border-emerald-500/30 hover:shadow-2xl overflow-hidden ${
        onClick ? 'cursor-pointer' : ''
      } ${className}`}
    >
      {/* Dynamic Hover Glow Gradient */}
      <div
        className="absolute inset-0 pointer-events-none transition-opacity duration-300 opacity-60 hover:opacity-100"
        style={{
          background: `radial-gradient(circle at 50% 0%, ${glowColor}, transparent 70%)`,
        }}
      />
      <div className="relative z-10">{children}</div>
    </motion.div>
  );
};
