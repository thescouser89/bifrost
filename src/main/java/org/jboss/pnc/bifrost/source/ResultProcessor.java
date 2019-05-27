package org.jboss.pnc.bifrost.source;

import org.jboss.pnc.bifrost.common.Strings;
import org.jboss.pnc.bifrost.source.dto.Direction;
import org.jboss.pnc.bifrost.source.dto.Line;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * @author <a href="mailto:matejonnet@gmail.com">Matej Lazar</a>
 */
public class ResultProcessor {

    private final Logger logger = LoggerFactory.getLogger(ResultProcessor.class);

    private final ElasticSearch elasticSearch;

    public ResultProcessor(ElasticSearch elasticSearch) {
        this.elasticSearch = elasticSearch;
    }

    /**
     * Queries the source and returns the matching results.
     */
    public List<Line> get(String matchFilters, String prefixFilters, Optional<Line> afterLine, Direction direction, int maxLines)
            throws IOException {
        List<Line> lines = new ArrayList<>();
        elasticSearch.get(
                Strings.toMap(matchFilters),
                Strings.toMap(prefixFilters),
                afterLine,
                direction,
                maxLines,
                line -> lines.add(line));
        return lines;
    }



}
