package org.jboss.pnc.bifrost.source;

import io.quarkus.test.junit.QuarkusTest;
import org.apache.http.HttpEntity;
import org.apache.http.entity.BasicHttpEntity;
import org.elasticsearch.client.Response;
import org.elasticsearch.client.RestClient;
import org.jboss.pnc.bifrost.endpoint.provider.DataProviderMock;
import org.jboss.pnc.bifrost.source.dto.Direction;
import org.jboss.pnc.bifrost.source.dto.Line;
import org.jboss.pnc.bifrost.test.DebugTest;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.Collections;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * @author <a href="mailto:matejonnet@gmail.com">Matej Lazar</a>
 */
@QuarkusTest
@DebugTest
public class RemoteConnectionTest {

    private static Logger logger = LoggerFactory.getLogger(RemoteConnectionTest.class);

    @Inject
    ElasticSearchConfig elasticSearchConfig;

    @Inject //TODO do not use the alternative
            DataProviderMock dataProvider;

    @Test
    public void connect() throws Exception {
        RestClient lowLevelRestClient = new ClientFactory(elasticSearchConfig).getConnectedClient();
        logger.info("Connected.");

        String index = elasticSearchConfig.getIndexes().split(",")[0];
        HttpEntity entity = new BasicHttpEntity();
        Response response = lowLevelRestClient.performRequest("GET", "/" + index + "/_search/", Collections.emptyMap(), entity);
        assert 200 == response.getStatusLine().getStatusCode();
        lowLevelRestClient.close();
    }

    @Test
    public void shouldQueryRemoteServer() throws Exception {
        Consumer<Line> onLine = line -> logger.info("line: " + line.getId() + " - " + line.getMessage());
//        dataProvider.get("", "", Optional.empty(), Direction.DESC, Optional.of(10), onLine);
        dataProvider.get("", "", Optional.empty(), Direction.DESC, Optional.empty(), onLine);
    }

}