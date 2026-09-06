package com.farmlink.api.service.forecast;

import com.farmlink.api.dto.MarketPriceSummaryResponse;
import com.farmlink.api.dto.forecast.ForecastHorizon;
import com.farmlink.api.dto.forecast.ForecastModelType;
import com.farmlink.api.dto.forecast.ForecastRequest;

import java.util.List;

public interface ForecastModel {

    ForecastModelType getModelType();

    String getModelVersion();

    /**
     * Forecasts the estimated modal price for the given horizon.
     *
     * @param observations Chronologically sorted valid historical observations (oldest to newest)
     * @param request      Forecast request parameters (crop, market, horizon)
     * @return Double point forecast, or null if observations are insufficient.
     */
    Double forecastPrice(List<MarketPriceSummaryResponse> observations, ForecastRequest request);

    /**
     * Minimum required observations for the specified horizon.
     */
    int getMinimumObservations(ForecastHorizon horizon);
}
