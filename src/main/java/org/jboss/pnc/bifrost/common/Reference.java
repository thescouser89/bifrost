package org.jboss.pnc.bifrost.common;

/**
 * @author <a href="mailto:matejonnet@gmail.com">Matej Lazar</a>
 */
public class Reference<T> {
    T value;

    public Reference() {
    }

    public Reference(T initialValue) {
        value = initialValue;
    }

    public T get() {
        return value;
    }

    public void set(T newValue) {
        value = newValue;
    }
}
