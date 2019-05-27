package org.jboss.pnc.bifrost.endpoint;

import org.jboss.pnc.bifrost.source.dto.Direction;
import org.jboss.pnc.bifrost.source.dto.Line;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.util.List;

/**
 * @author <a href="mailto:matejonnet@gmail.com">Matej Lazar</a>
 */
@Path("/")
@Consumes(MediaType.APPLICATION_JSON)
public interface Rest {

    @GET
    @Path("/")
    @Produces(MediaType.TEXT_PLAIN)
    Response getAllLines(String matchFilters, String prefixFilters, Line afterLine, boolean follow);

    @GET
    @Path("/")
    @Produces("application/json")
    List<Line> getLines(String matchFilters, String prefixFilters, Line afterLine, Direction direction, int maxLines)
            throws IOException;

}
