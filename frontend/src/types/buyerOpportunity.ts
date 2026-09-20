export type MatchReasonCode =
  | 'CROP_MATCH'
  | 'QUANTITY_MATCH'
  | 'PARTIAL_QUANTITY'
  | 'QUALITY_MATCH'
  | 'SAME_MARKET'
  | 'SAME_DISTRICT'
  | 'SAME_STATE'
  | 'UNKNOWN_QUALITY'
  | 'UNKNOWN_LOCATION'
  | 'UNKNOWN_QUANTITY';

export type ConfidenceLevel = 'HIGH' | 'MEDIUM' | 'LOW' | 'INSUFFICIENT_DATA';

export interface BuyerOpportunity {
  opportunityId: string;
  buyerRole: string;
  buyerRoleDisplay: string;
  anonymizedLocation: string;
  cropId: string;
  cropName: string;
  typicalQuantity?: number;
  quantityUnit?: string;
  relevanceScore: number;
  confidenceLevel: ConfidenceLevel;
  matchReasons: MatchReasonCode[];
  lastActiveAt?: string;
}

export interface BuyerOpportunityPageResponse {
  items: BuyerOpportunity[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  hasNext: boolean;
  statusState: 'SUCCESS' | 'NO_MATCHES' | 'INSUFFICIENT_DATA';
}
