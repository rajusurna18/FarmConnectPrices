# Source Mapping Guidelines & Location Hierarchy

## Controlled Mapping Principles
1. **No Auto-Creation of Markets**: External AGMARKNET market names are matched against existing Module 06 markets. If no reliable match exists, the observation is logged and skipped.
2. **No Auto-Creation of Crops**: External commodity names are matched against existing Module 05 crops. If commodity mapping is ambiguous, the observation is logged and skipped.
3. **No Fabrication of Mandals**: Because AGMARKNET resource `35985678-0d79-46b4-9ed6-6f13308a1d24` provides `State`, `District`, and `Market` but **no** sub-district field, Mandals/Areas are never fabricated. Sub-area options remain optional and appear only when verified market master attributes or future LGD mappings provide sub-district relationship data.
4. **Normalization**: Case-insensitive substring and synonym matching (e.g., "Chilli Red" -> `crop-chilli`, "Paddy(Dhan)" -> `crop-paddy`).
