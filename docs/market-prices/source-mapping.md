# Source Mapping Guidelines & Location Hierarchy

## Controlled Mapping Principles
1. **Canonical Market Master vs Observed Source Market**: External AGMARKNET market names are matched against existing Module 06 markets. If a reliable match exists, `marketId` points to the canonical market. If no match exists, the observation is safely preserved as an observed market entity (`obs-mkt-<hash>`) with `marketMappingStatus = "UNMAPPED"`. Records are **never** discarded merely because the reference master is incomplete.
2. **Canonical Crop Master vs Observed Commodity**: External commodity names are matched against existing Module 05 crops. If a match exists, `cropId` points to the canonical crop. If no match exists, the observation is preserved as an observed commodity (`obs-crop-<hash>`) with `cropMappingStatus = "UNMAPPED"`.
3. **No Fabrication of Mandals**: Because AGMARKNET datasets provide `State`, `District`, and `Market` but **no** native sub-district field, Mandals/Areas are never fabricated. Sub-area options remain optional and appear only when verified market master attributes or future LGD mappings provide sub-district relationship data.
4. **Deterministic Normalization**: String normalization converts strings to lowercase, trims whitespace, removes diacritics, and handles common plurals deterministically while keeping original raw values preserved under `source.*` for provenance.

