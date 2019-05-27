package org.jboss.pnc.bifrost.endpoint.configuration;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.Destroyed;
import javax.enterprise.context.Initialized;
import javax.enterprise.event.Observes;

/**
 * @author <a href="mailto:matejonnet@gmail.com">Matej Lazar</a>
 */
public class BootListener {

    public void init(@Observes @Initialized(ApplicationScoped.class) Object o) {

    }

    public void destroy(@Observes @Destroyed(ApplicationScoped.class) Object o) {

    }

}
