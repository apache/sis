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
package org.apache.sis.referencing.operation.gridded;

import java.util.Map;
import java.util.List;
import java.io.IOException;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import javax.measure.Quantity;
import org.opengis.util.FactoryException;
import org.opengis.referencing.operation.NoninvertibleTransformException;
import org.apache.sis.referencing.operation.transform.InterpolatedTransform;
import org.apache.sis.referencing.internal.Resources;
import org.apache.sis.referencing.internal.shared.AffineTransform2D;
import org.apache.sis.referencing.internal.shared.IntervalRectangle;
import org.apache.sis.util.collection.Containers;
import org.apache.sis.pending.jdk.JDK19;


/**
 * A group of datum shift grids. This is used when a NTv2 file contains more than one grid with no common parent.
 * This class creates a synthetic parent which always delegates its work to a child (as opposed to more classical
 * transform trees where the parent can do some work if no child can). Coordinate transformations will be applied
 * as below:
 *
 * <ol>
 *   <li>{@link org.apache.sis.referencing.operation.transform.SpecializableTransform} will try to locate the
 *       most appropriate grid for given coordinates. This is the class where to put our optimization efforts,
 *       for example by checking the last used grid before to check all other grids.</li>
 *   <li>Only if {@code SpecializableTransform} did not found a better transform, it will fallback on a transform
 *       backed by this {@code GridGroup}. In such case, {@link InterpolatedTransform} will perform its
 *       calculation by invoking {@link #interpolateInCell(double, double, double[])}. That method tries again to
 *       locate the best grid, but performance is less important there since that method is only a fallback.</li>
 *   <li>The default {@link LoadedGrid#interpolateInCell(double, double, double[])} implementation invokes
 *       {@link #getCellValue(int, int, int)}. We provide that method for consistency, but it should not be invoked
 *       since we overrode {@link #interpolateInCell(double, double, double[])}.</li>
 * </ol>
 *
 * @author  Martin Desruisseaux (Geomatys)
 *
 * @param <C>  dimension of the coordinate unit (usually angular).
 * @param <T>  dimension of the translation unit (usually angular or linear).
 */
public final class GridGroup<C extends Quantity<C>, T extends Quantity<T>> extends LoadedGrid<C,T> {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = -1602724619897451422L;

    /**
     * The bounds of a sub-grid, together with the subsampling level compared to the grid having the finest resolution.
     * All values in this class are integers, but nevertheless stored as {@code double} for avoiding to cast them every
     * time {@link GridGroup#interpolateInCell(double, double, double[])} is executed.
     */
    @SuppressWarnings("CloneableImplementsClone")
    private static final class Region extends IntervalRectangle {
        /** For cross-version compatibility. */
        private static final long serialVersionUID = -2925837396412170681L;

        /** Subsampling compared to the grid having finest resolution. */
        private final double sx, sy;

        /** Creates a new instance from the given {@link TileOrganizer} result. */
        Region(final Tile tile) throws IOException {
            final Rectangle r = tile.getRegionOnFinestLevel();      // In units of the grid having finest resolution.
            final Dimension s = tile.getSubsampling();
            xmin = r.getMinX();
            xmax = r.getMaxX();
            ymin = r.getMinY();
            ymax = r.getMaxY();
            sx   = s.width;
            sy   = s.height;
        }

        /** Converts a coordinate from the parent grid to this grid. */
        final double x(final double p) {return (p - xmin) / sx;}
        final double y(final double p) {return (p - ymin) / sy;}

        /** Returns the subsampling (compared to the grid having finest resolution) in the specified dimension. */
        final double relativeCellSize(final int dim) {
            switch (dim) {
                case 0:  return sx;
                case 1:  return sy;
                default: throw new IndexOutOfBoundsException();
            }
        }
    }

    /**
     * For each {@code subgrids[i]}, {@code regions[i]} is the range of indices valid for that grid.
     * This array will be used only as a fallback if {@code SpecializableTransform} has not been able
     * to find the sub-grid itself. Since it should be rarely used, we do not bother using a R-Tree.
     */
    private final Region[] regions;

