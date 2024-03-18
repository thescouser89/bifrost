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

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.ext.Provider;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.zip.GZIPOutputStream;

/**
 * From: https://github.com/quarkusio/quarkus/issues/9671#issuecomment-762970234
 *
 * It's strange but I cannot make gzip compression work with resteasy classic for 2.16.12. I suspect there's a bug
 * somewhere that is fixed in Quarkus 3. Therefore, please remove this when we upgrade to Quarkus 3
 */
@Provider
public class Quarkus2GzipCompression implements ContainerResponseFilter {
    @Override
    public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext)
            throws IOException {
        String encoding = requestContext.getHeaderString(HttpHeaders.ACCEPT_ENCODING);
        if (encoding != null && encoding.contains("gzip")) {
            responseContext.getHeaders().put(HttpHeaders.CONTENT_ENCODING, Arrays.asList("gzip"));
            OutputStream outputStream = responseContext.getEntityStream();
            responseContext.setEntityStream(new GZIPOutputStream(outputStream));
        }
    }
}