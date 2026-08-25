/**
 * 3D SPATIAL COORDINATE NODES FOR PLATFORM VISUALIZATIONS
 * 
 * Contains normalized 3D position coordinates and orbital angles
 * for Three.js / R3F canvases (IndiaMarketMap and AIDataSphere).
 * Contains NO financial prices, mock percentages, or fake rates.
 */

export interface MarketMapNode {
  id: string;
  name: string;
  state: string;
  position: [number, number, number];
  status: 'Major Hub' | 'Regional Node' | 'Logistics Hub';
  connectedNodes: string[];
}

export interface AINodePoint {
  id: string;
  label: string;
  category: string;
  status: string;
  orbitRadius: number;
  speed: number;
  angle: number;
}

// Normalized 3D position coordinates for India Mandi Map Visualization
export const INDIA_MARKET_NODES: MarketMapNode[] = [
  { id: 'hyd', name: 'Hyderabad', state: 'Telangana', position: [0.1, -0.2, 0.1], status: 'Major Hub', connectedNodes: ['wgl', 'nzb', 'vjw', 'blr'] },
  { id: 'wgl', name: 'Warangal', state: 'Telangana', position: [0.3, -0.1, 0.0], status: 'Regional Node', connectedNodes: ['hyd', 'vjw', 'ngp'] },
  { id: 'nzb', name: 'Nizamabad', state: 'Telangana', position: [-0.1, 0.0, 0.2], status: 'Logistics Hub', connectedNodes: ['hyd', 'ngp'] },
  { id: 'vjw', name: 'Vijayawada', state: 'Andhra Pradesh', position: [0.4, -0.3, 0.2], status: 'Major Hub', connectedNodes: ['hyd', 'wgl', 'blr'] },
  { id: 'ngp', name: 'Nagpur', state: 'Maharashtra', position: [0.0, 0.4, -0.1], status: 'Major Hub', connectedNodes: ['nzb', 'del', 'bom'] },
  { id: 'del', name: 'Delhi NCR', state: 'Delhi', position: [-0.2, 1.2, -0.3], status: 'Regional Node', connectedNodes: ['ngp', 'bom'] },
  { id: 'bom', name: 'Mumbai', state: 'Maharashtra', position: [-0.9, 0.2, 0.0], status: 'Major Hub', connectedNodes: ['ngp', 'blr', 'del'] },
  { id: 'blr', name: 'Bengaluru', state: 'Karnataka', position: [-0.2, -0.9, 0.3], status: 'Regional Node', connectedNodes: ['hyd', 'vjw', 'bom'] }
];

// 3D Orbital Angle Constants for Neural Core Visual Canvas
export const AI_ORBIT_NODES: AINodePoint[] = [
  { id: 'price-feed', label: 'Wholesale Mandi Feeds', category: 'Data Pipeline', status: 'Architecture Active', orbitRadius: 2.2, speed: 0.8, angle: 0 },
  { id: 'demand-ai', label: 'Demand Forecasting', category: 'Predictive', status: 'Model Ready', orbitRadius: 2.6, speed: 0.6, angle: 1.0 },
  { id: 'supply-map', label: 'Supply Logistics Flow', category: 'Logistics', status: 'Telemetry Ready', orbitRadius: 2.0, speed: 0.9, angle: 2.1 },
  { id: 'history-trend', label: 'Seasonal Pattern Analysis', category: 'Analytics', status: 'Engine Ready', orbitRadius: 2.8, speed: 0.5, angle: 3.4 },
  { id: 'buyer-activity', label: 'Mediator Connectivity', category: 'Marketplace', status: 'Pipeline Ready', orbitRadius: 2.4, speed: 0.7, angle: 4.5 },
  { id: 'weather-ai', label: 'Harvest Window Telemetry', category: 'Climate', status: 'Standing By', orbitRadius: 2.1, speed: 1.1, angle: 5.6 }
];
