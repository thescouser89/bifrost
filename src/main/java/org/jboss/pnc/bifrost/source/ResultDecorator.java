package org.jboss.pnc.bifrost.source;

import org.jboss.logging.Logger;
import org.jboss.pnc.bifrost.common.Strings;
import org.jboss.pnc.bifrost.source.dto.Direction;
import org.jboss.pnc.bifrost.source.dto.Line;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * @author <a href="mailto:matejonnet@gmail.com">Matej Lazar</a>
 */
public class ResultDecorator { //TODO is this class required ?

    private final Logger logger = Logger.getLogger(ResultDecorator.class);

    private final ElasticSearch elasticSearch;

    public ResultDecorator(ElasticSearch elasticSearch) {
        this.elasticSearch = elasticSearch;
    }

    /**
     * Queries the source and returns the matching results.
     */
    public List<Line> get(String matchFilters, String prefixFilters, Optional<Line> afterLine, Direction direction, int maxLines)
            throws IOException {
        List<Line> lines = new ArrayList<>();
            get(
                line -> lines.add(line),
                matchFilters,
                prefixFilters,
                afterLine,
                direction,
                maxLines
                );
        return lines;
    }

    public void get(Consumer<Line> onLine, String matchFilters, String prefixFilters, Optional<Line> afterLine, Direction direction, int maxLines)
            throws IOException {
        elasticSearch.get(
                Strings.toMap(matchFilters),
                Strings.toMap(prefixFilters),
                afterLine,
                direction,
                maxLines,
                onLine);
    }



}
