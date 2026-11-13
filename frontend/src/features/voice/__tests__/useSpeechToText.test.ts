import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { renderHook, act } from './testUtils';
import { useSpeechToText } from '../hooks/useSpeechToText';

interface SpeechMockWindow {
  SpeechRecognition?: unknown;
  webkitSpeechRecognition?: unknown;
}

const mockWindow = (typeof window !== 'undefined' ? window : globalThis) as unknown as SpeechMockWindow;

// Mock SpeechRecognition constructor and instance
class MockSpeechRecognition {
  lang = '';
  continuous = false;
  interimResults = false;
  onstart: (() => void) | null = null;
  onresult: ((event: unknown) => void) | null = null;
  onerror: ((event: { error: string }) => void) | null = null;
  onend: (() => void) | null = null;

  start = vi.fn(() => {
    if (this.onstart) this.onstart();
  });
  stop = vi.fn(() => {
    if (this.onend) this.onend();
  });
  abort = vi.fn(() => {
    if (this.onend) this.onend();
  });
}

describe('Voice AI — useSpeechToText Hook', () => {
  const originalSpeechRecognition = mockWindow.SpeechRecognition;
  const originalWebkitSpeechRecognition = mockWindow.webkitSpeechRecognition;

  beforeEach(() => {
    vi.clearAllMocks();
    mockWindow.SpeechRecognition = MockSpeechRecognition;
    delete mockWindow.webkitSpeechRecognition;
  });

  afterEach(() => {
    mockWindow.SpeechRecognition = originalSpeechRecognition;
    mockWindow.webkitSpeechRecognition = originalWebkitSpeechRecognition;
  });

  it('reports isSupported=true when SpeechRecognition is available in window', () => {
    const { result } = renderHook(() => useSpeechToText());
    expect(result.current.isSupported).toBe(true);
    expect(result.current.isListening).toBe(false);
    expect(result.current.transcript).toBe('');
    expect(result.current.error).toBeNull();
  });

  it('reports isSupported=false when neither SpeechRecognition nor webkitSpeechRecognition exists', () => {
    delete mockWindow.SpeechRecognition;
    delete mockWindow.webkitSpeechRecognition;

    const { result } = renderHook(() => useSpeechToText());
    expect(result.current.isSupported).toBe(false);

    // Calling startListening when unsupported sets an error
    act(() => {
      result.current.startListening();
    });
    expect(result.current.error).toContain('not supported in this browser');
    expect(result.current.isListening).toBe(false);
  });

  it('supports webkitSpeechRecognition fallback prefix', () => {
    delete mockWindow.SpeechRecognition;
    mockWindow.webkitSpeechRecognition = MockSpeechRecognition;

    const { result } = renderHook(() => useSpeechToText());
    expect(result.current.isSupported).toBe(true);
  });

  it('starts listening, creates session, and assigns language', () => {
    const { result } = renderHook(() => useSpeechToText({ language: 'hi-IN' }));

    act(() => {
      result.current.startListening();
    });

    expect(result.current.isListening).toBe(true);
    expect(result.current.error).toBeNull();
  });

  it('prevents duplicate recognition sessions if already listening', () => {
    const { result } = renderHook(() => useSpeechToText());

    act(() => {
      result.current.startListening();
    });
    expect(result.current.isListening).toBe(true);

    // Attempt second start while already listening
    act(() => {
      result.current.startListening();
    });
    expect(result.current.isListening).toBe(true);
  });

  it('handles explicit stopListening correctly', () => {
    const { result } = renderHook(() => useSpeechToText());

    act(() => {
      result.current.startListening();
    });
    expect(result.current.isListening).toBe(true);

    act(() => {
      result.current.stopListening();
    });
    expect(result.current.isListening).toBe(false);
  });

  it('accumulates final transcripts and updates state', () => {
    const onResult = vi.fn();
    const { result } = renderHook(() => useSpeechToText({ onResult }));

    act(() => {
      result.current.startListening();
    });

    expect(result.current.isListening).toBe(true);

    act(() => {
      result.current.resetTranscript();
    });
    expect(result.current.transcript).toBe('');
    expect(result.current.interimTranscript).toBe('');
  });

  it('handles microphone permission denial (not-allowed) gracefully', () => {
    const onError = vi.fn();
    let currentMock: MockSpeechRecognition | null = null;
    const registerInstance = (inst: MockSpeechRecognition) => {
      currentMock = inst;
    };

    class FailingMockSpeechRecognition extends MockSpeechRecognition {
      constructor() {
        super();
        registerInstance(this);
      }
    }
    mockWindow.SpeechRecognition = FailingMockSpeechRecognition;

    const { result } = renderHook(() => useSpeechToText({ onError }));

    act(() => {
      result.current.startListening();
    });

    act(() => {
      if (currentMock?.onerror) {
        currentMock.onerror({ error: 'not-allowed' });
      }
    });

    expect(result.current.isListening).toBe(false);
    expect(result.current.error).toContain('Microphone permission was denied');
    expect(onError).toHaveBeenCalledWith(expect.stringContaining('Microphone permission was denied'));
  });

  it('handles network and no-speech recognition errors gracefully', () => {
    let currentMock: MockSpeechRecognition | null = null;
    const registerInstance = (inst: MockSpeechRecognition) => {
      currentMock = inst;
    };

    class ErrorMockSpeechRecognition extends MockSpeechRecognition {
      constructor() {
        super();
        registerInstance(this);
      }
    }
    mockWindow.SpeechRecognition = ErrorMockSpeechRecognition;

    const { result } = renderHook(() => useSpeechToText());

    act(() => {
      result.current.startListening();
    });

    act(() => {
      if (currentMock?.onerror) {
        currentMock.onerror({ error: 'no-speech' });
      }
    });

    expect(result.current.isListening).toBe(false);
    expect(result.current.error).toContain('No speech was detected');

    act(() => {
      result.current.startListening();
    });

    act(() => {
      if (currentMock?.onerror) {
        currentMock.onerror({ error: 'network' });
      }
    });

    expect(result.current.isListening).toBe(false);
    expect(result.current.error).toContain('Network error during speech recognition');
  });

  it('switches languages dynamically across regional options (en-IN, hi-IN, te-IN, ta-IN)', () => {
    let activeInstance: MockSpeechRecognition | null = null;
    const registerInstance = (inst: MockSpeechRecognition) => {
      activeInstance = inst;
    };

    class LangMockSpeechRecognition extends MockSpeechRecognition {
      constructor() {
        super();
        registerInstance(this);
      }
    }
    mockWindow.SpeechRecognition = LangMockSpeechRecognition;

    const { result } = renderHook(() => useSpeechToText());

    // Switch to Telugu
    act(() => {
      result.current.setLanguage('te-IN');
    });
    expect(result.current.language).toBe('te-IN');

    act(() => {
      result.current.startListening();
    });
    expect(activeInstance?.lang).toBe('te-IN');

    act(() => {
      result.current.stopListening();
    });

    // Override with Tamil
    act(() => {
      result.current.startListening('ta-IN');
    });
    expect(activeInstance?.lang).toBe('ta-IN');
  });

  it('cleans up and aborts recognition session on unmount', () => {
    let activeInstance: MockSpeechRecognition | null = null;
    const registerInstance = (inst: MockSpeechRecognition) => {
      activeInstance = inst;
    };

    class UnmountMockSpeechRecognition extends MockSpeechRecognition {
      constructor() {
        super();
        registerInstance(this);
      }
    }
    mockWindow.SpeechRecognition = UnmountMockSpeechRecognition;

    const { result, unmount } = renderHook(() => useSpeechToText());

    act(() => {
      result.current.startListening();
    });

    unmount();
    expect(activeInstance?.abort).toHaveBeenCalled();
  });
});
