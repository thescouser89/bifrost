package org.jboss.pnc.bifrost.endpoint;

import org.jboss.pnc.bifrost.common.scheduler.Subscription;
import org.jboss.pnc.bifrost.endpoint.provider.DataProvider;
import org.jboss.pnc.bifrost.source.dto.Direction;
import org.jboss.pnc.bifrost.source.dto.Line;

import javax.inject.Inject;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * @author <a href="mailto:matejonnet@gmail.com">Matej Lazar</a>
 */
@Path("/")
public class RestImpl implements Rest {

    @Inject
    DataProvider dataProvider;

    @Override
    public Response getAllLines(String matchFilters, String prefixFilters, Line afterLine, boolean follow) {

        StreamingOutput stream = outputStream -> {
            Subscription subscription = new Subscription();
            Writer writer = new BufferedWriter(new OutputStreamWriter(outputStream));

            Consumer<Line> onLine = line -> {
                try {
                    writer.write(line.asString());
                    if (line.isLast()) {
                        writer.flush();
                        writer.close();
                    }
                } catch (IOException e) {
                    dataProvider.unsubscibe(subscription);
                }
            };
            if (follow) {
                dataProvider.subscribe(
                        matchFilters,
                        prefixFilters,
                        Optional.ofNullable(afterLine),
                        onLine,
                        subscription
                );
            } else {
                dataProvider.get(
                        matchFilters,
                        prefixFilters,
                        Optional.ofNullable(afterLine),
                        Direction.ASC,
                        -1,
                        onLine
                );
            }
        };
        return Response.ok(stream).build();
    }

    @Override
    public List<Line> getLines(String matchFilters, String prefixFilters, Line afterLine, Direction direction, Integer maxLines)
            throws IOException {
        List<Line> lines = new ArrayList<>();
        Consumer<Line> onLine = line -> lines.add(line);
        dataProvider.get(matchFilters, prefixFilters, Optional.ofNullable(afterLine), direction, maxLines, onLine);
        return lines;
    }

}
