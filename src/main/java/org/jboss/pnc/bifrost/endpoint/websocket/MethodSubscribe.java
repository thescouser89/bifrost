package org.jboss.pnc.bifrost.endpoint.websocket;

import lombok.Getter;
import lombok.Setter;
import org.jboss.pnc.bifrost.common.scheduler.Subscription;
import org.jboss.pnc.bifrost.endpoint.provider.DataProvider;
import org.jboss.pnc.bifrost.source.dto.Line;

import javax.enterprise.context.Dependent;
import javax.inject.Inject;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * @author <a href="mailto:matejonnet@gmail.com">Matej Lazar</a>
 */
@Dependent
public class MethodSubscribe extends MethodBase implements Method<SubscribeDto> {

    @Inject
    DataProvider dataProvider;

    @Override
    public String getName() {
        return "SUBSCRIBE";
    }

    @Override
    public Class<SubscribeDto> getParameterType() {
        return SubscribeDto.class;
    }

    @Override
    public Result apply(SubscribeDto methodSubscribeIn, Consumer<Line> responseConsumer) {
        String matchFilters = methodSubscribeIn.getMatchFilters();
        String prefixFilters = methodSubscribeIn.getPrefixFilters();

        Subscription subscription = new Subscription(getSession().getId(), methodSubscribeIn.getMatchFilters() + methodSubscribeIn.getPrefixFilters());

        Consumer<Line> onLine = line -> {
            line.setSubscriptionTopic(subscription.getTopic());
            responseConsumer.accept(line);
        };

        dataProvider.subscribe(matchFilters, prefixFilters, Optional.empty(), onLine, subscription);

        return new SubscribeResult(Result.Status.OK, subscription.getTopic());
    }

    @Getter
    @Setter
    public static class SubscribeResult extends Result {
        String subscriptionTopic;

        public SubscribeResult(Status status, String subscriptionTopic) {
            super(status);
            this.subscriptionTopic = subscriptionTopic;
        }


    }
}
