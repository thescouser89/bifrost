package org.jboss.pnc.bifrost.common.scheduler;

import org.jboss.logging.Logger;
import org.jboss.pnc.bifrost.mock.BackOffRunnableConfigFactory;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author <a href="mailto:matejonnet@gmail.com">Matej Lazar</a>
 */
public class BackOffRunnableTest {

    private Logger logger = Logger.getLogger(BackOffRunnableTest.class);

    BackOffRunnableConfig config = BackOffRunnableConfigFactory
            .get(100L, 5, 60000, 100L);

    @Test
    public void shouldRun() throws InterruptedException {
        AtomicInteger run = new AtomicInteger();

        BackOffRunnable backOffRunnable = new BackOffRunnable(config);
        Runnable task = () -> {
            run.incrementAndGet();
            backOffRunnable.receivedResult();
        };

        backOffRunnable.setRunnable(task);

        backOffRunnable.run();
        Thread.sleep(100);
        backOffRunnable.run();
        Thread.sleep(100);
        backOffRunnable.run();

        Assertions.assertEquals(3, run.get());
    }

    @Test
    public void shouldSkipCycle() throws InterruptedException {
        AtomicInteger run = new AtomicInteger();

        BackOffRunnable backOffRunnable = new BackOffRunnable(config);
        Runnable task = () -> {
            run.incrementAndGet();
            //no result
        };

        backOffRunnable.setRunnable(task);

        System.out.println("About to run ...");
        backOffRunnable.run();
        Thread.sleep(100);
        System.out.println("About to run ...");
        backOffRunnable.run();
        Thread.sleep(100);
        System.out.println("About to run ...");
        backOffRunnable.run();

        Assertions.assertEquals(2, run.get());
    }

    @Test
    public void shouldSkipOneCycle() throws InterruptedException {
        AtomicInteger run = new AtomicInteger();
        AtomicInteger skip = new AtomicInteger();

        BackOffRunnable backOffRunnable = new BackOffRunnable(config);
        Runnable task = () -> {
            run.incrementAndGet();
            if (skip.get() > 1) {
                backOffRunnable.receivedResult();
            }
        };

        backOffRunnable.setRunnable(task);

        System.out.println("About to run ...");
        backOffRunnable.run();
        skip.getAndIncrement();
        Thread.sleep(100);
        System.out.println("About to run ...");
        backOffRunnable.run();
        skip.getAndIncrement();
        Thread.sleep(100);
        System.out.println("About to run ...");
        backOffRunnable.run();
        skip.getAndIncrement();
        Thread.sleep(100);
        System.out.println("About to run ...");
        backOffRunnable.run();
        skip.getAndIncrement();

        Assertions.assertEquals(3, run.get());
    }

    @Test
    public void shouldCancelTheJobAfterTimeoutWhenNoresults() throws InterruptedException {
        BackOffRunnableConfig config = BackOffRunnableConfigFactory
                .get(100L, 0, 500, 100L);

        AtomicInteger run = new AtomicInteger(0);
        AtomicInteger cancel = new AtomicInteger(0);

        BackOffRunnable backOffRunnable = new BackOffRunnable(config);
        Runnable task = () -> {
            if (run.get() == 0) {
                backOffRunnable.receivedResult();
            }
            int loop = run.incrementAndGet();
            logger.info("Running: " + loop);
        };

        backOffRunnable.setRunnable(task);
        backOffRunnable.setCancelHook(() -> cancel.incrementAndGet());
        backOffRunnable.run();
        backOffRunnable.run();
        TimeUnit.MILLISECONDS.sleep(700);
        backOffRunnable.run();
        Assertions.assertEquals(3, run.get());
        Assertions.assertEquals(1, cancel.get());


    }
}
