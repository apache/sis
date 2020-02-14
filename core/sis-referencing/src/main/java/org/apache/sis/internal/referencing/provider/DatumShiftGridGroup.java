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
package org.apache.sis.internal.referencing.provider;

import java.util.Map;
import java.util.List;
import java.nio.file.Path;
import java.io.IOException;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import java.util.LinkedHashMap;
import javax.measure.Quantity;
import org.opengis.util.FactoryException;
import org.opengis.referencing.operation.NoninvertibleTransformException;
import org.apache.sis.referencing.operation.transform.InterpolatedTransform;
import org.apache.sis.internal.referencing.j2d.AffineTransform2D;
import org.apache.sis.internal.referencing.j2d.TileOrganizer;
import org.apache.sis.internal.referencing.j2d.Tile;
import org.apache.sis.internal.referencing.Resources;
import org.apache.sis.internal.util.CollectionsExt;


/**
 * A group of datum shift grids. This is used when a NTv2 file contains more than one grid with no common parent.
 * This class creates a synthetic parent which always delegate its work to a child (as opposed to more classical
 * trees where the parent can do some work if no child can).
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.1
 * @since   1.1
 * @module
 */
final class DatumShiftGridGroup<C extends Quantity<C>, T extends Quantity<T>> extends DatumShiftGridFile<C,T> {
    /**
     * The bounds of a sub-grid, together with the subsampling level compared to the grid having the finest resolution.
     * All values in this class are integers, but nevertheless stored as {@code double} for avoiding to cast them every
     * time {@link #interpolateInCell(double, double, double[])} is executed.
     */
    private static final class Region {
        /** Grid bounds in units of the grid having finest resolution. */
        private final double xmin, xmax, ymin, ymax;

        /** Subsampling compared to the grid having finest resolution. */
        private final double sx, sy;

        /** Creates a new instance from the given {@link TileOrganizer} result. */
        Region(final Tile tile) throws IOException {
            final Rectangle r = tile.getAbsoluteRegion();
            final Dimension s = tile.getSubsampling();
            xmin = r.getMinX();
            xmax = r.getMaxX();
            ymin = r.getMinY();
            ymax = r.getMaxY();
            sx   = s.width;
            sy   = s.height;
        }

