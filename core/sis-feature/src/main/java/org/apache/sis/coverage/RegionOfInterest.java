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
package org.apache.sis.coverage;

import java.awt.Shape;
import java.util.Objects;
import java.io.Serializable;
import org.opengis.util.FactoryException;
import org.opengis.referencing.datum.PixelInCell;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.CoordinateOperation;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.MathTransform2D;
import org.opengis.referencing.operation.TransformException;
import org.apache.sis.referencing.operation.transform.MathTransforms;
import org.apache.sis.referencing.CRS;
import org.apache.sis.coverage.grid.GridGeometry;
import org.apache.sis.util.LenientComparable;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.util.ComparisonMode;
import org.apache.sis.util.Utilities;


/**
 * Region of interest (ROI) for an operation to apply on a coverage.
 *
 * <h2>Multi-threading</h2>
 * Instances of {@code RegionOfInterest} are immutable and thread-safe.
 *
 * <h2>Limitations</h2>
 * Current implementation supports two-dimensional regions only.
 * This restriction will be relaxed progressively in future versions.
 *
 * <p>Current implementation defines ROI using a geometric shape only.
 * Future versions may allow other ways such as mask rasters.</p>
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.2
 * @since   1.2
 * @module
 */
public class RegionOfInterest implements LenientComparable, Serializable {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = -8408578541189424074L;

    /**
     * The region of interest as a geometry. Coordinates are in the CRS given be {@link #crs}.
     */
    private final Shape geometry;

    /**
     * The coordinate reference system of the region of interest,
     * In current version, it shall be a two-dimensional CRS.
     * May be {@code null} if unknown.
     */
    private final CoordinateReferenceSystem crs;

    /**
     * Creates a new region of interest from a two-dimensional shape.
     * If the given CRS is {@code null}, then it will be assumed the same
     * CRS than the CRS of the coverage on which operations are applied.
     *
     * @param  geometry  the ROI as a geometry. Coordinates are in the CRS given by {@code crs}.
     * @param  crs       coordinate reference system of the region of interest, or {@code null}.
     */
    public RegionOfInterest(final Shape geometry, final CoordinateReferenceSystem crs) {
        ArgumentChecks.ensureNonNull("geometry", geometry);
        ArgumentChecks.ensureDimensionMatches("crs", 2, crs);
        this.geometry = geometry;
        this.crs = crs;
    }

    /**
     * Returns the clip geometry in coordinates of grid cells.
     * The target space is specified by a {@code GridGeometry}.
     *
     * <h4>Limitations</h4>
     * In current implementation, the grid geometry most be two-dimensional.
     * This restriction will be relaxed progressively in future versions.
     *
     * @param  target  two-dimensional grid geometry of the target image.
     * @return clip in pixel coordinates of given grid.
     * @throws TransformException if ROI coordinates can not be transformed to grid coordinates.
     */
    public Shape toShape2D(final GridGeometry target) throws TransformException {
        final MathTransform2D crsToGrid;
        try {
            MathTransform tr = target.getGridToCRS(PixelInCell.CELL_CENTER).inverse();
            if (crs != null && target.isDefined(GridGeometry.CRS)) {
                final CoordinateOperation op = CRS.findOperation(crs,
                        target.getCoordinateReferenceSystem(),
                        target.getGeographicExtent().orElse(null));
                tr = MathTransforms.concatenate(op.getMathTransform(), tr);
            }
            crsToGrid = MathTransforms.bidimensional(tr);
        } catch (IllegalArgumentException | FactoryException e) {
            throw new TransformException(e);
        }
        return crsToGrid.createTransformedShape(geometry);
    }

    /**
     * Compares this region of interest with the given object for equality.
     *
     * @param  other  the other object to compare with this ROI.
     * @return whether the given object is equal to this ROI?
     */
    @Override
    public final boolean equals(final Object other) {
        return equals(other, ComparisonMode.STRICT);
    }

    /**
     * Compares this region of interest with the given object for equality.
     *
     * @param  other  the other object to compare with this ROI.
     * @param  mode   the comparison criterion.
     * @return whether the given object is equal to this ROI?
     */
    @Override
    public boolean equals(final Object other, final ComparisonMode mode) {
        if (other instanceof ComparisonMode) {
            final RegionOfInterest that = (RegionOfInterest) other;
            if (mode != ComparisonMode.STRICT || other.getClass() == getClass()) {
                return geometry.equals(that.geometry) && Utilities.deepEquals(crs, that.crs, mode);
            }
        }
        return false;
    }

    /**
     * Returns a hash code value for this region of interest.
     *
     * @return a hash code for this ROI.
     */
    @Override
    public int hashCode() {
        return geometry.hashCode() + Objects.hashCode(crs);
    }
}
