package org.jboss.pnc.bifrost.endpoint;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.StreamingOutput;
import javax.ws.rs.ext.MessageBodyWriter;
import javax.ws.rs.ext.Provider;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

/**
 * From: https://github.com/quarkusio/quarkus/issues/28385#issuecomment-1276536935
 *
 * I believe that once we switch to the latest version of Quarkus 3, we shouldn't need this since the latest one
 * supports StreamingOutput with Resteasy Reactive
 */
@Provider
public class Quarkus2StreamingOutputMessageBodyWriter implements MessageBodyWriter<StreamingOutput> {

    @Override
    public boolean isWriteable(Class<?> aClass, Type type, Annotation[] annotations, MediaType mediaType) {
        return StreamingOutput.class.isAssignableFrom(aClass);
    }

    @Override
    public void writeTo(
            StreamingOutput blob,
            Class<?> aClass,
            Type type,
            Annotation[] annotations,
            MediaType mediaType,
            MultivaluedMap<String, Object> httpHeaders,
            OutputStream outputStream) throws IOException, WebApplicationException {
        blob.write(outputStream);
    }
}
