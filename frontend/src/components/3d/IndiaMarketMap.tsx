import React, { useRef } from 'react';
import { Canvas, useFrame } from '@react-three/fiber';
import * as THREE from 'three';
import { INDIA_MARKET_NODES, type MarketMapNode } from '../../features/market/data/spatialNodeData';
import { WebGLBoundary } from './WebGLFallback';
import { useInView3D } from './useInView3D';

interface IndiaMarketMapProps {
  selectedNodeId?: string;
  onSelectNode?: (node: MarketMapNode) => void;
}

function MarketNodePoint({ node, isSelected, onClick }: { node: MarketMapNode; isSelected: boolean; onClick: () => void }) {
  const meshRef = useRef<THREE.Mesh>(null);
  const ringRef = useRef<THREE.Mesh>(null);

  useFrame(({ clock }) => {
    const time = clock.getElapsedTime();
    if (meshRef.current) {
      meshRef.current.rotation.y = time * 0.8;
    }
    if (ringRef.current) {
      const s = 1 + Math.sin(time * 3 + node.position[0]) * 0.25;
      ringRef.current.scale.set(s, s, s);
    }
  });

  const nodeColor = node.status === 'Major Hub' ? '#10b981' : node.status === 'Regional Node' ? '#f59e0b' : '#3b82f6';

  return (
    <group position={node.position} onClick={onClick}>
      {/* Outer Pulse Ring */}
      <mesh ref={ringRef}>
        <ringGeometry args={[0.18, 0.25, 24]} />
        <meshBasicMaterial color={nodeColor} transparent opacity={isSelected ? 0.8 : 0.4} side={THREE.DoubleSide} />
      </mesh>

      {/* Center Market Point */}
      <mesh ref={meshRef}>
        <sphereGeometry args={[isSelected ? 0.16 : 0.11, 16, 16]} />
        <meshStandardMaterial
          color={nodeColor}
          emissive={nodeColor}
          emissiveIntensity={isSelected ? 0.9 : 0.4}
          roughness={0.2}
        />
      </mesh>
    </group>
  );
}

function ConnectionPaths() {
  const linePositions = useRef<Float32Array | null>(null);

  if (!linePositions.current) {
    const points: number[] = [];
    const nodeMap = new Map(INDIA_MARKET_NODES.map(n => [n.id, n.position]));

    INDIA_MARKET_NODES.forEach(node => {
      node.connectedNodes.forEach(targetId => {
        const targetPos = nodeMap.get(targetId);
        if (targetPos) {
          points.push(node.position[0], node.position[1], node.position[2]);
          points.push(targetPos[0], targetPos[1], targetPos[2]);
        }
      });
    });

    linePositions.current = new Float32Array(points);
  }

  return (
    <lineSegments>
      <bufferGeometry>
        <bufferAttribute
          attach="attributes-position"
          args={[linePositions.current, 3]}
        />
      </bufferGeometry>
      <lineBasicMaterial color="#34d399" transparent opacity={0.35} linewidth={1.5} />
    </lineSegments>
  );
}

function AnimatedOrbitGroup({ children }: { children: React.ReactNode }) {
  const groupRef = useRef<THREE.Group>(null);

  useFrame(({ clock }) => {
    if (groupRef.current) {
      groupRef.current.rotation.y = Math.sin(clock.getElapsedTime() * 0.2) * 0.15;
      groupRef.current.rotation.x = Math.cos(clock.getElapsedTime() * 0.15) * 0.08;
    }
  });

  return <group ref={groupRef}>{children}</group>;
}

export const IndiaMarketMap: React.FC<IndiaMarketMapProps> = ({ selectedNodeId, onSelectNode }) => {
  const { containerRef, isInView } = useInView3D<HTMLDivElement>('150px');
  const prefersReducedMotion = typeof window !== 'undefined' && window.matchMedia('(prefers-reduced-motion: reduce)').matches;

  if (prefersReducedMotion) {
    return null;
  }

  return (
    <div ref={containerRef} className="w-full h-80 sm:h-96 relative">
      {isInView && (
        <WebGLBoundary>
          <Canvas camera={{ position: [0, 0, 4.2], fov: 45 }} gl={{ alpha: true, powerPreference: 'low-power' }} dpr={[1, 1.5]}>
            <ambientLight intensity={0.7} />
            <pointLight position={[5, 5, 5]} intensity={1.2} color="#10b981" />
            <AnimatedOrbitGroup>
              <ConnectionPaths />
              {INDIA_MARKET_NODES.map(node => (
                <MarketNodePoint
                  key={node.id}
                  node={node}
                  isSelected={selectedNodeId === node.id}
                  onClick={() => onSelectNode && onSelectNode(node)}
                />
              ))}
            </AnimatedOrbitGroup>
          </Canvas>
        </WebGLBoundary>
      )}
    </div>
  );
};