    /**
     * Creates a new group for the given list of sub-grids. That list shall contain at least 2 elements.
     * The first sub-grid is taken as a template for setting parameter values such as filename (all list
     * elements should declare the same filename parameters, so the selected element should not matter).
     *
     * @param  tiles      the tiles computed by {@link TileOrganizer}.
     * @param  grids      sub-grids associated to tiles computed by {@link TileOrganizer}.
     * @param  gridToCRS  conversion from grid indices to "real world" coordinates.
     * @param  gridSize   number of cells along the <var>x</var> and <var>y</var> axes in the grid.
     * @throws IOException declared because {@link Tile#getRegion()} declares it, but should not happen.
     */
    @SuppressWarnings({"rawtypes", "unchecked"})                        // For generic array creation.
    private GridGroup(final Tile[] tiles,
                      final Map<Tile,LoadedGrid<C,T>> grids,
                      final AffineTransform2D gridToCRS,
                      final Dimension gridSize)
            throws IOException, NoninvertibleTransformException
    {
        super(grids.get(tiles[0]), gridToCRS, gridSize.width, gridSize.height);
        final int n = grids.size();
        regions  = new Region[n];
        subgrids = new LoadedGrid[n];
        for (int i=0; i<n; i++) {
            final Tile tile = tiles[i];
            final LoadedGrid<C,T> grid = grids.get(tile);
            regions [i] = new Region(tile);
            subgrids[i] = grid;
            if (grid.accuracy > accuracy) {
                accuracy = grid.accuracy;           // Conservatively set accuracy to the largest value.
            }
        }
    }

    /**
     * Puts the given sub-grid in a group. This method infers itself what would be the size
     * of a grid containing all given sub-grids.
     *
     * @param  file      filename to report in case of error.
     * @param  subgrids  the sub-grids to put under a common root.
     * @throws FactoryException if the sub-grid cannot be combined in a single mosaic or pyramid.
     * @throws IOException declared because {@link Tile#getRegion()} declares it, but should not happen.
     */
    public static <C extends Quantity<C>, T extends Quantity<T>> GridGroup<C,T> create(
            final GridFile file, final List<LoadedGrid<C,T>> subgrids)
            throws IOException, FactoryException, NoninvertibleTransformException
    {
        final TileOrganizer mosaic = new TileOrganizer(null);
        final Map<Tile,LoadedGrid<C,T>> grids = JDK19.newLinkedHashMap(subgrids.size());
        for (final LoadedGrid<C,T> grid : subgrids) {
            final int[] size = grid.getGridSize();
            final Tile  tile = new Tile(new Rectangle(size[0], size[1]),
                    (AffineTransform) grid.getCoordinateToGrid().inverse());
            /*
             * Assertions below would fail if the tile has already been processed by TileOrganizer,
             * or if it duplicates another tile. Since we created that tile just above, a failure
             * would be a bug in Tile or TileOrganizer.
             */
            if (!mosaic.add(tile) || grids.put(tile, grid) != null) {
                throw new AssertionError(tile);
            }
        }
        /*
         * After processing by TileOrganizer, we should have only one group of tiles. If we have more groups,
         * it would mean that the cell size of the grid having larger cells is not a multiple of cell size of
         * the grid having smallest cells, or that cell indices in some grids, when expressed in units of the
         * smallest cells, would be fractional numbers. It should not happen in a NTv2 compliant file.
         */
        final Map.Entry<Tile, Tile[]> result = Containers.peekIfSingleton(mosaic.tiles().entrySet());
        if (result == null) {
            throw new FactoryException(Resources.format(Resources.Keys.MisalignedDatumShiftGrid_1, file.parameter));
        }
        final Tile global = result.getKey();
        return new GridGroup<>(result.getValue(), grids, global.getGridToCRS(), global.getSize());
    }

    /**
     * Creates a new grid sharing the same data as an existing grid.
     * This constructor is for {@link #setData(Object[])} usage only.
     */
    private GridGroup(final GridGroup<C,T> other, final LoadedGrid<C,T>[] data) {
        super(other);
        subgrids = data;
        regions  = other.regions;
    }

    /**
     * Returns a new grid with the same geometry as this grid but different data arrays.
     * This method is invoked by {@link #useSharedData()} when it detects that a newly created
     * grid uses the same data as an existing grid. The {@code other} object is the old grid,
     * so we can share existing data.
     */
    @Override
    @SuppressWarnings("unchecked")
    protected final LoadedGrid<C,T> setData(final Object[] other) {
        return new GridGroup<>(this, (LoadedGrid<C,T>[]) other);
    }

