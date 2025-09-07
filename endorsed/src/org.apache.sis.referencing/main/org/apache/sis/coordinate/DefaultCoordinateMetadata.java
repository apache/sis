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
import java.util.Optional;
import java.io.Serializable;
import java.time.temporal.Temporal;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.apache.sis.referencing.IdentifiedObjects;
import org.apache.sis.referencing.CRS;
import org.apache.sis.referencing.privy.WKTUtilities;
import org.apache.sis.referencing.privy.WKTKeywords;
import org.apache.sis.referencing.internal.Resources;
import org.apache.sis.referencing.internal.Epoch;
import org.apache.sis.io.wkt.FormattableObject;
import org.apache.sis.io.wkt.Formatter;
import org.apache.sis.util.ComparisonMode;
import org.apache.sis.util.LenientComparable;
import org.apache.sis.util.Utilities;

// Specific to the geoapi-3.1 and geoapi-4.0 branches:
import org.opengis.coordinate.CoordinateMetadata;


/**
 * Default implementation of metadata required to reference coordinates.
 * Metadata include a coordinate reference system and the epoch at which the coordinates are valid.
 * This default implementation provides <i>Well-Known Text</i> support.
 * It is immutable and serializable if the CRS and epoch are also serializable.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.5
 * @since   1.5
 */
public class DefaultCoordinateMetadata extends FormattableObject
        implements CoordinateMetadata, LenientComparable, Serializable
{
    /**
     * Serial number for inter-operability with different versions.
     */
    private static final long serialVersionUID = -754447822292824735L;

    /**
     * The coordinate reference system (<abbr>CRS</abbr>) in which the coordinate tuples are given.
     *
     * @see #getCoordinateReferenceSystem()
     */
    @SuppressWarnings("serial")     // Apache SIS implementations of this interface are serializable.
    private final CoordinateReferenceSystem crs;

    /**
     * Date at which coordinate tuples are valid, or {@code null} if the CRS is not dynamic.
     *
     * @see #getCoordinateEpoch()
     */
    @SuppressWarnings("serial")     // Java implementations of this interface are serializable.
    private final Temporal epoch;

    /**
     * Creates a new coordinate metadata.
     *
     * @param  crs    the coordinate reference system (<abbr>CRS</abbr>) in which the coordinate tuples are given.
     * @param  epoch  date at which coordinate tuples are valid, or {@code null} if the CRS is not dynamic.
     * @throws IllegalArgumentException if {@code epoch} is null while the CRS is dynamic or has a dynamic component.
     */
    public DefaultCoordinateMetadata(final CoordinateReferenceSystem crs, final Temporal epoch) {
        this.crs = Objects.requireNonNull(crs);
        this.epoch = epoch;
        if (epoch == null && CRS.getFrameReferenceEpoch(crs).isPresent()) {
            throw new IllegalArgumentException(Resources.format(Resources.Keys.MissingReferenceFrameEpoch_1,
                                               IdentifiedObjects.getDisplayName(crs, null)));
        }
    }

    /**
     * Creates a new coordinate metadata with the same values as the specified one.
     * This copy constructor provides a way to convert an arbitrary implementation into a SIS one
     * or a user-defined one (as a subclass), usually in order to leverage some implementation-specific API.
     *
     * <p>This constructor performs a shallow copy, i.e. the properties are not cloned.</p>
     *
     * @param  metadata  the coordinate metadata to copy.
     *
     * @see #castOrCopy(CoordinateMetadata)
     */
    protected DefaultCoordinateMetadata(final CoordinateMetadata metadata) {
        this(metadata.getCoordinateReferenceSystem(), metadata.getCoordinateEpoch().orElse(null));
    }

    /**
     * Returns a SIS datum implementation with the same values as the given arbitrary implementation.
     * If the given object is {@code null}, then this method returns {@code null}.
     * Otherwise if the given object is already a SIS implementation, then the given object is returned unchanged.
     * Otherwise a new SIS implementation is created and initialized to the attribute values of the given object.
     *
     * @param  object  the object to get as a SIS implementation, or {@code null} if none.
     * @return a SIS implementation containing the values of the given object (may be the
     *         given object itself), or {@code null} if the argument was null.
     */
    public static DefaultCoordinateMetadata castOrCopy(final CoordinateMetadata object) {
        if (object == null || object instanceof DefaultCoordinateMetadata) {
            return (DefaultCoordinateMetadata) object;
        } else {
            return new DefaultCoordinateMetadata(object);
        }
    }

    /**
     * Returns the <abbr>CRS</abbr> in which the coordinate tuples are given.
     * Should never be null in principle, however this implementation does not enforce this restriction.
     *
     * @return the coordinate reference system (CRS) of coordinate tuples.
     */
    @Override
    public CoordinateReferenceSystem getCoordinateReferenceSystem() {
        return crs;
    }

    /**
     * Returns the date at which coordinate tuples referenced to a dynamic <abbr>CRS</abbr> are valid.
     *
     * @return epoch at which coordinate tuples are valid.
     */
    @Override
    public Optional<Temporal> getCoordinateEpoch() {
        return Optional.ofNullable(epoch);
    }

    /**
     * Returns a hash code value for this coordinate metadata.
     *
     * @return a hash code value.
     */
    @Override
    public int hashCode() {
        return crs.hashCode() * 11 + Objects.hashCode(epoch);
    }

    /**
     * Compares this metadata with the given object for equality.
     *
     * @param  obj  the object to compare with this metadata.
     * @return whether the two objects are equal.
     */
    @Override
    public final boolean equals(final Object obj) {
        return equals(obj, ComparisonMode.STRICT);
    }

    /**
     * Compares this metadata with the given object for equality.
     *
     * @param  obj   the object to compare to {@code this}.
     * @param  mode  the strictness level of the comparison.
     * @return {@code true} if both objects are equal.
     */
    @Override
    public boolean equals(final Object obj, final ComparisonMode mode) {
        if (this == obj) {
            return true;
        }
        if (obj != null) {
            if (mode == ComparisonMode.STRICT) {
                if (obj.getClass() == getClass()) {
                    final var other = (DefaultCoordinateMetadata) obj;
                    return crs.equals(other.crs) && Objects.equals(epoch, other.epoch);
                }
            } else if (obj instanceof CoordinateMetadata) {
                final var other = (CoordinateMetadata) obj;
                return Utilities.deepEquals(getCoordinateReferenceSystem(), other.getCoordinateReferenceSystem(), mode)
                        && Objects.equals(getCoordinateEpoch(), other.getCoordinateEpoch());
            }
        }
        return false;
    }

    /**
     * Formats this metadata as a <i>Well Known Text</i> {@code CoordinateMetadata[…]} element.
     *
     * @param  formatter  the formatter where to format the inner content of this WKT element.
     * @return {@code "CoordinateMetadata"}.
     */
    @Override
    protected String formatTo(final Formatter formatter) {
        formatter.append(WKTUtilities.toFormattable(crs));
        if (epoch != null) {
            formatter.append(new Epoch(epoch, false));
        }
        return WKTKeywords.CoordinateMetadata;
    }
}
