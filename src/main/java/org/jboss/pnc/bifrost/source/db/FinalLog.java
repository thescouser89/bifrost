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
package org.jboss.pnc.bifrost.source.db;

import io.quarkus.hibernate.orm.panache.PanacheEntity;

import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.Lob;
import javax.persistence.ManyToOne;
import java.sql.Blob;
import java.time.OffsetDateTime;
import java.util.List;

@Entity
public class FinalLog extends PanacheEntity {

    @ManyToOne(optional = false)
    public LogEntry logEntry;

    @Column(nullable = false)
    public OffsetDateTime eventTimestamp;

    @Column(nullable = false)
    public String loggerName;

    @Column(length = 32, nullable = false)
    public String md5sum;

    @ElementCollection
    public List<String> tags;

    @Lob
    public Blob logContent;
}
