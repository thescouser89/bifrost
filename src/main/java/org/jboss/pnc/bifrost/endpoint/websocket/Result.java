package org.jboss.pnc.bifrost.endpoint.websocket;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * @author <a href="mailto:matejonnet@gmail.com">Matej Lazar</a>
 */
@Getter
@Setter
@ToString
public class Result {

    private Status status;

    private String message;

    public Result() {
    }

    public enum Status {
        OK, ERROR;
    }

    public Result(Status status, String message) {
        this.status = status;
        this.message = message;
    }

    public Result(Status status) {
        this.status = status;
    }
}
