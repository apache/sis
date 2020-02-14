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
import java.awt.Rectangle;
import java.awt.geom.Point2D;
import java.awt.geom.AffineTransform;
import javax.measure.Quantity;
import org.opengis.util.FactoryException;
import org.opengis.referencing.operation.NoninvertibleTransformException;
import org.apache.sis.internal.referencing.j2d.AffineTransform2D;
import org.apache.sis.internal.referencing.j2d.MosaicCalculator;
import org.apache.sis.internal.referencing.j2d.Tile;
import org.apache.sis.internal.referencing.Resources;
import org.apache.sis.internal.util.CollectionsExt;


/**
 * A group of datum shift grids. This is used when a NTv2 file contains more than one grid with no common parent.
 * This class creates a synthetic parent with an affine transform approximating all grids. The affine transform is
 * close to identity transform. Its main purpose is to locate a grid during inverse transforms, before refinements
 * using the real grids.  So a "best match" transform (for example estimated using least squares method) would not
 * be useful because the differences would be small compared to grid cell sizes.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.1
 * @since   1.1
 * @module
 */
final class DatumShiftGridGroup<C extends Quantity<C>, T extends Quantity<T>> extends DatumShiftGridFile<C,T> {
    /**
     * For each {@code subgrids[i]}, {@code regions[i]} is the range of indices valid of that grid.
     *
     * @todo We should replace by an R-Tree. For now we assume that the array is small enough.
     */
    private final Rectangle[] regions;

    /**
     * Converts indices from this grid to indices in sub-grids.
     */
    private final AffineTransform[] toSubGrids;

    /**
     * Creates a new group for the given list of sub-grids. That list shall contain at least 2 elements.
     * The first sub-grid is taken as a template for setting parameter values such as filename (all list
     * elements should declare the same filename parameters, so the selected element should not matter).
     *
     * @param grids      sub-grids with their indices range. The array is declared as {@code Tile[]}
     *                   because this is the type returned by {@link MosaicCalculator#tiles()}, but
     *                   each element shall be an instance of {@link Region}.
     * @param gridToCRS  conversion from grid indices to "real world" coordinates.
     * @param nx         number of cells along the <var>x</var> axis in the grid.
     * @param ny         number of cells along the <var>y</var> axis in the grid.
     * @throws IOException declared because {@link Tile#getRegion()} declares it, but should not happen.
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    private DatumShiftGridGroup(final Tile[] grids, final AffineTransform2D gridToCRS, final int nx, final int ny)
            throws IOException, NoninvertibleTransformException
    {
        super((DatumShiftGridFile<C,T>) ((Region) grids[0]).grid, gridToCRS.inverse(), nx, ny);
        subgrids   = new DatumShiftGridFile[grids.length];
        regions    = new Rectangle[grids.length];
        toSubGrids = new AffineTransform[grids.length];
        for (int i=0; i<grids.length; i++) {
            final Region r = (Region) grids[i];
            final AffineTransform tr = new AffineTransform(gridToCRS);
            tr.preConcatenate((AffineTransform) r.grid.getCoordinateToGrid());
            subgrids  [i] = (DatumShiftGridFile<C,T>) r.grid;
            regions   [i] = r.getAbsoluteRegion();
            toSubGrids[i] = tr;
        }
    }

    /**
     * A sub-grid wrapped with information about the region where it applies.
     * The region is expressed as indices in a larger grid. That larger grid
     * is what {@link MosaicCalculator} will try to infer.
     */
    @SuppressWarnings("serial")
    private static final class Region extends Tile {
        /** The wrapped sub-grid. */
        final DatumShiftGridFile<?,?> grid;

        /**
         * Creates a new wrapper for the given sub-grid.
         *
         * @param size  value of {@link DatumShiftGridFile#getGridSize()}.
         */
        Region(final DatumShiftGridFile<?,?> grid, final int[] size) throws NoninvertibleTransformException {
            super(new Rectangle(size[0], size[1]), (AffineTransform) grid.getCoordinateToGrid().inverse());
            this.grid = grid;
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
        final MosaicCalculator mosaic = new MosaicCalculator(null);
        for (final DatumShiftGridFile<C,T> grid : subgrids) {
            mosaic.add(new Region(grid, grid.getGridSize()));
        }
        final Map.Entry<Tile,Tile[]> result = CollectionsExt.singletonOrNull(mosaic.tiles().entrySet());
        if (result == null) {
            throw new FactoryException(Resources.format(Resources.Keys.MisalignedDatumShiftGrid_1, file));
        }
        final Tile global = result.getKey();
        final Rectangle r = global.getRegion();
        return new DatumShiftGridGroup<>(result.getValue(), global.getGridToCRS(), r.width, r.height);
    }

    /**
     * Creates a new grid sharing the same data than an existing grid.
     * This constructor is for {@link #setData(Object[])} usage only.
     */
    private DatumShiftGridGroup(final DatumShiftGridGroup<C,T> other, final DatumShiftGridFile<C,T>[] data) {
        super(other);
        subgrids   = data;
        regions    = other.regions;
        toSubGrids = other.toSubGrids;
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
     *
     * @param  dim    the dimension of the translation vector component to get.
     * @param  gridX  the grid index on the <var>x</var> axis, from 0 inclusive to {@code gridSize[0]} exclusive.
     * @param  gridY  the grid index on the <var>y</var> axis, from 0 inclusive to {@code gridSize[1]} exclusive.
     * @return the translation for the given dimension in the grid cell at the given index.
     */
    @Override
    public double getCellValue(final int dim, final int gridX, final int gridY) {
        for (int i=0; i<regions.length; i++) {
            final Rectangle r = regions[i];
            if (r.contains(gridX, gridY)) {
                Point2D pt = new Point2D.Double(gridX, gridY);
                pt = toSubGrids[i].transform(pt, pt);
                return subgrids[i].getCellValue(dim,
                        Math.toIntExact(Math.round(pt.getX())),
                        Math.toIntExact(Math.round(pt.getY())));
            }
        }
        throw new IndexOutOfBoundsException();
    }
}
