/**
 * DEMO / MOCK DATA FOR FARMCONNECTPRICES 3D MARKET INTELLIGENCE
 * 
 * IMPORTANT: This mock data is strictly separated from backend architecture.
 * It provides realistic visual data for 3D visualizations, market trends,
 * crop cards, and role experience previews until backend market APIs are linked.
 */

export interface CropMarketData {
  id: string;
  name: string;
  category: 'Cereals' | 'Vegetables' | 'Cash Crops' | 'Spices';
  pricePerQuintal: number;
  unit: string;
  marketLocation: string;
  state: string;
  trendPercentage: number;
  isTrendingUp: boolean;
  demandLevel: 'High' | 'Very High' | 'Moderate';
  qualityGrade: string;
  color: string;
  description: string;
}

export interface MarketNode {
  id: string;
  name: string;
  state: string;
  position: [number, number, number]; // [x, y, z] for 3D map representation
  status: 'Active' | 'High Demand' | 'Major Hub';
  volumeQuintals: number;
  connectedNodes: string[];
}

export interface PriceTrendPoint {
  date: string;
  tomatoPrice: number;
  ricePrice: number;
  wheatPrice: number;
  chilliPrice: number;
}

export interface AIDataPoint {
  id: string;
  label: string;
  category: string;
  value: string;
  status: string;
  orbitRadius: number;
  speed: number;
  angle: number;
}

// Interactive Mock Crop Data
export const MOCK_CROPS: CropMarketData[] = [
  {
    id: 'rice-bsw',
    name: 'Rice (BPT 5204)',
    category: 'Cereals',
    pricePerQuintal: 3200,
    unit: 'Quintal',
    marketLocation: 'Hyderabad Market',
    state: 'Telangana',
    trendPercentage: 8.4,
    isTrendingUp: true,
    demandLevel: 'Very High',
    qualityGrade: 'Grade A Sona Masoori',
    color: '#eab308', // Amber / Rice Golden
    description: 'High demand premium Sona Masoori variety with steady inter-state transport trends.'
  },
  {
    id: 'tomato-hybrid',
    name: 'Tomato (Hybrid)',
    category: 'Vegetables',
    pricePerQuintal: 2850,
    unit: 'Quintal',
    marketLocation: 'Warangal Market',
    state: 'Telangana',
    trendPercentage: 7.8,
    isTrendingUp: true,
    demandLevel: 'Very High',
    qualityGrade: 'Premium Red',
    color: '#ef4444', // Red
    description: 'Rapid demand surge driven by regional market supply constraints.'
  },
  {
    id: 'wheat-durum',
    name: 'Wheat (Sharbati)',
    category: 'Cereals',
    pricePerQuintal: 2450,
    unit: 'Quintal',
    marketLocation: 'Nagpur Market',
    state: 'Maharashtra',
    trendPercentage: -1.5,
    isTrendingUp: false,
    demandLevel: 'Moderate',
    qualityGrade: 'Standard Superfine',
    color: '#f59e0b', // Amber
    description: 'Stable supply arrivals maintaining steady wholesale pricing.'
  },
  {
    id: 'cotton-long-staple',
    name: 'Cotton (Long Staple)',
    category: 'Cash Crops',
    pricePerQuintal: 7100,
    unit: 'Quintal',
    marketLocation: 'Nizamabad Market',
    state: 'Telangana',
    trendPercentage: 12.3,
    isTrendingUp: true,
    demandLevel: 'High',
    qualityGrade: 'Export Grade',
    color: '#f8fafc', // White / Cotton
    description: 'Strong textile hub demand creating upward price momentum.'
  },
  {
    id: 'maize-yellow',
    name: 'Maize (Yellow)',
    category: 'Cereals',
    pricePerQuintal: 2150,
    unit: 'Quintal',
    marketLocation: 'Vijayawada Market',
    state: 'Andhra Pradesh',
    trendPercentage: 4.2,
    isTrendingUp: true,
    demandLevel: 'High',
    qualityGrade: 'Grade 1 Feed & Grain',
    color: '#fbbf24', // Yellow
    description: 'Consistent buyer bids from poultry and food processing industries.'
  },
  {
    id: 'chilli-teja',
    name: 'Teja Chilli (Red)',
    category: 'Spices',
    pricePerQuintal: 18500,
    unit: 'Quintal',
    marketLocation: 'Warangal Market',
    state: 'Telangana',
    trendPercentage: 14.7,
    isTrendingUp: true,
    demandLevel: 'Very High',
    qualityGrade: 'Teja A1 Premium',
    color: '#dc2626', // Crimson Red
    description: 'High export buyer activity driving competitive bidding.'
  },
  {
    id: 'onion-nashik',
    name: 'Onion (Red Nashik)',
    category: 'Vegetables',
    pricePerQuintal: 1950,
    unit: 'Quintal',
    marketLocation: 'Bengaluru Market',
    state: 'Karnataka',
    trendPercentage: -3.8,
    isTrendingUp: false,
    demandLevel: 'Moderate',
    qualityGrade: 'Medium Red',
    color: '#a855f7', // Purple / Red Onion
    description: 'Fresh harvest arrivals balancing urban retail demand.'
  }
];

