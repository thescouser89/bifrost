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
import io.quarkus.runtime.configuration.MemorySize;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.hibernate.engine.jdbc.BlobProxy;
import org.jboss.pnc.api.constants.MDCHeaderKeys;
import org.jboss.pnc.bifrost.common.ChecksumValidatingStream;
import org.jboss.pnc.bifrost.constants.UserRoles;
import org.jboss.pnc.bifrost.endpoint.dto.FinalLogUpload;
import org.jboss.pnc.bifrost.source.db.FinalLog;
import org.jboss.pnc.bifrost.source.db.LogEntry;
import org.jboss.pnc.bifrost.source.db.LogEntryRepository;
import org.jboss.pnc.bifrost.source.db.converter.ValueConverter;
import org.jboss.pnc.bifrost.source.db.converter.idConverter;
import org.jboss.resteasy.annotations.providers.multipart.MultipartForm;

import javax.annotation.security.RolesAllowed;
import javax.inject.Inject;
import javax.transaction.Transactional;
import javax.validation.Valid;
import javax.validation.ValidationException;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;

@Path("/upload")
@RolesAllowed({ UserRoles.USERS_ADMIN, UserRoles.USERS_BIFROST })
public class LogUpload {
    @ConfigProperty(name = "quarkus.http.limits.max-body-size")
    MemorySize maxPostValue;

    @Inject
    LogEntryRepository logEntryRepository;

    private final ValueConverter<Long> idConverter = new idConverter();

    @Path("/final-log")
    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Transactional
    public String uploadFinalLog(@MultipartForm @Valid FinalLogUpload logUpload, @Context HttpHeaders headers) {
        Log.info("Receiving logfile");

        FinalLog finalLog = new FinalLog();
        finalLog.logEntry = getLogEntry(headers);
        finalLog.eventTimestamp = logUpload.getEndTime();
        finalLog.loggerName = logUpload.getLoggerName();
        finalLog.md5sum = logUpload.getMd5sum();
        finalLog.sha512sum = logUpload.getSha512sum();

        ChecksumValidatingStream stream = ChecksumValidatingStream
                .validate(logUpload.getLogfile(), logUpload.getMd5sum(), logUpload.getSha512sum());

        // Configure the proxy to read up to the max body post size. The proxy behaves well if the input
        // stream size is less than that size
        finalLog.logContent = BlobProxy.generateProxy(stream, maxPostValue.asLongValue());
        finalLog.persistAndFlush();
        try {
            stream.validate();
        } catch (ValidationException ex) {
            throw new BadRequestException("The uploaded log has wrong checksums: " + ex.getMessage(), ex);
        }
        return "ok";
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
