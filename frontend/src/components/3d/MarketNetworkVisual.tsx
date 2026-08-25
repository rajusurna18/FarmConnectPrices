import React, { useRef, useMemo } from 'react';
import { Canvas, useFrame } from '@react-three/fiber';
import * as THREE from 'three';
import { WebGLBoundary } from './WebGLFallback';
import { useInView3D } from './useInView3D';

interface MarketNetworkVisualProps {
  isMobile?: boolean;
}

function FloatingMarketNodes({ count = 10 }: { count?: number }) {
  const meshRef = useRef<THREE.InstancedMesh>(null);
  const dummy = useMemo(() => new THREE.Object3D(), []);

  const nodes = useMemo(() => {
    const list = [];
    for (let i = 0; i < count; i++) {
      const x = (Math.random() - 0.5) * (count < 6 ? 5 : 10);
      const y = (Math.random() - 0.5) * (count < 6 ? 4 : 6);
      const z = (Math.random() - 0.5) * (count < 6 ? 2 : 4);
      const scale = 0.08 + Math.random() * 0.06;
      const speed = 0.1 + Math.random() * 0.2;
      const factor = Math.random() * Math.PI * 2;
      list.push({ x, y, z, scale, speed, factor });
    }
    return list;
  }, [count]);

  useFrame(({ clock }) => {
    if (!meshRef.current) return;
    const time = clock.getElapsedTime();

    nodes.forEach((node, i) => {
      const { x, y, z, scale, speed, factor } = node;
      dummy.position.set(
        x + Math.sin(time * speed + factor) * 0.15,
        y + Math.cos(time * speed * 0.7 + factor) * 0.15,
        z + Math.sin(time * speed * 0.4 + factor) * 0.1
      );
      dummy.scale.setScalar(scale * (1 + Math.sin(time * 1.2 + factor) * 0.1));
      dummy.updateMatrix();
      meshRef.current!.setMatrixAt(i, dummy.matrix);
    });

    meshRef.current.instanceMatrix.needsUpdate = true;
  });

  return (
    <instancedMesh ref={meshRef} args={[undefined, undefined, count]}>
      <sphereGeometry args={[0.3, 16, 16]} />
      <meshStandardMaterial
        color="#10b981"
        emissive="#047857"
        emissiveIntensity={0.5}
        roughness={0.3}
        metalness={0.7}
        transparent
        opacity={0.85}
      />
    </instancedMesh>
  );
}

function MarketNetworkLines() {
  const lineRef = useRef<THREE.LineSegments>(null);

  const { positions } = useMemo(() => {
    const points: number[] = [];
    const coords = [
      new THREE.Vector3(-3.0, 1.2, -0.5),
      new THREE.Vector3(-1.2, -0.4, 0.2),
      new THREE.Vector3(0.5, 0.8, -0.2),
      new THREE.Vector3(2.2, -0.6, 0.4),
      new THREE.Vector3(3.2, 1.0, -0.8),
      new THREE.Vector3(-0.4, 1.6, 0.0)
    ];

    const connections = [
      [0, 1], [1, 2], [2, 3], [3, 4], [2, 5], [1, 5]
    ];

    connections.forEach(([i, j]) => {
      points.push(coords[i].x, coords[i].y, coords[i].z);
      points.push(coords[j].x, coords[j].y, coords[j].z);
    });

    return { positions: new Float32Array(points) };
  }, []);

  useFrame(({ clock }) => {
    if (lineRef.current) {
      const mat = lineRef.current.material as THREE.LineBasicMaterial;
      mat.opacity = 0.3 + Math.sin(clock.getElapsedTime() * 1.2) * 0.1;
    }
  });

  return (
    <lineSegments ref={lineRef}>
      <bufferGeometry>
        <bufferAttribute attach="attributes-position" args={[positions, 3]} />
      </bufferGeometry>
      <lineBasicMaterial color="#34d399" transparent opacity={0.35} linewidth={1.5} />
    </lineSegments>
  );
}

export const MarketNetworkVisual: React.FC<MarketNetworkVisualProps> = ({ isMobile = false }) => {
  const { containerRef, isInView } = useInView3D<HTMLDivElement>('150px');
  const prefersReducedMotion = typeof window !== 'undefined' && window.matchMedia('(prefers-reduced-motion: reduce)').matches;

  if (prefersReducedMotion) {
    return null;
  }

  const nodeCount = isMobile ? 4 : 12;

  return (
    <div ref={containerRef} className="absolute inset-0 w-full h-full pointer-events-none z-10 overflow-hidden opacity-75">
      {isInView && (
        <WebGLBoundary fallback={<div className="hidden" />}>
          <Canvas
            camera={{ position: [0, 0, isMobile ? 6.0 : 6.5], fov: isMobile ? 50 : 45 }}
            gl={{ alpha: true, antialias: !isMobile }}
            dpr={isMobile ? 1 : [1, 1.5]}
          >
            <ambientLight intensity={0.8} />
            <directionalLight position={[5, 5, 5]} intensity={1.0} color="#a7f3d0" />
            <pointLight position={[-4, -3, -2]} intensity={0.6} color="#f59e0b" />

            <group>
              <FloatingMarketNodes count={nodeCount} />
              {!isMobile && <MarketNetworkLines />}
            </group>
          </Canvas>
        </WebGLBoundary>
      )}
    </div>
  );
};
