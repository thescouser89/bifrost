package org.jboss.pnc.bifrost.mock;

import org.jboss.pnc.bifrost.common.scheduler.BackOffRunnableConfig;

/**
 * @author <a href="mailto:matejonnet@gmail.com">Matej Lazar</a>
 */
public class BackOffRunnableConfigFactory {

    public static BackOffRunnableConfig get(long delayMilis, long maxBackOffCycles, long timeOutMillis, long poolIntervalMillis) {
        BackOffRunnableConfig config = new BackOffRunnableConfig();
        config.setDelayMillis(delayMilis);
        config.setMaxBackOffCycles(maxBackOffCycles);
        config.setPollIntervalMillis(poolIntervalMillis);
        config.setTimeOutMillis(timeOutMillis);
        return config;
    }

}
