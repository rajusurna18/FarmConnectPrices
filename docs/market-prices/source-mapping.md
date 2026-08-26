# Source Mapping Guidelines

## Controlled Mapping Principles
1. **No Auto-Creation of Markets**: External AGMARKNET market names are matched against existing Module 06 markets. If no reliable match exists, the observation is logged and skipped.
2. **No Auto-Creation of Crops**: External commodity names are matched against existing Module 05 crops. If commodity mapping is ambiguous, the observation is logged and skipped.
3. **Normalization**: Case-insensitive substring and synonym matching (e.g., "Chilli Red" -> `crop-chilli`, "Paddy(Dhan)" -> `crop-paddy`).
