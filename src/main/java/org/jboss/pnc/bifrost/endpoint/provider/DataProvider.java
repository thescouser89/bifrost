package org.jboss.pnc.bifrost.endpoint.provider;

import org.jboss.logging.Logger;
import org.jboss.pnc.bifrost.Config;
import org.jboss.pnc.bifrost.common.ObjectReference;
import org.jboss.pnc.bifrost.common.Strings;
import org.jboss.pnc.bifrost.common.scheduler.BackOffRunnableConfig;
import org.jboss.pnc.bifrost.common.scheduler.Subscription;
import org.jboss.pnc.bifrost.common.scheduler.Subscriptions;
import org.jboss.pnc.bifrost.source.ElasticSearch;
import org.jboss.pnc.bifrost.source.ResultDecorator;
import org.jboss.pnc.bifrost.source.dto.Direction;
import org.jboss.pnc.bifrost.source.dto.Line;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.io.IOException;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * @author <a href="mailto:matejonnet@gmail.com">Matej Lazar</a>
 */
@ApplicationScoped
public class DataProvider {

    private Logger logger = Logger.getLogger(DataProvider.class);

    @Inject
    BackOffRunnableConfig backOffRunnableConfig;

    @Inject
    Config config;

    @Inject
    ElasticSearch elasticSearch;

    @Inject
    Subscriptions subscriptions;

    public void unsubscribe(Subscription subscription) {
        subscriptions.unsubscribe(subscription);
    }

    public void subscribe(String matchFilters,
            String prefixFilters,
            Optional<Line> afterLine,
            Consumer<Line> onLine,
            Subscription subscription) {

        ResultDecorator resultProcessor = new ResultDecorator(elasticSearch);

        Consumer<Subscriptions.TaskParameters<Line>> searchTask = (parameters) -> {
            Optional<Line> lastResult = Optional.ofNullable(parameters.getLastResult());
            Consumer<Line> onLineInternal = line ->  parameters.getResultConsumer().accept(line);
            try {
                resultProcessor.get(
                        onLineInternal,
                        matchFilters,
                        prefixFilters,
                        lastResult,
                        Direction.ASC,
                        config.maxSourceFetchSize
                );
            } catch (IOException e) {
                //TODO unsubscribe ?
                logger.error("Error getting data from Elasticsearch.", e);
            }
        };

        subscriptions.subscribe(
                subscription,
                searchTask,
                afterLine,
                onLine,
                backOffRunnableConfig
        );
    }

    /**
     * Blocking call, <code>onLine<code/> is called in the calling thread.
     */
    public void get(
            String matchFilters,
            String prefixFilters,
            Optional<Line> afterLine,
            Direction direction,
            Optional<Integer> maxLines,
            Consumer<Line> onLine) throws IOException {

        final ObjectReference<Line> lastLine;
        if (afterLine.isPresent()) {
            // Make sure line is marked as last. Being non last will result in endless loop in case of no results.
            lastLine = new ObjectReference<>(afterLine.get().cloneBuilder().last(true).build());
        } else {
            lastLine = new ObjectReference<>();
        }

        final int[] fetchedLines = {0};
        Consumer<Line> onLineInternal = line -> {
            fetchedLines[0]++;
            lastLine.set(line);
            onLine.accept(line);
        };
        do {
            int fetchSize = getFetchSize(fetchedLines[0], maxLines);
            if (fetchSize < 1) {
                break;
            }
            elasticSearch.get(
                    Strings.toMap(matchFilters),
                    Strings.toMap(prefixFilters),
                    Optional.ofNullable(lastLine.get()),
                    Direction.ASC,
                    fetchSize,
                    onLineInternal);
        } while (lastLine.get() != null && !lastLine.get().isLast());
    }

    private int getFetchSize(int fetchedLines, Optional<Integer> maxLines) {
        int defaultFetchSize = config.getDefaultSourceFetchSize();
        if (maxLines.isPresent()) {
            int max = maxLines.get();
            if (fetchedLines + defaultFetchSize > max) {
                return max - fetchedLines;
            }
        }
        return defaultFetchSize;
    }
}
