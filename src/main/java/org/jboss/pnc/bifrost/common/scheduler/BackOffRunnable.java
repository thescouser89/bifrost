package org.jboss.pnc.bifrost.common.scheduler;

import org.jboss.logging.Logger;

import java.util.Optional;

/**
 * @author <a href="mailto:matejonnet@gmail.com">Matej Lazar</a>
 */
public class BackOffRunnable implements Runnable {

    private final static Logger logger = Logger.getLogger(BackOffRunnable.class);

    private final BackOffRunnableConfig config;

    private Long lastResult = 0L;
    private Long backOffNextCycles = 0L;

    private Runnable runnable;

    private Optional<Runnable> cancelHook = Optional.empty();

    public BackOffRunnable(BackOffRunnableConfig backOffRunnableConfig) {
        this.config = backOffRunnableConfig;
    }

    public void setRunnable(Runnable runnable) {
        this.runnable = runnable;
    }

    @Override
    public void run() {
        if (lastResult == 0L) {
            lastResult = System.currentTimeMillis() - config.getDelayMillis();
        }
        validateTimeout();
        if (backOffNextCycles > 0L) {
            backOffNextCycles--;
            logger.trace("Cycle skipped.");
            return;
        }
        Long backOff = (System.currentTimeMillis() - lastResult) / config.getDelayMillis();

        backOffNextCycles = Long.min(backOff - 1, config.getMaxBackOffCycles());
        logger.trace("Running task ...");
        runnable.run();
    }

    private void validateTimeout() {
        if (System.currentTimeMillis() - lastResult > config.getTimeOutMillis()) {
            cancelHook.ifPresent(Runnable::run);
        }
    }

    public void receivedResult() {
        this.lastResult = System.currentTimeMillis();
    }

    public void setCancelHook(Runnable cancelHook) {
        this.cancelHook = Optional.ofNullable(cancelHook);
    }
}