// India Agricultural Market Nodes (Normalized visual 3D position coordinates)
export const MOCK_INDIA_MARKETS: MarketNode[] = [
  { id: 'hyd', name: 'Hyderabad', state: 'Telangana', position: [0.1, -0.2, 0.1], status: 'Major Hub', volumeQuintals: 45000, connectedNodes: ['wgl', 'nzb', 'vjw', 'blr'] },
  { id: 'wgl', name: 'Warangal', state: 'Telangana', position: [0.3, -0.1, 0.0], status: 'High Demand', volumeQuintals: 32000, connectedNodes: ['hyd', 'vjw', 'ngp'] },
  { id: 'nzb', name: 'Nizamabad', state: 'Telangana', position: [-0.1, 0.0, 0.2], status: 'Active', volumeQuintals: 28000, connectedNodes: ['hyd', 'ngp'] },
  { id: 'vjw', name: 'Vijayawada', state: 'Andhra Pradesh', position: [0.4, -0.3, 0.2], status: 'Major Hub', volumeQuintals: 38000, connectedNodes: ['hyd', 'wgl', 'blr'] },
  { id: 'ngp', name: 'Nagpur', state: 'Maharashtra', position: [0.0, 0.4, -0.1], status: 'Major Hub', volumeQuintals: 52000, connectedNodes: ['nzb', 'del', 'bom'] },
  { id: 'del', name: 'Delhi NCR', state: 'Delhi', position: [-0.2, 1.2, -0.3], status: 'High Demand', volumeQuintals: 78000, connectedNodes: ['ngp', 'bom'] },
  { id: 'bom', name: 'Mumbai', state: 'Maharashtra', position: [-0.9, 0.2, 0.0], status: 'Major Hub', volumeQuintals: 85000, connectedNodes: ['ngp', 'blr', 'del'] },
  { id: 'blr', name: 'Bengaluru', state: 'Karnataka', position: [-0.2, -0.9, 0.3], status: 'High Demand', volumeQuintals: 62000, connectedNodes: ['hyd', 'vjw', 'bom'] }
];

// 7-Day Animated Price Trend Data
export const MOCK_PRICE_TRENDS: PriceTrendPoint[] = [
  { date: 'Mon', tomatoPrice: 2600, ricePrice: 3100, wheatPrice: 2480, chilliPrice: 17800 },
  { date: 'Tue', tomatoPrice: 2640, ricePrice: 3120, wheatPrice: 2470, chilliPrice: 18000 },
  { date: 'Wed', tomatoPrice: 2710, ricePrice: 3150, wheatPrice: 2465, chilliPrice: 18150 },
  { date: 'Thu', tomatoPrice: 2750, ricePrice: 3175, wheatPrice: 2460, chilliPrice: 18300 },
  { date: 'Fri', tomatoPrice: 2790, ricePrice: 3180, wheatPrice: 2455, chilliPrice: 18350 },
  { date: 'Sat', tomatoPrice: 2820, ricePrice: 3190, wheatPrice: 2450, chilliPrice: 18420 },
  { date: 'Sun', tomatoPrice: 2850, ricePrice: 3200, wheatPrice: 2450, chilliPrice: 18500 }
];

// AI Data Sphere Orbit Points
export const MOCK_AI_NODES: AIDataPoint[] = [
  { id: 'price-feed', label: 'Market Prices', category: 'Live Feeds', value: '14,200 Nodes Syncing', status: 'Optimal', orbitRadius: 2.2, speed: 0.8, angle: 0 },
  { id: 'demand-ai', label: 'Demand Surge', category: 'Predictive', value: '+14% Urban Demand', status: 'High Confidence', orbitRadius: 2.6, speed: 0.6, angle: 1.0 },
  { id: 'supply-map', label: 'Supply Forecast', category: 'Logistics', value: '2,400 MT In-Transit', status: 'Real-Time', orbitRadius: 2.0, speed: 0.9, angle: 2.1 },
  { id: 'history-trend', label: 'Historical Trends', category: 'Analytics', value: '5-Year Seasonality', status: 'Verified', orbitRadius: 2.8, speed: 0.5, angle: 3.4 },
  { id: 'buyer-activity', label: 'Mediator Activity', category: 'Marketplace', value: '1,850 Verified Buyers', status: 'Active Bidding', orbitRadius: 2.4, speed: 0.7, angle: 4.5 },
  { id: 'weather-ai', label: 'Climate Intelligence', category: 'Weather', value: 'Favorable Harvest Window', status: 'Updated 5m ago', orbitRadius: 2.1, speed: 1.1, angle: 5.6 }
];

// Role Information matching exact architectural definitions
export const MOCK_ROLE_CARDS = [
  {
    roleKey: 'FARMER',
    title: 'Farmer',
    icon: '🌾',
    tagline: 'Sell smarter. Discover better markets. Make data-driven decisions.',
    features: [
      'Real-time market price discovery across nearby mandis',
      'AI recommendation engine for optimal harvest selling time',
      'Direct connectivity with verified Mediators / Buyers'
    ],
    ctaText: 'Explore Opportunities',
    badgeText: 'For Agricultural Producers'
  },
  {
    roleKey: 'MEDIATOR_BUYER',
    title: 'Mediator / Buyer',
    icon: '🏪',
    tagline: 'Discover supply. Connect with farmers. Find better opportunities.',
    features: [
      'Direct access to regional crop harvest volumes',
      'Transparent pricing analytics and quality grading',
      'Streamlined negotiation and supply chain coordination'
    ],
    ctaText: 'Explore Supply',
    badgeText: 'For Aggregators & Buyers'
  },
  {
    roleKey: 'CUSTOMER',
    title: 'Customer',
    icon: '🛒',
    tagline: 'Discover products. Understand their source. Buy with transparency.',
    features: [
      'Full farm-to-table origin transparency and traceability',
      'Fair market price baseline insight for farm products',
      'High-quality fresh produce discovery'
    ],
    ctaText: 'Explore Products',
    badgeText: 'For Consumers & Retails'
  }
];
