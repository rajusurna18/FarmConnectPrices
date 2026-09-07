# Smart Selling System Limitations

1. **Non-Autonomous**: The engine provides decision support only. It does not execute automated trades, place orders, negotiate, or book transport.
2. **Forecast Uncertainty**: Forecast outputs represent probabilistic scenarios based on moving average models and do not guarantee future price movements.
3. **Data Freshness Dependency**: Decision accuracy depends on the availability and freshness of verified mandi price records from Data.gov.in / Agmarknet sources.
4. **User-Entered Estimates**: Selling costs provided via `customSellingCosts` are user estimates (`USER_ESTIMATE` / `NOT_VERIFIED`) and are subject to market conditions at transaction time.
