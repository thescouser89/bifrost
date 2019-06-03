package org.jboss.pnc.bifrost.endpoint.websocket;

import lombok.Getter;
import lombok.Setter;
import org.jboss.pnc.bifrost.source.dto.Direction;
import org.jboss.pnc.bifrost.source.dto.Line;

/**
 * @author <a href="mailto:matejonnet@gmail.com">Matej Lazar</a>
 */
@Getter
@Setter
public class GetLinesDto {

    private String matchFilters;

    private String prefixFilters;

    private Line afterLine;

    private int maxLines;

    private Direction direction;

}
