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
package org.apache.sis.storage;

import java.util.List;
import java.util.Arrays;
import java.util.Objects;
import java.io.Serializable;
import java.math.RoundingMode;
import org.opengis.util.GenericName;
import org.opengis.util.InternationalString;
import org.opengis.geometry.Envelope;
import org.opengis.metadata.extent.GeographicBoundingBox;
import org.apache.sis.measure.Angle;
import org.apache.sis.measure.Latitude;
import org.apache.sis.measure.Longitude;
import org.apache.sis.measure.AngleFormat;
import org.apache.sis.coverage.SampleDimension;
import org.apache.sis.coverage.grid.GridGeometry;
import org.apache.sis.coverage.grid.GridRoundingMode;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.util.resources.Errors;
import org.apache.sis.util.resources.Vocabulary;


/**
 * Definition of filtering to apply for fetching a subset of {@link GridCoverageResource}.
 * This query allows requesting a subset of the coverage domain and the range.
 *
 * <h2>Terminology</h2>
 * This class uses relational database terminology for consistency with generic queries:
 * <ul>
 *   <li>A <cite>selection</cite> is a filter choosing the cells or pixels to include in the subset.
 *       In this context, the selection is the <cite>coverage domain</cite>.</li>
 *   <li>A <cite>projection</cite> (not to be confused with map projection) is the set of sample values to keep.
 *       In this context, the projection is the <cite>coverage range</cite> (i.e. set of sample dimensions).</li>
 * </ul>
 *
 * <h2>Optional values</h2>
 * All aspects of this query are optional and initialized to "none".
 * Unless otherwise specified, all methods accept a null argument or can return a null value, which means "none".
 *
 * @author  Johann Sorel (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.2
 * @since   1.1
 * @module
 */
public class CoverageQuery extends Query implements Cloneable, Serializable {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = -4296814883807414158L;

    /**
     * Desired grid extent and resolution, or {@code null} for reading the whole domain.
     * This is the "selection" in query terminology.
     */
    private GridGeometry domain;

    /**
     * 0-based indices of sample dimensions to read, or {@code null} for reading them all.
     * This is the "projection" (not to be confused with map projection) in query terminology.
     */
    private int[] range;

    /**
     * The {@linkplain #range} specified by names instead of indices.
     * At most one of {@code range} and {@code rangeNames} shall be non-null.
     */
    private String[] rangeNames;

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
     * Sets the approximate area of cells or pixels to include in the subset.
     * This convenience method creates a grid geometry containing only the given envelope.
     * Note that the given envelope is approximate:
     * Coverages may expand the envelope to an integer amount of tiles.
     *
     * @param  domain  the approximate area of interest, or {@code null} if none.
     */
    @Override
    public void setSelection(final Envelope domain) {
        GridGeometry g = null;
        if (domain != null) {
            g = new GridGeometry(null, null, domain, GridRoundingMode.NEAREST);
        }
        setSelection(g);
    }

    /**
     * Sets the desired grid extent and resolution. The given domain is approximate:
     * Coverages may use a different resolution and expand the envelope to an integer amount of tiles.
     *
     * @param  domain  desired grid extent and resolution, or {@code null} for reading the whole domain.
     */
    public void setSelection(final GridGeometry domain) {
        this.domain = domain;
    }

    /**
     * Returns the desired grid extent and resolution.
     * This is the value set by the last call to {@link #setSelection(GridGeometry)}.
     *
     * <div class="note"><b>Note on terminology:</b>
     * "selection" is the generic term used in queries for designating a subset of feature instances.
     * In a grid coverage, feature instances are cells or pixels.
     * So this concept maps to the <cite>coverage domain</cite>.</div>
     *
     * @return desired grid extent and resolution, or {@code null} for reading the whole domain.
     */
    public GridGeometry getSelection() {
        return domain;
    }

    /**
     * Sets the sample dimensions to read by their names.
     *
     * @param  range  sample dimensions to retrieve, or {@code null} to retrieve all properties.
     * @throws IllegalArgumentException if a sample dimension is duplicated.
     */
    @Override
    @SuppressWarnings("AssignmentToCollectionOrArrayFieldFromParameter")
    public void setProjection(String... range) {
        if (range != null) {
            range = range.clone();
            ArgumentChecks.ensureNonEmpty("range", range);
            // Assign only after we verified that the argument is valid.
        }
        rangeNames = range;
        this.range = null;
    }

