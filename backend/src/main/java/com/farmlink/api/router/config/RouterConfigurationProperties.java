package com.farmlink.api.router.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "app.router")
public class RouterConfigurationProperties {

    private long sessionTtlMs = 1800000L;
    private long lockLeaseMs = 15000L;
    private long heartbeatIntervalMs = 5000L;
    private long resultTtlSeconds = 900L;
    private int maxPayloadBytes = 65536;
    private int maxQueryCodePoints = 1000;
    private int maxQueryBytes = 4096;
    private long minTurnIndex = 1L;
    private long maxTurnIndex = 2147483646L;
    private long terminalSentinelTurn = 2147483647L;

    public long getSessionTtlMs() {
        return sessionTtlMs;
    }

    public void setSessionTtlMs(long sessionTtlMs) {
        this.sessionTtlMs = sessionTtlMs;
    }

    public long getLockLeaseMs() {
        return lockLeaseMs;
    }

    public void setLockLeaseMs(long lockLeaseMs) {
        this.lockLeaseMs = lockLeaseMs;
    }

    public long getHeartbeatIntervalMs() {
        return heartbeatIntervalMs;
    }

    public void setHeartbeatIntervalMs(long heartbeatIntervalMs) {
        this.heartbeatIntervalMs = heartbeatIntervalMs;
    }

    public long getResultTtlSeconds() {
        return resultTtlSeconds;
    }

    public void setResultTtlSeconds(long resultTtlSeconds) {
        this.resultTtlSeconds = resultTtlSeconds;
    }

    public int getMaxPayloadBytes() {
        return maxPayloadBytes;
    }

    public void setMaxPayloadBytes(int maxPayloadBytes) {
        this.maxPayloadBytes = maxPayloadBytes;
    }

    public int getMaxQueryCodePoints() {
        return maxQueryCodePoints;
    }

    public void setMaxQueryCodePoints(int maxQueryCodePoints) {
        this.maxQueryCodePoints = maxQueryCodePoints;
    }

    public int getMaxQueryBytes() {
        return maxQueryBytes;
    }

    public void setMaxQueryBytes(int maxQueryBytes) {
        this.maxQueryBytes = maxQueryBytes;
    }

    public long getMinTurnIndex() {
        return minTurnIndex;
    }

    public void setMinTurnIndex(long minTurnIndex) {
        this.minTurnIndex = minTurnIndex;
    }

    public long getMaxTurnIndex() {
        return maxTurnIndex;
    }

    public void setMaxTurnIndex(long maxTurnIndex) {
        this.maxTurnIndex = maxTurnIndex;
    }

    public long getTerminalSentinelTurn() {
        return terminalSentinelTurn;
    }

    public void setTerminalSentinelTurn(long terminalSentinelTurn) {
        this.terminalSentinelTurn = terminalSentinelTurn;
    }
}

