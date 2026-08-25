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
  const lightRef = useRef<THREE.PointLight>(null);
  const dummy = useMemo(() => new THREE.Object3D(), []);

  const particles = useMemo(() => {
    const temp = [];
    for (let i = 0; i < count; i++) {
      const x = (Math.random() - 0.5) * 14;
      const y = (Math.random() - 0.5) * 9;
      const z = (Math.random() - 0.5) * 7;
      const scale = 0.03 + Math.random() * 0.06;
      const speed = 0.15 + Math.random() * 0.35;
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
        x + Math.sin(time * speed + factor) * 0.25,
        y + Math.cos(time * speed * 0.8 + factor) * 0.25,
        z + Math.sin(time * speed * 0.5 + factor) * 0.15
      );
      dummy.rotation.set(time * 0.15, time * 0.2, 0);
      dummy.scale.setScalar(scale * (1 + Math.sin(time * 1.8 + factor) * 0.15));
      dummy.updateMatrix();
      meshRef.current!.setMatrixAt(i, dummy.matrix);
    });

    meshRef.current.instanceMatrix.needsUpdate = true;

    if (lightRef.current) {
      lightRef.current.position.x = Math.sin(time * 0.4) * 4;
      lightRef.current.position.y = Math.cos(time * 0.3) * 3;
    }
  });

  return (
    <group>
      <pointLight ref={lightRef} distance={10} intensity={1.5} color="#10b981" />
      <instancedMesh ref={meshRef} args={[undefined, undefined, count]}>
        <octahedronGeometry args={[0.4, 0]} />
        <meshStandardMaterial
          color="#34d399"
          emissive="#059669"
          emissiveIntensity={0.6}
          roughness={0.3}
          metalness={0.7}
          transparent
          opacity={0.75}
        />
      </instancedMesh>
    </group>
  );
}

// 2. Primary 3D Agricultural Intelligence Sphere
function AgriculturalIntelligenceSphere({ isMobile }: { isMobile?: boolean }) {
  const outerGroupRef = useRef<THREE.Group>(null);
  const coreMeshRef = useRef<THREE.Mesh>(null);
  const ringMeshRef = useRef<THREE.Mesh>(null);

  useFrame(({ clock }) => {
    const time = clock.getElapsedTime();

    if (outerGroupRef.current) {
      outerGroupRef.current.rotation.y = time * 0.12; // Slow, elegant rotation
    }

    if (coreMeshRef.current) {
      coreMeshRef.current.rotation.x = time * 0.08;
      coreMeshRef.current.rotation.z = time * 0.05;
    }

    if (ringMeshRef.current) {
      ringMeshRef.current.rotation.z = -time * 0.15;
    }
  });

  const scaleFactor = isMobile ? 0.65 : 1.0;

  return (
    <group ref={outerGroupRef} scale={[scaleFactor, scaleFactor, scaleFactor]} position={[0, 0.2, 0]}>
      {/* Outer Holographic Shell */}
      <mesh>
        <sphereGeometry args={[1.6, 24, 24]} />
        <meshBasicMaterial color="#10b981" transparent opacity={0.12} wireframe />
      </mesh>

      {/* Orbiting Data Ring */}
      <mesh ref={ringMeshRef} rotation={[Math.PI / 3, 0, 0]}>
        <torusGeometry args={[2.1, 0.015, 16, 64]} />
        <meshStandardMaterial color="#34d399" emissive="#059669" emissiveIntensity={0.8} transparent opacity={0.5} />
      </mesh>

      {/* Primary Tech Core Sphere */}
      <mesh ref={coreMeshRef}>
        <icosahedronGeometry args={[1.1, 1]} />
        <meshStandardMaterial
          color="#047857"
          emissive="#10b981"
          emissiveIntensity={0.5}
          roughness={0.2}
          metalness={0.8}
          wireframe
        />
      </mesh>

      {/* Inner Glowing Kernel */}
      <mesh>
        <sphereGeometry args={[0.55, 16, 16]} />
        <meshStandardMaterial
          color="#f59e0b"
          emissive="#d97706"
          emissiveIntensity={0.9}
          roughness={0.1}
          transparent
          opacity={0.85}
        />
      </mesh>
    </group>
  );
}

// 3. Subtle India & Market Network Nodes
function MarketNetworkConnections() {
  const lineRef = useRef<THREE.LineSegments>(null);

  const { positions } = useMemo(() => {
    const points: number[] = [];
    // Abstract India market node positions in 3D space
    const nodes = [
      new THREE.Vector3(-3.2, 1.4, -1.0),  // Delhi
      new THREE.Vector3(-1.8, 0.6, -0.5),  // Nagpur
      new THREE.Vector3(-0.6, -0.2, 0.2),  // Nizamabad
      new THREE.Vector3(0.0, -0.6, 0.4),   // Hyderabad
      new THREE.Vector3(0.8, -0.4, 0.1),   // Warangal
      new THREE.Vector3(1.6, -0.8, 0.3),   // Vijayawada
      new THREE.Vector3(-2.8, -0.2, 0.0),  // Mumbai
      new THREE.Vector3(-1.0, -1.8, 0.5)   // Bengaluru
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
      mat.opacity = 0.35 + Math.sin(clock.getElapsedTime() * 1.5) * 0.15;
    }
  });

  return (
    <lineSegments ref={lineRef}>
      <bufferGeometry>
        <bufferAttribute attach="attributes-position" args={[positions, 3]} />
      </bufferGeometry>
      <lineBasicMaterial color="#34d399" transparent opacity={0.4} linewidth={1.5} />
    </lineSegments>
  );
}

// 4. Parallax Group with Mouse Parallax (Desktop)
function ParallaxSceneGroup({ mousePos, isMobile, children }: { mousePos: { x: number; y: number }; isMobile: boolean; children: React.ReactNode }) {
  const groupRef = useRef<THREE.Group>(null);

  useFrame(() => {
    if (!groupRef.current || isMobile) return;
    // Smooth lerp to target rotation
    groupRef.current.rotation.y = THREE.MathUtils.lerp(groupRef.current.rotation.y, mousePos.x * 0.12, 0.05);
    groupRef.current.rotation.x = THREE.MathUtils.lerp(groupRef.current.rotation.x, -mousePos.y * 0.08, 0.05);
  });

  return <group ref={groupRef}>{children}</group>;
}

export const Hero3DElements: React.FC<Hero3DElementsProps> = ({ isMobile = false }) => {
  const [mousePos, setMousePos] = useState({ x: 0, y: 0 });

  // Handle Desktop Mouse Movement Parallax
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

  // Respect prefers-reduced-motion
  const prefersReducedMotion = typeof window !== 'undefined' && window.matchMedia('(prefers-reduced-motion: reduce)').matches;

  if (prefersReducedMotion) {
    return null;
  }

  // Mobile adaptive particle count
  const particleCount = isMobile ? 12 : 30;

  return (
    <div className="absolute inset-0 w-full h-full pointer-events-none z-[2] overflow-hidden opacity-90">
      <WebGLBoundary fallback={<div className="hidden" />}>
        <Canvas
          camera={{ position: [0, 0, isMobile ? 8.5 : 7.2], fov: isMobile ? 55 : 50 }}
          gl={{ alpha: true, antialias: true }}
          dpr={[1, isMobile ? 1.2 : 1.5]}
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

