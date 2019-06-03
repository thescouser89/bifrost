package org.jboss.pnc.bifrost.endpoint.websocket;

import javax.websocket.Session;

/**
 * @author <a href="mailto:matejonnet@gmail.com">Matej Lazar</a>
 */
public abstract class MethodBase {
    private Session session;

    public void setSession(Session session) {
        this.session = session;
    }

    protected Session getSession() {
        return session;
    }
}
