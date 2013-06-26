/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.sis.internal.jaxb.gml;

import java.sql.Time;
import java.util.Date;
import java.util.Collection;
import java.util.Collections;
import org.apache.sis.internal.geoapi.temporal.Instant;
import org.apache.sis.internal.geoapi.temporal.Position;
import org.opengis.util.InternationalString;


/**
 * A dummy {@link Instant} implementation, for testing the JAXB elements without dependency
 * toward the {@code sis-temporal} module.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.3 (derived from geotk-3.20)
 * @version 0.3
 * @module
 */
final class DummyInstant implements Instant, Position {
    /**
     * The time, in milliseconds elapsed since January 1st, 1970.
     */
    private final long time;

    /**
     * Creates a new instant initialized to the given value.
     */
    DummyInstant(final Date time) {
        this.time = time.getTime();
    }

    /**
     * Returns the date of this instant object.
     */
    @Override
    public Date getDate() {
        return new Date(time);
    }

    /**
     * Returns the position, which is {@code this}.
     */
    @Override
    public Position getPosition() {
        return this;
    }
}
