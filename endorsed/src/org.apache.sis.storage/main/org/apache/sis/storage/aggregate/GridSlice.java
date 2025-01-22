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
package org.apache.sis.storage.aggregate;

import java.util.List;
import java.util.Arrays;
import java.awt.image.RenderedImage;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.NoninvertibleTransformException;
import org.apache.sis.storage.GridCoverageResource;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.DataStoreReferencingException;
import org.apache.sis.storage.internal.Resources;
import org.apache.sis.coverage.grid.GridCoverage;
import org.apache.sis.coverage.grid.GridGeometry;
import org.apache.sis.coverage.grid.GridExtent;
import org.apache.sis.coverage.grid.PixelInCell;
import org.apache.sis.image.privy.ReshapedImage;
import org.apache.sis.util.ComparisonMode;
import org.apache.sis.util.logging.Logging;
import org.apache.sis.util.privy.Strings;
import static org.apache.sis.image.privy.ImageUtilities.LOGGER;


/**
 * Wrapper for a grid resource which is a slice in a larger coverage.
 * A slice is not necessarily 1 cell tick, larger slices are accepted.
 *
 * <h2>Usage context</h2>
 * Instances of {@code GridSlice} are grouped by CRS, then instances having the same CRS
 * are grouped by "grid to CRS" transform in the {@link GroupByTransform#members} list.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
final class GridSlice implements Comparable<GridSlice> {
    /**
     * For easier identification of codes that assume two-dimensional slices.
     */
    static final int BIDIMENSIONAL = 2;

    /**
     * The "pixel in cell" value used for all "grid to CRS" computations.
     */
    static final PixelInCell CELL_ANCHOR = PixelInCell.CELL_CORNER;

    /**
     * The order of this slice relative to other slices. The slices with smaller numbers
     * should be rendered on top (foreground) of slices with larger numbers (background).
     *
     * @see #compareTo(GridSlice)
     */
    private final int order;

    /**
     * The resource which will be used for reading the data of this slice.
     * Note that the grid cells may not by in the coordinate system of the
     * concatenated resource grid cells.
     */
    final GridCoverageResource resource;

    /**
     * Extent of this slice in units of the concatenated grid resource.
     * This is the {@linkplain #resource} extent translated by the {@link #offsets} (by subtraction).
     */
    final GridExtent extentInGroup;

    /**
     * Translation for converting coordinates of the group to the coordinate system of this slice.
     * This is a translation from source coordinates of {@link GroupByTransform#gridToCRS} to grid coordinates
     * of the {@linkplain #resource}. Shall be handled as read-only, because potentially shared by many slices.
     */
    private final long[] offset;

    /**
     * Adds a new slice to the group of slices associated to compatible CRS and "grid to CRS" transform.
     * The CRS comparisons ignore metadata and the transform comparisons ignore integer translations.
     * This method takes a synchronization lock on the given list.
     *
     * @param  order     the order of this slice relative to other slices.
     * @param  resource  resource associated to this slice.
     * @param  bySample  the list where to search for a group.
     * @param  strategy  algorithm to apply when more than one grid coverage can be found at the same grid index.
     * @throws NoninvertibleTransformException if the transform is not invertible.
     * @throws ArithmeticException if a translation term is NaN or overflows {@code long} integer capacity.
     */
    GridSlice(final int order,
              final GridCoverageResource resource,
              final GroupBySample bySample,
              final MergeStrategy strategy)
            throws DataStoreException, NoninvertibleTransformException
    {
        this.order    = order;
        this.resource = resource;
        final GridGeometry  geometry  = resource.getGridGeometry();
        final MathTransform gridToCRS = geometry.getGridToCRS(CELL_ANCHOR);
        final MathTransform crsToGrid = gridToCRS.inverse();
        /*
         * Finds the pyramid level where to insert this slice or tile.
         * A pyramid level is a group of similar transforms in a group of CRS.
         */
        @SuppressWarnings("LocalVariableHidesMemberVariable")
        long[] offset;
        GroupByTransform slices;
        final var crs = geometry.isDefined(GridGeometry.CRS) ? geometry.getCoordinateReferenceSystem() : null;
        final GroupByCRS<GroupByTransform> crsGroup = bySample.getOrAdd(crs);
        final List<GroupByTransform> transforms = crsGroup.members;
search: synchronized (transforms) {
            for (int i = transforms.size(); --i >= 0;) {
                slices = transforms.get(i);
                offset = slices.integerTranslation(crsToGrid);
                if (offset != null) {
                    break search;
                }
            }
            offset = new long[geometry.getDimension()];
            slices = new GroupByTransform(crsGroup, geometry, gridToCRS);
            transforms.add(slices);
        }
        slices.strategy = strategy;
        this.offset     = offset = slices.unique(offset);
        extentInGroup   = slices.unique(geometry.getExtent().translate(offset, true));
        final List<GridSlice> addTo = slices.members;
        synchronized (addTo) {
            addTo.add(this);
        }
    }

    /**
     * Creates a slice for the same data as the specified slice, but at a different location or resolution.
     */
    private GridSlice(GridSlice source, GridCoverageResource coverage, GridExtent inGroup, long[] toSlice) {
        order         = source.order;
        resource      = coverage;
        extentInGroup = inGroup;
        offset        = toSlice;
    }

    /**
     * Returns a slice for the result of reading the given source slice.
     *
     * @param  coverage  the coverage which has been read, wrapped in a resource.
     * @param  extent    the extent of the given coverage.
     * @param  inGroup   the extent of this slice in units of the concatenated grid resource.
     * @throws DataStoreReferencingException if this constructor cannot fit the given coverage in the grid mosaic.
     */
    final GridSlice resolve(final GridCoverageResource coverage, final GridExtent extent, GridExtent inGroup)
            throws DataStoreReferencingException
    {
        long[] toSlice = new long[inGroup.getDimension()];
        for (int i = toSlice.length; --i >= 0;) {
            long value = extent .getSize(i);
            long scale = inGroup.getSize(i);    // Higher subsampling → smaller size.
            if (value % scale == 0) {
                scale = value / scale;
                value = extent.getLow(i);
                if (value % scale == 0) {
                    value /= scale;             // Low coordinate with same resolution as `inGroup`.
                    toSlice[i] = Math.subtractExact(inGroup.getLow(i), value);
                    continue;
                }
            }
            throw new DataStoreReferencingException(Resources.format(Resources.Keys.IncompatibleGridGeometry));
        }
        if (Arrays.equals(toSlice, offset)) {
            toSlice = offset;
        }
        if (inGroup.equals(extentInGroup, ComparisonMode.IGNORE_METADATA)) {
            inGroup = extentInGroup;
            if (toSlice == offset && coverage.equals(resource)) {
                return this;
            }
        }
        return new GridSlice(this, coverage, inGroup, toSlice);
    }

    /**
     * Returns an identifier of this slice for formatting error messages.
     * In case of exception, this method pretends that the warning has been logged by
     * {@link ConcatenatedGridCoverage#render(GridExtent)} because it is the caller of this method.
     */
    final Object getIdentifier() {
        try {
            var identifier = resource.getIdentifier();
            if (identifier.isPresent()) {
                return identifier.get();
            }
        } catch (DataStoreException e) {
            Logging.unexpectedException(LOGGER, ConcatenatedGridCoverage.class, "render", e);
        }
        return order;
    }

    /**
     * Renders this slice or tile in an extent specified in coordinates relative to the aggregation.
     * This method translates the request extent to the cell coordinate system of the coverage to load,
     * then translates the result back to the original coordinate system of the given extent.
     *
     * @param  coverage  the grid coverage to render.
     * @param  request   requested extent in units of aggregated grid coverage cells.
     * @param  subdim    an array of length 2 containing the dimensions of <var>x</var> and <var>y</var> axes.
     * @return the rendered image with (0,0) locate at the beginning of the requested extent.
     */
    final RenderedImage render(final GridCoverage coverage, final GridExtent request, final int[] subdim) {
        final RenderedImage image = coverage.render(request.translate(offset));
        final long tx = Math.negateExact(offset[subdim[0]]);
        final long ty = Math.negateExact(offset[subdim[1]]);
        if ((tx | ty) == 0) {
            return image;
        }
        final var translated = new ReshapedImage(image, tx, ty);
        return translated.isIdentity() ? translated.source : translated;
    }

    /**
     * Compares this slice with the given slice for rendering order.
     *
     * @param  other  the other slice to compare with this slice.
     * @return -1 if this slice should be rendered first, +1 if the other slice should be first.
     */
    @Override
    public final int compareTo(final GridSlice other) {
        return Integer.compare(order, other.order);
    }

    /**
     * Returns a string representation for debugging purposes.
     */
    @Override
    public String toString() {
        Object id = null;
        if (resource != null) try {
            id = resource.getIdentifier().orElse(null);
        } catch (DataStoreException e) {
            id = e.toString();
        }
        final long[] location = new long[extentInGroup.getDimension()];
        final long[] size = new long[location.length];
        for (int i=0; i<location.length; i++) {
            location[i] = extentInGroup.getLow(i);
            size[i] = extentInGroup.getSize(i);
        }
        return Strings.toString(getClass(), null, id, "location", location, "size", size, "order", order);
    }
}
