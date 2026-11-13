export type VoiceLanguage = 'en-IN' | 'hi-IN' | 'te-IN' | 'ta-IN';

export interface VoiceLanguageOption {
  code: VoiceLanguage;
  label: string;
  nativeLabel: string;
}

export const SUPPORTED_VOICE_LANGUAGES: VoiceLanguageOption[] = [
  { code: 'en-IN', label: 'English (India)', nativeLabel: 'English' },
  { code: 'hi-IN', label: 'Hindi', nativeLabel: 'हिन्दी' },
  { code: 'te-IN', label: 'Telugu', nativeLabel: 'తెలుగు' },
  { code: 'ta-IN', label: 'Tamil', nativeLabel: 'தமிழ்' },
];

export interface UseSpeechToTextOptions {
  language?: VoiceLanguage;
  continuous?: boolean;
  interimResults?: boolean;
  onResult?: (transcript: string) => void;
  onError?: (error: string) => void;
}

export interface UseSpeechToTextReturn {
  isListening: boolean;
  transcript: string;
  interimTranscript: string;
  error: string | null;
  isSupported: boolean;
  language: VoiceLanguage;
  setLanguage: (lang: VoiceLanguage) => void;
  startListening: (lang?: VoiceLanguage) => void;
  stopListening: () => void;
  resetTranscript: () => void;
}

export interface UseTextToSpeechOptions {
  language?: VoiceLanguage;
  pitch?: number;
  rate?: number;
  volume?: number;
  onEnd?: () => void;
  onError?: (error: string) => void;
}

export interface UseTextToSpeechReturn {
  isSpeaking: boolean;
  isSupported: boolean;
  speak: (text: string, lang?: VoiceLanguage) => void;
  cancel: () => void;
  pause: () => void;
  resume: () => void;
}

