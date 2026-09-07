# Consequence-Verified Trade-Off Engine

## Overview

The trade-off engine tests actual calculated consequences rather than comparing raw price and transport in isolation:

- **Higher Raw Price + Higher Transport Cost**:
  - *If Price Premium > Extra Transport Cost*: Market A yields higher net realization despite higher transport.
  - *If Price Premium < Extra Transport Cost*: Market B yields higher net realization because lower transport erodes Market A's price advantage.
- **Stale Price vs Fresh Price**: Flags markets offering higher margin on paper but relying on stale price data ($> 24\text{h}$ old).
- **Trend Divergence**: Flags markets with higher current price but a `FALLING` trend vs a market exhibiting a `RISING` trend.