    /**
     * Returns direct references (not cloned) to the data arrays. This method is for cache management,
     * {@link #equals(Object)} and {@link #hashCode()} implementations only and should not be invoked
     * in other context.
     */
    @Override
    @SuppressWarnings("ReturnOfCollectionOrArrayField")
    protected Object[] getData() {
        return subgrids;
    }

    /**
     * Returns the number of dimensions of the translation vectors interpolated by this datum shift grid.
     * This implementation takes the first sub-grid as a template. The choice of the grid does not matter
     * since all grids have the same number of target dimensions.
     */
    @Override
    public int getTranslationDimensions() {
        return subgrids[0].getTranslationDimensions();
    }

    /**
     * Returns the translation stored at the given two-dimensional grid indices for the given dimension.
     * This method is defined for consistency with {@link #interpolateInCell(double, double, double[])}
     * but should never be invoked. The {@link InterpolatedTransform} class will rather invoke the
     * {@code interpolateInCell(…)} method for efficiency.
     *
     * <p>Caller must ensure that all arguments given to this method are in their expected ranges.
     * The behavior of this method is undefined if any argument value is out-of-range.</p>
     *
     * @param  dim    the dimension of the translation vector component to get.
     * @param  gridX  the grid index on the <var>x</var> axis, from 0 inclusive to {@code gridSize[0]} exclusive.
     * @param  gridY  the grid index on the <var>y</var> axis, from 0 inclusive to {@code gridSize[1]} exclusive.
     * @return the translation for the given dimension in the grid cell at the given index.
     */
    @Override
    public double getCellValue(final int dim, final int gridX, final int gridY) {
        for (int i=0; i<regions.length; i++) {
            final Region r = regions[i];
            if (r.containsInclusive(gridX, gridY)) {
                double shift = subgrids[i].getCellValue(dim,
                        Math.toIntExact(Math.round(r.x(gridX))),
                        Math.toIntExact(Math.round(r.y(gridY))));
                /*
                 * If the translations have been divided by the cell size, we may need to compensate.
                 * The size of the cells of the grid used below may be bigger than the cells of this
                 * pseudo-grid.
                 */
                if (isCellValueRatio()) {
                    shift *= r.relativeCellSize(dim);
                }
                return shift;
            }
        }
        /*
         * May be in the valid range of this GridGroup but not in the range of a subgrid.
         * This situation may happen if there is holes in the data coverage provided by subgrids.
         */
        throw new IllegalArgumentException();
    }

    /**
     * Interpolates the translation to apply for the given two-dimensional grid indices.
     * During forward coordinate transformations, this method is invoked only if {@code SpecializableTransform}
     * has been unable to use directly one of the child transforms — so performance is not the priority in that
     * situation. During inverse transformations, this method is invoked for estimating an initial position before
     * iterative refinements. The given point may be outside all sub-grids (otherwise {@code SpecializableTransform}
     * would have done the work itself at least in the forward transformation case). Consequently, searching a sub-grid
     * containing the given point is not sufficient; we need to search for the nearest grid even if the point is outside.
     *
     * @param  gridX   first grid coordinate of the point for which to get the translation.
     * @param  gridY   second grid coordinate of the point for which to get the translation.
     * @param  vector  a pre-allocated array where to write the translation vector.
     */
    @Override
    public void interpolateInCell(final double gridX, final double gridY, final double[] vector) {
        int ni = 0;
        Region nearest = regions[ni];
        double dmin = nearest.distanceSquared(gridX, gridY);
        for (int i=1; i<regions.length; i++) {
            final Region r = regions[i];
            final double d = r.distanceSquared(gridX, gridY);
            if (d < dmin) {
                dmin     = d;
                nearest  = r;
                ni       = i;
                if (d == 0) break;
            }
        }
        subgrids[ni].interpolateInCell(nearest.x(gridX), nearest.y(gridY), vector);
        if (isCellValueRatio()) {
            for (int dim=0; dim < INTERPOLATED_DIMENSIONS; dim++) {
                vector[dim] *= nearest.relativeCellSize(dim);
            }
        }
    }
}
