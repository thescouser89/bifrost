/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2020-2022 Red Hat, Inc., and individual contributors
 * as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jboss.pnc.bifrost.endpoint;

import io.micrometer.core.annotation.Timed;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.opentelemetry.extension.annotations.WithSpan;

import io.quarkus.narayana.jta.QuarkusTransaction;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import org.jboss.pnc.api.bifrost.dto.Line;
import org.jboss.pnc.api.bifrost.dto.MetaData;
import org.jboss.pnc.api.bifrost.enums.Direction;
import org.jboss.pnc.api.bifrost.enums.Format;
import org.jboss.pnc.api.bifrost.rest.Bifrost;
import org.jboss.pnc.api.dto.ComponentVersion;
import org.jboss.pnc.bifrost.common.DateUtil;
import org.jboss.pnc.bifrost.common.Reference;
import org.jboss.pnc.bifrost.common.scheduler.Subscription;
import org.jboss.pnc.bifrost.common.scheduler.TimeoutExecutor;
import org.jboss.pnc.bifrost.constants.BuildInformationConstants;
import org.jboss.pnc.bifrost.endpoint.provider.DataProvider;
import org.jboss.pnc.bifrost.source.db.FinalLog;
import org.jboss.pnc.common.pnc.LongBase32IdConverter;
import org.jboss.pnc.common.security.Md5;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.ServerErrorException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.security.NoSuchAlgorithmException;
import java.sql.SQLException;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * @author <a href="mailto:matejonnet@gmail.com">Matej Lazar</a>
 */
@Path("/")
public class RestImpl implements Bifrost {
    @ConfigProperty(name = "quarkus.application.name")
    String name;

    private static final String className = RestImpl.class.getName();

    private static Logger logger = LoggerFactory.getLogger(RestImpl.class);

    @Inject
    DataProvider dataProvider;

    private Map<String, ScheduledThreadPoolExecutor> probeExecutor = new ConcurrentHashMap<>();

    @Inject
    MeterRegistry registry;

    private Counter errCounter;
    private Counter warnCounter;

    @PostConstruct
    void initMetrics() {
        errCounter = registry.counter(className + ".error.count");
        warnCounter = registry.counter(className + ".warning.count");
    }

