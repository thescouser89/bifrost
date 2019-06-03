package org.jboss.pnc.bifrost.common.scheduler;

import io.quarkus.test.junit.QuarkusTest;
import org.jboss.pnc.bifrost.mock.BackOffRunnableConfigFactory;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author <a href="mailto:matejonnet@gmail.com">Matej Lazar</a>
 */
@QuarkusTest
public class BackOffRunnableTest {

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
}
