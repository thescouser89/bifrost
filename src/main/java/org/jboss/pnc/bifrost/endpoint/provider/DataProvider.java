package org.jboss.pnc.bifrost.endpoint.provider;

import org.jboss.pnc.bifrost.common.Strings;
import org.jboss.pnc.bifrost.common.scheduler.BackOffRunnableConfig;
import org.jboss.pnc.bifrost.common.scheduler.Subscription;
import org.jboss.pnc.bifrost.common.scheduler.Subscriptions;
import org.jboss.pnc.bifrost.source.ElasticSearch;
import org.jboss.pnc.bifrost.source.ElasticSearchConfig;
import org.jboss.pnc.bifrost.source.ResultProcessor;
import org.jboss.pnc.bifrost.source.dto.Direction;
import org.jboss.pnc.bifrost.source.dto.Line;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.core.StreamingOutput;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

/**
 * @author <a href="mailto:matejonnet@gmail.com">Matej Lazar</a>
 */
@ApplicationScoped
public class DataProvider {

    //TODO configurable
    BackOffRunnableConfig backOffRunnableConfig = new BackOffRunnableConfig(1000L, 10, 5 * 60000, 1000);

    ElasticSearch elasticSearch;
    ResultProcessor resultProcessor;
    //TODO move out of endpoint
    Subscriptions subscriptions;

    @Inject
    public DataProvider(ElasticSearchConfig elasticSearchConfig) throws Exception {
        this.elasticSearch = new ElasticSearch(elasticSearchConfig);
        resultProcessor = new ResultProcessor(elasticSearch);
        this.subscriptions = new Subscriptions();
    }

    public void getAllLines(String matchFilters, String prefixFilters, Line afterLine, boolean follow) {
        //TODO unfollow on client connection close. do we end in onLine IOException ?
        Subscription subscription = new Subscription();
        Consumer<Subscriptions.TaskParameters<Line>> searchTask = (parameters) -> {
            Optional<Line> lastResult = Optional.ofNullable(parameters.getLastResult());

            try {
                resultProcessor.get(
                        matchFilters,
                        prefixFilters,
                        lastResult,
                        Direction.ASC,
                        1000 //TODO configurable
                );
            } catch (IOException e) {
                e.printStackTrace(); //TODO
            }
        };

        StreamingOutput stream = outputStream -> {
            Writer writer = new BufferedWriter(new OutputStreamWriter(outputStream));

            Consumer<Line> onLine = line -> {
                try {
                    writer.write(line.asString());
                    if (line.isLast()) {
                        writer.flush();
                        writer.close();
                    }
                } catch (IOException e) {
                    subscriptions.unsubscribe(subscription);
                }
            };
            if (follow) {
                subscriptions.subscribe(
                        subscription,
                        searchTask,
                        Optional.ofNullable(afterLine),
                        onLine,
                        backOffRunnableConfig
                );
            } else {
                subscriptions.run(
                        searchTask,
                        Optional.ofNullable(afterLine),
                        onLine
                );
            }
        };
    }

    public void unsubscibe(Subscription subscription) {
    }

    public void subscribe(String matchFilters,
            String prefixFilters,
            Optional<Line> afterLine,
            Consumer<Line> onLine,
            Subscription subscription) {

    }

    /**
     * Blocking call, <code>onLine<code/> is called in the calling thread.
     */
    public void get(String matchFilters, String prefixFilters, Optional<Line> afterLine, Direction direction, int fetchSize, Consumer<Line> onLine) throws IOException {
        final AtomicReference<Line> lastLine;
        if (afterLine.isPresent()) {
            // Make sure line is marked as last. Being non last will result in endless loop in case of no results.
            lastLine = new AtomicReference<>(afterLine.get().cloneBuilder().last(true).build());
        } else {
            lastLine = new AtomicReference<>();
        }

        do {
            Consumer<Line> onLineInternal = line -> {
                lastLine.set(line);
                onLine.accept(line);
            };
            elasticSearch.get(
                    Strings.toMap(matchFilters),
                    Strings.toMap(prefixFilters),
                    Optional.ofNullable(lastLine.get()),
                    Direction.ASC,
                    fetchSize,
                    onLineInternal);
        } while (lastLine.get() != null && !lastLine.get().isLast());
    }
}
