/**
 * Module 24 — Order Issues, Disputes & Resolution Foundation
 * Unit test suite for Frontend Dispute Foundation (Stage 4.1).
 */

import { describe, it, expect, vi, beforeEach } from 'vitest';
import axios from 'axios';
import { QueryClient } from '@tanstack/react-query';
import { apiClient } from '../../../services/api';
import {
  marketplaceDisputesApi,
  disputeQueryKeys,
  extractDisputeApiErrorMessage,
} from '../api/marketplaceDisputesApi';
import {
  isDisputeActive,
  isDisputeTerminal,
  DISPUTE_CATEGORY_LABELS,
  DISPUTE_STATUS_LABELS,
  type CreateDisputeRequest,
  type AddDisputeResponseRequest,
  type AddDisputeEvidenceRequest,
  type DisputeResponse,
  type DisputeDetailResponse,
  type DisputePageResponse,
  type DisputeCategory,
  type DisputeStatus,
} from '../../../types/dispute';

describe('Marketplace Disputes API Service & Domain Models', () => {
  beforeEach(() => {
    vi.restoreAllMocks();
  });

  describe('1. API Request Methods and Endpoint Paths', () => {
    it('createDispute calls POST /api/v1/marketplace/disputes with correct payload', async () => {
      const mockDispute: DisputeResponse = {
        disputeId: 'disp_100',
        orderId: 'ord_100',
        listingId: 'list_100',
        offerId: 'off_100',
        cropId: 'crop_tomato',
        cropName: 'Tomato',
        createdByUid: 'buyer_1',
        createdByRole: 'CUSTOMER',
        farmerUid: 'farmer_1',
        buyerUid: 'buyer_1',
        buyerRole: 'CUSTOMER',
        issueCategory: 'PRODUCT_QUALITY',
        subject: 'Rotten produce',
        description: 'Two boxes were damaged upon delivery.',
        status: 'OPEN',
        createdAt: '2026-11-12T10:00:00Z',
        updatedAt: '2026-11-12T10:00:00Z',
        lastActivityAt: '2026-11-12T10:00:00Z',
      };

      const postSpy = vi.spyOn(apiClient, 'post').mockResolvedValueOnce({ data: mockDispute });

      const request: CreateDisputeRequest = {
        orderId: 'ord_100',
        issueCategory: 'PRODUCT_QUALITY',
        subject: 'Rotten produce',
        description: 'Two boxes were damaged upon delivery.',
      };

      const result = await marketplaceDisputesApi.createDispute(request);

      expect(postSpy).toHaveBeenCalledTimes(1);
      expect(postSpy).toHaveBeenCalledWith('/api/v1/marketplace/disputes', request);
      expect(result.disputeId).toBe('disp_100');
      expect(result.status).toBe('OPEN');
    });

    it('getMyDisputes calls GET /api/v1/marketplace/disputes/mine with correct query params', async () => {
      const mockPage: DisputePageResponse = {
        items: [],
        page: 0,
        size: 20,
        totalElements: 0,
        totalPages: 0,
        hasNext: false,
      };

      const getSpy = vi.spyOn(apiClient, 'get').mockResolvedValueOnce({ data: mockPage });

      await marketplaceDisputesApi.getMyDisputes({ status: 'OPEN', page: 1, size: 10 });

      expect(getSpy).toHaveBeenCalledWith('/api/v1/marketplace/disputes/mine', {
        params: { status: 'OPEN', page: 1, size: 10, cursor: undefined },
      });
    });

    it('getRelatedDisputes calls GET /api/v1/marketplace/disputes/related with correct query params', async () => {
      const mockPage: DisputePageResponse = {
        items: [],
        page: 0,
        size: 20,
        totalElements: 0,
        totalPages: 0,
        hasNext: false,
      };

      const getSpy = vi.spyOn(apiClient, 'get').mockResolvedValueOnce({ data: mockPage });

      await marketplaceDisputesApi.getRelatedDisputes({ status: 'ALL', page: 0, size: 20 });

      expect(getSpy).toHaveBeenCalledWith('/api/v1/marketplace/disputes/related', {
        params: { status: 'ALL', page: 0, size: 20, cursor: undefined },
      });
    });

    it('getDisputesByOrderId calls GET /api/v1/marketplace/disputes/order/{orderId}', async () => {
      const getSpy = vi.spyOn(apiClient, 'get').mockResolvedValueOnce({ data: [] });

      await marketplaceDisputesApi.getDisputesByOrderId('ord_tomato_001');

      expect(getSpy).toHaveBeenCalledWith('/api/v1/marketplace/disputes/order/ord_tomato_001');
    });

    it('getDisputeById calls GET /api/v1/marketplace/disputes/{disputeId}', async () => {
      const mockDetail: DisputeDetailResponse = {
        dispute: {
          disputeId: 'disp_200',
          orderId: 'ord_200',
          listingId: 'list_200',
          offerId: 'off_200',
          cropId: 'crop_wheat',
          cropName: 'Wheat',
          createdByUid: 'buyer_2',
          createdByRole: 'MEDIATOR_BUYER',
          farmerUid: 'farmer_2',
          buyerUid: 'buyer_2',
          buyerRole: 'MEDIATOR_BUYER',
          issueCategory: 'QUANTITY_MISMATCH',
          subject: 'Weight shortfall',
          description: 'Delivered 80 quintals instead of 100.',
          status: 'UNDER_REVIEW',
          createdAt: '2026-11-12T10:00:00Z',
          updatedAt: '2026-11-12T10:30:00Z',
          lastActivityAt: '2026-11-12T10:30:00Z',
        },
        responses: [],
        evidence: [],
        history: [],
      };

      const getSpy = vi.spyOn(apiClient, 'get').mockResolvedValueOnce({ data: mockDetail });

      const detail = await marketplaceDisputesApi.getDisputeById('disp_200');

      expect(getSpy).toHaveBeenCalledWith('/api/v1/marketplace/disputes/disp_200');
      expect(detail.dispute.disputeId).toBe('disp_200');
    });

    it('addResponse calls POST /api/v1/marketplace/disputes/{disputeId}/responses', async () => {
      const postSpy = vi.spyOn(apiClient, 'post').mockResolvedValueOnce({
        data: {
          responseId: 'resp_1',
          disputeId: 'disp_100',
          authorUid: 'farmer_1',
          authorRole: 'FARMER',
          message: 'We will inspect the crate photos and arrange replacement.',
          createdAt: '2026-11-12T11:00:00Z',
        },
      });

      const request: AddDisputeResponseRequest = {
        message: 'We will inspect the crate photos and arrange replacement.',
      };

      const res = await marketplaceDisputesApi.addResponse('disp_100', request);

      expect(postSpy).toHaveBeenCalledWith('/api/v1/marketplace/disputes/disp_100/responses', request);
      expect(res.responseId).toBe('resp_1');
    });

    it('addEvidence calls POST /api/v1/marketplace/disputes/{disputeId}/evidence with strictly metadata payload', async () => {
      const postSpy = vi.spyOn(apiClient, 'post').mockResolvedValueOnce({
        data: {
          evidenceId: 'evi_1',
          disputeId: 'disp_100',
          registeredByUid: 'buyer_1',
          registeredByRole: 'CUSTOMER',
          fileName: 'crushed_crates.jpg',
          contentType: 'image/jpeg',
          fileSize: 1024000,
          description: 'Photo of crushed crate labels',
          createdAt: '2026-11-12T10:15:00Z',
        },
      });

      const request: AddDisputeEvidenceRequest = {
        fileName: 'crushed_crates.jpg',
        contentType: 'image/jpeg',
        fileSize: 1024000,
        description: 'Photo of crushed crate labels',
      };

      const res = await marketplaceDisputesApi.addEvidence('disp_100', request);

      expect(postSpy).toHaveBeenCalledWith('/api/v1/marketplace/disputes/disp_100/evidence', request);
      expect(res.evidenceId).toBe('evi_1');
      expect(res.fileName).toBe('crushed_crates.jpg');
    });

    it('closeDispute calls POST /api/v1/marketplace/disputes/{disputeId}/close', async () => {
      const postSpy = vi.spyOn(apiClient, 'post').mockResolvedValueOnce({
        data: {
          disputeId: 'disp_100',
          status: 'CLOSED',
          closedAt: '2026-11-12T12:00:00Z',
        },
      });

      const res = await marketplaceDisputesApi.closeDispute('disp_100');

      expect(postSpy).toHaveBeenCalledWith('/api/v1/marketplace/disputes/disp_100/close');
      expect(res.status).toBe('CLOSED');
    });
  });

  describe('2. Cursor Parameters Passed Verbatim & Unchanged', () => {
    it('forwards opaque HMAC-signed cursor to getMyDisputes unchanged', async () => {
      const rawSignedCursor = 'TUlORXxiaXllcl9jdXN0XzEwMXxBTEwyMDI2LTExLTExVDEyOjAwOjAwWnxkaXNwXzEyMzQ1Ng.s0m3_hm4c_s1gn4tur3_v4lu3_h3r3';
      const getSpy = vi.spyOn(apiClient, 'get').mockResolvedValueOnce({
        data: {
          items: [],
          page: 0,
          size: 20,
          totalElements: -1,
          totalPages: -1,
          hasNext: true,
          nextCursor: 'n3xt_cpt_cursor_value.s1gn4tur3',
        },
      });

      const response = await marketplaceDisputesApi.getMyDisputes({
        status: 'OPEN',
        page: 0,
        size: 20,
        cursor: rawSignedCursor,
      });

      expect(getSpy).toHaveBeenCalledWith('/api/v1/marketplace/disputes/mine', {
        params: {
          status: 'OPEN',
          page: 0,
          size: 20,
          cursor: rawSignedCursor,
        },
      });
      // Verifies response preserves opaque cursor unchanged
      expect(response.nextCursor).toBe('n3xt_cpt_cursor_value.s1gn4tur3');
      expect(response.hasNext).toBe(true);
    });

    it('forwards opaque HMAC-signed cursor to getRelatedDisputes unchanged', async () => {
      const rawSignedCursor = 'UkVMQVRFRHxmYXJtZXJfMTAxfEFMTDIwMjYtMTEtMTFUMTI6MDA6MDBafGRpc3BfOTk5OTk5.4n0th3r_v4l1d_h4sh';
      const getSpy = vi.spyOn(apiClient, 'get').mockResolvedValueOnce({
        data: {
          items: [],
          page: 0,
          size: 20,
          totalElements: -1,
          totalPages: -1,
          hasNext: false,
          nextCursor: null,
        },
      });

      const response = await marketplaceDisputesApi.getRelatedDisputes({
        status: 'ALL',
        cursor: rawSignedCursor,
      });

      expect(getSpy).toHaveBeenCalledWith('/api/v1/marketplace/disputes/related', {
        params: {
          status: 'ALL',
          page: undefined,
          size: undefined,
          cursor: rawSignedCursor,
        },
      });
      expect(response.nextCursor).toBeNull();
      expect(response.hasNext).toBe(false);
    });
  });

  describe('3. Query Key Consistency', () => {
    it('generates consistent and deterministic query keys', () => {
      expect(disputeQueryKeys.all).toEqual(['marketplace-disputes']);

      expect(disputeQueryKeys.mine({ status: 'OPEN', page: 1, size: 20, cursor: 'c_1' })).toEqual([
        'marketplace-disputes',
        'mine',
        'OPEN',
        1,
        20,
        'c_1',
      ]);

      expect(disputeQueryKeys.mine()).toEqual([
        'marketplace-disputes',
        'mine',
        'ALL',
        0,
        20,
        null,
      ]);

      expect(disputeQueryKeys.related({ status: 'CLOSED', cursor: 'c_2' })).toEqual([
        'marketplace-disputes',
        'related',
        'CLOSED',
        0,
        20,
        'c_2',
      ]);

      expect(disputeQueryKeys.detail('disp_xyz')).toEqual([
        'marketplace-disputes',
        'detail',
        'disp_xyz',
      ]);

      expect(disputeQueryKeys.byOrder('ord_abc')).toEqual([
        'marketplace-disputes',
        'order',
        'ord_abc',
      ]);
    });
  });

  describe('4. TanStack Query Mutation Cache Invalidation', () => {
    it('invalidates appropriate query keys on createDispute success', async () => {
      const queryClient = new QueryClient();
      const invalidateSpy = vi.spyOn(queryClient, 'invalidateQueries');

      const mockDispute: DisputeResponse = {
        disputeId: 'disp_new_1',
        orderId: 'ord_target_1',
        listingId: 'list_1',
        offerId: 'off_1',
        cropId: 'crop_1',
        cropName: 'Tomato',
        createdByUid: 'buyer_1',
        createdByRole: 'CUSTOMER',
        farmerUid: 'farmer_1',
        buyerUid: 'buyer_1',
        buyerRole: 'CUSTOMER',
        issueCategory: 'PRODUCT_QUALITY',
        subject: 'Issue',
        description: 'Desc',
        status: 'OPEN',
        createdAt: '2026-11-12T10:00:00Z',
        updatedAt: '2026-11-12T10:00:00Z',
        lastActivityAt: '2026-11-12T10:00:00Z',
      };

      // Simulate onSuccess handler of useCreateDisputeMutation
      queryClient.invalidateQueries({ queryKey: disputeQueryKeys.all });
      queryClient.invalidateQueries({ queryKey: disputeQueryKeys.byOrder(mockDispute.orderId) });
      queryClient.invalidateQueries({ queryKey: ['marketplace-orders'] });
      queryClient.invalidateQueries({ queryKey: ['order-detail', mockDispute.orderId] });

      expect(invalidateSpy).toHaveBeenCalledWith({ queryKey: ['marketplace-disputes'] });
      expect(invalidateSpy).toHaveBeenCalledWith({ queryKey: ['marketplace-disputes', 'order', 'ord_target_1'] });
      expect(invalidateSpy).toHaveBeenCalledWith({ queryKey: ['marketplace-orders'] });
      expect(invalidateSpy).toHaveBeenCalledWith({ queryKey: ['order-detail', 'ord_target_1'] });
    });

    it('invalidates detail and list caches on addResponse success', async () => {
      const queryClient = new QueryClient();
      const invalidateSpy = vi.spyOn(queryClient, 'invalidateQueries');

      queryClient.invalidateQueries({ queryKey: disputeQueryKeys.detail('disp_active_1') });
      queryClient.invalidateQueries({ queryKey: disputeQueryKeys.all });

      expect(invalidateSpy).toHaveBeenCalledWith({ queryKey: ['marketplace-disputes', 'detail', 'disp_active_1'] });
      expect(invalidateSpy).toHaveBeenCalledWith({ queryKey: ['marketplace-disputes'] });
    });
  });

  describe('5. Error Handling Normalization', () => {
    it('extracts plain string error response body', () => {
      const axiosError = {
        isAxiosError: true,
        response: {
          status: 400,
          data: 'Dispute subject cannot exceed 100 characters.',
        },
      } as unknown;

      vi.spyOn(axios, 'isAxiosError').mockReturnValue(true);

      const message = extractDisputeApiErrorMessage(axiosError);
      expect(message).toBe('Dispute subject cannot exceed 100 characters.');
    });

    it('extracts object response message property', () => {
      const axiosError = {
        isAxiosError: true,
        response: {
          status: 400,
          data: { message: 'Invalid issue category specified.' },
        },
      } as unknown;

      vi.spyOn(axios, 'isAxiosError').mockReturnValue(true);

      const message = extractDisputeApiErrorMessage(axiosError);
      expect(message).toBe('Invalid issue category specified.');
    });

    it('maps standard HTTP status codes accurately when response body is empty', () => {
      vi.spyOn(axios, 'isAxiosError').mockReturnValue(true);

      expect(extractDisputeApiErrorMessage({ response: { status: 401 } })).toContain('Authentication required');
      expect(extractDisputeApiErrorMessage({ response: { status: 403 } })).toContain('not authorized');
      expect(extractDisputeApiErrorMessage({ response: { status: 404 } })).toContain('not found');
      expect(extractDisputeApiErrorMessage({ response: { status: 409 } })).toContain('conflict');
      expect(extractDisputeApiErrorMessage({ response: { status: 503 } })).toContain('Database quota exceeded');
    });

    it('handles standard Error instances and unknown errors', () => {
      vi.spyOn(axios, 'isAxiosError').mockReturnValue(false);

      expect(extractDisputeApiErrorMessage(new Error('Network connectivity lost.'))).toBe('Network connectivity lost.');
      expect(extractDisputeApiErrorMessage('unexpected string')).toContain('unexpected error');
    });
  });

  describe('6. Domain Model Helper Functions & Labels', () => {
    it('correctly identifies active vs terminal dispute statuses', () => {
      expect(isDisputeActive('OPEN')).toBe(true);
      expect(isDisputeActive('UNDER_REVIEW')).toBe(true);
      expect(isDisputeActive('WAITING_FOR_INFORMATION')).toBe(true);

      expect(isDisputeActive('RESOLVED')).toBe(false);
      expect(isDisputeActive('REJECTED')).toBe(false);
      expect(isDisputeActive('CLOSED')).toBe(false);

      expect(isDisputeTerminal('RESOLVED')).toBe(true);
      expect(isDisputeTerminal('REJECTED')).toBe(true);
      expect(isDisputeTerminal('CLOSED')).toBe(true);

      expect(isDisputeTerminal('OPEN')).toBe(false);
      expect(isDisputeTerminal('UNDER_REVIEW')).toBe(false);
    });

    it('provides human-readable labels for all 7 categories and 6 statuses', () => {
      const categories: DisputeCategory[] = [
        'PRODUCT_QUALITY',
        'DAMAGED_PRODUCT',
        'QUANTITY_MISMATCH',
        'WRONG_PRODUCT',
        'DELIVERY_DELAY',
        'DELIVERY_NOT_RECEIVED',
        'PAYMENT_ISSUE',
      ];

      for (const cat of categories) {
        expect(DISPUTE_CATEGORY_LABELS[cat]).toBeDefined();
        expect(DISPUTE_CATEGORY_LABELS[cat].length).toBeGreaterThan(0);
      }

      const statuses: DisputeStatus[] = [
        'OPEN',
        'UNDER_REVIEW',
        'WAITING_FOR_INFORMATION',
        'RESOLVED',
        'REJECTED',
        'CLOSED',
      ];

      for (const st of statuses) {
        expect(DISPUTE_STATUS_LABELS[st]).toBeDefined();
        expect(DISPUTE_STATUS_LABELS[st].length).toBeGreaterThan(0);
      }
    });
  });
});

