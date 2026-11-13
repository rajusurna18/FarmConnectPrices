import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { renderHook, act } from './testUtils';
import { useTextToSpeech } from '../hooks/useTextToSpeech';

interface SpeechMockWindow {
  speechSynthesis?: unknown;
  SpeechSynthesisUtterance?: unknown;
}

const mockWindow = (typeof window !== 'undefined' ? window : globalThis) as unknown as SpeechMockWindow;

class MockSpeechSynthesisUtterance {
  text: string;
  lang = 'en-IN';
  pitch = 1.0;
  rate = 1.0;
  volume = 1.0;
  voice: unknown = null;
  onstart: (() => void) | null = null;
  onend: (() => void) | null = null;
  onerror: ((event: { error: string }) => void) | null = null;

  constructor(text: string) {
    this.text = text;
  }
}

interface MockSpeechSynthesis {
  speak: ReturnType<typeof vi.fn>;
  cancel: ReturnType<typeof vi.fn>;
  pause: ReturnType<typeof vi.fn>;
  resume: ReturnType<typeof vi.fn>;
  getVoices: ReturnType<typeof vi.fn>;
}

describe('Voice AI — useTextToSpeech Hook', () => {
  const originalSpeechSynthesis = mockWindow.speechSynthesis;
  const originalUtterance = mockWindow.SpeechSynthesisUtterance;

  let mockSpeechSynthesis: MockSpeechSynthesis;

  beforeEach(() => {
    vi.clearAllMocks();

    mockSpeechSynthesis = {
      speak: vi.fn(),
      cancel: vi.fn(),
      pause: vi.fn(),
      resume: vi.fn(),
      getVoices: vi.fn(() => []),
    };

    mockWindow.speechSynthesis = mockSpeechSynthesis;
    mockWindow.SpeechSynthesisUtterance = MockSpeechSynthesisUtterance;
  });

  afterEach(() => {
    mockWindow.speechSynthesis = originalSpeechSynthesis;
    mockWindow.SpeechSynthesisUtterance = originalUtterance;
  });

  it('reports isSupported=true when speechSynthesis and SpeechSynthesisUtterance exist', () => {
    const { result } = renderHook(() => useTextToSpeech());
    expect(result.current.isSupported).toBe(true);
    expect(result.current.isSpeaking).toBe(false);
  });

  it('reports isSupported=false when window.speechSynthesis is not defined', () => {
    delete mockWindow.speechSynthesis;

    const onError = vi.fn();
    const { result } = renderHook(() => useTextToSpeech({ onError }));
    expect(result.current.isSupported).toBe(false);

    act(() => {
      result.current.speak('Test speech');
    });

    expect(onError).toHaveBeenCalledWith(expect.stringContaining('not supported in this browser'));
    expect(result.current.isSpeaking).toBe(false);
  });

  it('ignores empty or whitespace-only text', () => {
    const { result } = renderHook(() => useTextToSpeech());

    act(() => {
      result.current.speak('');
      result.current.speak('   ');
    });

    expect(mockSpeechSynthesis.speak).not.toHaveBeenCalled();
    expect(result.current.isSpeaking).toBe(false);
  });

  it('creates SpeechSynthesisUtterance with options and dispatches speech', () => {
    const onEnd = vi.fn();
    const { result } = renderHook(() =>
      useTextToSpeech({
        language: 'hi-IN',
        pitch: 1.2,
        rate: 0.9,
        volume: 0.8,
        onEnd,
      })
    );

    act(() => {
      result.current.speak('नमस्ते किसान भाई');
    });

    expect(mockSpeechSynthesis.speak).toHaveBeenCalledTimes(1);
    const utterance = mockSpeechSynthesis.speak.mock.calls[0][0] as MockSpeechSynthesisUtterance;
    expect(utterance.text).toBe('नमस्ते किसान भाई');
    expect(utterance.lang).toBe('hi-IN');
    expect(utterance.pitch).toBe(1.2);
    expect(utterance.rate).toBe(0.9);
    expect(utterance.volume).toBe(0.8);

    // Simulate speech start
    act(() => {
      utterance.onstart?.();
    });
    expect(result.current.isSpeaking).toBe(true);

    // Simulate speech end
    act(() => {
      utterance.onend?.();
    });
    expect(result.current.isSpeaking).toBe(false);
    expect(onEnd).toHaveBeenCalledTimes(1);
  });

  it('cancels ongoing speech before starting a new one to prevent overlapping narration', () => {
    const { result } = renderHook(() => useTextToSpeech());

    act(() => {
      result.current.speak('First message');
    });
    expect(mockSpeechSynthesis.cancel).toHaveBeenCalledTimes(1);

    act(() => {
      result.current.speak('Second message');
    });
    expect(mockSpeechSynthesis.cancel).toHaveBeenCalledTimes(2);
    expect(mockSpeechSynthesis.speak).toHaveBeenCalledTimes(2);
  });

  it('supports explicit pause and resume controls', () => {
    const { result } = renderHook(() => useTextToSpeech());

    act(() => {
      result.current.pause();
    });
    expect(mockSpeechSynthesis.pause).toHaveBeenCalledTimes(1);

    act(() => {
      result.current.resume();
    });
    expect(mockSpeechSynthesis.resume).toHaveBeenCalledTimes(1);
  });

  it('supports explicit cancel control', () => {
    const { result } = renderHook(() => useTextToSpeech());

    act(() => {
      result.current.speak('Testing cancel');
    });
    const utterance = mockSpeechSynthesis.speak.mock.calls[0][0] as MockSpeechSynthesisUtterance;
    act(() => {
      utterance.onstart?.();
    });
    expect(result.current.isSpeaking).toBe(true);

    act(() => {
      result.current.cancel();
    });
    expect(mockSpeechSynthesis.cancel).toHaveBeenCalled();
    expect(result.current.isSpeaking).toBe(false);
  });

  it('selects matched voice when available in window.speechSynthesis.getVoices', () => {
    const mockVoices = [
      { name: 'US English', lang: 'en-US' },
      { name: 'Indian English', lang: 'en-IN' },
      { name: 'Hindi Voice', lang: 'hi-IN' },
      { name: 'Tamil Voice', lang: 'ta-IN' },
    ];
    mockSpeechSynthesis.getVoices = vi.fn(() => mockVoices);

    const { result } = renderHook(() => useTextToSpeech({ language: 'en-IN' }));

    act(() => {
      result.current.speak('Hello farmer', 'hi-IN');
    });

    const utterance = mockSpeechSynthesis.speak.mock.calls[0][0] as MockSpeechSynthesisUtterance;
    expect(utterance.voice).toEqual({ name: 'Hindi Voice', lang: 'hi-IN' });
  });

  it('falls back to language prefix matching when exact voice is not available', () => {
    const mockVoices = [
      { name: 'Generic Tamil', lang: 'ta' },
    ];
    mockSpeechSynthesis.getVoices = vi.fn(() => mockVoices);

    const { result } = renderHook(() => useTextToSpeech());

    act(() => {
      result.current.speak('வணக்கம்', 'ta-IN');
    });

    const utterance = mockSpeechSynthesis.speak.mock.calls[0][0] as MockSpeechSynthesisUtterance;
    expect(utterance.voice).toEqual({ name: 'Generic Tamil', lang: 'ta' });
  });

  it('suppresses canceled / interrupted error events during speech narration', () => {
    const onError = vi.fn();
    const { result } = renderHook(() => useTextToSpeech({ onError }));

    act(() => {
      result.current.speak('Cancel test');
    });
    const utterance = mockSpeechSynthesis.speak.mock.calls[0][0] as MockSpeechSynthesisUtterance;

    act(() => {
      utterance.onerror?.({ error: 'canceled' });
    });
    expect(onError).not.toHaveBeenCalled();

    act(() => {
      utterance.onerror?.({ error: 'interrupted' });
    });
    expect(onError).not.toHaveBeenCalled();
  });

  it('triggers onError callback on real speech synthesis error', () => {
    const onError = vi.fn();
    const { result } = renderHook(() => useTextToSpeech({ onError }));

    act(() => {
      result.current.speak('Real error test');
    });
    const utterance = mockSpeechSynthesis.speak.mock.calls[0][0] as MockSpeechSynthesisUtterance;

    act(() => {
      utterance.onerror?.({ error: 'audio-busy' });
    });

    expect(onError).toHaveBeenCalledWith('Text-to-speech error: audio-busy');
    expect(result.current.isSpeaking).toBe(false);
  });

  it('cancels ongoing speech on unmount', () => {
    const { unmount } = renderHook(() => useTextToSpeech());
    unmount();
    expect(mockSpeechSynthesis.cancel).toHaveBeenCalledTimes(1);
  });
});
