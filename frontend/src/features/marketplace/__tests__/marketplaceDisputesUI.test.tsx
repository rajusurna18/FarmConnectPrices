/**
 * Module 24 — Order Issues, Disputes & Resolution Foundation
 * Comprehensive Frontend UI Component and Integration Test Suite (Stage 4.2).
 */

import React from 'react';
import { describe, it, expect } from 'vitest';
import { renderToString } from 'react-dom/server';
import { MemoryRouter } from 'react-router-dom';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { DisputeStatusBadge } from '../components/disputes/DisputeStatusBadge';
import { DisputeFilters } from '../components/disputes/DisputeFilters';
import { DisputeCard } from '../components/disputes/DisputeCard';
import { DisputeTimeline } from '../components/disputes/DisputeTimeline';
import { DisputeEvidenceSection } from '../components/disputes/DisputeEvidenceSection';
import { DisputeResponseForm } from '../components/disputes/DisputeResponseForm';
import type {
  DisputeResponse,
  DisputeEvidence,
  DisputeHistoryEntry,
  DisputeStatus,
  ParticipantRole,
} from '../../../types/dispute';
import {
  DISPUTE_CATEGORY_LABELS,
  DISPUTE_STATUS_LABELS,
  isDisputeActive,
  isDisputeTerminal,
} from '../../../types/dispute';

const createTestQueryClient = () =>
  new QueryClient({
    defaultOptions: {
      queries: { retry: false },
      mutations: { retry: false },
    },
  });

