/**
 * Module 24 — Order Issues, Disputes & Resolution Foundation
 * TypeScript domain models, enums, DTOs, and utility definitions for Disputes.
 */

/**
 * Three primary participant profiles supported by FarmLink AI.
 */
export type ParticipantRole = 'FARMER' | 'MEDIATOR_BUYER' | 'CUSTOMER';

/**
 * History audit trail actor roles, including internal SYSTEM actor.
 */
export type HistoryActorRole = ParticipantRole | 'SYSTEM';

/**
 * Supported dispute issue categories matching backend DisputeCategory.java.
 */
export type DisputeCategory =
  | 'PRODUCT_QUALITY'
  | 'DAMAGED_PRODUCT'
  | 'QUANTITY_MISMATCH'
  | 'WRONG_PRODUCT'
  | 'DELIVERY_DELAY'
  | 'DELIVERY_NOT_RECEIVED'
  | 'PAYMENT_ISSUE'
  | 'ORDER_CANCELLATION'
  | 'OTHER';

/**
 * Dispute lifecycle statuses matching backend DisputeStatus.java.
 */
export type DisputeStatus =
  | 'OPEN'
  | 'UNDER_REVIEW'
  | 'WAITING_FOR_INFORMATION'
  | 'RESOLVED'
  | 'REJECTED'
  | 'CLOSED';

/**
 * Audit trail actions matching backend DisputeHistoryAction.java.
 */
export type DisputeHistoryAction =
  | 'DISPUTE_CREATED'
  | 'RESPONSE_ADDED'
  | 'EVIDENCE_ADDED'
  | 'STATUS_CHANGED'
  | 'INFORMATION_REQUESTED'
  | 'INFORMATION_SUBMITTED'
  | 'DISPUTE_RESOLVED'
  | 'DISPUTE_REJECTED'
  | 'DISPUTE_CLOSED';

/**
 * Adjudication resolution outcomes (read-only domain representation; adjudication is privileged).
 */
export type DisputeResolutionOutcome =
  | 'FULL_REFUND'
  | 'PARTIAL_REFUND'
  | 'REPLACEMENT_DELIVERED'
  | 'ORDER_CANCELLED'
  | 'NO_ACTION'
  | 'MUTUAL_AGREEMENT';

/**
 * Single dispute summary / list item matching DisputeResponse.java.
 */
export interface DisputeResponse {
  disputeId: string;
  orderId: string;
  listingId: string;
  offerId: string;
  cropId: string;
  cropName: string;
  createdByUid: string;
  createdByRole: ParticipantRole | string;
  farmerUid: string;
  buyerUid: string;
  buyerRole: ParticipantRole | string;
  issueCategory: DisputeCategory | string;
  subject: string;
  description: string;
  status: DisputeStatus | string;
  resolutionSummary?: string | null;
  resolutionOutcome?: DisputeResolutionOutcome | string | null;
  resolvedBy?: string | null;
  resolvedAt?: string | null;
  closedAt?: string | null;
  createdAt: string;
  updatedAt: string;
  lastActivityAt: string;
}

/**
 * Counterparty communication record matching DisputeResponseRecord.java.
 */
export interface DisputeResponseRecord {
  responseId: string;
  disputeId: string;
  authorUid: string;
  authorRole: ParticipantRole | string;
  message: string;
  createdAt: string;
}

/**
 * Supporting evidence metadata matching DisputeEvidence.java.
 * Strictly metadata-only; no client storage path is accepted or implied.
 */
export interface DisputeEvidence {
  evidenceId: string;
  disputeId: string;
  registeredByUid: string;
  registeredByRole: ParticipantRole | string;
  fileName: string;
  storagePath?: string | null;
  contentType: string;
  fileSize: number;
  description: string;
  createdAt: string;
}

/**
 * Immutable audit trail entry matching DisputeHistoryEntry.java.
 */
export interface DisputeHistoryEntry {
  historyId: string;
  disputeId: string;
  actorUid: string;
  actorRole: HistoryActorRole | string;
  actionType: DisputeHistoryAction | string;
  previousStatus?: string | null;
  newStatus?: string | null;
  description: string;
  createdAt: string;
}

/**
 * Dispute details payload matching DisputeDetailResponse.java.
 */
export interface DisputeDetailResponse {
  dispute: DisputeResponse;
  responses: DisputeResponseRecord[];
  evidence: DisputeEvidence[];
  history: DisputeHistoryEntry[];
}

/**
 * Page response matching DisputePageResponse.java.
 * Supports cursor-based pagination with opaque nextCursor.
 */
export interface DisputePageResponse {
  items: DisputeResponse[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  hasNext: boolean;
  nextCursor?: string | null;
  content?: DisputeResponse[];
  pageSize?: number;
}

/**
 * Request payload for creating a dispute.
 */
export interface CreateDisputeRequest {
  orderId: string;
  issueCategory: DisputeCategory | string;
  subject: string;
  description: string;
}

/**
 * Request payload for adding a participant response message.
 */
export interface AddDisputeResponseRequest {
  message: string;
}

/**
 * Request payload for registering evidence metadata.
 * Note: storagePath is server-assigned/restricted and must not be supplied by the client.
 */
export interface AddDisputeEvidenceRequest {
  fileName: string;
  contentType: string;
  fileSize: number;
  description: string;
}

/**
 * Query parameters for paginated dispute listings.
 */
export interface DisputeQueryParams {
  status?: string;
  page?: number;
  size?: number;
  cursor?: string;
}

/**
 * Human-readable label mappings for UI and badge rendering.
 */
export const DISPUTE_CATEGORY_LABELS: Record<DisputeCategory, string> = {
  PRODUCT_QUALITY: 'Product Quality Issue',
  DAMAGED_PRODUCT: 'Damaged Product',
  QUANTITY_MISMATCH: 'Quantity Mismatch',
  WRONG_PRODUCT: 'Wrong Product Delivered',
  DELIVERY_DELAY: 'Delivery Delay',
  DELIVERY_NOT_RECEIVED: 'Delivery Not Received',
  PAYMENT_ISSUE: 'Payment Issue',
  ORDER_CANCELLATION: 'Order Cancellation Issue',
  OTHER: 'Other Commercial Issue',
};

export const DISPUTE_STATUS_LABELS: Record<DisputeStatus, string> = {
  OPEN: 'Open',
  UNDER_REVIEW: 'Under Review',
  WAITING_FOR_INFORMATION: 'Waiting for Information',
  RESOLVED: 'Resolved',
  REJECTED: 'Rejected',
  CLOSED: 'Closed',
};

/**
 * Helper to test whether a dispute is active (can receive responses / evidence).
 */
export function isDisputeActive(status: DisputeStatus | string): boolean {
  return status === 'OPEN' || status === 'UNDER_REVIEW' || status === 'WAITING_FOR_INFORMATION';
}

/**
 * Helper to test whether a dispute is in a terminal state.
 */
export function isDisputeTerminal(status: DisputeStatus | string): boolean {
  return status === 'RESOLVED' || status === 'REJECTED' || status === 'CLOSED';
}

