package com.farmlink.api.client;

import com.farmlink.api.config.DataGovMandiProperties;
import com.farmlink.api.dto.external.DataGovMandiResponseDto;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class DataGovMandiClientTest {

    @Test
    void fetchMandiPrices_returnsEmpty_whenApiKeyNotConfigured() {
        DataGovMandiProperties props = new DataGovMandiProperties();
        props.setApiKey(""); // Unconfigured

        DataGovMandiClient client = new DataGovMandiClient(props);
        Optional<DataGovMandiResponseDto> result = client.fetchMandiPrices(10, 0);

        assertTrue(result.isEmpty());
    }
}