    @Timed
    @Override
    public Response getAllLines(
            String matchFilters,
            String prefixFilters,
            Line afterLine,
            Direction direction,
            Format format,
            Integer maxLines,
            Integer batchSize,
            Integer batchDelay,
            boolean follow,
            String timeoutProbeString) {

        validateAndFixInputDate(afterLine);

        ArrayBlockingQueue<Optional<Line>> queue = new ArrayBlockingQueue(1024);

        Runnable addEndOfDataMarker = () -> {
            try {
                queue.offer(Optional.empty(), 5, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                errCounter.increment();
                logger.error("Cannot add end of data marker.", e);
            }
        };

        Subscription subscription = new Subscription(addEndOfDataMarker);

        StreamingOutput stream = outputStream -> {

            Reference<TimeoutExecutor.Task> timeoutProbeTask = new Reference<>();
            if (follow && timeoutProbeString != null && !timeoutProbeString.equals("")) {
                TimeoutExecutor timeoutExecutor = new TimeoutExecutor(getExecutorService());
                Runnable sendProbe = () -> {
                    Writer writer = new BufferedWriter(new OutputStreamWriter(outputStream));
                    try {
                        writer.write(timeoutProbeString);
                        writer.flush();
                    } catch (IOException e) {
                        timeoutProbeTask.get().cancel();
                        warnCounter.increment();
                        logger.warn("Cannot send connection probe, client might closed the connection.", e);
                        complete(subscription, outputStream);
                    }
                };
                timeoutProbeTask.set(timeoutExecutor.submit(sendProbe, 15000, TimeUnit.MILLISECONDS));
            }

            while (true) {
                try {
                    Optional<Line> maybeLine = queue.poll(30, TimeUnit.MINUTES);
                    if (maybeLine.isPresent()) {
                        Line line = maybeLine.get();
                        String message = line.asString(format);
                        logger.trace("Sending line: " + message);

                        Writer writer = new BufferedWriter(new OutputStreamWriter(outputStream));
                        writer.write(handleNewLine(message));
                        writer.flush();

                        if (line.isLast() && follow == false) { // when follow is true, the connection must be
                                                                // terminated from
                                                                // the client side
                            timeoutProbeTask.ifPresent(t -> t.cancel());
                            complete(subscription, outputStream);
                            break;
                        }
                        timeoutProbeTask.ifPresent(t -> t.update());
                    } else { // empty line indicating end of results
                        logger.info("Closing connection, no results.");
                        timeoutProbeTask.ifPresent(t -> t.cancel());
                        complete(subscription, outputStream);
                        break;
                    }
                } catch (IOException e) {
                    warnCounter.increment();
                    logger.warn(
                            "Cannot write output. Client might closed the connection. Unsubscribing ... "
                                    + e.getMessage());
                    timeoutProbeTask.ifPresent(t -> t.cancel());
                    complete(subscription, outputStream);
                    break;
                } catch (InterruptedException e) {
                    errCounter.increment();
                    logger.error("Cannot read from queue.", e);
                    timeoutProbeTask.ifPresent(t -> t.cancel());
                    complete(subscription, outputStream);
                    break;
                }
            }
        };

        fillQueueWithLines(
                matchFilters,
                prefixFilters,
                afterLine,
                maxLines,
                batchSize,
                batchDelay,
                follow,
                queue,
                addEndOfDataMarker,
                subscription);
        return Response.ok(stream).build();
    }

    private String handleNewLine(String message) {
        if (message == null || message.isEmpty()) {
            return "\n";
        }

        if (message.endsWith("\n")) {
            return message;
        }

        return message.concat(System.lineSeparator());
    }

    @Timed
    @WithSpan()
    protected void fillQueueWithLines(
            String matchFilters,
            String prefixFilters,
            Line afterLine,
            Integer maxLines,
            Integer batchSize,
            Integer batchDelay,
            boolean follow,
            ArrayBlockingQueue<Optional<Line>> queue,
            Runnable addEndOfDataMarker,
            Subscription subscription) {
        int[] receivedLines = { 0 };
        Consumer<Line> onLine = line -> {
            try {
                if (line != null) {
                    logger.trace("Adding line to output queue: " + line.asString());
                    queue.offer(Optional.of(line), 5, TimeUnit.SECONDS); // TODO
                    receivedLines[0]++;

                    if (maxLines != null && receivedLines[0] >= maxLines) {
                        logger.debug("Received max lines, unsubscribing ...");
                        addEndOfDataMarker.run();
                        dataProvider.unsubscribe(subscription);
                    }
                } else {
                    logger.debug("Received null line.");
                }

                if (follow == false && (line == null || line.isLast())) {
                    logger.debug("Received last line or no results, unsubscribing and closing ...");
                    // signal connection close
                    addEndOfDataMarker.run();
                    dataProvider.unsubscribe(subscription);
                }
            } catch (Exception e) {
                warnCounter.increment();
                logger.warn("Unsubscribing due to the exception.", e);
                addEndOfDataMarker.run();
                dataProvider.unsubscribe(subscription);
            }
        };
        dataProvider.subscribe(
                matchFilters,
                prefixFilters,
                Optional.ofNullable(afterLine),
                onLine,
                subscription,
                Optional.ofNullable(maxLines),
                Optional.ofNullable(batchSize),
                Optional.ofNullable(batchDelay));
    }

    private ScheduledThreadPoolExecutor getExecutorService() {
        return probeExecutor.computeIfAbsent("INSTANCE", k -> new ScheduledThreadPoolExecutor(1));
    }

    private void complete(Subscription subscription, OutputStream outputStream) {
        dataProvider.unsubscribe(subscription);
        try {
            outputStream.close();
        } catch (IOException e) {
            warnCounter.increment();
            logger.warn("Cannot close output stream.", e);
        }
    }

    @Override
    public List<Line> getLines(
            String matchFilters,
            String prefixFilters,
            Line afterLine,
            Direction direction,
            Integer maxLines,
            Integer batchSize) throws IOException {
        validateAndFixInputDate(afterLine);

        List<Line> lines = new ArrayList<>();
        Consumer<Line> onLine = lines::add;
        dataProvider.get(
                matchFilters,
                prefixFilters,
                Optional.ofNullable(afterLine),
                direction,
                Optional.ofNullable(maxLines),
                Optional.ofNullable(batchSize),
                onLine);
        return lines;
    }

    @Override
    public MetaData getMetaData(
            String matchFilters,
            String prefixFilters,
            Line afterLine,
            Direction direction,
            Integer maxLines,
            Integer batchSize) throws IOException {

        validateAndFixInputDate(afterLine);

        Md5 md5;
        try {
            md5 = new Md5();
        } catch (NoSuchAlgorithmException e) {
            errCounter.increment();
            throw new ServerErrorException(e.getMessage(), Response.Status.INTERNAL_SERVER_ERROR);
        }

        Consumer<Line> onLine = line -> {
            try {
                md5.add(line.getMessage());
            } catch (UnsupportedEncodingException e) {
                errCounter.increment();
                throw new ServerErrorException(e.getMessage(), Response.Status.INTERNAL_SERVER_ERROR);
            }
        };
        dataProvider.get(
                matchFilters,
                prefixFilters,
                Optional.ofNullable(afterLine),
                direction,
                Optional.ofNullable(maxLines),
                Optional.ofNullable(batchSize),
                onLine);

        return new MetaData(md5.digest());
    }

    @Override
    @Consumes
    @Produces(MediaType.TEXT_PLAIN)
    @GET
    @Path("/final-log/{buildId}/{tag}")
    /**
     * Get the final log of the build + tag
     */
    public Response getFinalLog(@PathParam("buildId") String buildId, @PathParam("tag") String tag) {

        return Response.ok().entity((StreamingOutput) output -> {
            try {
                QuarkusTransaction.begin();
                // build id and process context should be the same
                FinalLog.copyFinalLogsToOutputStream(LongBase32IdConverter.toLong(buildId), tag, output);
                QuarkusTransaction.commit();
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        }).build();
    }

    @Override
    public ComponentVersion getVersion() {
        return ComponentVersion.builder()
                .name(name)
                .version(BuildInformationConstants.VERSION)
                .commit(BuildInformationConstants.COMMIT_HASH)
                .builtOn(ZonedDateTime.parse(BuildInformationConstants.BUILD_TIME))
                .build();
    }

    public static void validateAndFixInputDate(Line afterLine) {
        if (afterLine != null) {
            afterLine.setTimestamp(DateUtil.validateAndFixInputDate(afterLine.getTimestamp()));
        }
    }

}
