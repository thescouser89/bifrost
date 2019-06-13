package org.jboss.pnc.bifrost.common.scheduler;

import org.jboss.logging.Logger;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author <a href="mailto:matejonnet@gmail.com">Matej Lazar</a>
 */
class TimeoutExecutorTest {

    private Logger logger = Logger.getLogger(TimeoutExecutorTest.class);

    @Test
    void shouldRunTaskAfterTimeout() throws InterruptedException {
        TimeoutExecutor timeoutExecutor = new TimeoutExecutor(new ScheduledThreadPoolExecutor(2));

        AtomicInteger run = new AtomicInteger();
        Runnable runnable = () -> {
            int i = run.incrementAndGet();
        };
        TimeoutExecutor.Task task = timeoutExecutor.submit(runnable, 100, TimeUnit.MILLISECONDS);
        for (int i = 0; i < 10; i++) {
            TimeUnit.MILLISECONDS.sleep(50);
            task.update();
            Assertions.assertEquals(0, run.get());
        }
        TimeUnit.MILLISECONDS.sleep(110);
        Assertions.assertEquals(1, run.get());
        TimeUnit.MILLISECONDS.sleep(110);
        Assertions.assertEquals(2, run.get());
        task.cancel();
        TimeUnit.MILLISECONDS.sleep(110);
        Assertions.assertEquals(2, run.get());
    }
}