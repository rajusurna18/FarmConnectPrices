# Deterministic Confidence & Empirical Residual Bounds

## 🎯 Order of Precedence Confidence Algorithm

Confidence level (`HIGH`, `MEDIUM`, `LOW`, `INSUFFICIENT_DATA`) is evaluated deterministically using an explicit order-of-precedence ruleset:

---

## 1. Precedence Step 1: Data Sufficiency Overrides
- If observation count $K < M_{min}(h)$ OR freshness gap $D_{gap} > 7$ days $\longrightarrow$ `confidence: INSUFFICIENT_DATA`, `dataQuality: INSUFFICIENT`.
- Minimum observation threshold $M_{min}$:
  - `1_DAY`: 5 observations
  - `3_DAYS`: 5 observations
  - `7_DAYS`: 7 observations
  - `14_DAYS`: 10 observations

---

## 2. Precedence Step 2: Freshness & Residual Bounds Overrides
- If $3 < D_{gap} \le 7$ days (STALE) $\longrightarrow$ Confidence is capped at max `MEDIUM`.
- If backtest residual count $M_h < 5$ OR backtest $\text{MAPE}_h > 15.0\% \longrightarrow$ Confidence is capped at max `LOW`.

---

## 3. Precedence Step 3: Volatility & Horizon Degradation Caps
- If price volatility $CV = \frac{\sigma}{\mu} > 0.20 \longrightarrow$ Downgrade tier by 1 level.
- If horizon $h = 14$ days $\longrightarrow$ Confidence is capped at max `MEDIUM` (unless $\text{MAPE}_{14} \le 5.0\%$, $K \ge 14$, and $D_{gap} \le 2$).

---

## 4. Base Tiers
- `HIGH`: $K \ge 10$, $D_{gap} \le 2$ days, $\text{MAPE}_h \le 5.0\%$, $CV \le 0.15$, $h \le 7$.
- `MEDIUM`: $5 \le K < 10$ OR $2 < D_{gap} \le 3$ days OR $5.0\% < \text{MAPE}_h \le 12.0\%$ OR $0.15 < CV \le 0.20$ OR $h = 14$.
- `LOW`: $D_{gap} \le 7$ days, but $\text{MAPE}_h > 12.0\%$ OR $CV > 0.20$ OR $M_h < 5$.

---

## 📐 Empirical Residual Uncertainty Bounds

- **Residual Standard Error ($S_h$)**:
  $$S_h = \sqrt{ \frac{1}{M_h - 1} \sum_{m=1}^{M_h} (e_{m,h} - \bar{e}_h)^2 }$$
- **Bounds Formula**:
  $$\text{Lower Bound} = \max\left(0, \text{round}\left(\hat{y}_{t+h} - 1.96 \times S_h, 2\right)\right)$$
  $$\text{Upper Bound} = \max\left(0, \text{round}\left(\hat{y}_{t+h} + 1.96 \times S_h, 2\right)\right)$$
- **Statistically Justified Fallback**: If residual count $M_h < 5$, lower and upper bounds return `null`, confidence is set to `LOW`, and an explicit limitation note is attached (*"Insufficient historical backtest observations to derive an empirical uncertainty interval for this horizon"*).
- **Interpretation Guarantee**: $S_h$ is historical out-of-sample residual dispersion for horizon $h$, and $\text{forecast} \pm 1.96 \times S_h$ is explicitly an **empirical residual-based uncertainty interval**, NOT a guaranteed or formal 95% statistical prediction interval.
