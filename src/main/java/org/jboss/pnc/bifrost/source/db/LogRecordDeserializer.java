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
package org.jboss.pnc.bifrost.source.db;

import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import org.jboss.logging.Logger;
import org.jboss.pnc.bifrost.kafkaconsumer.MissingValueException;
import org.jboss.pnc.common.Json;
import org.jboss.pnc.common.Strings;
import org.jboss.pnc.common.concurrent.Sequence;
import org.jboss.pnc.common.pnc.LongBase32IdConverter;

import java.io.IOException;
import java.time.Instant;
import java.util.Optional;

/**
 * Use to deserialize json messages from a Kafka stream.
 *
 * @author <a href="mailto:matejonnet@gmail.com">Matej Lazar</a>
 */
public class LogRecordDeserializer extends StdDeserializer<LogRecord> {

    private final Logger logger = Logger.getLogger(LogRecordDeserializer.class);

    public LogRecordDeserializer() {
        super(LogRecord.class);
    }

    protected LogRecordDeserializer(Class<?> vc) {
        super(vc);
    }

    @Override
    public LogRecord deserialize(JsonParser jsonParser, DeserializationContext deserializationContext)
            throws IOException, JacksonException {

        JsonNode node = jsonParser.getCodec().readTree(jsonParser);
        int sequence = Json.<Integer> getNumber(node, "/sequence").orElse(0);

        String time = Json.getText(node, "/@timestamp").or(() -> Json.getText(node, "/timestamp")).orElseGet(() -> {
            logger.warn("Missing timestamp in input message, setting it now.");
            return Instant.now().toString();
        });

        Instant timestamp = Instant.parse(time);
        String level = Json.getText(node, "/level").orElseThrow(() -> new MissingValueException("Missing level."));
        String loggerName = Json.getText(node, "/loggerName")
                .orElseThrow(() -> new MissingValueException("Missing loggerName."));
        String processContext = Json.getText(node, "/mdc/processContext").orElse(null);
        String processContextVariant = Json.getText(node, "/mdc/processContextVariant").orElse(null);
        String requestContext = Json.getText(node, "/mdc/requestContext").orElse(null);
        Boolean temp = Boolean.parseBoolean(Json.getText(node, "/mdc/tmp").orElse("false"));
        Long buildId = Json.<Long> getNumber(node, "/mdc/buildId").orElse(null);

        String message = Json.getText(node, "/message").orElse(null);
        String logLine;
        if (!Strings.isEmpty(message)) {
            logLine = message;
        } else {
            Optional<String> stackTrace = Json.getText(node, "/stackTrace");
            if (stackTrace.isPresent()) {
                logLine = stackTrace.get().lines().findFirst().orElse("");
            } else {
                logLine = "";
            }
        }
        return new LogRecord(
                Sequence.nextId(),
                timestamp,
                sequence,
                LogLevel.valueOf(level.toUpperCase()),
                loggerName,
                logLine,
                LongBase32IdConverter.toLong(processContext),
                LongBase32IdConverter.toLong(processContextVariant),
                requestContext,
                temp,
                buildId);
    }

}