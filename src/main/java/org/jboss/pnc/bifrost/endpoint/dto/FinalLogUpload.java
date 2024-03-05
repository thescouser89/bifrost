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
package org.jboss.pnc.bifrost.endpoint.dto;

import lombok.Data;
import org.jboss.resteasy.annotations.providers.multipart.PartType;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import javax.ws.rs.FormParam;
import javax.ws.rs.core.MediaType;
import java.io.InputStream;
import java.time.OffsetDateTime;
import java.util.List;

@Data
public class FinalLogUpload {

    @FormParam("md5sum")
    @PartType("text/plain")
    @NotBlank
    private String md5sum;

    @FormParam("sha512sum")
    @PartType("text/plain")
    @NotBlank
    private String sha512sum;

    @FormParam("endTime")
    @PartType("text/plain")
    @NotNull
    private OffsetDateTime endTime;

    @FormParam("loggerName")
    @PartType("text/plain")
    @NotBlank
    private String loggerName;

    @FormParam("tag")
    @PartType("text/plain")
    @NotEmpty
    private String tag;

    @FormParam("logfile")
    // @PartType(MediaType.APPLICATION_OCTET_STREAM)
    public InputStream logfile;
}
