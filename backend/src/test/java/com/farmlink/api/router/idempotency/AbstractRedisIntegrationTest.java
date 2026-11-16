package com.farmlink.api.router.idempotency;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;

public abstract class AbstractRedisIntegrationTest {

    private static final Logger logger = LoggerFactory.getLogger(AbstractRedisIntegrationTest.class);
    private static GenericContainer<?> redisContainer;
    private static boolean dockerAvailable = false;

    @BeforeAll
    static void checkDocker() {
        try {
            dockerAvailable = DockerClientFactory.instance().isDockerAvailable();
        } catch (Throwable t) {
            dockerAvailable = false;
        }

        if (dockerAvailable) {
            try {
                if (redisContainer == null) {
                    redisContainer = new GenericContainer<>(DockerImageName.parse("redis:7.2-alpine"))
                            .withExposedPorts(6379);
                    redisContainer.start();
                }
            } catch (Throwable t) {
                logger.warn("Failed to start Redis container even though Docker appeared available", t);
                dockerAvailable = false;
            }
        }

        Assumptions.assumeTrue(dockerAvailable, "Docker daemon not running; skipping Redis integration test");
    }

    @DynamicPropertySource
    static void redisProperties(DynamicPropertyRegistry registry) {
        if (dockerAvailable && redisContainer != null && redisContainer.isRunning()) {
            registry.add("spring.data.redis.host", redisContainer::getHost);
            registry.add("spring.data.redis.port", () -> redisContainer.getMappedPort(6379));
        }
    }
}

