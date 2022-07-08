/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2020-2022 Red Hat, Inc., and individual contributors
 * as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jboss.pnc.bifrost.common;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

/**
 * @author <a href="mailto:andrea.vibelli@gmail.com">Andrea Vibelli</a>
 */
public class Strings {

    /**
     * Encode a string to UTF8
     *
     * @param string
     * @return
     */
    public static String encodeToUTF8(String rawString) {
        if (rawString == null) {
            return null;
        }

        ByteBuffer buffer = StandardCharsets.UTF_8.encode(rawString);
        String utf8EncodedString = StandardCharsets.UTF_8.decode(buffer).toString();

        return utf8EncodedString;
    }
}
