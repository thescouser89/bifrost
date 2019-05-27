package org.jboss.pnc.bifrost.endpoint;

import io.quarkus.test.junit.QuarkusTest;
import org.jboss.pnc.bifrost.source.dto.Direction;
import org.jboss.pnc.bifrost.source.dto.Line;
import org.jboss.resteasy.client.jaxrs.ResteasyWebTarget;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import java.io.IOException;
import java.util.List;

/**
 * @author <a href="mailto:matejonnet@gmail.com">Matej Lazar</a>
 */
@QuarkusTest
public class RestTest {

    private static WebTarget target;

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

}
