package org.jboss.pnc.bifrost.common;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * @author <a href="mailto:matejonnet@gmail.com">Matej Lazar</a>
 */
public class Strings {

    public static Map<String, String> toMap(String string) {
        if (string == null || string.equals("")) {
            return Collections.emptyMap();
        }
        Map<String, String> map = new HashMap<>();
        try {
            String[] pairs = string.split(",");

            for (String pair : pairs) {
                String[] keyValue = pair.split(":");
                map.put(keyValue[0], keyValue[1]);
            }
        } catch (RuntimeException e) {
            throw new RuntimeException("Invalid input string: " + string + ".", e);
        }
        return map;
    }
}
