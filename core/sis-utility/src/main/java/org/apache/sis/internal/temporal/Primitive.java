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

import org.opengis.metadata.Identifier;
import org.opengis.temporal.Duration;
import org.opengis.temporal.RelativePosition;
import org.opengis.temporal.TemporalGeometricPrimitive;
import org.opengis.temporal.TemporalPrimitive;


/**
 * Base implementation of GeoAPI temporal primitives. This is a temporary class;
 * GeoAPI temporal interfaces are expected to change a lot in a future revision.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.2
 * @since   1.2
 * @module
 */
class Primitive implements TemporalGeometricPrimitive, Identifier {
    /**
     * For sub-class constructors.
     */
    Primitive() {
    }

    /**
     * The primary name by which this object is identified.
     * This field is inherited from ISO 19111 {@code IdentifiedObject} and is in principle mandatory.
     */
    @Override
    public final Identifier getName() {
        return this;
    }

    /**
     * Returns the string representation as the code for this object. This is not a correct identifier code,
     * but we use that as a trick for forcing {@link org.apache.sis.util.collection.TreeTableFormat} to show
     * the temporal value, because the formatter handles {@link org.opengis.referencing.IdentifiedObject} in
     * a special way.
     */
    @Override public final String getCode() {
        return toString();
    }

    /** position of this primitive relative to another primitive. */
    @Override public final RelativePosition relativePosition(TemporalPrimitive other) {
        throw DefaultTemporalFactory.unsupported();
    }

    /** Absolute value of the difference between temporal positions. */
    @Override public final Duration distance(TemporalGeometricPrimitive other) {
        throw DefaultTemporalFactory.unsupported();
    }

    /** Duration of this temporal geometric primitive. */
    @Override public final Duration length() {
        return null;    // Do not throw an exception here; this is invoked by reflection.
    }
}
