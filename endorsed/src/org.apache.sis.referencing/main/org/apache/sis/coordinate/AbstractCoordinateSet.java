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
package org.apache.sis.coordinate;

import java.util.Objects;
import java.io.Serializable;
import org.apache.sis.referencing.IdentifiedObjects;

// Specific to the geoapi-3.1 and geoapi-4.0 branches:
import org.opengis.coordinate.CoordinateSet;
import org.opengis.coordinate.CoordinateMetadata;


/**
 * Skeletal implementation of a collection of coordinate tuples referenced to the same <abbr>CRS</abbr> and epoch.
 * This implementation is serializable if the coordinate metadata given at construction time is also serializable.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.5
 * @since   1.5
 */
public abstract class AbstractCoordinateSet implements CoordinateSet, Serializable {
    /**
     * Serial number for inter-operability with different versions.
     */
    private static final long serialVersionUID = 3656426153519035462L;

    /**
     * Coordinate reference system and epoch (if dynamic) of this coordinate set.
     */
    @SuppressWarnings("serial")     // Apache SIS implementations of this interface are serializable.
    private final CoordinateMetadata metadata;

    /**
     * Creates a new set of coordinate tuples.
     *
     * @param metadata  coordinate reference system and epoch (if dynamic) of this coordinate set.
     */
    protected AbstractCoordinateSet(final CoordinateMetadata metadata) {
        this.metadata = Objects.requireNonNull(metadata);
    }

    /**
     * Returns the coordinate metadata to which this coordinate set is referenced.
     *
     * @return coordinate metadata to which this coordinate set is referenced.
     */
    @Override
    public CoordinateMetadata getCoordinateMetadata() {
        return metadata;
    }

    /**
     * Returns a string representation of this coordinate set for debugging purposes.
     * This string representation may change in any future version.
     *
     * @return a string representation for debugging purposes.
     */
    @Override
    public String toString() {
        final var sb = new StringBuilder("CoordinateSet");
        sb.append('[').append(IdentifiedObjects.getDisplayName(metadata.getCoordinateReferenceSystem(), null));
        metadata.getCoordinateEpoch().ifPresent((epoch) -> sb.append(" @ ").append(epoch));
        return sb.toString();
    }
}
