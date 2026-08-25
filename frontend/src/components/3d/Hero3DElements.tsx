import React, { useRef, useMemo, useEffect, useState } from 'react';
import { Canvas, useFrame } from '@react-three/fiber';
import * as THREE from 'three';
import { WebGLBoundary } from './WebGLFallback';

interface Hero3DElementsProps {
  isMobile?: boolean;
}

// 1. Floating Agricultural & Data Particles
function FloatingParticles({ count = 30 }: { count?: number }) {
  const meshRef = useRef<THREE.InstancedMesh>(null);
  const dummy = useMemo(() => new THREE.Object3D(), []);

  const particles = useMemo(() => {
    const temp = [];
    for (let i = 0; i < count; i++) {
      const x = (Math.random() - 0.5) * (count < 10 ? 5 : 14);
      const y = (Math.random() - 0.5) * (count < 10 ? 4 : 9);
      const z = (Math.random() - 0.5) * (count < 10 ? 2 : 7);
      const scale = 0.02 + Math.random() * 0.04;
      const speed = 0.08 + Math.random() * 0.18;
      const factor = Math.random() * Math.PI * 2;
      temp.push({ x, y, z, scale, speed, factor });
    }
    return temp;
  }, [count]);

  useFrame(({ clock }) => {
    if (!meshRef.current) return;
    const time = clock.getElapsedTime();

    particles.forEach((particle, i) => {
      const { x, y, z, scale, speed, factor } = particle;
      dummy.position.set(
        x + Math.sin(time * speed + factor) * 0.15,
        y + Math.cos(time * speed * 0.8 + factor) * 0.15,
        z + Math.sin(time * speed * 0.5 + factor) * 0.08
      );
      dummy.rotation.set(time * 0.08, time * 0.1, 0);
      dummy.scale.setScalar(scale * (1 + Math.sin(time * 1.2 + factor) * 0.1));
      dummy.updateMatrix();
      meshRef.current!.setMatrixAt(i, dummy.matrix);
    });

    meshRef.current.instanceMatrix.needsUpdate = true;
  });

  return (
    <group>
      <instancedMesh ref={meshRef} args={[undefined, undefined, count]}>
        <octahedronGeometry args={[0.3, 0]} />
        <meshStandardMaterial
          color="#34d399"
          emissive="#059669"
          emissiveIntensity={0.5}
          roughness={0.4}
          metalness={0.6}
          transparent
          opacity={0.65}
        />
      </instancedMesh>
    </group>
  );
}

// 2. Primary 3D Agricultural Intelligence Sphere Core (Safely in background)
function AgriculturalIntelligenceSphere({ isMobile }: { isMobile?: boolean }) {
  const outerGroupRef = useRef<THREE.Group>(null);
  const coreMeshRef = useRef<THREE.Mesh>(null);
  const ringMeshRef = useRef<THREE.Mesh>(null);

  useFrame(({ clock }) => {
    const time = clock.getElapsedTime();

    if (outerGroupRef.current) {
      outerGroupRef.current.rotation.y = time * (isMobile ? 0.04 : 0.09);
    }

    if (coreMeshRef.current) {
      coreMeshRef.current.rotation.x = time * 0.04;
      coreMeshRef.current.rotation.z = time * 0.02;
    }

    if (ringMeshRef.current) {
      ringMeshRef.current.rotation.z = -time * 0.06;
    }
  });

  // Background position framing: On mobile, pushed far back (z = -1.5) and scaled down to prevent overlapping text/video
  const position: [number, number, number] = isMobile ? [0, 1.8, -1.5] : [0, 0.9, -1.2];
  const scaleFactor = isMobile ? 0.28 : 0.72;

  return (
    <group ref={outerGroupRef} scale={[scaleFactor, scaleFactor, scaleFactor]} position={position}>
      {/* Outer Holographic Grid Shell */}
      <mesh>
        <sphereGeometry args={[1.5, isMobile ? 12 : 24, isMobile ? 12 : 24]} />
        <meshBasicMaterial color="#10b981" transparent opacity={isMobile ? 0.08 : 0.12} wireframe />
      </mesh>

      {/* Orbiting Data Torus Ring */}
      <mesh ref={ringMeshRef} rotation={[Math.PI / 3.5, 0, 0]}>
        <torusGeometry args={[1.9, 0.015, 10, isMobile ? 24 : 64]} />
        <meshStandardMaterial color="#34d399" emissive="#059669" emissiveIntensity={0.5} transparent opacity={isMobile ? 0.3 : 0.45} />
      </mesh>

      {/* Primary Tech Core Sphere */}
      <mesh ref={coreMeshRef}>
        <icosahedronGeometry args={[1.05, 1]} />
        <meshStandardMaterial
          color="#047857"
          emissive="#10b981"
          emissiveIntensity={0.35}
          roughness={0.3}
          metalness={0.7}
          wireframe
        />
      </mesh>

      {/* Inner Glowing Kernel */}
      <mesh>
        <sphereGeometry args={[0.5, 12, 12]} />
        <meshStandardMaterial
          color="#f59e0b"
          emissive="#d97706"
          emissiveIntensity={0.6}
          roughness={0.2}
          transparent
          opacity={isMobile ? 0.5 : 0.75}
        />
      </mesh>
    </group>
  );
}

