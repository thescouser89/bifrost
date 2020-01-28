package org.jboss.pnc.bifrost.common.scheduler;

import org.apache.lucene.util.NamedThreadFactory;
import org.jboss.logging.Logger;
import org.jboss.pnc.bifrost.Config;
import org.jboss.pnc.bifrost.common.Reference;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * @author <a href="mailto:matejonnet@gmail.com">Matej Lazar</a>
 */
@ApplicationScoped
public class Subscriptions {

    private Logger logger = Logger.getLogger(Subscriptions.class);

    private Map<Subscription, ScheduledFuture> subscriptions;

    private ScheduledExecutorService executor;

    /**
     * CDI workaround
     */
    @Deprecated
    public Subscriptions() {
    }

    @Inject
    public Subscriptions(Config config) {
        subscriptions = new ConcurrentHashMap<>();
        executor = Executors.newScheduledThreadPool(config.getSourcePollThreads(), new NamedThreadFactory("subscriptions"));
    }

    public void submit(Runnable task) {
        executor.submit(task);
    }

    public <T> void run(Consumer<TaskParameters<T>> task, Optional<T> initialLastResult, Consumer<T> onResult) {
        task.accept(new TaskParameters(initialLastResult.get(), onResult));
    }

    public <T> void submit(Consumer<TaskParameters<T>> task, Optional<T> initialLastResult, Consumer<T> onResult) {
        executor.submit(() -> task.accept(new TaskParameters(initialLastResult.get(), onResult)));
    }

    public <T> void subscribe(Subscription subscription,
                              Consumer<TaskParameters<T>> task,
                              Optional<T> initialLastResult,
                              Consumer<T> onResult,
                              BackOffRunnableConfig backOffRunnableConfig) {

        BackOffRunnable backOffRunnable = new BackOffRunnable(backOffRunnableConfig);

        Reference<T> lastResult = new Reference<>(initialLastResult.orElse(null));
        Runnable internalTask = () -> {
            Consumer<T> onResultInternal = result -> {
                if (result != null) { // null indicates end of message stream
                    lastResult.set(result);
                    backOffRunnable.receivedResult();
                }
                onResult.accept(result);
            };

            task.accept(new TaskParameters(lastResult.get(), onResultInternal));

        };

        backOffRunnable.setRunnable(internalTask);
        backOffRunnable.setCancelHook(() -> unsubscribe(subscription, UnsubscribeReason.NO_DATA_FROM_SOURCE));

        ScheduledFuture<?> scheduledFuture = executor.scheduleAtFixedRate(backOffRunnable,
                                                                          0,
                                                                          backOffRunnableConfig.getPollIntervalMillis(),
                                                                          TimeUnit.MILLISECONDS);
        subscriptions.put(subscription, scheduledFuture);
    }

    public void unsubscribe(Subscription subscription, UnsubscribeReason reason) {
        logger.debug("Unsubscribing: " + subscription + " Reason:" + reason);
        ScheduledFuture scheduledFuture = subscriptions.remove(subscription);
        if (UnsubscribeReason.NO_DATA_FROM_SOURCE.equals(reason)) {
            subscription.runOnUnsubscribe();
        }
        if (scheduledFuture != null) {
            scheduledFuture.cancel(true);
            logger.debug("Cancelled: " + subscription);
        }
    }

    public void unsubscribe(Subscription subscription) {
        unsubscribe(subscription, UnsubscribeReason.OTHER);
    }

    public void unsubscribeAll() {
        subscriptions.keySet().stream().forEach(s -> unsubscribe(s));
    }

    public Set<Subscription> getAll() {
        return Collections.unmodifiableSet(subscriptions.keySet());
    }

    public enum UnsubscribeReason {
        NO_DATA_FROM_SOURCE, OTHER;
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
