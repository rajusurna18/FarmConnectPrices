import React, { useRef, useMemo } from 'react';
import { Canvas, useFrame } from '@react-three/fiber';
import * as THREE from 'three';
import { WebGLBoundary } from './WebGLFallback';

function FloatingParticles({ count = 40 }: { count?: number }) {
  const meshRef = useRef<THREE.InstancedMesh>(null);
  const lightRef = useRef<THREE.PointLight>(null);

  const dummy = useMemo(() => new THREE.Object3D(), []);
  
  // Random particle attributes
  const particles = useMemo(() => {
    const temp = [];
    for (let i = 0; i < count; i++) {
      const x = (Math.random() - 0.5) * 12;
      const y = (Math.random() - 0.5) * 8;
      const z = (Math.random() - 0.5) * 6;
      const scale = 0.04 + Math.random() * 0.08;
      const speed = 0.2 + Math.random() * 0.5;
      const factor = Math.random() * Math.PI * 2;
      temp.push({ x, y, z, scale, speed, factor, currentY: y });
    }
    return temp;
  }, [count]);

  useFrame(({ clock }) => {
    if (!meshRef.current) return;
    const time = clock.getElapsedTime();

    particles.forEach((particle, i) => {
      const { x, y, z, scale, speed, factor } = particle;
      
      // Floating animation
      dummy.position.set(
        x + Math.sin(time * speed + factor) * 0.3,
        y + Math.cos(time * speed * 0.8 + factor) * 0.3,
        z + Math.sin(time * speed * 0.5 + factor) * 0.2
      );
      
      dummy.rotation.set(time * 0.2, time * 0.3, 0);
      dummy.scale.setScalar(scale * (1 + Math.sin(time * 2 + factor) * 0.2));
      dummy.updateMatrix();

      meshRef.current!.setMatrixAt(i, dummy.matrix);
    });
    
    meshRef.current.instanceMatrix.needsUpdate = true;

    if (lightRef.current) {
      lightRef.current.position.x = Math.sin(time * 0.5) * 3;
      lightRef.current.position.y = Math.cos(time * 0.3) * 2;
    }
  });

  return (
    <group>
      <pointLight ref={lightRef} distance={8} intensity={2} color="#10b981" />
      <instancedMesh ref={meshRef} args={[undefined, undefined, count]}>
        <octahedronGeometry args={[0.5, 0]} />
        <meshStandardMaterial
          color="#34d399"
          emissive="#059669"
          emissiveIntensity={0.8}
          roughness={0.2}
          metalness={0.8}
          transparent
          opacity={0.85}
        />
      </instancedMesh>
    </group>
  );
}

function NodeConnections() {
  const lineRef = useRef<THREE.LineSegments>(null);

  const { positions } = useMemo(() => {
    const points: number[] = [];
    const nodes = [
      new THREE.Vector3(-3.5, 1.5, 0),
      new THREE.Vector3(-1.2, 0.5, 0.5),
      new THREE.Vector3(1.2, -0.2, 0.2),
      new THREE.Vector3(3.2, -1.2, -0.5)
    ];

    for (let i = 0; i < nodes.length - 1; i++) {
      points.push(nodes[i].x, nodes[i].y, nodes[i].z);
      points.push(nodes[i + 1].x, nodes[i + 1].y, nodes[i + 1].z);
    }

    return { positions: new Float32Array(points) };
  }, []);

  useFrame(({ clock }) => {
    if (lineRef.current) {
      const mat = lineRef.current.material as THREE.LineBasicMaterial;
      mat.opacity = 0.4 + Math.sin(clock.getElapsedTime() * 2) * 0.2;
    }
  });

  return (
    <lineSegments ref={lineRef}>
      <bufferGeometry>
        <bufferAttribute
          attach="attributes-position"
          args={[positions, 3]}
        />
      </bufferGeometry>
      <lineBasicMaterial color="#34d399" transparent opacity={0.5} linewidth={2} />
    </lineSegments>
  );
}

export const Hero3DElements: React.FC = () => {
  // Respect reduced motion
  const prefersReducedMotion = typeof window !== 'undefined' && window.matchMedia('(prefers-reduced-motion: reduce)').matches;

  if (prefersReducedMotion) {
    return null;
  }

  return (
    <div className="absolute inset-0 w-full h-full pointer-events-none z-[1] overflow-hidden opacity-90">
      <WebGLBoundary fallback={<div className="hidden" />}>
        <Canvas
          camera={{ position: [0, 0, 7], fov: 50 }}
          gl={{ alpha: true, antialias: true }}
          dpr={[1, 1.5]}
        >
          <ambientLight intensity={0.6} />
          <directionalLight position={[5, 5, 5]} intensity={1} color="#6ee7b7" />
          <FloatingParticles count={35} />
          <NodeConnections />
        </Canvas>
      </WebGLBoundary>
    </div>
  );
};
