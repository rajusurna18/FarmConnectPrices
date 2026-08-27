import React, { Component } from 'react';
import type { ErrorInfo, ReactNode } from 'react';

interface WebGLBoundaryProps {
  fallback?: ReactNode;
  children: ReactNode;
}

interface WebGLBoundaryState {
  hasError: boolean;
}

/**
 * Robust WebGL Error Boundary to catch 3D context creation errors, WebGL context loss, or device GPU limitations.
 * Listens directly to canvas webglcontextlost events and degrades gracefully to an optimized 2D fallback.
 */
export class WebGLBoundary extends Component<WebGLBoundaryProps, WebGLBoundaryState> {
  public state: WebGLBoundaryState = {
    hasError: false,
  };

  private containerRef = React.createRef<HTMLDivElement>();
  private observer: MutationObserver | null = null;
  private attachedCanvases = new Set<HTMLCanvasElement>();

  private handleContextLost = (event: Event) => {
    event.preventDefault();
    console.info('[WebGLBoundary] WebGL Context Lost caught. Degrading gracefully to 2D ambient fallback.');
    this.setState({ hasError: true });
  };

  private attachCanvasListeners = () => {
    if (!this.containerRef.current) return;
    const canvases = this.containerRef.current.querySelectorAll('canvas');
    canvases.forEach((canvas) => {
      if (!this.attachedCanvases.has(canvas)) {
        canvas.addEventListener('webglcontextlost', this.handleContextLost, true);
        this.attachedCanvases.add(canvas);
      }
    });
  };

  public componentDidMount() {
    if (typeof window !== 'undefined') {
      window.addEventListener('webglcontextlost', this.handleContextLost, true);
    }
    
    this.attachCanvasListeners();

    if (typeof MutationObserver !== 'undefined' && this.containerRef.current) {
      this.observer = new MutationObserver(() => {
        this.attachCanvasListeners();
      });
      this.observer.observe(this.containerRef.current, { childList: true, subtree: true });
    }
  }

  public componentWillUnmount() {
    if (typeof window !== 'undefined') {
      window.removeEventListener('webglcontextlost', this.handleContextLost, true);
    }
    if (this.observer) {
      this.observer.disconnect();
      this.observer = null;
    }
    this.attachedCanvases.forEach((canvas) => {
      canvas.removeEventListener('webglcontextlost', this.handleContextLost, true);
    });
    this.attachedCanvases.clear();
  }

  public static getDerivedStateFromError(): WebGLBoundaryState {
    return { hasError: true };
  }

  public componentDidCatch(error: Error, errorInfo: ErrorInfo) {
    console.warn('[WebGLBoundary] WebGL / R3F Canvas Error caught by boundary:', error.message || error, errorInfo);
  }

  private handleRetry = () => {
    this.setState({ hasError: false });
  };

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
            Optimized high-performance 2D interface active for your device configuration.
          </p>
          <button
            type="button"
            onClick={this.handleRetry}
            className="relative z-10 mt-3 px-3 py-1.5 rounded-lg bg-emerald-500/10 hover:bg-emerald-500/20 border border-emerald-500/30 text-emerald-300 text-xs font-medium transition-colors"
          >
            Reload 3D Graphics
          </button>
        </div>
      );
    }

    return (
      <div ref={this.containerRef} className="w-full h-full">
        {this.props.children}
      </div>
    );
  }
}
