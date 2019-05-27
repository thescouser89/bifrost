package org.jboss.pnc.bifrost.mock;

import org.jboss.pnc.bifrost.endpoint.provider.DataProvider;
import org.jboss.pnc.bifrost.mock.LineProducer;
import org.jboss.pnc.bifrost.source.ElasticSearchConfig;
import org.jboss.pnc.bifrost.source.dto.Direction;
import org.jboss.pnc.bifrost.source.dto.Line;

import javax.annotation.Priority;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Alternative;
import javax.inject.Inject;
import java.io.IOException;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * @author <a href="mailto:matejonnet@gmail.com">Matej Lazar</a>
 */
@Alternative()
@Priority(1)
@ApplicationScoped
public class MockDataProvider extends DataProvider {

    public MockDataProvider() {
        super();
    }

    @Inject
    public MockDataProvider(ElasticSearchConfig elasticSearchConfig) throws Exception {
        super(elasticSearchConfig);
    }

    @Override
    public void get(
            String matchFilters,
            String prefixFilters,
            Optional<Line> afterLine,
            Direction direction,
            int fetchSize, Consumer<Line> onLine) throws
            IOException {

        LineProducer.getLines(5, "abc123").forEach(
                line -> onLine.accept(line)
        );
    }
}