        /** Tests whether the given coordinates are included in this region. */
        final boolean contains(final double x, final double y) {
            return x >= xmin && x <= xmax && y >= ymin && y <= ymax;
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
     * For each {@code subgrids[i]}, {@code regions[i]} is the range of indices valid of that grid.
     * This array will be used only as a fallback if the {@code MathTransform} has not been able to
     * find the sub-grid itself. Since it should be rarely used, we do not bother using a R-Tree.
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
     * @param  nx         number of cells along the <var>x</var> axis in the grid.
     * @param  ny         number of cells along the <var>y</var> axis in the grid.
     * @throws IOException declared because {@link Tile#getRegion()} declares it, but should not happen.
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    private DatumShiftGridGroup(final Tile[] tiles, final Map<Tile,DatumShiftGridFile<C,T>> grids,
            final AffineTransform2D gridToCRS, final int nx, final int ny)
            throws IOException, NoninvertibleTransformException
    {
        super(grids.get(tiles[0]), gridToCRS.inverse(), nx, ny);
        final int n = grids.size();
        regions  = new Region[n];
        subgrids = new DatumShiftGridFile[n];
        for (int i=0; i<n; i++) {
            regions [i] = new Region(tiles[i]);
            subgrids[i] = grids.get(tiles[i]);
        }
    }

    /**
     * Puts the given sub-grid in a group. This method infers itself what would be the size
     * of a grid containing all given sub-grids.
     *
     * @param  file  filename to report in case of error.
     * @param  subgrids  the sub-grids to put under a common root.
     * @throws FactoryException if the sub-grid can not be combined in a single mosaic or pyramid.
     * @throws IOException declared because {@link Tile#getRegion()} declares it, but should not happen.
     */
    static <C extends Quantity<C>, T extends Quantity<T>> DatumShiftGridGroup<C,T> create(
            final Path file, final List<DatumShiftGridFile<C,T>> subgrids)
            throws IOException, FactoryException, NoninvertibleTransformException
    {
        final TileOrganizer mosaic = new TileOrganizer(null);
        final Map<Tile,DatumShiftGridFile<C,T>> grids = new LinkedHashMap<>();
        for (final DatumShiftGridFile<C,T> grid : subgrids) {
            final int[] size = grid.getGridSize();
            final Tile tile = new Tile(new Rectangle(size[0], size[1]),
                    (AffineTransform) grid.getCoordinateToGrid().inverse());
            if (mosaic.add(tile)) {                                     // Should never be false, but check anyway.
                if (grids.put(tile, grid) != null) {
                    throw new AssertionError(tile);                     // Should never happen (paranoiac check).
                }
            }
        }
        final Map.Entry<Tile,Tile[]> result = CollectionsExt.singletonOrNull(mosaic.tiles().entrySet());
        if (result == null) {
            throw new FactoryException(Resources.format(Resources.Keys.MisalignedDatumShiftGrid_1, file));
        }
        final Tile global = result.getKey();
        final Rectangle r = global.getRegion();
        return new DatumShiftGridGroup<>(result.getValue(), grids, global.getGridToCRS(), r.width, r.height);
    }

    /**
     * Creates a new grid sharing the same data than an existing grid.
     * This constructor is for {@link #setData(Object[])} usage only.
     */
    private DatumShiftGridGroup(final DatumShiftGridGroup<C,T> other, final DatumShiftGridFile<C,T>[] data) {
        super(other);
        subgrids = data;
        regions  = other.regions;
    }

    /**
     * Returns a new grid with the same geometry than this grid but different data arrays.
     * This method is invoked by {@link #useSharedData()} when it detects that a newly created
     * grid uses the same data than an existing grid. The {@code other} object is the old grid,
     * so we can share existing data.
     */
    @Override
    @SuppressWarnings("unchecked")
    protected final DatumShiftGridFile<C,T> setData(final Object[] other) {
        return new DatumShiftGridGroup<>(this, (DatumShiftGridFile<C,T>[]) other);
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
     * This implementation takes the first sub-grid as a template. The selected grid should not matter
     * since they shall all have the same number of target dimensions.
     */
    @Override
    public int getTranslationDimensions() {
        return subgrids[0].getTranslationDimensions();
    }

    /**
     * Returns the translation stored at the given two-dimensional grid indices for the given dimension.
     * This method is defined for consistency with {@link #interpolateInCell(double, double, double[])}
     * but should never be invoked. The {@link InterpolatedTransform} class will rather invoke the
     * {@code interpolateInCell} method for efficiency.
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
            if (r.contains(gridX, gridY)) {
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
        throw new IndexOutOfBoundsException();
    }

    /**
     * Interpolates the translation to apply for the given two-dimensional grid indices. The result is stored
     * in the given {@code vector} array. This method is invoked only as a fallback if the transform has not
     * been able to use directly one of the child transforms. Consequently this implementation does not need
     * to be very fast.
     *
     * @param  gridX   first grid coordinate of the point for which to get the translation.
     * @param  gridY   second grid coordinate of the point for which to get the translation.
     * @param  vector  a pre-allocated array where to write the translation vector.
     */
    @Override
    public void interpolateInCell(final double gridX, final double gridY, final double[] vector) {
        for (int i=0; i<regions.length; i++) {
            final Region r = regions[i];
            if (r.contains(gridX, gridY)) {
                subgrids[i].interpolateInCell(r.x(gridX), r.y(gridY), vector);
                if (isCellValueRatio()) {
                    for (int dim=0; dim < INTERPOLATED_DIMENSIONS; dim++) {
                        vector[dim] *= r.relativeCellSize(dim);
                    }
                }
                return;
            }
        }
        super.interpolateInCell(gridX, gridY, vector);
    }
}
