package org.jboss.pnc.bifrost.endpoint.websocket;

/**
 * @author <a href="mailto:matejonnet@gmail.com">Matej Lazar</a>
 */
public abstract class Result<T> {

    String type;

    T value;

    public String getType() {
        if (type != null) {
            return type;
        } else {
            return this.getClass().getCanonicalName();
        }
    }

    public void setValue(T value) {
        this.value = value;
    }

    public T getValue() {
        return value;
    }
}
