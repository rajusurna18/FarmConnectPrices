# AI Decision Integration & Fallback Architecture

## Explanation-Only Architecture

```
Deterministic Decision Engine
              │
    Structured Decision Result
              │
          AI Context
              │
    Module 11 AI Layer (AiDecisionService)
              │
    Sanitized Narrative & Risks
```

## Guardrails & Sanitization
- `AiDecisionValidator` inspects generated text and strips forbidden speculative claims (e.g. "definitely sell", "guaranteed profit").
- If the AI provider fails or returns malformed schema, `RuleBasedAiDecisionEngine` seamlessly supplies rule-based fallback narratives without failing the API response.
