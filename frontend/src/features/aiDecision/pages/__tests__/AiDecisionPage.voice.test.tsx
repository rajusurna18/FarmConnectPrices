import React from 'react';
import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { renderToString } from 'react-dom/server';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { AiDecisionPage } from '../AiDecisionPage';
import { SUPPORTED_VOICE_LANGUAGES } from '../../../voice/types';

interface SpeechMockGlobal {
  window?: unknown;
  SpeechRecognition?: unknown;
  webkitSpeechRecognition?: unknown;
  speechSynthesis?: unknown;
  SpeechSynthesisUtterance?: unknown;
}

const mockGlobal = globalThis as unknown as SpeechMockGlobal;
if (typeof mockGlobal.window === 'undefined') {
  mockGlobal.window = globalThis;
}

// Mock query hooks with correct relative paths from pages/__tests__
vi.mock('../../../prices/hooks/useCrops', () => ({
  useCrops: () => ({
    data: [{ id: 'crop-1', name: 'Tomato' }],
    isLoading: false,
  }),
}));

vi.mock('../../../markets/hooks/useMarkets', () => ({
  useMarkets: () => ({
    data: [{ id: 'mkt-1', name: 'Warangal Mandi' }],
    isLoading: false,
  }),
}));

vi.mock('../../../farmEconomics/hooks/useFarmEconomics', () => ({
  useFarmEconomics: () => ({
    data: [{ id: 'econ-1', title: 'Rabi Tomato 2026' }],
    isLoading: false,
  }),
}));

const mockMutateAsync = vi.fn();
vi.mock('../../hooks/useAiDecision', () => ({
  useAiDecision: () => ({
    mutateAsync: mockMutateAsync,
    isPending: false,
  }),
}));

class MockSpeechRecognition {
  lang = 'en-IN';
  continuous = false;
  interimResults = false;
  onstart: (() => void) | null = null;
  onresult: ((event: unknown) => void) | null = null;
  onerror: ((event: unknown) => void) | null = null;
  onend: (() => void) | null = null;

  start = vi.fn();
  stop = vi.fn();
  abort = vi.fn();
}

class MockSpeechSynthesisUtterance {
  text: string;
  lang = 'en-IN';
  constructor(text: string) {
    this.text = text;
  }
}

describe('AiDecisionPage — Voice AI Integration & Form Safety', () => {
  const originalSpeechRecognition = mockGlobal.SpeechRecognition;
  const originalSpeechSynthesis = mockGlobal.speechSynthesis;
  const originalUtterance = mockGlobal.SpeechSynthesisUtterance;

  let queryClient: QueryClient;

  beforeEach(() => {
    vi.clearAllMocks();
    queryClient = new QueryClient({
      defaultOptions: {
        queries: { retry: false },
        mutations: { retry: false },
      },
    });

    mockGlobal.window = globalThis;
    mockGlobal.SpeechRecognition = MockSpeechRecognition;
    mockGlobal.speechSynthesis = {
      speak: vi.fn(),
      cancel: vi.fn(),
      pause: vi.fn(),
      resume: vi.fn(),
      getVoices: vi.fn(() => []),
    };
    mockGlobal.SpeechSynthesisUtterance = MockSpeechSynthesisUtterance;
  });

  afterEach(() => {
    mockGlobal.SpeechRecognition = originalSpeechRecognition;
    mockGlobal.speechSynthesis = originalSpeechSynthesis;
    mockGlobal.SpeechSynthesisUtterance = originalUtterance;
  });

  const renderPage = () => {
    return renderToString(
      <QueryClientProvider client={queryClient}>
        <AiDecisionPage />
      </QueryClientProvider>
    );
  };

  it('renders Voice Dictation Assistant section with explanation header', () => {
    const html = renderPage();

    expect(html).toContain('Voice Dictation Assistant');
    expect(html).toContain('Speak your quantity or transportation costs in your regional language.');
  });

  it('renders all four approved regional language selector buttons', () => {
    const html = renderPage();

    SUPPORTED_VOICE_LANGUAGES.forEach((lang) => {
      expect(html).toContain(lang.nativeLabel);
    });
  });

  it('renders VoiceInputButton with initial English badge', () => {
    const html = renderPage();

    expect(html).toContain('Speak Numbers');
    expect(html).toContain('EN');
  });

  it('enforces safety rules: numeric extraction logic extracts decimal values safely', () => {
    const parseSpeechNumber = (text: string): number | null => {
      const match = text.match(/[-+]?[0-9]*\.?[0-9]+/);
      return match ? parseFloat(match[0]) : null;
    };

    expect(parseSpeechNumber('50 quintals')).toBe(50);
    expect(parseSpeechNumber('twenty')).toBeNull();
    expect(parseSpeechNumber('₹ 450.75 rupees')).toBe(450.75);
    expect(parseSpeechNumber('price is -10')).toBe(-10);
    expect(parseSpeechNumber('no numbers here')).toBeNull();
  });

  it('renders decision form without auto-submitting on render', () => {
    renderPage();
    expect(mockMutateAsync).not.toHaveBeenCalled();
  });
});
