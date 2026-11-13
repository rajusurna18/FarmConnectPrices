import React from 'react';
import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { renderToString } from 'react-dom/server';
import { VoiceInputButton } from '../components/VoiceInputButton';
import { AudioNarrationButton } from '../components/AudioNarrationButton';

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

describe('Voice AI UI Components', () => {
  const originalSpeechRecognition = mockGlobal.SpeechRecognition;
  const originalSpeechSynthesis = mockGlobal.speechSynthesis;
  const originalUtterance = mockGlobal.SpeechSynthesisUtterance;

  let mockSpeechSynthesis: {
    speak: ReturnType<typeof vi.fn>;
    cancel: ReturnType<typeof vi.fn>;
    pause: ReturnType<typeof vi.fn>;
    resume: ReturnType<typeof vi.fn>;
    getVoices: ReturnType<typeof vi.fn>;
  };

  beforeEach(() => {
    vi.clearAllMocks();

    mockSpeechSynthesis = {
      speak: vi.fn(),
      cancel: vi.fn(),
      pause: vi.fn(),
      resume: vi.fn(),
      getVoices: vi.fn(() => []),
    };

    mockGlobal.window = globalThis;
    mockGlobal.SpeechRecognition = MockSpeechRecognition;
    mockGlobal.speechSynthesis = mockSpeechSynthesis;
    mockGlobal.SpeechSynthesisUtterance = MockSpeechSynthesisUtterance;
  });

  afterEach(() => {
    mockGlobal.SpeechRecognition = originalSpeechRecognition;
    mockGlobal.speechSynthesis = originalSpeechSynthesis;
    mockGlobal.SpeechSynthesisUtterance = originalUtterance;
  });

  describe('VoiceInputButton Component', () => {
    it('renders unsupported fallback notice when SpeechRecognition is absent', () => {
      delete mockGlobal.SpeechRecognition;
      delete mockGlobal.webkitSpeechRecognition;

      const html = renderToString(<VoiceInputButton onTranscript={vi.fn()} />);
      expect(html).toContain('Voice unsupported');
    });

    it('renders idle button with accessible label and language badge', () => {
      const html = renderToString(
        <VoiceInputButton
          onTranscript={vi.fn()}
          language="hi-IN"
          label="Dictate Price"
        />
      );

      expect(html).toContain('Dictate Price');
      expect(html).toContain('aria-label="Dictate Price"');
      expect(html).toContain('HI');
    });

    it('renders regional language codes correctly on badge', () => {
      const htmlTelugu = renderToString(
        <VoiceInputButton onTranscript={vi.fn()} language="te-IN" />
      );
      expect(htmlTelugu).toContain('TE');

      const htmlTamil = renderToString(
        <VoiceInputButton onTranscript={vi.fn()} language="ta-IN" />
      );
      expect(htmlTamil).toContain('TA');

      const htmlEnglish = renderToString(
        <VoiceInputButton onTranscript={vi.fn()} language="en-IN" />
      );
      expect(htmlEnglish).toContain('EN');
    });

    it('respects disabled prop and renders disabled button', () => {
      const html = renderToString(
        <VoiceInputButton onTranscript={vi.fn()} disabled={true} />
      );
      expect(html).toContain('disabled=""');
    });

    it('hides language badge when showLanguageBadge=false', () => {
      const html = renderToString(
        <VoiceInputButton onTranscript={vi.fn()} language="hi-IN" showLanguageBadge={false} />
      );
      expect(html).not.toContain('HI');
    });
  });

  describe('AudioNarrationButton Component', () => {
    it('returns empty output (null) when SpeechSynthesis is unsupported', () => {
      delete mockGlobal.speechSynthesis;

      const html = renderToString(<AudioNarrationButton text="Sample narration" />);
      expect(html).toBe('');
    });

    it('renders disabled button when narration text is empty or blank', () => {
      const htmlEmpty = renderToString(<AudioNarrationButton text="" />);
      expect(htmlEmpty).toContain('disabled=""');

      const htmlBlank = renderToString(<AudioNarrationButton text="    " />);
      expect(htmlBlank).toContain('disabled=""');
    });

    it('renders active narration button with accessible label', () => {
      const html = renderToString(
        <AudioNarrationButton
          text="Market prices are rising"
          label="Listen Summary"
        />
      );

      expect(html).toContain('Listen Summary');
      expect(html).toContain('aria-label="Listen Summary"');
      expect(html).not.toContain('disabled=""');
    });

    it('supports compact size="sm" styling', () => {
      const html = renderToString(
        <AudioNarrationButton
          text="Price note"
          size="sm"
        />
      );

      expect(html).toContain('px-2.5');
      expect(html).toContain('text-xs');
    });
  });
});
