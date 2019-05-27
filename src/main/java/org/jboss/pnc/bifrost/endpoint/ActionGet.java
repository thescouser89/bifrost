package org.jboss.pnc.bifrost.endpoint;

import org.jboss.pnc.bifrost.source.dto.Direction;
import org.jboss.pnc.bifrost.source.dto.Line;

/**
 * @author <a href="mailto:matejonnet@gmail.com">Matej Lazar</a>
 */
public class ActionGet {

    Action action = Action.GET;

    private String matchFilters;

    private String prefixFilters;

    private Line afterLine;

    private int maxLines;

    private Direction direction;

    //TODO lombok
    public Action getAction() {
        return action;
    }

    public String getMatchFilters() {
        return matchFilters;
    }

    public void setMatchFilters(String matchFilters) {
        this.matchFilters = matchFilters;
    }

    public String getPrefixFilters() {
        return prefixFilters;
    }

    public void setPrefixFilters(String prefixFilters) {
        this.prefixFilters = prefixFilters;
    }

    public Line getAfterLine() {
        return afterLine;
    }

    public void setAfterLine(Line afterLine) {
        this.afterLine = afterLine;
    }

    public int getMaxLines() {
        return maxLines;
    }

    public void setMaxLines(int maxLines) {
        this.maxLines = maxLines;
    }

    public Direction getDirection() {
        return direction;
    }

    public void setDirection(Direction direction) {
        this.direction = direction;
    }
}
