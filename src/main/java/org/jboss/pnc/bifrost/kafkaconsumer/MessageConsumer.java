/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2022 Red Hat, Inc., and individual contributors
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
package org.jboss.pnc.bifrost.kafkaconsumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.annotation.Timed;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.smallrye.common.annotation.Blocking;
import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.jboss.pnc.bifrost.source.db.LogEntryRepository;
import org.jboss.pnc.bifrost.source.db.LogLine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.transaction.Transactional;
import javax.validation.ConstraintViolationException;

/**
 * Consume from a Kafka topic, parse the data and store it in the database
 */
@ApplicationScoped
public class MessageConsumer {

    private static final String className = MessageConsumer.class.getName();

    private final Logger logger = LoggerFactory.getLogger(MessageConsumer.class);

    @Inject
    ObjectMapper mapper;

    @Inject
    MeterRegistry registry;

    @Inject
    Configuration configuration;

    @Inject
    StoredCounter storedCounter;

    @Inject
    private LogEntryRepository logEntryRepository;

    Filter acceptFilter;

    private Counter errCounter;

    @PostConstruct
    void initMetrics() {
        errCounter = registry.counter(className + ".error.count");
        acceptFilter = new Filter(configuration.acceptFilters());
    }

    /**
     * Consume messages from a Kafka topic and store them to the database.
     */
    @Timed
    @Blocking
    @Incoming("logs")
    @Transactional
    public void consume(String json) {
        logger.trace("Received json line: " + json);
        try {
            LogLine logLine = mapper.readValue(json, LogLine.class);
            logger.trace("Received line: " + logLine.toString());

            if (logLine.getLogEntry() != null && logLine.getLogEntry().getProcessContext() == null) {
                logger.warn("Skipping log line due to null processContext. Line: " + logLine);
                return;
            }
            if (acceptFilter.match(logLine)) {
                try {
                    logLine.setLogEntry(logEntryRepository.get(logLine.getLogEntry()));
                    logLine.persistAndFlush();
                    storedCounter.increment();
                } catch (ConstraintViolationException e) {
                    logger.warn("Skipping log line due to: " + e.getMessage() + ". Line: " + logLine);
                }
            }
        } catch (Exception e) {
            errCounter.increment();
            logger.error("Error while reading and saving the data", e);
            throw new RuntimeException(e);
        }
    }

}
