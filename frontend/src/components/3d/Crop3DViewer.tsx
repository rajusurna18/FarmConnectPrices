import React, { useRef } from 'react';
import { Canvas, useFrame } from '@react-three/fiber';
import * as THREE from 'three';
import { WebGLBoundary } from './WebGLFallback';

interface Crop3DViewerProps {
  color: string;
  category: string;
  isHovered?: boolean;
}

function CropMesh({ color, category, isHovered }: Crop3DViewerProps) {
  const groupRef = useRef<THREE.Group>(null);
  const meshRef = useRef<THREE.Mesh>(null);

  useFrame(({ clock }) => {
    if (groupRef.current) {
      const rotSpeed = isHovered ? 1.5 : 0.5;
      groupRef.current.rotation.y = clock.getElapsedTime() * rotSpeed;
      groupRef.current.rotation.x = Math.sin(clock.getElapsedTime() * 0.8) * 0.2;
    }
  });

  const getGeometry = () => {
    switch (category) {
      case 'Vegetables':
        // Tomato / Onion spherical form
        return <sphereGeometry args={[0.9, 32, 32]} />;
      case 'Cereals':
        // Rice / Wheat crystal grain form
        return <cylinderGeometry args={[0.2, 0.7, 1.6, 8]} />;
      case 'Cash Crops':
        // Cotton fluffy cloud dodecahedron
        return <dodecahedronGeometry args={[0.9, 1]} />;
      case 'Spices':
        // Chili tapered cone
        return <coneGeometry args={[0.6, 1.7, 16]} />;
      default:
        return <octahedronGeometry args={[0.9, 0]} />;
    }
  };

  return (
    <group ref={groupRef}>
      <mesh ref={meshRef}>
        {getGeometry()}
        <meshStandardMaterial
          color={color}
          emissive={color}
          emissiveIntensity={isHovered ? 0.6 : 0.25}
          roughness={0.3}
          metalness={0.6}
        />
      </mesh>
      
      {/* Subtle Glow Ring */}
      <mesh rotation-x={Math.PI / 2} position-y={-1}>
        <ringGeometry args={[0.8, 1.1, 32]} />
        <meshBasicMaterial color={color} transparent opacity={isHovered ? 0.4 : 0.15} side={THREE.DoubleSide} />
      </mesh>
    </group>
  );
}

export const Crop3DViewer: React.FC<Crop3DViewerProps> = (props) => {
  return (
    <div className="w-full h-36 sm:h-44 relative">
      <WebGLBoundary fallback={
        <div className="w-full h-full flex items-center justify-center">
          <div className="w-16 h-16 rounded-full flex items-center justify-center text-3xl shadow-lg border border-slate-700" style={{ backgroundColor: `${props.color}20` }}>
            🌾
          </div>
        </div>
      }>
        <Canvas camera={{ position: [0, 0, 3.8], fov: 45 }} gl={{ alpha: true }}>
          <ambientLight intensity={0.8} />
          <pointLight position={[3, 3, 3]} intensity={1.5} color="#ffffff" />
          <directionalLight position={[-3, 5, 2]} intensity={1} color={props.color} />
          <CropMesh {...props} />
        </Canvas>
      </WebGLBoundary>
    </div>
  );
};
