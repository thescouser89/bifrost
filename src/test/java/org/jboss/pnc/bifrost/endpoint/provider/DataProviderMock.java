package org.jboss.pnc.bifrost.endpoint.provider;

import org.jboss.pnc.bifrost.common.scheduler.Subscription;
import org.jboss.pnc.bifrost.common.scheduler.Subscriptions;
import org.jboss.pnc.bifrost.mock.LineProducer;
import org.jboss.pnc.bifrost.source.dto.Direction;
import org.jboss.pnc.bifrost.source.dto.Line;

import javax.annotation.Priority;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Alternative;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * @author <a href="mailto:matejonnet@gmail.com">Matej Lazar</a>
 */
@Alternative()
@Priority(1)
@ApplicationScoped
public class DataProviderMock extends DataProvider {

    Deque<Line> lines = new LinkedList<>();

//    @Inject
//    Subscriptions subscriptions;

    public DataProviderMock() {
        super();
    }

    public void get(
            String matchFilters,
            String prefixFilters,
            Optional<Line> afterLine,
            Direction direction,
            Optional<Integer> maxLines,
            Consumer<Line> onLine) {
        LineProducer.getLines(5, "abc123").forEach(
                line -> onLine.accept(line)
        );
    }

    public void subscribe(String matchFilters,
            String prefixFilters,
            Optional<Line> afterLine,
            Consumer<Line> onLine,
            Subscription subscription) {

        Consumer<Subscriptions.TaskParameters<Line>> searchTask =  (parameters) -> {
                Optional<Line> lastResult = Optional.ofNullable(parameters.getLastResult());
            for (int i = 0; i < 3; i++) {
                Line line = lines.pop();
                parameters.getResultConsumer().accept(line);
            }
//            try {
//                TimeUnit.MILLISECONDS.sleep(200);
//            } catch (InterruptedException e) {
//                e.printStackTrace();
//            }
        };

        subscriptions.subscribe(
                subscription,
                searchTask,
                afterLine,
                onLine,
                backOffRunnableConfig
        );
    }

    public void addLine(Line line) {
        lines.add(line);
    }

    public void addAllLines(List<Line> lines) {
        for (Line line : lines) {
            addLine(line);
        }
    }
}