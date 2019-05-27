package org.jboss.pnc.bifrost.endpoint;

/**
 * @author <a href="mailto:matejonnet@gmail.com">Matej Lazar</a>
 */
public class ActionUnSubscribe {

    Action action = Action.UNSUBSCRIBE;

    private final String matchFilters;

    private final String prefixFilters;

    public ActionUnSubscribe(String matchFilters, String prefixFilters) {
        this.matchFilters = matchFilters;
        this.prefixFilters = prefixFilters;
    }

    public Action getAction() {
        return action;
    }

    public String getMatchFilters() {
        return matchFilters;
    }

    public String getPrefixFilters() {
        return prefixFilters;
    }
}
