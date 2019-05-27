package org.jboss.pnc.bifrost.common.scheduler;

import javax.enterprise.context.ApplicationScoped;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

/**
 * @author <a href="mailto:matejonnet@gmail.com">Matej Lazar</a>
 */
@ApplicationScoped
public class Subscriptions {

    private final Map<Subscription, ScheduledFuture> subscriptions = new ConcurrentHashMap<>();

    private final ScheduledExecutorService executor = Executors.newScheduledThreadPool(4); //TODO configurable

    public void submit(Runnable task) {
        executor.submit(task);
    }

    public <T> void run(
            Consumer<TaskParameters<T>> task,
            Optional<T> initialLastResult,
            Consumer<T> onResult) {
        task.accept(new TaskParameters(initialLastResult.get(), onResult));
    }

    public <T> void submit(
            Consumer<TaskParameters<T>> task,
            Optional<T> initialLastResult,
            Consumer<T> onResult) {
        executor.submit(() -> task.accept(new TaskParameters(initialLastResult.get(), onResult)));
    }

    public <T> void subscribe(
            Subscription subscription,
            Consumer<TaskParameters<T>> task,
            Optional<T> initialLastResult,
            Consumer<T> onResult,
            BackOffRunnableConfig backOffRunnableConfig
            ) {

        AtomicReference<T> lastResult = new AtomicReference<>(initialLastResult.orElse(null));
        BackOffRunnable backOffRunnable = new BackOffRunnable(backOffRunnableConfig);

        Runnable internalTask = () -> {
            Consumer<T> onResultInternal = result -> {
                lastResult.set(result);
                backOffRunnable.receivedResult();
                onResult.accept(result);
            };

            task.accept(new TaskParameters(lastResult.get(), onResultInternal));

        };

        backOffRunnable.setRunnable(internalTask);
        backOffRunnable.setCancelHook(() -> unsubscribe(subscription));

        ScheduledFuture<?> scheduledFuture = executor.scheduleAtFixedRate(backOffRunnable, 0, backOffRunnableConfig.getPoolIntervalMillis(), TimeUnit.MILLISECONDS);
        subscriptions.put(subscription, scheduledFuture);
    }

    public void unsubscribe(Subscription subscription) {
        ScheduledFuture scheduledFuture = subscriptions.remove(subscription);
        if (scheduledFuture != null) {
            scheduledFuture.cancel(true);
        }
    }

    public void unsubscribeAll() {
        subscriptions.keySet().stream()
                .forEach(s -> unsubscribe(s));
    }

    public Set<Subscription> getAll() {
        return Collections.unmodifiableSet(subscriptions.keySet());
    }

    public static class TaskParameters<T> {

        private T lastResult;

        private Consumer<T> resultConsumer;

        public TaskParameters(T lastResult, Consumer<T> resultConsumer) {
            this.lastResult = lastResult;
            this.resultConsumer = resultConsumer;
        }

        public T getLastResult() {
            return lastResult;
        }

        public Consumer<T> getResultConsumer() {
            return resultConsumer;
        }
    }
}
