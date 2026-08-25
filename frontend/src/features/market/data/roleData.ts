/**
 * ARCHITECTURAL ROLE DEFINITIONS FOR FARMCONNECTPRICES
 * 
 * Defines the three primary system profiles:
 * 1. FARMER
 * 2. MEDIATOR_BUYER
 * 3. CUSTOMER
 */

export interface RoleCardData {
  roleKey: 'FARMER' | 'MEDIATOR_BUYER' | 'CUSTOMER';
  title: string;
  icon: string;
  tagline: string;
  features: string[];
  ctaText: string;
  badgeText: string;
}

export const PRIMARY_ROLE_CARDS: RoleCardData[] = [
  {
    roleKey: 'FARMER',
    title: 'Farmer',
    icon: '🌾',
    tagline: 'Sell smarter. Discover better markets. Make data-driven decisions.',
    features: [
      'Real-time market price discovery across regional Mandis',
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
    badgeText: 'For Consumers & Retailers'
  }
];
