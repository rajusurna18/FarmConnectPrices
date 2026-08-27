import React, { useRef } from 'react';
import { Canvas, useFrame } from '@react-three/fiber';
import * as THREE from 'three';
import { AI_ORBIT_NODES, type AINodePoint } from '../../features/market/data/spatialNodeData';
import { WebGLBoundary } from './WebGLFallback';
import { useInView3D } from './useInView3D';

function CentralCoreSphere() {
  const meshRef = useRef<THREE.Mesh>(null);
  const glowRef = useRef<THREE.Mesh>(null);

  useFrame(({ clock }) => {
    const time = clock.elapsedTime;
    if (meshRef.current) {
      meshRef.current.rotation.y = time * 0.4;
      meshRef.current.rotation.x = time * 0.2;
    }
    if (glowRef.current) {
      const scale = 1 + Math.sin(time * 2) * 0.08;
      glowRef.current.scale.set(scale, scale, scale);
    }
  });

  return (
    <group>
      {/* Outer Holographic Glow Shell */}
      <mesh ref={glowRef}>
        <sphereGeometry args={[1.25, 32, 32]} />
        <meshBasicMaterial color="#10b981" transparent opacity={0.15} wireframe />
      </mesh>

      {/* Primary Tech Sphere Core */}
      <mesh ref={meshRef}>
        <icosahedronGeometry args={[0.9, 2]} />
        <meshStandardMaterial
          color="#059669"
          emissive="#34d399"
          emissiveIntensity={0.6}
          roughness={0.1}
          metalness={0.9}
          wireframe
        />
      </mesh>
    </group>
  );
}

function OrbitingDataNode({ node }: { node: AINodePoint }) {
  const groupRef = useRef<THREE.Group>(null);
  const meshRef = useRef<THREE.Mesh>(null);

  useFrame(({ clock }) => {
    const time = clock.elapsedTime;
    if (groupRef.current) {
      const currentAngle = node.angle + time * node.speed * 0.4;
      groupRef.current.position.x = Math.cos(currentAngle) * node.orbitRadius;
      groupRef.current.position.z = Math.sin(currentAngle) * node.orbitRadius;
      groupRef.current.position.y = Math.sin(time * 1.2 + node.angle) * 0.4;
    }
    if (meshRef.current) {
      meshRef.current.rotation.y = time;
    }
  });

  return (
    <group ref={groupRef}>
      <mesh ref={meshRef}>
        <octahedronGeometry args={[0.16, 0]} />
        <meshStandardMaterial
          color="#f59e0b"
          emissive="#fbbf24"
          emissiveIntensity={0.8}
          roughness={0.2}
        />
      </mesh>
    </group>
  );
}

function ConnectingBeams() {
  const linesRef = useRef<THREE.LineSegments>(null);

  useFrame(({ clock }) => {
    if (linesRef.current) {
      const mat = linesRef.current.material as THREE.LineBasicMaterial;
      mat.opacity = 0.3 + Math.sin(clock.elapsedTime * 3) * 0.15;
    }
  });

  const points: number[] = [];
  AI_ORBIT_NODES.forEach(node => {
    // Ray lines from center to outer orbit
    points.push(0, 0, 0);
    points.push(
      Math.cos(node.angle) * node.orbitRadius,
      0,
      Math.sin(node.angle) * node.orbitRadius
    );
  });

  const positions = new Float32Array(points);

  return (
    <lineSegments ref={linesRef}>
      <bufferGeometry>
        <bufferAttribute attach="attributes-position" args={[positions, 3]} />
      </bufferGeometry>
      <lineBasicMaterial color="#34d399" transparent opacity={0.4} linewidth={1.5} />
    </lineSegments>
  );
}

export const AIDataSphere: React.FC = () => {
  const { containerRef, isInView } = useInView3D<HTMLDivElement>('150px');
  const prefersReducedMotion = typeof window !== 'undefined' && window.matchMedia('(prefers-reduced-motion: reduce)').matches;

  if (prefersReducedMotion) {
    return null;
  }

  return (
    <div ref={containerRef} className="w-full h-80 sm:h-96 relative">
      {isInView && (
        <WebGLBoundary>
          <Canvas camera={{ position: [0, 1.2, 5], fov: 45 }} gl={{ alpha: true, powerPreference: 'low-power' }} dpr={[1, 1.5]}>
            <ambientLight intensity={0.8} />
            <pointLight position={[5, 5, 5]} intensity={1.5} color="#10b981" />
            <pointLight position={[-5, -5, -5]} intensity={0.8} color="#f59e0b" />
            
            <CentralCoreSphere />
            <ConnectingBeams />
            
            {AI_ORBIT_NODES.map(node => (
              <OrbitingDataNode key={node.id} node={node} />
            ))}
          </Canvas>
        </WebGLBoundary>
      )}
    </div>
  );
};
