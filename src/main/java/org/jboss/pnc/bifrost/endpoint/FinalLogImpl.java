/**
 * JBoss, Home of Professional Open Source.
 * Copyright 2021 Red Hat, Inc., and individual contributors
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

import io.quarkus.logging.Log;
import io.quarkus.narayana.jta.QuarkusTransaction;
import io.quarkus.runtime.configuration.MemorySize;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.hibernate.engine.jdbc.BlobProxy;
import org.jboss.pnc.api.bifrost.rest.FinalLogRest;
import org.jboss.pnc.api.constants.MDCHeaderKeys;
import org.jboss.pnc.bifrost.common.ChecksumValidatingStream;
import org.jboss.pnc.bifrost.endpoint.dto.FinalLogUpload;
import org.jboss.pnc.bifrost.source.db.FinalLog;
import org.jboss.pnc.bifrost.source.db.LogEntry;
import org.jboss.pnc.bifrost.source.db.LogEntryRepository;
import org.jboss.pnc.bifrost.source.db.converter.ValueConverter;
import org.jboss.pnc.bifrost.source.db.converter.IdConverter;
import org.jboss.pnc.common.concurrent.Sequence;
import org.jboss.pnc.common.pnc.LongBase32IdConverter;

import javax.annotation.security.PermitAll;
import javax.annotation.security.RolesAllowed;
import javax.inject.Inject;
import javax.transaction.Transactional;
import javax.validation.Valid;
import javax.validation.ValidationException;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;
import java.sql.SQLException;
import java.util.List;
import java.util.Set;

@PermitAll
@Slf4j
public class FinalLogImpl implements FinalLogRest {
    @ConfigProperty(name = "quarkus.http.limits.max-body-size")
    MemorySize maxPostValue;

    @Inject
    LogEntryRepository logEntryRepository;

    private final ValueConverter<Long> idConverter = new IdConverter();

    @Path("/upload")
    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Transactional
    public String uploadFinalLog(@Valid FinalLogUpload logUpload, @Context HttpHeaders headers) {
        Log.info("Receiving logfile");

        FinalLog finalLog = new FinalLog();
        finalLog.id = Sequence.nextId();
        finalLog.logEntry = getLogEntry(headers);
        finalLog.eventTimestamp = logUpload.getEndTime();
        finalLog.loggerName = logUpload.getLoggerName();
        finalLog.md5sum = logUpload.getMd5sum();

        if (logUpload.getTag() != null) {
            finalLog.tags = Set.of(logUpload.getTag().split(","));
        }

        ChecksumValidatingStream stream = ChecksumValidatingStream
                .validate(logUpload.getLogfile(), logUpload.getMd5sum());

        // Configure the proxy to read up to the max body post size. The proxy behaves well if the input
        // stream size is less than that size
        finalLog.logContent = BlobProxy.generateProxy(stream, maxPostValue.asLongValue());
        finalLog.persistAndFlush();
        try {
            stream.validate();
        } catch (ValidationException ex) {
            throw new BadRequestException("The uploaded log has wrong checksums: " + ex.getMessage(), ex);
        }

        FinalLog.getEntityManager().refresh(finalLog);
        finalLog.size = stream.readSize();

        return "ok";
    }

    @Path("/{processContext}/delete")
    @DELETE
    @RolesAllowed("**")
    @Transactional // FIXME change to specific allowed roles
    public Response deleteFinalLog(@PathParam("processContext") String processContext) {
        // parse process context
        Long processContextLong = idConverter.convert(processContext);
        if (processContextLong == null) {
            throw new BadRequestException("Process context " + processContext + "is not a number nor a Build process.");
        }

        List<LogEntry> logEntries = LogEntry.list("processContext", processContextLong);
        if (logEntries.isEmpty()) {
            return Response.noContent().build();
        }
        if (logEntries.stream().noneMatch(LogEntry::getTemporary)) {
            throw new BadRequestException("Can't delete logs of persistent entries.");
        }

        log.debug("Deleting final logs with processContext: {} (long -> {})", processContext, processContextLong);
        long deleted = FinalLog.deleteByProcessContext(processContextLong, null, true);
        log.debug("Deleted {} rows with processContext {} (long -> {}).", deleted, processContext, processContextLong);

        return Response.noContent().build();
    }

    @Override
    public Response getFinalLog(@PathParam("buildId") String buildId, @PathParam("tag") String tag) {

        // if logs not present, return status 404
        if (FinalLog.getFinalLogsWithoutPreviousRetries(LongBase32IdConverter.toLong(buildId), tag).isEmpty()) {
            return Response.status(404).build();
        }

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
    public long getFinalLogSize(@PathParam("buildId") String buildId, @PathParam("tag") String tag) {
        return FinalLog.getFinalLogsWithoutPreviousRetries(LongBase32IdConverter.toLong(buildId), tag)
                .stream()
                .mapToLong(m -> m.size)
                .sum();
    }

    private LogEntry getLogEntry(HttpHeaders headers) {
        LogEntry logEntry = new LogEntry();
        String processContext = headers.getHeaderString(MDCHeaderKeys.PROCESS_CONTEXT.getHeaderName());
        if (processContext == null) {
            throw new BadRequestException("Header " + MDCHeaderKeys.PROCESS_CONTEXT.getHeaderName() + " required.");
        }
        logEntry.setProcessContext(idConverter.convert(processContext));
        logEntry.setProcessContextVariant(
                headers.getHeaderString(MDCHeaderKeys.PROCESS_CONTEXT_VARIANT.getHeaderName()));
        logEntry.setTemporary(Boolean.valueOf(headers.getHeaderString(MDCHeaderKeys.TMP.getHeaderName())));
        logEntry.setRequestContext(headers.getHeaderString(MDCHeaderKeys.REQUEST_CONTEXT.getHeaderName()));
        return logEntryRepository.get(logEntry);
    }
}
