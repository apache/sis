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
package org.apache.sis.internal.temporal;

import java.util.Date;
import org.opengis.temporal.Instant;
import org.opengis.temporal.TemporalPosition;


/**
 * Default implementation of GeoAPI instant. This is a temporary class;
 * GeoAPI temporal interfaces are expected to change a lot in a future revision.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.2
 * @since   1.2
 * @module
 */
final class DefaultInstant extends Primitive implements Instant {
    /** The date in milliseconds since epoch. */
    private final long millis;

    /** Creates a new instant for the given date. */
    DefaultInstant(final Date time) {
        millis = time.getTime();
    }

    /** Returns the date used for describing temporal position. */
    @Override public Date getDate() {
        return new Date(millis);
    }

    /** Association to a temporal reference system. */
    @Override public TemporalPosition getTemporalPosition() {
        throw DefaultTemporalFactory.unsupported();
    }

    /** String representation in ISO format. */
    @Override public String toString() {
        return java.time.Instant.ofEpochMilli(millis).toString();
    }

    /** Hash code value of the time position. */
    @Override public int hashCode() {
        return Long.hashCode(millis) ^ 57;
    }

    /** Compares with given object for equality. */
    @Override public boolean equals(final Object obj) {
        return (obj instanceof DefaultInstant) && ((DefaultInstant) obj).millis == millis;
    }
}
