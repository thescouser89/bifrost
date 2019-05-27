package org.jboss.pnc.bifrost.common.scheduler;

/**
 * @author <a href="mailto:matejonnet@gmail.com">Matej Lazar</a>
 */
public class BackOffRunnableConfig {

    /**
     * Back-off for millis step
     */
    private final long delayMillis;
    private final long maxBackOffCycles;
    private final long timeOutMillis;
    private final long poolIntervalMillis;

    public BackOffRunnableConfig(long delayMillis, long maxBackOffCycles, long timeOutMillis, long poolIntervalMillis) {
        this.delayMillis = delayMillis;
        this.maxBackOffCycles = maxBackOffCycles;
        this.timeOutMillis = timeOutMillis;
        this.poolIntervalMillis = poolIntervalMillis;
    }

    public long getDelayMillis() {
        return delayMillis;
    }

    public long getMaxBackOffCycles() {
        return maxBackOffCycles;
    }

    public long getTimeOutMillis() {
        return timeOutMillis;
    }

    public long getPoolIntervalMillis() {
        return poolIntervalMillis;
    }
}