    /**
     * Sets the indices of samples dimensions to read (the <cite>coverage range</cite>).
     * A {@code null} value means to read all sample dimensions (no filtering on range).
     * If non-null, then the {@code range} array shall contain at least one element,
     * all elements must be positive and no value can be duplicated.
     *
     * @param  range   0-based indices of sample dimensions to read, or {@code null} for reading them all.
     * @throws IllegalArgumentException if the given array is empty or contains negative or duplicated values.
     */
    @SuppressWarnings("AssignmentToCollectionOrArrayFieldFromParameter")
    public void setProjection(int... range) {
        if (range != null) {
            range = range.clone();
            ArgumentChecks.ensureNonEmpty("range", range, 0, Integer.MAX_VALUE, true);
            // Assign only after we verified that the argument is valid.
        }
        this.range = range;
        rangeNames = null;
    }

    /**
     * Returns the indices of samples dimensions to read, or {@code null} if there is no filtering on range.
     * If non-null, the returned array shall never be empty.
     *
     * <div class="note"><b>Note on terminology:</b>
     * "projection" (not to be confused with map projection) is the generic term used in queries
     * for designating a subset of feature properties retained in each feature instances.
     * In a coverage, this concept maps to the <cite>coverage range</cite>.</div>
     *
     * @return 0-based indices of sample dimensions to read, or {@code null} for reading them all.
     */
    public int[] getProjection() {
        return (range != null) ? range.clone() : null;
    }

    /**
     * Converts the sample dimension names to sample dimension indices.
     * This conversion depends on the resource on which the query will be applied.
     *
     * @param  source  the resource for which to to the conversion.
     */
    private void namesToIndices(final GridCoverageResource source) throws DataStoreException {
        if (rangeNames != null) {
            final List<SampleDimension> sd = source.getSampleDimensions();
            final int numBands = sd.size();
            range = new int[rangeNames.length];
next:       for (int i=0; i<rangeNames.length; i++) {
                final String name = rangeNames[i];
                for (int j=0; j<numBands; j++) {
                    if (name.equals(sd.get(j).getName().toString())) {
                        range[i] = j;
                        continue next;
                    }
                }
                InternationalString id = source.getIdentifier().map(GenericName::toInternationalString)
                            .orElseGet(() -> Vocabulary.formatInternational(Vocabulary.Keys.Unnamed));
                throw new UnsupportedQueryException(Errors.format(Errors.Keys.PropertyNotFound_2, id, name));
            }
            rangeNames = null;
        }
    }

    /**
     * Sets a number of additional cells to read on each border of the source grid coverage.
     * If non-zero, this property expands the {@link #getSelection() domain} to be read
     * by the specified margin.
     *
     * <h4>Unit of measurement</h4>
     * The parameter value is a number of cells in the {@code domain} argument specified
     * in a {@linkplain GridCoverageResource#read(GridGeometry, int...) read operation}.
     * If no {@code domain} is specified at read time, then this is a number of cells in
     * the full image to be read from the resource. Cells are counted after subsampling,
     * e.g. cells are twice bigger if a subsampling of 2 is applied.
     * Those cells do not necessarily have the same size than the cells
     * of the {@link #getSelection() domain of this query}.
     *
     * <h4>Use case</h4>
     * At reading time it may be necessary to add a margin to the coverage extent.
     * This margin is used when the user knows that an image processing operation
     * will need to iterate over a little bit more data than the area of interest.
     * For example the bilinear interpolation uses a 2×2 pixels window.
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
     * Applies this query on the given coverage resource.
     * This method is invoked by the default implementation of {@link GridCoverageResource#subset(Query)}.
     *
     * @param  source  the coverage resource to filter.
     * @return a view over the given coverage resource containing only the given domain and range.
     * @throws DataStoreException if an error occurred during creation of the subset.
     *
     * @see GridCoverageResource#subset(CoverageQuerty)
     * @see FeatureQuery#execute(FeatureSet)
     *
     * @since 1.2
     */
    protected GridCoverageResource execute(final GridCoverageResource source) throws DataStoreException {
        ArgumentChecks.ensureNonNull("source", source);
        final CoverageQuery query = clone();
        query.namesToIndices(source);
        return new CoverageSubset(source, query);
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
     * Returns a textual representation of this query for debugging purposes.
     * The default implementation returns a string that looks like an SQL Select query.
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
