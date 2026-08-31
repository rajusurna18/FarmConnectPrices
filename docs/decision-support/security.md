# Decision Support Security Architecture

## Principles
1. **Server-Authoritative Price & Quality**: The frontend is forbidden from passing `selectedPrice` or `qualityStatus`. The backend queries the verified price store directly.
2. **Server-Derived Identity**: The client UID is extracted from the cryptographically verified Firebase ID token attached to the `Authorization` header. Body-supplied UID parameters are ignored.
3. **Role Enforcement**: Access is strictly limited to users with the `FARMER` role. `MEDIATOR_BUYER` and `CUSTOMER` profiles receive `403 Forbidden`.
4. **No Write Privilege Leak**: Decision support evaluation performs pure on-demand calculations. It requires zero write access to `marketPrices` or master collection rules.
