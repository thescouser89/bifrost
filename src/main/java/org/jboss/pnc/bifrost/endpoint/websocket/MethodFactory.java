package org.jboss.pnc.bifrost.endpoint.websocket;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;
import java.util.Optional;

@ApplicationScoped
public class MethodFactory {

    @Inject
    Instance<Method<?>> availableMethods;

    public Optional<Method<?>> get(String methodName) {
        return availableMethods.stream().filter(m -> m.getName().equals(methodName)).findAny();
    }
}
