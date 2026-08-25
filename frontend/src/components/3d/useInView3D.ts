import { useState, useEffect, useRef } from 'react';

/**
 * Lightweight IntersectionObserver hook for 3D Canvas viewport-aware rendering.
 * Automatically unmounts/pauses offscreen WebGL contexts to prevent GPU context exhaustion.
 */
export function useInView3D<T extends HTMLElement = HTMLDivElement>(rootMargin = '150px') {
  const containerRef = useRef<T | null>(null);
  const [isInView, setIsInView] = useState<boolean>(false);

  useEffect(() => {
    const element = containerRef.current;
    if (!element) return;

    if (typeof IntersectionObserver === 'undefined') {
      setIsInView(true);
      return;
    }

    const observer = new IntersectionObserver(
      ([entry]) => {
        setIsInView(entry.isIntersecting);
      },
      { rootMargin }
    );

    observer.observe(element);
    return () => {
      observer.disconnect();
    };
  }, [rootMargin]);

  return { containerRef, isInView };
}
