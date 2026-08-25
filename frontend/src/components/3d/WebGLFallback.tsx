import { Component } from 'react';
import type { ErrorInfo, ReactNode } from 'react';

interface WebGLBoundaryProps {
  fallback?: ReactNode;
  children: ReactNode;
}

interface WebGLBoundaryState {
  hasError: boolean;
}

/**
 * WebGL Error Boundary to catch any 3D context creation errors or WebGL loss on unsupported devices.
 * Ensures the web application never crashes and displays an elegant 2D fallback layout.
 */
export class WebGLBoundary extends Component<WebGLBoundaryProps, WebGLBoundaryState> {
  public state: WebGLBoundaryState = {
    hasError: false,
  };

  public static getDerivedStateFromError(): WebGLBoundaryState {
    return { hasError: true };
  }

  public componentDidCatch(error: Error, errorInfo: ErrorInfo) {
    console.warn('WebGL / R3F Canvas Error caught by boundary:', error, errorInfo);
  }

  public render() {
    if (this.state.hasError) {
      return this.props.fallback || (
        <div className="relative w-full h-full min-h-[260px] flex flex-col items-center justify-center p-6 bg-slate-900/70 border border-emerald-500/20 rounded-3xl backdrop-blur-xl text-center overflow-hidden">
          <div className="absolute inset-0 bg-gradient-to-br from-emerald-950/20 via-slate-950/40 to-slate-950/80 pointer-events-none" />
          <div className="relative z-10 w-12 h-12 rounded-2xl bg-emerald-500/10 border border-emerald-500/30 flex items-center justify-center text-emerald-400 text-xl shadow-lg mb-3">
            🌾
          </div>
          <p className="relative z-10 text-sm font-bold text-slate-100 tracking-wide">Agricultural Market Intelligence</p>
          <p className="relative z-10 text-xs text-slate-400 max-w-xs mt-1 leading-relaxed">
            Optimized high-performance interface active for your device configuration.
          </p>
        </div>
      );
    }

    return this.props.children;
  }
}