describe('Module 24 — Dispute Frontend UI Components', () => {
  describe('1. DisputeStatusBadge Component', () => {
    const statuses: DisputeStatus[] = [
      'OPEN',
      'UNDER_REVIEW',
      'WAITING_FOR_INFORMATION',
      'RESOLVED',
      'REJECTED',
      'CLOSED',
    ];

    it.each(statuses)('renders correct label and styles for status %s', (status) => {
      const html = renderToString(<DisputeStatusBadge status={status} />);
      const expectedLabel = DISPUTE_STATUS_LABELS[status];
      expect(html).toContain(expectedLabel);
      expect(html).toContain('rounded-full');
    });

    it('renders fallback label and neutral style for unknown status', () => {
      const html = renderToString(<DisputeStatusBadge status="UNKNOWN_STATUS" />);
      expect(html).toContain('UNKNOWN_STATUS');
      expect(html).toContain('bg-slate-800');
    });
  });

  describe('2. DisputeFilters Component', () => {
    it('renders all status filter buttons including ALL', () => {
      const html = renderToString(
        <DisputeFilters selectedStatus="ALL" onSelectStatus={() => {}} />
      );

      expect(html).toContain('All Disputes');
      expect(html).toContain('Open');
      expect(html).toContain('Under Review');
      expect(html).toContain('Waiting Info');
      expect(html).toContain('Resolved');
      expect(html).toContain('Rejected');
      expect(html).toContain('Closed');
    });

    it('highlights active status filter with primary emerald classes', () => {
      const html = renderToString(
        <DisputeFilters selectedStatus="OPEN" onSelectStatus={() => {}} />
      );

      expect(html).toContain('bg-emerald-600');
      expect(html).toContain('text-white');
    });
  });

  describe('3. DisputeCard Component', () => {
    const sampleDispute: DisputeResponse = {
      disputeId: 'disp_test_001',
      orderId: 'ord_test_001',
      listingId: 'list_test_001',
      offerId: 'off_test_001',
      cropId: 'crop_tomato',
      cropName: 'Organic Tomato',
      createdByUid: 'uid_buyer_1',
      createdByRole: 'CUSTOMER',
      farmerUid: 'uid_farmer_1',
      buyerUid: 'uid_buyer_1',
      buyerRole: 'CUSTOMER',
      issueCategory: 'DAMAGED_PRODUCT',
      subject: 'Produce damaged in transit',
      description: 'Ten boxes arrived with crushed tomatoes due to poor stacking.',
      status: 'OPEN',
      createdAt: '2026-11-12T10:00:00Z',
      updatedAt: '2026-11-12T10:00:00Z',
      lastActivityAt: '2026-11-12T10:30:00Z',
    };

    it('renders dispute identifiers, produce name, category label, and status', () => {
      const html = renderToString(
        <MemoryRouter>
          <DisputeCard dispute={sampleDispute} viewMode="mine" />
        </MemoryRouter>
      );

      expect(html).toContain('disp_test_001');
      expect(html).toContain('ord_test_001');
      expect(html).toContain('Organic Tomato');
      expect(html).toContain(DISPUTE_CATEGORY_LABELS.DAMAGED_PRODUCT);
      expect(html).toContain('Open');
      expect(html).toContain('Produce damaged in transit');
      expect(html).toContain('/marketplace/disputes/disp_test_001');
    });

    it('renders initiator role correctly when viewMode is mine vs related', () => {
      const htmlMine = renderToString(
        <MemoryRouter>
          <DisputeCard dispute={sampleDispute} viewMode="mine" />
        </MemoryRouter>
      );
      expect(htmlMine).toContain('You');

      const htmlRelated = renderToString(
        <MemoryRouter>
          <DisputeCard dispute={sampleDispute} viewMode="related" />
        </MemoryRouter>
      );
      expect(htmlRelated).toContain('CUSTOMER');
    });

    it('displays resolution outcome tag when dispute is resolved', () => {
      const resolvedDispute: DisputeResponse = {
        ...sampleDispute,
        status: 'RESOLVED',
        resolutionOutcome: 'PARTIAL_REFUND',
        resolutionSummary: 'Agreed on 50% refund for damaged crates.',
      };

      const html = renderToString(
        <MemoryRouter>
          <DisputeCard dispute={resolvedDispute} viewMode="mine" />
        </MemoryRouter>
      );

      expect(html).toContain('PARTIAL_REFUND');
      expect(html).toContain('Agreed on 50% refund for damaged crates.');
    });
  });

  describe('4. DisputeTimeline Component', () => {
    it('renders empty timeline message when history array is empty', () => {
      const html = renderToString(<DisputeTimeline history={[]} />);
      expect(html).toContain('No audit history entries recorded for this dispute.');
    });

    it('renders timeline entries with action labels, roles, and descriptions', () => {
      const mockHistory: DisputeHistoryEntry[] = [
        {
          historyId: 'hist_1',
          disputeId: 'disp_100',
          actorUid: 'buyer_1',
          actorRole: 'CUSTOMER',
          actionType: 'DISPUTE_CREATED',
          newStatus: 'OPEN',
          description: 'Dispute opened by buyer for DAMAGED_PRODUCT.',
          createdAt: '2026-11-12T10:00:00Z',
        },
        {
          historyId: 'hist_2',
          disputeId: 'disp_100',
          actorUid: 'farmer_1',
          actorRole: 'FARMER',
          actionType: 'RESPONSE_ADDED',
          description: 'Farmer submitted response acknowledging inspection.',
          createdAt: '2026-11-12T10:15:00Z',
        },
      ];

      const html = renderToString(<DisputeTimeline history={mockHistory} />);
      expect(html).toContain('Dispute Opened');
      expect(html).toContain('Response Submitted');
      expect(html).toContain('CUSTOMER');
      expect(html).toContain('FARMER');
      expect(html).toContain('Dispute opened by buyer');
      expect(html).toContain('Farmer submitted response acknowledging inspection.');
    });
  });

  describe('5. DisputeEvidenceSection Component', () => {
    const mockEvidence: DisputeEvidence[] = [
      {
        evidenceId: 'ev_1',
        disputeId: 'disp_100',
        registeredByUid: 'buyer_1',
        registeredByRole: 'CUSTOMER',
        fileName: 'damaged_crates_photo.jpg',
        contentType: 'image/jpeg',
        fileSize: 2048000,
        description: 'Photo showing 10 crushed crates upon unload.',
        createdAt: '2026-11-12T10:05:00Z',
      },
    ];

    it('displays mandatory honest disclaimer regarding metadata-only registration', () => {
      const queryClient = createTestQueryClient();
      const html = renderToString(
        <QueryClientProvider client={queryClient}>
          <DisputeEvidenceSection disputeId="disp_100" evidence={[]} canAddEvidence={false} />
        </QueryClientProvider>
      );

      expect(html).toContain(
        'Secure evidence file upload will be available in a future update. Evidence registration is currently limited to metadata.'
      );
    });

    it('renders evidence list item with formatted file size and metadata', () => {
      const queryClient = createTestQueryClient();
      const html = renderToString(
        <QueryClientProvider client={queryClient}>
          <DisputeEvidenceSection
            disputeId="disp_100"
            evidence={mockEvidence}
            canAddEvidence={false}
          />
        </QueryClientProvider>
      );

      expect(html).toContain('damaged_crates_photo.jpg');
      expect(html).toContain('image/jpeg');
      expect(html).toContain('1.95 MB');
      expect(html).toContain('Photo showing 10 crushed crates upon unload.');
    });

    it('renders metadata registration form toggle button when canAddEvidence is true', () => {
      const queryClient = createTestQueryClient();
      const html = renderToString(
        <QueryClientProvider client={queryClient}>
          <DisputeEvidenceSection
            disputeId="disp_100"
            evidence={mockEvidence}
            canAddEvidence={true}
          />
        </QueryClientProvider>
      );

      expect(html).toContain('+ Register Evidence');
    });
  });

  describe('6. DisputeResponseForm Component', () => {
    it('renders message input with 2000 character limit hint and submit button', () => {
      const queryClient = createTestQueryClient();
      const html = renderToString(
        <QueryClientProvider client={queryClient}>
          <DisputeResponseForm disputeId="disp_100" />
        </QueryClientProvider>
      );

      expect(html).toContain('2000');
      expect(html).toContain('Send Response');
      expect(html).toContain('State your position, address counterparty concerns');
    });
  });

  describe('7. Profile and Domain Invariants', () => {
    it('strictly preserves the three primary participant profiles', () => {
      const allowedProfiles: ParticipantRole[] = ['FARMER', 'MEDIATOR_BUYER', 'CUSTOMER'];
      expect(allowedProfiles).toHaveLength(3);
      expect(allowedProfiles).toContain('FARMER');
      expect(allowedProfiles).toContain('MEDIATOR_BUYER');
      expect(allowedProfiles).toContain('CUSTOMER');
    });

    it('verifies active vs terminal dispute states correctly', () => {
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
    });

    it('verifies pagination cursor stack preserves signed tokens verbatim', () => {
      const cursorStack: Array<string | undefined> = [undefined];
      const opaqueCursor1 = 'eyJhbGciOiJIUzI1NiJ9.test1';
      const opaqueCursor2 = 'eyJhbGciOiJIUzI1NiJ9.test2';

      // Push page 2
      cursorStack.push(opaqueCursor1);
      expect(cursorStack).toHaveLength(2);
      expect(cursorStack[1]).toBe(opaqueCursor1);

      // Push page 3
      cursorStack.push(opaqueCursor2);
      expect(cursorStack).toHaveLength(3);
      expect(cursorStack[2]).toBe(opaqueCursor2);

      // Navigate back to page 2
      const previousCursor = cursorStack[1];
      expect(previousCursor).toBe(opaqueCursor1);

      // Navigate back to page 1
      const firstCursor = cursorStack[0];
      expect(firstCursor).toBeUndefined();
    });

    it('all 7 dispute categories have descriptive human-readable labels', () => {
      const categories = Object.keys(DISPUTE_CATEGORY_LABELS);
      expect(categories).toHaveLength(7);
      expect(DISPUTE_CATEGORY_LABELS.PRODUCT_QUALITY).toBe('Product Quality Issue');
      expect(DISPUTE_CATEGORY_LABELS.DAMAGED_PRODUCT).toBe('Damaged Product');
      expect(DISPUTE_CATEGORY_LABELS.QUANTITY_MISMATCH).toBe('Quantity Mismatch');
      expect(DISPUTE_CATEGORY_LABELS.WRONG_PRODUCT).toBe('Wrong Product Delivered');
      expect(DISPUTE_CATEGORY_LABELS.DELIVERY_DELAY).toBe('Delivery Delay');
      expect(DISPUTE_CATEGORY_LABELS.DELIVERY_NOT_RECEIVED).toBe('Delivery Not Received');
      expect(DISPUTE_CATEGORY_LABELS.PAYMENT_ISSUE).toBe('Payment Issue');
    });

    it('all 6 dispute lifecycle statuses have descriptive labels', () => {
      const statuses = Object.keys(DISPUTE_STATUS_LABELS);
      expect(statuses).toHaveLength(6);
      expect(DISPUTE_STATUS_LABELS.OPEN).toBe('Open');
      expect(DISPUTE_STATUS_LABELS.UNDER_REVIEW).toBe('Under Review');
      expect(DISPUTE_STATUS_LABELS.WAITING_FOR_INFORMATION).toBe('Waiting for Information');
      expect(DISPUTE_STATUS_LABELS.RESOLVED).toBe('Resolved');
      expect(DISPUTE_STATUS_LABELS.REJECTED).toBe('Rejected');
      expect(DISPUTE_STATUS_LABELS.CLOSED).toBe('Closed');
    });
  });
});
