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

// Specific to the main branch:
import java.util.Iterator;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import static org.opengis.annotation.Obligation.MANDATORY;
import static org.opengis.annotation.Specification.ISO_19111;
import org.opengis.annotation.UML;
import org.opengis.geometry.DirectPosition;


/**
 * Skeletal implementation of a collection of coordinate tuples referenced to the same <abbr>CRS</abbr> and epoch.
 * This implementation is serializable if the coordinate metadata given at construction time is also serializable.
 *
 * <h2>Future evolution</h2>
 * This class is expected to implement a {@code CoordinateSet} interface after the next GeoAPI release.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.5
 * @since   1.5
 */
public abstract class AbstractCoordinateSet implements Iterable<DirectPosition>, Serializable {
    /**
     * Serial number for inter-operability with different versions.
     */
    private static final long serialVersionUID = 3656426153519035461L;

    /**
     * Coordinate reference system and epoch (if dynamic) of this coordinate set.
     */
    private final DefaultCoordinateMetadata metadata;

    /**
     * Creates a new set of coordinate tuples.
     *
     * @param metadata  coordinate reference system and epoch (if dynamic) of this coordinate set.
     */
    protected AbstractCoordinateSet(final DefaultCoordinateMetadata metadata) {
        this.metadata = Objects.requireNonNull(metadata);
    }

    /**
     * Returns the coordinate metadata to which this coordinate set is referenced.
     *
     * <div class="warning"><b>Upcoming API change</b><br>
     * {@code DefaultCoordinateMetadata} class may be replaced by {@code CoordinateMetadata} interface
     * after upgrade to GeoAPI 3.1.
     * </div>
     *
     * @return coordinate metadata to which this coordinate set is referenced.
     */
    public DefaultCoordinateMetadata getCoordinateMetadata() {
        return metadata;
    }

    /**
     * Returns the number of dimensions of coordinate tuples. This is determined by the
     * {@linkplain DefaultCoordinateMetadata#getCoordinateReferenceSystem() coordinate reference system}.
     *
     * @return the number of dimensions of coordinate tuples.
     */
    public int getDimension() {
        // All methods invoked below are for attributes declared as mandatory. Values shall not be null.
        return getCoordinateMetadata().getCoordinateReferenceSystem().getCoordinateSystem().getDimension();
    }

    /**
     * Returns the positions described by coordinate tuples.
     *
     * @return position described by coordinate tuples.
     */
    @Override
    @UML(identifier="coordinateTuple", obligation=MANDATORY, specification=ISO_19111)
    public abstract Iterator<DirectPosition> iterator();

    /**
     * Returns a stream of coordinate tuples.
     * Whether the stream is sequential or parallel is implementation dependent.
     * The default implementation creates a sequential stream.
     *
     * @return a sequential or parallel stream of coordinate tuples.
     */
    public Stream<DirectPosition> stream() {
        return StreamSupport.stream(spliterator(), false);
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
