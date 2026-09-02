import { useMutation } from '@tanstack/react-query';
import { aiDecisionService } from '../services/aiDecisionService';
import type { AiDecisionRequest } from '../types';

export function useAiDecision() {
  return useMutation({
    mutationFn: (payload: AiDecisionRequest) => aiDecisionService.processDecision(payload),
  });
}
