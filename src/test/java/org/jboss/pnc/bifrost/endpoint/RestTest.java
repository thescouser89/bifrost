package org.jboss.pnc.bifrost.endpoint;

import io.quarkus.test.junit.QuarkusTest;
import org.jboss.pnc.bifrost.endpoint.provider.DataProviderMock;
import org.jboss.pnc.bifrost.mock.LineProducer;
import org.jboss.pnc.bifrost.source.RemoteConnectionTest;
import org.jboss.pnc.bifrost.source.dto.Direction;
import org.jboss.pnc.bifrost.source.dto.Line;
import org.jboss.resteasy.client.jaxrs.ResteasyWebTarget;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

/**
 * @author <a href="mailto:matejonnet@gmail.com">Matej Lazar</a>
 */
@QuarkusTest
public class RestTest {

    private static Logger logger = LoggerFactory.getLogger(RemoteConnectionTest.class);

    private static WebTarget target;

    @Inject
    DataProviderMock dataProvider;

    @BeforeAll
    public static void init() {
        Client client = ClientBuilder.newClient();
        target = client.target("http://localhost:8081/");
    }

    @Test
    public void shouldGetLines() throws IOException {
        ResteasyWebTarget rtarget = (ResteasyWebTarget)target;
        Rest rest = rtarget.proxy(Rest.class);

        List<Line> mockLines = rest.getLines("", "", null, Direction.ASC, 10);
        mockLines.forEach(line -> System.out.println(line.asString()));
        Assertions.assertEquals(5, mockLines.size());
        Assertions.assertEquals("abc123", mockLines.get(0).getCtx());
    }

    @Test
    public void shouldGetTextStream() throws Exception {
        int numLines = 10;
        Semaphore semaphore = new Semaphore(0);
        List<Line> lines = LineProducer.getLines(numLines, "xx");
        dataProvider.addAllLines(lines);
        List<String> receivedLines = new ArrayList<>();

        Thread thread = new Thread(() -> {
            Client client = ClientBuilder.newClient();
            WebTarget target = client.target("http://localhost:8081/text");
            ResteasyWebTarget rtarget = (ResteasyWebTarget) target;
            rtarget.setChunked(true);

            Invocation.Builder request = rtarget.request(MediaType.TEXT_PLAIN);
            Invocation invocation = request.buildGet();
            InputStream inputStream = invocation.invoke(InputStream.class);

            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
            logger.info("Reading stream ...");
            try {
                for (String line; (line = reader.readLine()) != null; ) {
                    logger.info("Log line: " + line);
                    receivedLines.add(line);
                    if (receivedLines.size() == numLines) {
                        semaphore.release();
                    }
                }
            } catch (Exception e) {
                logger.error("Client error: ", e);
            }
            logger.info("Request processing ended.");
        });
        thread.start();

        boolean acquired = semaphore.tryAcquire(15, TimeUnit.SECONDS);

        Assertions.assertTrue(acquired);

        logger.info("done.");
    }


}
