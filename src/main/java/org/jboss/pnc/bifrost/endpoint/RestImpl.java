package org.jboss.pnc.bifrost.endpoint;

import org.jboss.logging.Logger;
import org.jboss.pnc.bifrost.common.Reference;
import org.jboss.pnc.bifrost.common.scheduler.Subscription;
import org.jboss.pnc.bifrost.endpoint.provider.DataProvider;
import org.jboss.pnc.bifrost.source.dto.Direction;
import org.jboss.pnc.bifrost.source.dto.Line;

import javax.inject.Inject;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * @author <a href="mailto:matejonnet@gmail.com">Matej Lazar</a>
 */
@Path("/")
public class RestImpl implements Rest {

    private static Logger logger = Logger.getLogger(RestImpl.class);

    @Inject
    DataProvider dataProvider;

    @Override
    public Response getAllLines(
            String matchFilters,
            String prefixFilters,
            Line afterLine,
            Direction direction,
            Integer maxLines,
            boolean follow) {

        ArrayBlockingQueue<Line> queue = new ArrayBlockingQueue(1024); //TODO

        Reference<Boolean> complete = new Reference<>(false);

        Subscription subscription = new Subscription();

        StreamingOutput stream = outputStream -> {
            while (true) {
                try {
                    Writer writer = new BufferedWriter(new OutputStreamWriter(outputStream));
                    Line line = queue.take();
                    logger.trace("Sending line ..." + line.asString());
                    writer.write(line.asString() + "\n");
                    writer.flush();
                    if (line.isLast() && follow == false) { //when follow is true, the connection must be terminated from the client side
                        complete(subscription, outputStream);
                        break;
                    }
                } catch (IOException e) {
                    logger.warn("Cannot write output. Client might closed the connection. Unsubscribing ... " + e.getMessage());
                    complete(subscription, outputStream);
                    break;
                } catch (InterruptedException e) {
                    logger.error("Cannot read from queue.", e);
                    complete(subscription, outputStream);
                    break;
                }
                if (complete.get()) {
                    complete(subscription, outputStream);
                }
            }
        };

        int[] receivedLines = {0};
        Consumer<Line> onLine = line -> {
            try {
                logger.trace("Adding line to output queue: " + line.asString());
                receivedLines[0]++;
                queue.offer(line, 5, TimeUnit.SECONDS); //TODO

                if (maxLines != null && receivedLines[0] >= maxLines) {
                    logger.debug("Received max lines, unsubscribing ...");
                    dataProvider.unsubscribe(subscription);
                    complete.set(true);
                }
            } catch (Exception e) {
                dataProvider.unsubscribe(subscription);
                complete.set(true);
            }
        };
        if (follow) {
            dataProvider.subscribe(
                    matchFilters,
                    prefixFilters,
                    Optional.ofNullable(afterLine),
                    onLine,
                    subscription
            );
        } else {
            Consumer<Line> onLineInternal = line -> {
                onLine.accept(line);
                if (line.isLast()) {
                    logger.debug("Received last line, unsubscribing ...");
                    dataProvider.unsubscribe(subscription);
                    complete.set(true);
                }
            };
            dataProvider.subscribe(
                    matchFilters,
                    prefixFilters,
                    Optional.ofNullable(afterLine),
                    onLineInternal,
                    subscription
            );
        }

        return Response.ok(stream).build();
    }

    private void complete(Subscription subscription, OutputStream outputStream) {
        dataProvider.unsubscribe(subscription);
        try {
            outputStream.close();
        } catch (IOException e) {
            logger.warn("Cannot close output stream.", e);
        }
    }

    @Override
    public List<Line> getLines(String matchFilters, String prefixFilters, Line afterLine, Direction direction, Integer maxLines)
            throws IOException {
        List<Line> lines = new ArrayList<>();
        Consumer<Line> onLine = line -> lines.add(line);
        dataProvider.get(
                matchFilters,
                prefixFilters,
                Optional.ofNullable(afterLine),
                direction,
                Optional.ofNullable(maxLines),
                onLine);
        return lines;
    }

    @Override
    public Response readinessProbe() {
        return Response.ok().build();
    }

    @Override
    public Response livenessProbe() {
        return Response.ok().build(); //TODO test ES connection
    }

}
