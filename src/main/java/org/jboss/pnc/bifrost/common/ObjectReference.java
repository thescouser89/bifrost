package org.jboss.pnc.bifrost.common;

/**
 * @author <a href="mailto:matejonnet@gmail.com">Matej Lazar</a>
 */
public class ObjectReference<T> {
    T value;

    public ObjectReference() {
    }

    public ObjectReference(T initialValue) {
        value = initialValue;
    }

    public T get() {
        return value;
    }

    public void set(T newValue) {
        value = newValue;
    }
}
