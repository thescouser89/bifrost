package org.jboss.pnc.bifrost.source.mock;

import org.jboss.pnc.bifrost.source.dto.Line;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * @author <a href="mailto:matejonnet@gmail.com">Matej Lazar</a>
 */
public class LineProducer {

    public static Line getLine(Integer lineNumber, boolean last, String ctx) {
        return new Line(
                UUID.randomUUID().toString(),
                Long.toString(System.currentTimeMillis()),
                "org.jboss.pnc._userlog_",
                "Message " + lineNumber,
                last,
                ctx,
                false,
                null
        );
    }

    public static List<Line> getLines(Integer numberOfLines, String ctx) {
        List<Line> lines = new ArrayList<>();
        for (int i = 0; i < numberOfLines; i++) {
            boolean last = i == numberOfLines - 1;
            lines.add(getLine(i, last, ctx));
        }
        return lines;
    }
}
