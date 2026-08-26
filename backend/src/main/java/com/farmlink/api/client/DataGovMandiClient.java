package com.farmlink.api.client;

import com.farmlink.api.config.DataGovMandiProperties;
import com.farmlink.api.dto.external.DataGovMandiResponseDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.Optional;

@Component
public class DataGovMandiClient {

    private static final Logger logger = LoggerFactory.getLogger(DataGovMandiClient.class);

    private final DataGovMandiProperties properties;
    private final RestClient restClient;

    public DataGovMandiClient(DataGovMandiProperties properties) {
        this.properties = properties;
        this.restClient = RestClient.builder().build();
    }

    public Optional<DataGovMandiResponseDto> fetchMandiPrices(Integer limit, Integer offset) {
        String apiKey = properties.getApiKey();
        if (apiKey == null || apiKey.trim().isEmpty()) {
            logger.warn("DATA_GOV_IN_API_KEY is not configured. External ingestion skipped.");
            return Optional.empty();
        }

        int fetchLimit = (limit != null && limit > 0) ? limit : 100;
        int fetchOffset = (offset != null && offset >= 0) ? offset : 0;

        try {
            URI uri = UriComponentsBuilder.fromHttpUrl(properties.getBaseUrl())
                    .pathSegment(properties.getResourceId())
                    .queryParam("api-key", apiKey.trim())
                    .queryParam("format", "json")
                    .queryParam("limit", fetchLimit)
                    .queryParam("offset", fetchOffset)
                    .build()
                    .toUri();

            logger.info("Fetching mandi price data from data.gov.in resourceId: {}, limit: {}", properties.getResourceId(), fetchLimit);

            DataGovMandiResponseDto response = restClient.get()
                    .uri(uri)
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .body(DataGovMandiResponseDto.class);

            if (response != null && response.getRecords() != null) {
                logger.info("Successfully fetched {} records from data.gov.in (Total reported: {})",
                        response.getRecords().size(), response.getTotal());
                return Optional.of(response);
            }
        } catch (Exception e) {
            logger.error("Failed to fetch data from data.gov.in: {}", e.getMessage());
        }

        return Optional.empty();
    }
}
