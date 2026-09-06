# Forecast Error Metrics Specification

## 📏 Mathematical Error Metrics

Evaluating baseline forecasting performance relies on out-of-sample walk-forward historical error metrics computed separately per horizon ($h \in \{1, 3, 7, 14\}$).

---

## 1. Mean Absolute Error (MAE)

Mean Absolute Error measures the average magnitude of prediction errors in actual currency units (INR):

$$\text{MAE}_h = \frac{1}{M_h} \sum_{m=1}^{M_h} |y_{m,h} - \hat{y}_{m,h}|$$

Where:
- $M_h$: Total number of valid backtest iterations for horizon $h$.
- $y_{m,h}$: Actual verified modal price observed at date $T_{cutoff} + h$.
- $\hat{y}_{m,h}$: Point forecast generated using only training data available up to $T_{cutoff}$.

---

## 2. Mean Absolute Percentage Error (MAPE)

Mean Absolute Percentage Error measures relative error percentage:

$$\text{MAPE}_h = \frac{1}{M_h} \sum_{m=1}^{M_h} \left( \frac{|y_{m,h} - \hat{y}_{m,h}|}{y_{m,h}} \right) \times 100$$

### 🛡️ Zero-Actual Protection
- If actual price $y_{m,h} = 0$, the observation is excluded from MAPE calculations to prevent division-by-zero errors.
- If all actual prices are zero, the system returns `mape: null`.
