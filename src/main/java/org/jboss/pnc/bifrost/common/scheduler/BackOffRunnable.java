package org.jboss.pnc.bifrost.common.scheduler;

/**
 * @author <a href="mailto:matejonnet@gmail.com">Matej Lazar</a>
 */
public class BackOffRunnable implements Runnable {

    private final BackOffRunnableConfig config;

    private Long lastResult = 0L;
    private Long backOffNextCycles = 0L;

    private Runnable runnable;

    private Runnable cancelHook;

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
        if (backOffNextCycles > 0L) {
            backOffNextCycles--;
            validateTimeout();
            return;
        }
        Long backOff = (System.currentTimeMillis() - lastResult) / config.getDelayMillis();

        backOffNextCycles = Long.min(backOff - 1, config.getMaxBackOffCycles());
        runnable.run();
    }

    private void validateTimeout() {
        if (System.currentTimeMillis() - lastResult > config.getTimeOutMillis()) {
            cancelHook.run();
        }
    }

    public void receivedResult() {
        this.lastResult = System.currentTimeMillis();
    }

    public void setCancelHook(Runnable cancelHook) {
        this.cancelHook = cancelHook;
    }
}
