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
package org.apache.sis.internal.netcdf;

import java.util.List;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.Collections;
import java.io.IOException;
import org.opengis.util.FactoryException;
import org.opengis.referencing.datum.PixelInCell;
import org.opengis.referencing.operation.Matrix;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.MathTransformFactory;
import org.opengis.referencing.cs.CoordinateSystem;
import org.opengis.referencing.crs.SingleCRS;
import org.opengis.referencing.crs.GeographicCRS;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.metadata.spatial.DimensionNameType;
import org.apache.sis.internal.metadata.AxisDirections;
import org.apache.sis.referencing.operation.matrix.Matrices;
import org.apache.sis.referencing.operation.builder.LocalizationGridBuilder;
import org.apache.sis.referencing.CRS;
import org.apache.sis.coverage.grid.GridExtent;
import org.apache.sis.coverage.grid.GridGeometry;
import org.apache.sis.coverage.grid.IllegalGridGeometryException;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.util.NullArgumentException;
import org.apache.sis.util.ArraysExt;


/**
 * Information about the grid geometry and the conversion from grid coordinates to geodetic coordinates.
 * A grid is associated to all variables that are georeferenced coverages and the same grid may be shared
 * by many variables. The {@linkplain #getSourceDimensions() number of source dimensions} is normally the
 * number of {@linkplain Variable#getGridDimensions() netCDF dimensions in the variable}, but may be less
 * if a variable dimensions should considered as bands instead than spatio-temporal dimensions.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.0
 *
 * @see Decoder#getGrids()
 *
 * @since 0.3
 * @module
 */
public abstract class Grid extends NamedElement {
    /**
     * Minimal number of dimension for accepting a variable as a coverage variable.
     */
    public static final int MIN_DIMENSION = 2;

    /**
     * Minimal number of cells along {@value #MIN_DIMENSION} dimensions for accepting a variable as a coverage variable.
     */
    static final int MIN_SPAN = 2;

    /**
     * The axes, created when first needed.
     * The ordering of axes is based on the order in which dimensions are declared for variables using this grid.
     * This is not necessarily the same order than the order in which variables are listed in the netCDF file.
     *
     * @see #getAxes(Decoder)
     */
    private Axis[] axes;

    /**
     * The coordinate reference system, created when first needed.
     * May be {@code null} even after we attempted to create it.
     *
     * @see #getCoordinateReferenceSystem(Decoder)
     */
    private CoordinateReferenceSystem crs;

    /**
     * Whether we determined the {@link #crs} value, which may be {@code null}.
     */
    private boolean isCRSDetermined;

    /**
     * The geometry of this grid (its extent, its CRS and its conversion to the CRS).
     * May be {@code null} even after we attempted to create it.
     */
    private GridGeometry geometry;

    /**
     * Whether we determined the {@link #geometry} value, which may be {@code null}.
     */
    private boolean isGeometryDetermined;

    /**
     * Constructs a new grid geometry information.
     */
    protected Grid() {
    }

    /**
     * Returns a localization grid having the same dimensions than this grid but in a different order.
     * This method is invoked by {@link Variable#getGrid} when the localization grids created by
     * {@link Decoder} subclasses are not sufficient and must be tailored for a particular variable.
     * Subclasses shall verify that the given {@code dimensions} array met the following conditions:
     *
     * <ul>
     *   <li>The length of the given array is equal or greater than {@link #getSourceDimensions()}.</li>
     *   <li>The array contains all elements contained in {@link #getDimensions()}.
     *       Additional elements, if any, are ignored (they may be considered as bands by the caller).</li>
     * </ul>
     *
     * If elements in the given array are in same order than elements in {@link #getDimensions()} list,
     * then this method returns {@code this}. If the given array does not contain all grid dimensions,
     * then this method returns {@code null}. Otherwise a grid with reordered dimensions is returned.
     * It is caller's responsibility to verify if the grid has less dimensions than the given argument.
     *
     * @param  dimensions  the desired dimensions, in order. May contain more dimensions than this grid.
     * @return localization grid with the exact same set of dimensions than this grid (no more and no less),
     *         but in the order specified by the given array (ignoring dimensions not in this grid).
     *         May be {@code this} or {@code null}.
     */
    protected abstract Grid forDimensions(Dimension[] dimensions);

