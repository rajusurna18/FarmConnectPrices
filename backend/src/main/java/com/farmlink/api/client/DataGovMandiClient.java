package com.farmlink.api.client;

import com.farmlink.api.config.DataGovMandiProperties;
import com.farmlink.api.dto.external.DataGovMandiResponseDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.Map;
import java.util.Optional;

@Component
public class DataGovMandiClient {

    private static final Logger logger = LoggerFactory.getLogger(DataGovMandiClient.class);

    private final DataGovMandiProperties properties;
    private final RestClient restClient;

    public DataGovMandiClient(DataGovMandiProperties properties) {
        this.properties = properties;
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(30000);
        requestFactory.setReadTimeout(60000);
        this.restClient = RestClient.builder()
                .requestFactory(requestFactory)
                .build();
    }


    public Optional<DataGovMandiResponseDto> fetchMandiPrices(Integer limit, Integer offset) {
        return fetchMandiPrices(properties.getResourceId(), limit, offset, null);
    }

    public Optional<DataGovMandiResponseDto> fetchMandiPrices(String resourceId, Integer limit, Integer offset) {
        return fetchMandiPrices(resourceId, limit, offset, null);
    }

    public Optional<DataGovMandiResponseDto> fetchMandiPrices(
            String targetResourceId,
            Integer limit,
            Integer offset,
            Map<String, String> filters
    ) {
        String apiKey = properties.getApiKey();
        if (apiKey == null || apiKey.trim().isEmpty()) {
            logger.warn("DATA_GOV_IN_API_KEY is not configured. External ingestion skipped.");
            return Optional.empty();
        }

        String resId = (targetResourceId != null && !targetResourceId.trim().isEmpty())
                ? targetResourceId.trim()
                : properties.getResourceId();

        int fetchLimit = (limit != null && limit > 0) ? limit : properties.getPageSize();
        int fetchOffset = (offset != null && offset >= 0) ? offset : 0;
        int maxAttempts = Math.max(1, properties.getRetryAttempts());

        UriComponentsBuilder uriBuilder = UriComponentsBuilder.fromHttpUrl(properties.getBaseUrl())
                .pathSegment(resId)
                .queryParam("api-key", apiKey.trim())
                .queryParam("format", "json")
                .queryParam("limit", fetchLimit)
                .queryParam("offset", fetchOffset);

        if (filters != null && !filters.isEmpty()) {
            for (Map.Entry<String, String> entry : filters.entrySet()) {
                if (entry.getKey() != null && entry.getValue() != null && !entry.getValue().trim().isEmpty()) {
                    uriBuilder.queryParam("filters[" + entry.getKey().trim() + "]", entry.getValue().trim());
                }
            }
        }

        URI uri = uriBuilder.build().toUri();

        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                logger.info("Fetching mandi price page from data.gov.in resourceId: {}, offset: {}, limit: {} (Attempt {}/{})",
                        resId, fetchOffset, fetchLimit, attempt, maxAttempts);

                DataGovMandiResponseDto response = restClient.get()
                        .uri(uri)
                        .accept(MediaType.APPLICATION_JSON)
                        .retrieve()
                        .body(DataGovMandiResponseDto.class);

                if (response != null && response.getRecords() != null) {
                    logger.info("Successfully fetched {} records from data.gov.in resource {} at offset {} (Total reported: {})",
                            response.getRecords().size(), resId, fetchOffset, response.getTotal());
                    return Optional.of(response);
                }
            } catch (Exception e) {
                logger.warn("Attempt {}/{} failed fetching data from data.gov.in resource {}: {}",
                        attempt, maxAttempts, resId, e.getMessage());
                if (attempt < maxAttempts) {
                    try {
                        Thread.sleep(attempt * 1000L);
                    } catch (InterruptedException ie) {

                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
        }

        logger.error("Exhausted all {} retry attempts fetching data from data.gov.in resource {} at offset {}",
                maxAttempts, resId, fetchOffset);
        return Optional.empty();
    }
}

