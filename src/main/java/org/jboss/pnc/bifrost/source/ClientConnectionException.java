package org.jboss.pnc.bifrost.source;

/**
 * @author <a href="mailto:matejonnet@gmail.com">Matej Lazar</a>
 */
public class ClientConnectionException extends Exception {

    public ClientConnectionException(String message, Exception e) {
        super(message, e);
    }
}
