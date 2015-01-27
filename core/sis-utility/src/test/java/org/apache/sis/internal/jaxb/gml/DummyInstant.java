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

import java.util.Date;
import org.opengis.temporal.Instant;
import org.opengis.temporal.Duration;
import org.opengis.temporal.RelativePosition;
import org.opengis.temporal.TemporalPosition;
import org.opengis.temporal.TemporalPrimitive;
import org.opengis.temporal.TemporalGeometricPrimitive;
import org.apache.sis.internal.simple.SimpleIdentifiedObject;


/**
 * A dummy {@link Instant} implementation, for testing the JAXB elements without dependency
 * toward the {@code sis-temporal} module.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.3
 * @version 0.5
 * @module
 */
@SuppressWarnings("serial")
final class DummyInstant extends SimpleIdentifiedObject implements Instant {
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
     * Unsupported operations.
     */
    @Override public Duration         length()                                   {throw new UnsupportedOperationException();}
    @Override public RelativePosition relativePosition(TemporalPrimitive  other) {throw new UnsupportedOperationException();}
    @Override public Duration         distance(TemporalGeometricPrimitive other) {throw new UnsupportedOperationException();}
    @Override public TemporalPosition getTemporalPosition()                      {throw new UnsupportedOperationException();}
}
