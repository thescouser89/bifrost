package org.jboss.pnc.bifrost.source.dto;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

/**
 * @author <a href="mailto:matejonnet@gmail.com">Matej Lazar</a>
 */
@Data
@AllArgsConstructor
@Builder
@JsonDeserialize(builder = Line.LineBuilder.class)
public class Line {

    private final String id;

    private final String timestamp;

    private final String logger;

    private final String message;

    private final boolean last;

    private final String ctx;

    private final boolean tmp;

    private final String exp;

    public String asString() {
        return getTimestamp() + " " + getLogger() + " " + getMessage();
    }

    @JsonPOJOBuilder(withPrefix = "")
    public static final class LineBuilder {
    }

    public LineBuilder cloneBuilder() {
        return builder()
                .id(id)
                .timestamp(timestamp)
                .logger(logger)
                .message(message)
                .last(last)
                .ctx(ctx)
                .tmp(tmp)
                .exp(exp);
    }
}