    /**
     * Returns the number of dimensions of source coordinates in the <cite>"grid to CRS"</cite> conversion.
     * This is the number of dimensions of the <em>grid</em>.
     * It should be equal to the size of {@link #getDimensions()} list.
     *
     * @return number of grid dimensions.
     */
    public abstract int getSourceDimensions();

    /**
     * Returns the number of dimensions of target coordinates in the <cite>"grid to CRS"</cite> conversion.
     * This is the number of dimensions of the <em>coordinate reference system</em>.
     * It should be equal to the length of the array returned by {@link #getAxes(Decoder)},
     * but caller should be robust to inconsistencies.
     *
     * @return number of CRS dimensions.
     */
    public abstract int getTargetDimensions();

    /**
     * Returns the dimensions of this grid, in netCDF (reverse of "natural") order. Each element in the list
     * contains the number of cells in the dimension, together with implementation-specific information.
     * The list length should be equal to {@link #getSourceDimensions()}.
     *
     * <p>This list is usually equal to the {@link Variable#getGridDimensions()} list for all variables
     * that are {@linkplain Variable#getGrid associated to this grid}. But those lists can also differ
     * in the following aspects:</p>
     *
     * <ul>
     *   <li>This grid may have less dimensions than the variable using this grid. In such case the additional
     *       dimensions in the variable can be considered as bands instead than spatiotemporal dimensions.</li>
     *   <li>The dimensions in this grid may have a different {@linkplain Dimension#length() length} than the
     *       dimensions in the variable. In such case {@link Variable#getGridGeometry()} is responsible for
     *       concatenating a scale factor to the "grid to CRS" transform.</li>
     * </ul>
     *
     * @return the source dimensions of this grid, in netCDF order.
     *
     * @see Variable#getGridDimensions()
     */
    protected abstract List<Dimension> getDimensions();

    /**
     * Returns the axes of the coordinate reference system. The size of this array is expected equals to the
     * value returned by {@link #getTargetDimensions()}, but the caller should be robust to inconsistencies.
     * The axis order is CRS order (reverse of netCDF order) for consistency with the common practice in the
     * {@code "coordinates"} attribute.
     *
     * <p>This method returns a direct reference to the cached array; do not modify.</p>
     *
     * @param  decoder  the decoder, given in case this method needs to create axes.
     * @return the CRS axes, in "natural" order (reverse of netCDF order).
     * @throws IOException if an I/O operation was necessary but failed.
     * @throws DataStoreException if a logical error occurred.
     * @throws ArithmeticException if the size of an axis exceeds {@link Integer#MAX_VALUE}, or other overflow occurs.
     */
    @SuppressWarnings("ReturnOfCollectionOrArrayField")
    public final Axis[] getAxes(final Decoder decoder) throws IOException, DataStoreException {
        if (axes == null) {
            axes = createAxes(decoder);
            /*
             * The grid dimension which varies fastest should be first.  The code below will swap axes if needed in order to
             * achieve that goal, except if a previous axis was already using the same order. We avoid collision only in the
             * first dimension because it is the one used by metadata and by trySetTransform(…).
             */
            final Axis[] workspace = new Axis[axes.length];
            int i = 0, deferred = workspace.length;
            for (final Axis axis : axes) {
                // Put one-dimensional axes first, all other axes last.
                workspace[axis.getDimension() <= 1 ? i++ : --deferred] = axis;
            }
            deferred = workspace.length;        // Will become index of the first axis whose examination has been deferred.
            while (i < workspace.length) {      // Start the loop at the first n-dimensional axis (n > 1).
                final Axis axis = workspace[i];
                /*
                 * If an axis has a "wraparound" range (for example a longitude axis where the next value after +180°
                 * may be -180°), we will examine it last. The reason is that if a wraparound occurs in the middle of
                 * the localization grid, it will confuse the computation based on 'coordinateForAxis(…)' calls below.
                 * We are better to resolve the latitude axis first, and then resolve the longitude axis with the code
                 * path checking for dimension collisions, without using coordinateForAxis(…) on longitude axis.
                 */
                if (i < deferred && axis.isWraparound()) {
                    System.arraycopy(workspace, i+1, workspace, i, --deferred - i);
                    workspace[deferred] = axis;
                } else {
                    axis.mainDimensionFirst(workspace, i);
                    i++;
                }
            }
        }
        return axes;
    }

