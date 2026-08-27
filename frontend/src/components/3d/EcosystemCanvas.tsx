import React, { useRef } from 'react';
import { Canvas, useFrame } from '@react-three/fiber';
import * as THREE from 'three';
import { WebGLBoundary } from './WebGLFallback';
import { useInView3D } from './useInView3D';

interface NodeProps {
  position: [number, number, number];
  color: string;
  label: string;
  symbol: string;
  active: boolean;
}

function EcosystemNode({ position, color, active }: NodeProps) {
  const meshRef = useRef<THREE.Mesh>(null);
  const ringRef = useRef<THREE.Mesh>(null);

  useFrame(({ clock }) => {
    const time = clock.getElapsedTime();
    if (meshRef.current) {
      meshRef.current.rotation.y = time * 0.5;
      meshRef.current.position.y = position[1] + Math.sin(time * 1.5) * 0.08;
    }
    if (ringRef.current) {
      ringRef.current.rotation.z = -time * 0.8;
      const scale = 1 + Math.sin(time * 3) * 0.15;
      ringRef.current.scale.set(scale, scale, scale);
    }
  });

  return (
    <group position={position}>
      {/* Outer Pulsing Ring */}
      <mesh ref={ringRef}>
        <ringGeometry args={[0.55, 0.65, 32]} />
        <meshBasicMaterial color={color} transparent opacity={active ? 0.6 : 0.2} side={THREE.DoubleSide} />
      </mesh>

      {/* 3D Core Node Geometry */}
      <mesh ref={meshRef}>
        <icosahedronGeometry args={[0.4, 1]} />
        <meshStandardMaterial
          color={color}
          emissive={color}
          emissiveIntensity={active ? 0.8 : 0.3}
          roughness={0.2}
          metalness={0.8}
          wireframe={!active}
        />
      </mesh>
    </group>
  );
}

function DataPulseStream() {
  const pulseRef = useRef<THREE.Mesh>(null);

  useFrame(({ clock }) => {
    if (!pulseRef.current) return;
    const t = (clock.getElapsedTime() * 0.4) % 1;
    // Animate along path from -4.5 to +4.5
    pulseRef.current.position.x = -4.5 + t * 9;
    pulseRef.current.position.y = Math.sin(t * Math.PI * 3) * 0.2;
  });

  return (
    <mesh ref={pulseRef}>
      <sphereGeometry args={[0.12, 16, 16]} />
      <meshBasicMaterial color="#6ee7b7" />
    </mesh>
  );
}

function ConnectingLines() {
  const linePositions = useRef<Float32Array>(
    new Float32Array([
      -4.5, 0, 0,
      -1.5, 0, 0,
      -1.5, 0, 0,
      1.5, 0, 0,
      1.5, 0, 0,
      4.5, 0, 0
    ])
  );

  return (
    <lineSegments>
      <bufferGeometry>
        <bufferAttribute attach="attributes-position" args={[linePositions.current, 3]} />
      </bufferGeometry>
      <lineBasicMaterial color="#10b981" transparent opacity={0.4} linewidth={3} />
    </lineSegments>
  );
}

export const EcosystemCanvas: React.FC<{ activeStep?: number }> = ({ activeStep = 4 }) => {
  const { containerRef, isInView } = useInView3D<HTMLDivElement>('150px');
  const prefersReducedMotion = typeof window !== 'undefined' && window.matchMedia('(prefers-reduced-motion: reduce)').matches;

  if (prefersReducedMotion) {
    return null;
  }
  const nodes = [
    { position: [-4.5, 0, 0] as [number, number, number], color: '#34d399', label: 'Farm', symbol: '🌾' },
    { position: [-1.5, 0, 0] as [number, number, number], color: '#3b82f6', label: 'Market Data', symbol: '📊' },
    { position: [1.5, 0, 0] as [number, number, number], color: '#f59e0b', label: 'Mediator / Buyer', symbol: '🏪' },
    { position: [4.5, 0, 0] as [number, number, number], color: '#ec4899', label: 'Customer', symbol: '🛒' }
  ];

  return (
    <div ref={containerRef} className="w-full h-48 sm:h-64 relative">
      {isInView && (
        <WebGLBoundary>
          <Canvas camera={{ position: [0, 0, 7], fov: 45 }} gl={{ alpha: true, powerPreference: 'low-power' }} dpr={[1, 1.5]}>
            <ambientLight intensity={0.7} />
            <directionalLight position={[5, 10, 5]} intensity={1.2} />
            <ConnectingLines />
            <DataPulseStream />
            {nodes.map((node, idx) => (
              <EcosystemNode
                key={node.label}
                position={node.position}
                color={node.color}
                label={node.label}
                symbol={node.symbol}
                active={idx < activeStep}
              />
            ))}
          </Canvas>
        </WebGLBoundary>
      )}
    </div>
  );
};
