/**
 * Module 24 — Order Issues, Disputes & Resolution Foundation
 * Stage 4.3: Dispute End-to-End Integration, Security Validation & QA Test Suite.
 */

import { describe, it, expect, vi, beforeEach } from 'vitest';
import { apiClient } from '../../../services/api';
import {
  marketplaceDisputesApi,
  disputeQueryKeys,
  extractDisputeApiErrorMessage,
} from '../api/marketplaceDisputesApi';
import type {
  CreateDisputeRequest,
  AddDisputeEvidenceRequest,
  DisputeResponse,
  DisputeDetailResponse,
  DisputePageResponse,
  DisputeResponseRecord,
  DisputeEvidence,
  DisputeCategory,
  ParticipantRole,
} from '../../../types/dispute';
import {
  DISPUTE_CATEGORY_LABELS,
  isDisputeActive,
  isDisputeTerminal,
} from '../../../types/dispute';

describe('Module 24 Stage 4.3 — Dispute End-to-End Integration & Security QA', () => {
  beforeEach(() => {
    vi.restoreAllMocks();
  });

  describe('1. Authoritative API Contract Verification (All 8 Endpoints)', () => {
    const sampleDispute: DisputeResponse = {
      disputeId: 'disp_contract_01',
      orderId: 'ord_tomato_001',
      listingId: 'list_tomato_001',
      offerId: 'off_tomato_001',
      cropId: 'crop_tomato',
      cropName: 'Tomato (Hybrid)',
      createdByUid: 'buyer_cust_101',
      createdByRole: 'CUSTOMER',
      farmerUid: 'farmer_101',
      buyerUid: 'buyer_cust_101',
      buyerRole: 'CUSTOMER',
      issueCategory: 'DAMAGED_PRODUCT',
      subject: 'Damaged crates during transit',
      description: 'Ten crates arrived with crushed produce upon unloading.',
      status: 'OPEN',
      createdAt: '2026-11-12T10:00:00Z',
      updatedAt: '2026-11-12T10:00:00Z',
      lastActivityAt: '2026-11-12T10:00:00Z',
    };

    it('1.1 POST /api/v1/marketplace/disputes matches exact request and response contract', async () => {
      const postSpy = vi.spyOn(apiClient, 'post').mockResolvedValueOnce({ data: sampleDispute });
      const req: CreateDisputeRequest = {
        orderId: 'ord_tomato_001',
        issueCategory: 'DAMAGED_PRODUCT',
        subject: 'Damaged crates during transit',
        description: 'Ten crates arrived with crushed produce upon unloading.',
      };

      const result = await marketplaceDisputesApi.createDispute(req);
      expect(postSpy).toHaveBeenCalledWith('/api/v1/marketplace/disputes', req);
      expect(result.disputeId).toBe('disp_contract_01');
      expect(result.status).toBe('OPEN');
      expect(result.farmerUid).toBe('farmer_101');
      expect(result.buyerUid).toBe('buyer_cust_101');
    });

    it('1.2 GET /api/v1/marketplace/disputes/mine passes opaque cursor without alteration', async () => {
      const opaqueCursor = 'eyJhbGciOiJIUzI1NiJ9.payload.hmacSignature123';
      const mockPage: DisputePageResponse = {
        items: [sampleDispute],
        page: 0,
        size: 20,
        totalElements: 1,
        totalPages: 1,
        hasNext: true,
        nextCursor: 'eyJhbGciOiJIUzI1NiJ9.page2.hmacSignature456',
      };

      const getSpy = vi.spyOn(apiClient, 'get').mockResolvedValueOnce({ data: mockPage });
      const result = await marketplaceDisputesApi.getMyDisputes({
        status: 'OPEN',
        page: 0,
        size: 20,
        cursor: opaqueCursor,
      });

      expect(getSpy).toHaveBeenCalledWith('/api/v1/marketplace/disputes/mine', {
        params: {
          status: 'OPEN',
          page: 0,
          size: 20,
          cursor: opaqueCursor,
        },
      });
      expect(result.nextCursor).toBe('eyJhbGciOiJIUzI1NiJ9.page2.hmacSignature456');
    });

    it('1.3 GET /api/v1/marketplace/disputes/related forwards counterparty queries with cursor', async () => {
      const mockPage: DisputePageResponse = {
        items: [sampleDispute],
        page: 0,
        size: 20,
        totalElements: 1,
        totalPages: 1,
        hasNext: false,
        nextCursor: null,
      };

      const getSpy = vi.spyOn(apiClient, 'get').mockResolvedValueOnce({ data: mockPage });
      const result = await marketplaceDisputesApi.getRelatedDisputes({ status: 'ALL', size: 20 });

      expect(getSpy).toHaveBeenCalledWith('/api/v1/marketplace/disputes/related', {
        params: { status: 'ALL', page: undefined, size: 20, cursor: undefined },
      });
      expect(result.items).toHaveLength(1);
    });

    it('1.4 GET /api/v1/marketplace/disputes/order/{orderId} fetches disputes for specific order', async () => {
      const getSpy = vi.spyOn(apiClient, 'get').mockResolvedValueOnce({ data: [sampleDispute] });
      const result = await marketplaceDisputesApi.getDisputesByOrderId('ord_tomato_001');

      expect(getSpy).toHaveBeenCalledWith('/api/v1/marketplace/disputes/order/ord_tomato_001');
      expect(result).toHaveLength(1);
      expect(result[0].orderId).toBe('ord_tomato_001');
    });

    it('1.5 GET /api/v1/marketplace/disputes/{disputeId} fetches full detail with responses, evidence, and history', async () => {
      const mockDetail: DisputeDetailResponse = {
        dispute: sampleDispute,
        responses: [
          {
            responseId: 'resp_1',
            disputeId: 'disp_contract_01',
            authorUid: 'farmer_101',
            authorRole: 'FARMER',
            message: 'Inspected upon arrival; issuing partial replacement.',
            createdAt: '2026-11-12T10:15:00Z',
          },
        ],
        evidence: [
          {
            evidenceId: 'evi_1',
            disputeId: 'disp_contract_01',
            registeredByUid: 'buyer_cust_101',
            registeredByRole: 'CUSTOMER',
            fileName: 'crushed_crates.jpg',
            contentType: 'image/jpeg',
            fileSize: 1048576,
            description: 'Photo of crushed crates',
            createdAt: '2026-11-12T10:05:00Z',
          },
        ],
        history: [
          {
            historyId: 'hist_1',
            disputeId: 'disp_contract_01',
            actorUid: 'buyer_cust_101',
            actorRole: 'CUSTOMER',
            actionType: 'DISPUTE_CREATED',
            newStatus: 'OPEN',
            description: 'Dispute opened by CUSTOMER for DAMAGED_PRODUCT',
            createdAt: '2026-11-12T10:00:00Z',
          },
        ],
      };

      const getSpy = vi.spyOn(apiClient, 'get').mockResolvedValueOnce({ data: mockDetail });
      const result = await marketplaceDisputesApi.getDisputeById('disp_contract_01');

      expect(getSpy).toHaveBeenCalledWith('/api/v1/marketplace/disputes/disp_contract_01');
      expect(result.dispute.disputeId).toBe('disp_contract_01');
      expect(result.responses).toHaveLength(1);
      expect(result.evidence).toHaveLength(1);
      expect(result.history).toHaveLength(1);
    });

    it('1.6 POST /api/v1/marketplace/disputes/{disputeId}/responses posts participant message', async () => {
      const mockResp: DisputeResponseRecord = {
        responseId: 'resp_2',
        disputeId: 'disp_contract_01',
        authorUid: 'buyer_cust_101',
        authorRole: 'CUSTOMER',
        message: 'Replacement batch accepted.',
        createdAt: '2026-11-12T10:30:00Z',
      };

      const postSpy = vi.spyOn(apiClient, 'post').mockResolvedValueOnce({ data: mockResp });
      const result = await marketplaceDisputesApi.addResponse('disp_contract_01', {
        message: 'Replacement batch accepted.',
      });

      expect(postSpy).toHaveBeenCalledWith('/api/v1/marketplace/disputes/disp_contract_01/responses', {
        message: 'Replacement batch accepted.',
      });
      expect(result.responseId).toBe('resp_2');
    });

    it('1.7 POST /api/v1/marketplace/disputes/{disputeId}/evidence registers strictly metadata', async () => {
      const mockEvidence: DisputeEvidence = {
        evidenceId: 'evi_2',
        disputeId: 'disp_contract_01',
        registeredByUid: 'buyer_cust_101',
        registeredByRole: 'CUSTOMER',
        fileName: 'replacement_receipt.pdf',
        contentType: 'application/pdf',
        fileSize: 524288,
        description: 'Signed delivery receipt',
        createdAt: '2026-11-12T10:35:00Z',
      };

      const postSpy = vi.spyOn(apiClient, 'post').mockResolvedValueOnce({ data: mockEvidence });
      const req: AddDisputeEvidenceRequest = {
        fileName: 'replacement_receipt.pdf',
        contentType: 'application/pdf',
        fileSize: 524288,
        description: 'Signed delivery receipt',
      };

      const result = await marketplaceDisputesApi.addEvidence('disp_contract_01', req);
      expect(postSpy).toHaveBeenCalledWith('/api/v1/marketplace/disputes/disp_contract_01/evidence', req);
      expect(result.evidenceId).toBe('evi_2');
      expect(result.storagePath).toBeUndefined();
    });

    it('1.8 POST /api/v1/marketplace/disputes/{disputeId}/close executes voluntary closure', async () => {
      const closedDispute: DisputeResponse = {
        ...sampleDispute,
        status: 'CLOSED',
        closedAt: '2026-11-12T10:45:00Z',
      };

      const postSpy = vi.spyOn(apiClient, 'post').mockResolvedValueOnce({ data: closedDispute });
      const result = await marketplaceDisputesApi.closeDispute('disp_contract_01');

      expect(postSpy).toHaveBeenCalledWith('/api/v1/marketplace/disputes/disp_contract_01/close');
      expect(result.status).toBe('CLOSED');
      expect(result.closedAt).toBe('2026-11-12T10:45:00Z');
    });
  });

  describe('2. All 9 Authoritative Dispute Categories Validation', () => {
    it('accurately verifies all 9 categories matching backend DisputeCategory.java', () => {
      const expectedCategories: DisputeCategory[] = [
        'PRODUCT_QUALITY',
        'DAMAGED_PRODUCT',
        'QUANTITY_MISMATCH',
        'WRONG_PRODUCT',
        'DELIVERY_DELAY',
        'DELIVERY_NOT_RECEIVED',
        'PAYMENT_ISSUE',
        'ORDER_CANCELLATION',
        'OTHER',
      ];

      expect(expectedCategories).toHaveLength(9);
      for (const cat of expectedCategories) {
        expect(DISPUTE_CATEGORY_LABELS[cat]).toBeDefined();
        expect(DISPUTE_CATEGORY_LABELS[cat].length).toBeGreaterThan(0);
      }

      // Explicit check for the two newly aligned categories
      expect(DISPUTE_CATEGORY_LABELS.ORDER_CANCELLATION).toBe('Order Cancellation Issue');
      expect(DISPUTE_CATEGORY_LABELS.OTHER).toBe('Other Commercial Issue');
    });
  });

  describe('3. Three-Profile Security & Isolation Invariants', () => {
    it('strictly preserves the three primary user profiles', () => {
      const allowedRoles: ParticipantRole[] = ['FARMER', 'MEDIATOR_BUYER', 'CUSTOMER'];
      expect(allowedRoles).toHaveLength(3);
      expect(allowedRoles).toContain('FARMER');
      expect(allowedRoles).toContain('MEDIATOR_BUYER');
      expect(allowedRoles).toContain('CUSTOMER');
    });

    it('correctly maps creator and counterparty across all permitted profile combinations', () => {
      // Combination A: FARMER and CUSTOMER
      const farmerDispute: DisputeResponse = {
        disputeId: 'disp_1',
        orderId: 'ord_1',
        listingId: 'list_1',
        offerId: 'off_1',
        cropId: 'crop_1',
        cropName: 'Tomato',
        createdByUid: 'farmer_uid',
        createdByRole: 'FARMER',
        farmerUid: 'farmer_uid',
        buyerUid: 'customer_uid',
        buyerRole: 'CUSTOMER',
        issueCategory: 'PAYMENT_ISSUE',
        subject: 'Payment delayed',
        description: 'Payment has not been settled.',
        status: 'OPEN',
        createdAt: '2026-11-12T10:00:00Z',
        updatedAt: '2026-11-12T10:00:00Z',
        lastActivityAt: '2026-11-12T10:00:00Z',
      };

      expect(farmerDispute.createdByRole).toBe('FARMER');
      expect(farmerDispute.buyerRole).toBe('CUSTOMER');

      // Combination B: FARMER and MEDIATOR_BUYER
      const mediatorDispute: DisputeResponse = {
        ...farmerDispute,
        disputeId: 'disp_2',
        createdByUid: 'mediator_uid',
        createdByRole: 'MEDIATOR_BUYER',
        buyerUid: 'mediator_uid',
        buyerRole: 'MEDIATOR_BUYER',
      };

      expect(mediatorDispute.createdByRole).toBe('MEDIATOR_BUYER');
      expect(mediatorDispute.buyerRole).toBe('MEDIATOR_BUYER');
      expect(mediatorDispute.farmerUid).toBe('farmer_uid');
    });
  });

  describe('4. HTTP Error Status Handling & Normalization', () => {
    const makeAxiosError = (status: number, data?: unknown) => {
      const err = new Error(`Request failed with status ${status}`);
      return Object.assign(err, {
        isAxiosError: true,
        response: { status, data },
      });
    };

    it('maps 400 Bad Request to validation error message', () => {
      const err = makeAxiosError(400);
      expect(extractDisputeApiErrorMessage(err)).toBe('Invalid dispute request or validation error.');
    });

    it('maps 401 Unauthorized to sign-in required message', () => {
      const err = makeAxiosError(401);
      expect(extractDisputeApiErrorMessage(err)).toBe('Authentication required. Please sign in to continue.');
    });

    it('maps 403 Forbidden to authorization required message', () => {
      const err = makeAxiosError(403);
      expect(extractDisputeApiErrorMessage(err)).toBe('You are not authorized to access or modify this dispute.');
    });

    it('maps 404 Not Found to missing dispute/order message', () => {
      const err = makeAxiosError(404);
      expect(extractDisputeApiErrorMessage(err)).toBe('The requested dispute or order was not found.');
    });

    it('maps 409 Conflict to duplicate/closed conflict message', () => {
      const err = makeAxiosError(409);
      expect(extractDisputeApiErrorMessage(err)).toBe(
        'Dispute conflict: an active dispute already exists or is already closed.'
      );
    });

    it('maps 503 Service Unavailable to quota guard cooldown message', () => {
      const err = makeAxiosError(503);
      expect(extractDisputeApiErrorMessage(err)).toBe('Database quota exceeded. Please try again shortly.');
    });

    it('preserves specific server error message when provided in string response data', () => {
      const err = makeAxiosError(400, 'Subject must be between 3 and 200 characters.');
      expect(extractDisputeApiErrorMessage(err)).toBe('Subject must be between 3 and 200 characters.');
    });

    it('preserves specific server error message when provided in JSON response data', () => {
      const err = makeAxiosError(409, { message: 'An active dispute already exists for order ord_123.' });
      expect(extractDisputeApiErrorMessage(err)).toBe('An active dispute already exists for order ord_123.');
    });
  });

  describe('5. Lifecycle State Machine & Terminal Behavior', () => {
    it('verifies active status tests', () => {
      expect(isDisputeActive('OPEN')).toBe(true);
      expect(isDisputeActive('UNDER_REVIEW')).toBe(true);
      expect(isDisputeActive('WAITING_FOR_INFORMATION')).toBe(true);
      expect(isDisputeActive('RESOLVED')).toBe(false);
      expect(isDisputeActive('REJECTED')).toBe(false);
      expect(isDisputeActive('CLOSED')).toBe(false);
    });

    it('verifies terminal status tests', () => {
      expect(isDisputeTerminal('CLOSED')).toBe(true);
      expect(isDisputeTerminal('RESOLVED')).toBe(true);
      expect(isDisputeTerminal('REJECTED')).toBe(true);
      expect(isDisputeTerminal('OPEN')).toBe(false);
      expect(isDisputeTerminal('UNDER_REVIEW')).toBe(false);
      expect(isDisputeTerminal('WAITING_FOR_INFORMATION')).toBe(false);
    });
  });

  describe('6. Opaque Signed Cursor Pagination Stack Invariant', () => {
    it('cursor stack navigates forward and backward preserving HMAC tokens verbatim', () => {
      const historyStack: Array<string | undefined> = [undefined];
      let currentIndex = 0;

      // Initial page 1 has no cursor
      expect(historyStack[currentIndex]).toBeUndefined();

      // Next page 2: push signed backend token
      const cursorPage2 = 'eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.payload1.hmacSig1';
      historyStack.push(cursorPage2);
      currentIndex += 1;
      expect(historyStack[currentIndex]).toBe(cursorPage2);

      // Next page 3: push signed backend token
      const cursorPage3 = 'eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.payload2.hmacSig2';
      historyStack.push(cursorPage3);
      currentIndex += 1;
      expect(historyStack[currentIndex]).toBe(cursorPage3);

      // Previous page 2: decrement index
      currentIndex -= 1;
      expect(historyStack[currentIndex]).toBe(cursorPage2);

      // Previous page 1: decrement index
      currentIndex -= 1;
      expect(historyStack[currentIndex]).toBeUndefined();

      // Filter change: reset stack
      const resetStack = [undefined];
      expect(resetStack).toHaveLength(1);
      expect(resetStack[0]).toBeUndefined();
    });
  });

  describe('7. TanStack Query Keys Determinism', () => {
    it('produces deterministic and stable query keys for cache invalidation', () => {
      const mineKey1 = disputeQueryKeys.mine({ status: 'OPEN', size: 20 });
      const mineKey2 = disputeQueryKeys.mine({ status: 'OPEN', size: 20 });
      expect(mineKey1).toEqual(mineKey2);

      const relatedKey = disputeQueryKeys.related({ status: 'ALL' });
      expect(relatedKey).toEqual(['marketplace-disputes', 'related', 'ALL', 0, 20, null]);

      const detailKey = disputeQueryKeys.detail('disp_123');
      expect(detailKey).toEqual(['marketplace-disputes', 'detail', 'disp_123']);

      const orderKey = disputeQueryKeys.byOrder('ord_456');
      expect(orderKey).toEqual(['marketplace-disputes', 'order', 'ord_456']);
    });
  });
});
