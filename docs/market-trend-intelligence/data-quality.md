# Data Quality & Freshness Rules

## Data Quality Classification

Data Quality evaluates observation density across the selected historical time period:

| Observation Count ($N$) | Classification | Description |
|-------------------------|----------------|-------------|
| $N < 2$ | `INSUFFICIENT` | Fewer than 2 observations; trend cannot be established. |
| $2 \le N < 5$ | `LIMITED` | Sparse data; basic trend computed, but low statistical density. |
| $N \ge 5$ | `GOOD` | High observation density suitable for reliable trend evaluation. |

## Freshness Status Rules

Freshness is computed from the latest verified price observation date on record:

- **`FRESH`**: The latest verified observation date is $\le 7$ days old relative to current date.
- **`STALE`**: The latest verified observation date is $> 7$ days old relative to current date.
- **`UNAVAILABLE`**: Zero verified observations matching query criteria.

*Data freshness never fabricates today's date or invents synthetic observation timestamps.*
