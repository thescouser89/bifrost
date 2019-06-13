package org.jboss.pnc.bifrost.common.scheduler;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * @author <a href="mailto:matejonnet@gmail.com">Matej Lazar</a>
 */
public class TimeoutExecutor {

    private ScheduledExecutorService executorService;

    public TimeoutExecutor(ScheduledExecutorService executorService) {
        this.executorService = executorService;
    }

    public Task submit(Runnable task, long runTimeout, TimeUnit timeUnit) {
        ScheduledFuture<?> future = schedule(task, runTimeout, timeUnit);
        return new Task(future, task, runTimeout, timeUnit);
    }

    private ScheduledFuture<?> schedule(Runnable task, long runTimeout, TimeUnit timeUnit) {
        return executorService.scheduleAtFixedRate(task, runTimeout, runTimeout, timeUnit);
    }

    public class Task {
        private final Runnable task;
        private final long runTimeout;
        private final TimeUnit timeUnit;
        private ScheduledFuture<?> future;

        public Task(ScheduledFuture<?> future, Runnable task, long runTimeout, TimeUnit timeUnit) {
            this.future = future;
            this.task = task;
            this.runTimeout = runTimeout;
            this.timeUnit = timeUnit;
        }

        public void update() {
            future.cancel(false);
            future = schedule(task, runTimeout, timeUnit);
        }

        public void cancel() {
            future.cancel(false);
        }
    }

}