    /**
     * Creates the axes to be returned by {@link #getAxes(Decoder)}. This method is invoked only once when first needed.
     * Axes shall be returned in the order they will appear in the Coordinate Reference System.
     *
     * @param  decoder  the decoder of the netCDF file from which to create axes.
     * @return the CRS axes, in "natural" order (reverse of CRS order).
     * @throws IOException if an I/O operation was necessary but failed.
     * @throws DataStoreException if a logical error occurred.
     * @throws ArithmeticException if the size of an axis exceeds {@link Integer#MAX_VALUE}, or other overflow occurs.
     */
    protected abstract Axis[] createAxes(Decoder decoder) throws IOException, DataStoreException;

    /**
     * Returns {@code true} if this grid contains all axes of the specified names. This is used for filtering
     * coordinate systems according the axes specified by {@link Convention#namesOfAxisVariables(Variable)}.
     * If the given array is null, then no filtering is applied and this method returns {@code true}.
     * If the grid contains more axes than the named ones, then the additional axes are ignored.
     *
     * @param  axisNames  name of axes to test for inclusion, or {@code null} for no filtering.
     * @return whether this grid contains at least all the names axes.
     */
    protected abstract boolean containsAllNamedAxes(String[] axisNames);

    /**
     * Returns the coordinate reference system, or {@code null} if none.
     * This method creates the CRS the first time it is invoked and cache the result.
     *
     * @param   decoder  the decoder for which CRS are constructed.
     * @return  the CRS for this grid geometry, or {@code null}.
     * @throws  IOException if an I/O operation was necessary but failed.
     * @throws  DataStoreException if the CRS can not be constructed.
     */
    public final CoordinateReferenceSystem getCoordinateReferenceSystem(final Decoder decoder) throws IOException, DataStoreException {
        if (!isCRSDetermined) try {
            isCRSDetermined = true;                             // Set now for avoiding new attempts if creation fail.
            final List<CRSBuilder<?,?>> builders = new ArrayList<>();
            for (final Axis axis : getAxes(decoder)) {
                CRSBuilder.dispatch(builders, axis);
            }
            final SingleCRS[] components = new SingleCRS[builders.size()];
            for (int i=0; i < components.length; i++) {
                components[i] = builders.get(i).build(decoder);
            }
            switch (components.length) {
                case 0:  break;                                 // Leave 'crs' to null.
                case 1:  crs = components[0]; break;
                default: crs = decoder.getCRSFactory().createCompoundCRS(
                                        Collections.singletonMap(CoordinateSystem.NAME_KEY, getName()), components);
            }
        } catch (FactoryException | NullArgumentException ex) {
            canNotCreate(decoder, "getCoordinateReferenceSystem", Resources.Keys.CanNotCreateCRS_3, ex);
        }
        return crs;
    }

    /**
     * Builds the grid extent if the shape is available. The shape may not be available
     * if a dimension has unlimited length. The dimension names are informative only.
     *
     * @param  axes  value of {@link #getAxes(Decoder)}. Element order does not matter for this method.
     * @return the extent, or {@code null} if not available.
     */
    @SuppressWarnings("fallthrough")
    private GridExtent getExtent(final Axis[] axes) {
        final List<Dimension> dimensions = getDimensions();
        final int n = dimensions.size();
        final long[] high = new long[n];
        for (int i=0; i<n; i++) {
            final long length = dimensions.get(i).length();
            if (length <= 0) return null;
            high[(n-1) - i] = length;
        }
        final DimensionNameType[] names = new DimensionNameType[n];
        switch (n) {
            default: names[1] = DimensionNameType.ROW;      // Fall through
            case 1:  names[0] = DimensionNameType.COLUMN;   // Fall through
            case 0:  break;
        }
        for (final Axis axis : axes) {
            if (axis.getDimension() == 1) {
                final DimensionNameType name;
                if (AxisDirections.isVertical(axis.direction)) {
                    name = DimensionNameType.VERTICAL;
                } else if (AxisDirections.isTemporal(axis.direction)) {
                    name = DimensionNameType.TIME;
                } else {
                    continue;
                }
                int dim = axis.sourceDimensions[0];
                dim = names.length - 1 - dim;               // Convert netCDF order to "natural" order.
                if (dim >= 0) names[dim] = name;
            }
        }
        return new GridExtent(names, null, high, false);
    }

