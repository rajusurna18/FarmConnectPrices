# Module 11 — AI Architecture & Data Flow

## Data Flow Architecture

```text
Farmer (UI Component: AiDecisionPage)
   │
   │ POST /api/v1/ai/decisions (Bearer token)
   ▼
Spring Boot Controller (AiDecisionController)
   │
   ├─► Verify Firebase Auth Token (FirebaseAuthenticationToken)
   ├─► Enforce Role Authorization (FarmService.verifyFarmerRole -> FARMER only)
   │
   ▼
AiDecisionService (Orchestrator)
   │
   ├─► 1. AiDecisionContextBuilder
   │      ├── Fetch Crop (CropMasterService)
   │      ├── Fetch Farm (FarmService - Owner UID verified)
   │      ├── Fetch Economic Record (FarmEconomicsService - Owner UID verified)
   │      ├── Fetch Market Details (MarketService)
   │      ├── Evaluate Single/Multiple Markets (DecisionSupportService - Module 09)
   │      └── Evaluate Farm Profitability & Comparisons (FarmEconomicsService - Module 10)
   │
   ├─► 2. Deterministic Confidence Calculator
   │      ├── HIGH: Verified current price, quantity, crop, markets, economics available.
   │      ├── MEDIUM: Relevant data present but price is stale or minor inputs missing.
   │      └── LOW: Verified price missing or unprocessable.
   │
   ├─► 3. AiDecisionEngine / RuleBasedAiDecisionEngine
   │      ├── Evaluates reasoning & metrics directly over context.
   │      └── Formulates structured response without overriding backend math.
   │
   ├─► 4. AiDecisionValidator (Guardrails)
   │      ├── Sanitizes forbidden speculative phrases ("definitely sell", "guaranteed profit").
   │      └── Attaches mandatory disclaimers and data freshness statements.
   │
   ▼
Structured AiDecisionResponse
```

## Security & Scoping
- **Stateless Execution**: Requests build context on demand and return structured responses. No temporary or persistent AI history collections are stored in Firestore.
- **Circuit Breaker & Caching**: All underlying Firestore calls execute through `FirestoreQuotaGuard` and Spring Cache (`@Cacheable`).