// 3. Subtle India & Market Network Nodes (Desktop Only)
function MarketNetworkConnections() {
  const lineRef = useRef<THREE.LineSegments>(null);

  const { positions } = useMemo(() => {
    const points: number[] = [];
    const nodes = [
      new THREE.Vector3(-3.2, 1.4, -1.0),
      new THREE.Vector3(-1.8, 0.6, -0.5),
      new THREE.Vector3(-0.6, -0.2, 0.2),
      new THREE.Vector3(0.0, -0.6, 0.4),
      new THREE.Vector3(0.8, -0.4, 0.1),
      new THREE.Vector3(1.6, -0.8, 0.3),
      new THREE.Vector3(-2.8, -0.2, 0.0),
      new THREE.Vector3(-1.0, -1.8, 0.5)
    ];

    const connections = [
      [0, 1], [1, 2], [2, 3], [3, 4], [4, 5], [3, 7], [1, 6], [6, 7]
    ];

    connections.forEach(([i, j]) => {
      points.push(nodes[i].x, nodes[i].y, nodes[i].z);
      points.push(nodes[j].x, nodes[j].y, nodes[j].z);
    });

    return { positions: new Float32Array(points) };
  }, []);

  useFrame(({ clock }) => {
    if (lineRef.current) {
      const mat = lineRef.current.material as THREE.LineBasicMaterial;
      mat.opacity = 0.25 + Math.sin(clock.getElapsedTime() * 1.5) * 0.1;
    }
  });

  return (
    <lineSegments ref={lineRef}>
      <bufferGeometry>
        <bufferAttribute attach="attributes-position" args={[positions, 3]} />
      </bufferGeometry>
      <lineBasicMaterial color="#34d399" transparent opacity={0.3} linewidth={1.5} />
    </lineSegments>
  );
}

// 4. Parallax Group with Mouse Parallax (Desktop Only)
function ParallaxSceneGroup({ mousePos, isMobile, children }: { mousePos: { x: number; y: number }; isMobile: boolean; children: React.ReactNode }) {
  const groupRef = useRef<THREE.Group>(null);

  useFrame(() => {
    if (!groupRef.current || isMobile) return;
    groupRef.current.rotation.y = THREE.MathUtils.lerp(groupRef.current.rotation.y, mousePos.x * 0.08, 0.04);
    groupRef.current.rotation.x = THREE.MathUtils.lerp(groupRef.current.rotation.x, -mousePos.y * 0.05, 0.04);
  });

  return <group ref={groupRef}>{children}</group>;
}

export const Hero3DElements: React.FC<Hero3DElementsProps> = ({ isMobile = false }) => {
  const [mousePos, setMousePos] = useState({ x: 0, y: 0 });

  useEffect(() => {
    if (isMobile) return;

    const handleMouseMove = (e: MouseEvent) => {
      const x = (e.clientX / window.innerWidth) * 2 - 1;
      const y = -(e.clientY / window.innerHeight) * 2 + 1;
      setMousePos({ x, y });
    };

    window.addEventListener('mousemove', handleMouseMove);
    return () => window.removeEventListener('mousemove', handleMouseMove);
  }, [isMobile]);

  const prefersReducedMotion = typeof window !== 'undefined' && window.matchMedia('(prefers-reduced-motion: reduce)').matches;

  if (prefersReducedMotion) {
    return null;
  }

  const particleCount = isMobile ? 3 : 20;

  return (
    <div className="absolute inset-0 w-full h-full pointer-events-none z-20 overflow-hidden opacity-75">
      <WebGLBoundary fallback={<div className="hidden" />}>
        <Canvas
          camera={{ position: [0, 0, isMobile ? 7.2 : 7.2], fov: isMobile ? 52 : 50 }}
          gl={{ alpha: true, antialias: !isMobile }}
          dpr={isMobile ? 1 : [1, 1.5]}
        >
          <ambientLight intensity={0.7} />
          <directionalLight position={[6, 6, 6]} intensity={1.2} color="#a7f3d0" />
          <pointLight position={[-5, -4, -3]} intensity={0.8} color="#f59e0b" />

          <ParallaxSceneGroup mousePos={mousePos} isMobile={isMobile}>
            <AgriculturalIntelligenceSphere isMobile={isMobile} />
            <FloatingParticles count={particleCount} />
            {!isMobile && <MarketNetworkConnections />}
          </ParallaxSceneGroup>
        </Canvas>
      </WebGLBoundary>
    </div>
  );
};