    /**
     * Returns an object containing the grid size, the CRS and the conversion from grid indices to CRS coordinates.
     * {@code GridGeometry} is the public object exposed to users. It uses the dimensions given by axes, which are
     * usually the same dimensions than the ones of the variable using this grid geometry but not always.
     * Caller may need to call {@link GridExtent#resize(long...)} for adjusting.
     *
     * @param   decoder   the decoder for which grid geometries are constructed.
     * @return  the public grid geometry (may be {@code null}).
     * @throws  IOException if an I/O operation was necessary but failed.
     * @throws  DataStoreException if the CRS can not be constructed.
     */
    final GridGeometry getGridGeometry(final Decoder decoder) throws IOException, DataStoreException {
        if (!isGeometryDetermined) try {
            isGeometryDetermined = true;                    // Set now for avoiding new attempts if creation fail.
            final Axis[] axes = getAxes(decoder);           // In CRS order (reverse of netCDF order).
            /*
             * Creates the "grid to CRS" transform. The number of columns is the number of dimensions in the grid
             * (the source) +1, and the number of rows is the number of dimensions in the CRS (the target) +1.
             * The order of dimensions in the transform is the reverse of the netCDF dimension order.
             */
            int lastSrcDim = getSourceDimensions();                         // Will be decremented later, then kept final.
            int lastTgtDim = getTargetDimensions();
            final int[] deferred = new int[axes.length];                    // Indices of axes that have been deferred.
            final List<MathTransform> nonLinears = new ArrayList<>(axes.length);
            final Matrix affine = Matrices.createZero(lastTgtDim + 1, lastSrcDim + 1);
            affine.setElement(lastTgtDim--, lastSrcDim--, 1);
            for (int tgtDim=0; tgtDim < axes.length; tgtDim++) {
                if (!axes[tgtDim].trySetTransform(affine, lastSrcDim, tgtDim, nonLinears)) {
                    deferred[nonLinears.size() - 1] = tgtDim;
                }
            }
            /*
             * If we have not been able to set some coefficients in the matrix (because some transforms are non-linear),
             * set a single scale factor to 1 in the matrix row. The coefficient that we set to 1 is the one for the source
             * dimension which is not already taken by another row. If we have choice, we give preference to the dimension
             * which seems most closely oriented toward axis direction (i.e. the first element in axis.sourceDimensions).
             *
             * Example: if the 'axes' array contains (longitude, latitude) in that order, and if the longitude axis said
             * that its preferred dimension is 1 (after conversion to "natural" order) while the latitude axis said that
             * its preferred dimension is 0, then we build the following matrix:
             *
             *    ┌         ┐
             *    │ 0  1  0 │   axes[0] (longitude), preferred grid dimension = 1
             *    │ 1  0  0 │   axes[1] (latitude),  preferred grid dimension = 0
             *    │ 0  0  1 │
             *    └         ┘
             *
             * The preferred grid dimensions are stored in the 'sourceDimensions' array. In above example this is {1, 0}.
             */
            final int[] sourceDimensions = new int[nonLinears.size()];
            Arrays.fill(sourceDimensions, -1);
            for (int i=0; i<sourceDimensions.length; i++) {
                final int tgtDim = deferred[i];
                final Axis axis = axes[tgtDim];
findFree:       for (int srcDim : axis.sourceDimensions) {                      // In preference order (will take only one).
                    srcDim = lastSrcDim - srcDim;                               // Convert netCDF order to "natural" order.
                    for (int j=affine.getNumRow(); --j>=0;) {
                        if (affine.getElement(j, srcDim) != 0) {
                            continue findFree;
                        }
                    }
                    sourceDimensions[i] = srcDim;
                    affine.setElement(tgtDim, srcDim, 1);
                    break;
                }
            }
            /*
             * Search for non-linear transforms not yet constructed. It may be because the transform requires a
             * two-dimensional localization grid. Those transforms require two variables, i.e. "two-dimensional"
             * axes come in pairs.
             */
            final MathTransformFactory factory = decoder.getMathTransformFactory();
            for (int i=0; i<nonLinears.size(); i++) {         // Length of 'nonLinears' may change in this loop.
                if (nonLinears.get(i) == null) {
                    for (int j=i; ++j < nonLinears.size();) {
                        if (nonLinears.get(j) == null) {
                            final Axis[] gridAxes = new Axis[] {
                                axes[deferred[i]],
                                axes[deferred[j]]
                            };
                            final int srcDim   = sourceDimensions[i];
                            final int otherDim = sourceDimensions[j];
                            switch (srcDim - otherDim) {
                                case -1: break;
                                case +1: ArraysExt.swap(gridAxes, 0, 1); break;
                                default: continue;            // Needs axes at consecutive source dimensions.
                            }
                            final LocalizationGridBuilder grid = gridAxes[0].createLocalizationGrid(gridAxes[1]);
                            if (grid != null) {
                                /*
                                 * Replace the first transform by the two-dimensional localization grid and
                                 * remove the other transform. Removals need to be done in arrays too.
                                 */
                                Linearizer.applyTo(grid, gridAxes);
                                nonLinears.set(i, grid.create(factory));
                                nonLinears.remove(j);
                                final int n = nonLinears.size() - j;
                                System.arraycopy(deferred,         j+1, deferred,         j, n);
                                System.arraycopy(sourceDimensions, j+1, sourceDimensions, j, n);
                                if (otherDim < srcDim) {
                                    sourceDimensions[i] = otherDim;         // Index of the first dimension.
                                }
                                break;                                      // Continue the 'i' loop.
                            }
                        }
                    }
                }
            }
            /*
             * Final transform, as the concatenation of the non-linear transforms followed by the affine transform.
             * We concatenate the affine transform last because it may change axis order.
             */
            MathTransform gridToCRS = null;
            final int nonLinearCount = nonLinears.size();
            nonLinears.add(factory.createAffineTransform(affine));
            for (int i=0; i <= nonLinearCount; i++) {
                MathTransform tr = nonLinears.get(i);
                if (tr != null) {
                    if (i < nonLinearCount) {
                        final int srcDim = sourceDimensions[i];
                        tr = factory.createPassThroughTransform(srcDim, tr,
                                        (lastSrcDim + 1) - (srcDim + tr.getSourceDimensions()));
                    }
                    gridToCRS = (gridToCRS == null) ? tr : factory.createConcatenatedTransform(gridToCRS, tr);
                }
            }
            /*
             * From CF-Convention: "If bounds are not provided, an application might reasonably assume the gridpoints
             * to be at the centers of the cells, but we do not require that in this standard". We nevertheless check
             * if an axis thinks otherwise.
             */
            PixelInCell anchor = PixelInCell.CELL_CENTER;
            final CoordinateReferenceSystem crs = getCoordinateReferenceSystem(decoder);
            if (CRS.getHorizontalComponent(crs) instanceof GeographicCRS) {
                for (final Axis axis : axes) {
                    if (axis.isCellCorner()) {
                        anchor = PixelInCell.CELL_CORNER;
                        break;
                    }
                }
            }
            geometry = new GridGeometry(getExtent(axes), anchor, gridToCRS, crs);
        } catch (FactoryException | IllegalGridGeometryException ex) {
            canNotCreate(decoder, "getGridGeometry", Resources.Keys.CanNotCreateGridGeometry_3, ex);
        }
        return geometry;
    }

    /**
     * Logs a warning about a CRS or grid geometry that can not be created.
     */
    private void canNotCreate(final Decoder decoder, final String caller, final short key, final Exception ex) {
        warning(decoder.listeners, Grid.class, caller, ex, null, key, decoder.getFilename(), getName(), ex.getLocalizedMessage());
    }
}
