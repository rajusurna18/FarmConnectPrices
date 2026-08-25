import React from 'react';

interface SectionHeadingProps {
  badgeText?: string;
  title: string;
  subtitle?: string;
  align?: 'center' | 'left';
}

export const SectionHeading: React.FC<SectionHeadingProps> = ({
  badgeText,
  title,
  subtitle,
  align = 'center',
}) => {
  const alignClass = align === 'center' ? 'text-center items-center' : 'text-left items-start';

  return (
    <div className={`flex flex-col space-y-3 mb-10 sm:mb-14 ${alignClass}`}>
      {badgeText && (
        <div className="inline-flex items-center space-x-2 px-3.5 py-1 rounded-full bg-slate-900/80 border border-emerald-500/30 text-emerald-300 text-xs sm:text-sm font-medium backdrop-blur-md shadow-inner">
          <span className="w-2 h-2 rounded-full bg-emerald-400 animate-pulse"></span>
          <span>{badgeText}</span>
        </div>
      )}

      <h2 className="text-3xl sm:text-5xl font-extrabold tracking-tight text-white drop-shadow-md leading-tight">
        {title}
      </h2>

      {subtitle && (
        <p className="text-base sm:text-lg text-slate-400 font-medium max-w-2xl leading-relaxed">
          {subtitle}
        </p>
      )}
    </div>
  );
};
