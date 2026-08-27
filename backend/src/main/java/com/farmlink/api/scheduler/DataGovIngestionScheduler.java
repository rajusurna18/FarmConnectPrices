package com.farmlink.api.scheduler;

import com.farmlink.api.service.DataGovIngestionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class DataGovIngestionScheduler {

    private static final Logger logger = LoggerFactory.getLogger(DataGovIngestionScheduler.class);

    private final DataGovIngestionService ingestionService;

    @Value("${market-price.ingestion.enabled:false}")
    private boolean ingestionEnabled;

    public DataGovIngestionScheduler(DataGovIngestionService ingestionService) {
        this.ingestionService = ingestionService;
    }

    @Scheduled(cron = "${market-price.ingestion.cron:0 30 18 * * *}")
    public void runScheduledIngestion() {
        if (!ingestionEnabled) {
            logger.debug("Scheduled market-price ingestion is currently disabled (market-price.ingestion.enabled=false). Skipping.");
            return;
        }

        logger.info("Starting scheduled daily mandi market-price incremental ingestion from data.gov.in...");
        try {
            ingestionService.ingestMandiPrices(null);
        } catch (Exception e) {
            logger.error("Error running scheduled market-price ingestion: {}", e.getMessage(), e);
        }
    }
}

