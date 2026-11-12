/**
 * Module 24 — Order Issues, Disputes & Resolution Foundation
 * Service re-export for dispute operations.
 */

export {
  marketplaceDisputesApi,
  disputeQueryKeys,
  extractDisputeApiErrorMessage,
  useMyDisputesQuery,
  useRelatedDisputesQuery,
  useOrderDisputesQuery,
  useDisputeDetailQuery,
  useCreateDisputeMutation,
  useAddDisputeResponseMutation,
  useAddDisputeEvidenceMutation,
  useCloseDisputeMutation,
} from '../features/marketplace/api/marketplaceDisputesApi';

export * from '../types/dispute';

