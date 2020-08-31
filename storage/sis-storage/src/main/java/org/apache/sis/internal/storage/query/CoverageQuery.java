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
package org.apache.sis.internal.storage.query;

import java.util.Arrays;
import java.util.Objects;
import java.math.RoundingMode;
import org.opengis.metadata.extent.GeographicBoundingBox;
import org.apache.sis.measure.Angle;
import org.apache.sis.measure.Latitude;
import org.apache.sis.measure.Longitude;
import org.apache.sis.measure.AngleFormat;
import org.apache.sis.coverage.grid.GridGeometry;
import org.apache.sis.storage.GridCoverageResource;
import org.apache.sis.storage.Query;
import org.apache.sis.storage.UnsupportedQueryException;
import org.apache.sis.util.ArgumentChecks;


/**
 * A simple query configuration for coverage resources for requesting a subset of the domain and the range.
 * Experimental for now, may move to public API in a future version.
 *
 * @author  Johann Sorel (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.1
 * @since   1.1
 * @module
 */
public final class CoverageQuery extends Query implements Cloneable {
    /**
     * Desired grid extent and resolution, or {@code null} for reading the whole domain.
     */
    private GridGeometry domain;

    /**
     * 0-based indices of sample dimensions to read, or {@code null} for reading them all.
     */
    private int[] range;

    /**
     * Number of additional cells to read on each border of the source grid coverage.
     * If non-zero, this property expands the {@linkplain #domain} to be read by an amount
     * specified in unit of cells of the image to be read. Those cells do not necessarily
     * have the same size than the cells of <code>{@linkplain #domain}.getExtent()</code>.
     */
    private int sourceDomainExpansion;

    /**
     * Creates a new query performing no filtering.
     */
    public CoverageQuery() {
    }

    /**
     * Sets the desired grid extent and resolution.
     *
     * @param  domain  desired grid extent and resolution, or {@code null} for reading the whole domain.
     */
    public void setDomain(final GridGeometry domain) {
        this.domain = domain;
    }

    /**
     * Returns the desired grid extent and resolution.
     * This is the value set by the last call to {@link #setDomain(GridGeometry)}.
     *
     * @return desired grid extent and resolution, or {@code null} for reading the whole domain.
     */
    public GridGeometry getDomain() {
        return domain;
    }

    /**
     * Sets the indices of samples dimensions to read.
     * A {@code null} value means to read all sample dimensions (no filtering on range).
     * If non-null, then the {@code range} array shall contain at least one element,
     * all elements must be positive and no value can be duplicated.
     *
     * @param  range   0-based indices of sample dimensions to read, or {@code null} for reading them all.
     * @throws IllegalArgumentException if the given array is empty or contains negative or duplicated values.
     */
    @SuppressWarnings("AssignmentToCollectionOrArrayFieldFromParameter")
    public void setRange(int... range) {
        if (range != null) {
            range = range.clone();
            ArgumentChecks.ensureNonEmpty("range", range, 0, Integer.MAX_VALUE, true);
            // Assign only after we verified that the argument is valid.
        }
        this.range = range;
    }

    /**
     * Returns the indices of samples dimensions to read, or {@code null} if there is no filtering on range.
     * If non-null, the returned array shall never be empty.
     *
     * @return 0-based indices of sample dimensions to read, or {@code null} for reading them all.
     */
    public int[] getRange() {
        return (range != null) ? range.clone() : null;
    }

    /**
     * Set a number of additional cells to read on each border of the source grid coverage.
     * If non-zero, this property expands the {@linkplain #getDomain() domain} to be read
     * by an amount specified in unit of cells of the image to be read. Those cells do not
     * necessarily have the same size than the cells of <code>domain.getExtent()</code>.
     *
     * <p>At reading time it may be necessary to add such margin to the coverage extent.
     * This margin is used when the user knows that an image processing operation will
     * need to iterate over a little bit more data than the area of interest.
     * For example the bilinear interpolation uses a 2×2 pixels window.</p>
     *
     * @param  margin  read margin, which must be zero or positive.
     */
    public void setSourceDomainExpansion(final int margin) {
        ArgumentChecks.ensurePositive("margin", margin);
        sourceDomainExpansion = margin;
    }

