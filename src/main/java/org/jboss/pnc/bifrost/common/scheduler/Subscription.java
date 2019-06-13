package org.jboss.pnc.bifrost.common.scheduler;

import java.util.UUID;

/**
 * @author <a href="mailto:matejonnet@gmail.com">Matej Lazar</a>
 */
public class Subscription {

    private final String clientId;

    private final String topic;

    /**
     * Run when subscription is canceled/unsubscribed internally. Eg. on {@link BackOffRunnable} timeout.
     */
    private Runnable onUnsubscribe;

    public Subscription(Runnable onUnsubscribe) {
        clientId = UUID.randomUUID().toString();
        topic = "";
        this.onUnsubscribe = onUnsubscribe;
    }

    public Subscription(String clientId, String topic, Runnable onUnsubscribe) {
        this.clientId = clientId;
        this.topic = topic;
        this.onUnsubscribe = onUnsubscribe;
    }

    public String getClientId() {
        return clientId;
    }

    public String getTopic() {
        return topic;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof Subscription))
            return false;

        Subscription that = (Subscription) o;

        if (!clientId.equals(that.clientId))
            return false;
        return topic.equals(that.topic);
    }

    @Override
    public int hashCode() {
        int result = clientId.hashCode();
        result = 31 * result + topic.hashCode();
        return result;
    }

    public void runOnUnsubscribe() {
        onUnsubscribe.run();
    }
}
