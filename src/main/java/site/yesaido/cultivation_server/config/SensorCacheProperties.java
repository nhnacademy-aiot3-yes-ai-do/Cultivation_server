package site.yesaido.cultivation_server.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "sensor-cache")
public class SensorCacheProperties {
    private long historyHours = 12;
    private long pollIntervalMs = 2000;
    private long freshnessSeconds = 9;
    private long ttlGraceSeconds = 3;
    private long queryOverlapSeconds = 60;
    private long pollInitialDelayMs = 10000;
    private long lockLeaseSeconds = 600;
    private long reconciliationIntervalSeconds = 300;
    private long sensorSnapshotCacheSeconds = 5;
    private long fallbackWaitSeconds = 25;

    public long getHistoryHours() { return historyHours; }
    public void setHistoryHours(long historyHours) { this.historyHours = historyHours; }
    public long getPollIntervalMs() { return pollIntervalMs; }
    public void setPollIntervalMs(long pollIntervalMs) { this.pollIntervalMs = pollIntervalMs; }
    public long getFreshnessSeconds() { return freshnessSeconds; }
    public void setFreshnessSeconds(long freshnessSeconds) { this.freshnessSeconds = freshnessSeconds; }
    public long getTtlGraceSeconds() { return ttlGraceSeconds; }
    public void setTtlGraceSeconds(long ttlGraceSeconds) { this.ttlGraceSeconds = ttlGraceSeconds; }
    public long getQueryOverlapSeconds() { return queryOverlapSeconds; }
    public void setQueryOverlapSeconds(long queryOverlapSeconds) { this.queryOverlapSeconds = queryOverlapSeconds; }
    public long getPollInitialDelayMs() { return pollInitialDelayMs; }
    public void setPollInitialDelayMs(long pollInitialDelayMs) { this.pollInitialDelayMs = pollInitialDelayMs; }
    public long getLockLeaseSeconds() { return lockLeaseSeconds; }
    public void setLockLeaseSeconds(long lockLeaseSeconds) { this.lockLeaseSeconds = lockLeaseSeconds; }
    public long getReconciliationIntervalSeconds() { return reconciliationIntervalSeconds; }
    public void setReconciliationIntervalSeconds(long reconciliationIntervalSeconds) { this.reconciliationIntervalSeconds = reconciliationIntervalSeconds; }
    public long getSensorSnapshotCacheSeconds() { return sensorSnapshotCacheSeconds; }
    public void setSensorSnapshotCacheSeconds(long sensorSnapshotCacheSeconds) { this.sensorSnapshotCacheSeconds = sensorSnapshotCacheSeconds; }
    public long getFallbackWaitSeconds() { return fallbackWaitSeconds; }
    public void setFallbackWaitSeconds(long fallbackWaitSeconds) { this.fallbackWaitSeconds = fallbackWaitSeconds; }
}