    /**
     * Returns the number of additional cells to read on each border of the source grid coverage.
     * This is the value sets by the last call to {@link #setSourceDomainExpansion(int)}.
     *
     * @return read margin, zero or positive.
     */
    public int getSourceDomainExpansion() {
        return sourceDomainExpansion;
    }

    /**
     * Applies this query on the given coverage.
     *
     * Current implementation apply the source domain expansion when the source
     * grid geometry extent is defined, otherwise it is ignored.
     *
     * @param  source  the coverage resource to filter.
     * @return a view over the given coverage resource containing only the given domain and range.
     * @throws UnsupportedQueryException if this query contains filtering options not yet supported.
     */
    public GridCoverageResource execute(final GridCoverageResource source) throws UnsupportedQueryException {
        ArgumentChecks.ensureNonNull("source", source);
        return new CoverageSubset(source, clone());
    }

    /**
     * Returns a clone of this query.
     *
     * @return a clone of this query.
     */
    @Override
    public CoverageQuery clone() {
        /*
         * Implementation note: no need to clone the arrays. It is safe to share the same array instances
         * because this class does not modify them and does not return them directly to the user.
         */
        try {
            return (CoverageQuery) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new AssertionError(e);
        }
    }

    /**
     * Returns a hash code value for this query.
     *
     * @return a hash value for this query.
     */
    @Override
    public int hashCode() {
        return 59 * Objects.hashCode(domain) +
               37 *  Arrays.hashCode(range)  +
               31 *  sourceDomainExpansion;
    }

    /**
     * Compares this query with the given object for equality.
     *
     * @param  obj  the object to compare with this query.
     * @return whether the two objects are equal.
     */
    @Override
    public boolean equals(final Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj != null && getClass() == obj.getClass()) {
            final CoverageQuery other = (CoverageQuery) obj;
            return sourceDomainExpansion  == other.sourceDomainExpansion &&
                      Objects.equals(domain, other.domain) &&
                       Arrays.equals(range,  other.range);
        }
        return false;
    }

    /**
     * Returns a textual representation looking like an SQL Select query.
     *
     * @return textual representation of this query.
     */
    @Override
    public String toString() {
        final StringBuffer sb = new StringBuffer(80);
        sb.append("SELECT ");
        if (range != null) {
            sb.append("range[");
            for (int i=0; i<range.length; i++) {
                if (i != 0) sb.append(", ");
                sb.append(range[i]);
            }
            sb.append(']');
        } else {
            sb.append('*');
        }
        if (domain != null) {
            sb.append(" WHERE domain ∩ [");
            final GeographicBoundingBox box = domain.getGeographicExtent().orElse(null);
            if (box != null) {
                final AngleFormat f = new AngleFormat("D°");
                append(sb, f, new Latitude (box.getSouthBoundLatitude()),
                              new Latitude (box.getNorthBoundLatitude()));
                append(sb, f, new Longitude(box.getWestBoundLongitude()),
                              new Longitude(box.getEastBoundLongitude()));
            } else {
                sb.append('?');     // If the domain can not be expressed as a geographic bounding box.
            };
            sb.append(']');
            if (sourceDomainExpansion != 0) {
                sb.append(" + margin(").append(sourceDomainExpansion).append(')');
            }
        }
        return sb.toString();
    }

    /**
     * Formats a range of longitude or latitude values in the pseudo-SQL statement.
     * This is a helper method for {@link #toString()}.
     */
    private static void append(final StringBuffer sb, final AngleFormat f, final Angle start, final Angle end) {
        final double span = Math.abs(end.degrees() - start.degrees()) / 1000;
        f.setPrecision(span > 0 && span < Double.POSITIVE_INFINITY ? span : 0, false);  // Also filter NaN values.
        f.setRoundingMode(RoundingMode.DOWN); f.format(start, sb, null);
        f.setRoundingMode(RoundingMode.UP);   f.format(end,   sb, null);
    }
}
