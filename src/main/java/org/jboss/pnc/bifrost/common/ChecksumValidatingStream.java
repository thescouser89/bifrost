/**
 * JBoss, Home of Professional Open Source.
 * Copyright 2021 Red Hat, Inc., and individual contributors
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

import io.quarkus.logging.Log;
import org.jboss.pnc.bifrost.endpoint.LogUpload;

import javax.validation.ValidationException;
import javax.xml.bind.DatatypeConverter;
import java.io.FilterInputStream;
import java.io.InputStream;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class ChecksumValidatingStream extends FilterInputStream {

    private final MessageDigest md5;

    private final String md5sum;

    private ChecksumValidatingStream(InputStream stream, MessageDigest md5, String md5sum) {
        super(stream);
        this.md5 = md5;
        this.md5sum = md5sum;
    }

    public static ChecksumValidatingStream validate(InputStream is, String md5sum) {
        MessageDigest md5;
        try {
            md5 = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
        DigestInputStream md5dis = new DigestInputStream(is, md5);
        return new ChecksumValidatingStream(md5dis, md5, md5sum);
    }

    public void validate() throws ValidationException {
        String md5computedSum = DatatypeConverter.printHexBinary(md5.digest());
        if (!md5computedSum.equalsIgnoreCase(md5sum)) {
            throw new ValidationException(
                    "Stream validation failed, expected " + md5sum + " got " + md5computedSum + ".");
        }

    }
}
