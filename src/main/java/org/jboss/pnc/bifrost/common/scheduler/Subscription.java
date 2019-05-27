package org.jboss.pnc.bifrost.common.scheduler;

import java.util.UUID;

/**
 * @author <a href="mailto:matejonnet@gmail.com">Matej Lazar</a>
 */
public class Subscription {

    private final String clientId;

    private final String topic;

    public Subscription() {
        clientId = UUID.randomUUID().toString();
        topic = "";
    }

    public Subscription(String clientId, String topic) {
        this.clientId = clientId;
        this.topic = topic;
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
}
