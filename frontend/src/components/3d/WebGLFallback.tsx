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
        <div className="w-full h-full min-h-[250px] flex flex-col items-center justify-center p-6 bg-slate-900/60 border border-slate-800 rounded-3xl backdrop-blur-md text-center">
          <div className="w-12 h-12 rounded-full bg-emerald-500/10 border border-emerald-500/30 flex items-center justify-center text-emerald-400 mb-3">
            🌾
          </div>
          <p className="text-sm font-semibold text-slate-200">Interactive 2D Visualization Active</p>
          <p className="text-xs text-slate-400 max-w-xs mt-1">
            Displaying optimized visual interface for your device.
          </p>
        </div>
      );
    }

    return this.props.children;
  }
}
