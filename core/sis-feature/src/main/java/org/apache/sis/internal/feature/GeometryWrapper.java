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
package org.apache.sis.internal.feature;

import java.util.Objects;
import org.opengis.geometry.Envelope;
import org.opengis.geometry.Geometry;


/**
 * Wraps a JTS or ESRI geometry behind a {@code Geometry} interface.
 * This is a temporary class to be refactored later as a more complete geometry framework.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 0.8
 * @since   0.8
 * @module
 */
public final class GeometryWrapper implements Geometry {
    /**
     * The JTS or ESRI geometric object.
     */
    public final Object geometry;

    /**
     * Geometry bounding box, together with its coordinate reference system.
     */
    private final Envelope envelope;

    /**
     * Creates a new geometry object.
     *
     * @param  geometry  the JTS or ESRI geometric object.
     * @param  envelope  geometry bounding box, together with its coordinate reference system.
     */
    public GeometryWrapper(final Object geometry, final Envelope envelope) {
        this.geometry = geometry;
        this.envelope = envelope;
    }

    @Override public Geometry       clone() throws CloneNotSupportedException {throw new CloneNotSupportedException();}

    @Override
    public boolean equals(final Object obj) {
        return (obj instanceof GeometryWrapper) && Objects.equals(((GeometryWrapper) obj).geometry, geometry);
    }

    @Override
    public int hashCode() {
        return ~Objects.hashCode(geometry);
    }

    @Override
    public String toString() {
        return String.valueOf(geometry);
    }
}
