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
package org.apache.sis.storage.netcdf.base;

import java.util.List;
import java.util.Arrays;
import java.util.ArrayList;
import java.io.IOException;
import org.opengis.util.FactoryException;
import org.opengis.referencing.operation.Matrix;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.MathTransformFactory;
import org.opengis.referencing.operation.TransformException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.metadata.spatial.DimensionNameType;
import org.apache.sis.referencing.internal.shared.AxisDirections;
import org.apache.sis.referencing.operation.matrix.Matrices;
import org.apache.sis.referencing.operation.builder.LocalizationGridException;
import org.apache.sis.coverage.grid.PixelInCell;
import org.apache.sis.coverage.grid.GridExtent;
import org.apache.sis.coverage.grid.GridGeometry;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.netcdf.internal.Resources;
import org.apache.sis.util.Exceptions;
import org.apache.sis.util.ArraysExt;


/**
 * Information about the grid geometry and the conversion from grid coordinates to geodetic coordinates.
 * A grid is associated to all variables that are georeferenced coverages and the same grid may be shared
 * by many variables. The {@linkplain #getSourceDimensions() number of source dimensions} is normally the
 * number of {@linkplain Variable#getGridDimensions() netCDF dimensions in the variable}, but may be less
 * if a variable dimensions should considered as bands instead of spatiotemporal dimensions.
 *
 * @author  Martin Desruisseaux (Geomatys)
 *
 * @see Decoder#getGridCandidates()
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
     * This is not necessarily the same order as the order in which variables are listed in the netCDF file.
     *
     * @see #getAxes(Decoder)
     */
    private Axis[] axes;

    /**
     * The coordinate reference system, created when first needed.
     * May be {@code null} even after we attempted to create it.
     *
     * @see #getCoordinateReferenceSystem(Decoder, List, List, Matrix)
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
     * Whether we computed a "grid to CRS" transform relative to pixel center or pixel corner.
     * CF-Convention said: "If bounds are not provided, an application might reasonably assume
     * the grid points to be at the centers of the cells, but we do not require that in this
     * standard".
     *
     * @see #getAnchor()
     */
    private PixelInCell anchor = PixelInCell.CELL_CENTER;

    /**
     * Constructs a new grid geometry information.
     */
    protected Grid() {
    }

    /**
     * Returns a localization grid having the same dimensions as this grid but in a different order.
     * This method is invoked by {@link Variable#findGrid(GridAdjustment)} when the localization grids created by
     * {@link Decoder} subclasses are not sufficient and must be tailored for a particular variable.
     * Subclasses shall verify that the given {@code dimensions} array met the following conditions:
     *
     * <ul>
     *   <li>The length of the given array is equal or greater than {@link #getSourceDimensions()}.</li>
     *   <li>The array contains all elements contained in {@link #getDimensions()}.
     *       Additional elements, if any, are ignored (they may be considered as bands by the caller).</li>
     * </ul>
     *
     * If elements in the given array are in same order as elements in {@link #getDimensions()} list,
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
     * Returns the number of dimensions of source coordinates in the <q>grid to CRS</q> conversion.
     * This is the number of dimensions of the <em>grid</em>.
     * It should be equal to the size of {@link #getDimensions()} list.
     *
     * <h4>Note on target dimensions</h4>
     * A {@code getTargetDimensions()} method would return the number of dimensions of the
     * <em>coordinate reference system</em>, which is the target of the <q>grid to CRS</q> conversion.
     * However, we do not provide that method because, while it should be equal to {@code getAxes(decoder).length},
     * it sometimes differs because {@link #getAxes(Decoder)} may exclude axis with zero dimensions.
     * The latter method should be used as the authoritative one.
     *
     * @return number of grid dimensions.
     */
    public abstract int getSourceDimensions();

    /**
     * Returns the dimensions of this grid, in netCDF (reverse of "natural") order. Each element in the list
     * contains the number of cells in the dimension, together with implementation-specific information.
     * The list length should be equal to {@link #getSourceDimensions()}.
     *
     * <p>This list is usually equal to the {@link Variable#getGridDimensions()} list for all variables
     * that are {@linkplain Variable#findGrid associated to this grid}. But those lists can also differ
     * in the following aspects:</p>
     *
     * <ul>
     *   <li>This grid may have less dimensions than the variable using this grid. In such case the additional
     *       dimensions in the variable can be considered as bands instead of spatiotemporal dimensions.</li>
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
     * Returns the axes of the coordinate reference system. The axis order is CRS order (reverse of netCDF order)
     * for consistency with the common practice in the {@code "coordinates"} attribute.
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
                workspace[axis.getNumDimensions() <= 1 ? i++ : --deferred] = axis;
            }
            deferred = workspace.length;        // Will become index of the first axis whose examination has been deferred.
            while (i < workspace.length) {      // Start the loop at the first n-dimensional axis (n > 1).
                final Axis axis = workspace[i];
                /*
                 * If an axis has a "wraparound" range (for example a longitude axis where the next value after +180°
                 * may be -180°), we will examine it last. The reason is that if a wraparound occurs in the middle of
                 * the localization grid, it will confuse the computation based on `coordinateForAxis(…)` calls below.
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
            /*
             * If some variables are scalar, those variables can safely be moved anywhere because they do not
             * use any input coordinate (their output coordinates are constant). Try to move those scalars to
             * more natural positions. We rely on `AxisDirection` ordering: North, East, South, West, Up/Down,
             * Future/Past directions in that order.
             */
            for (i=0; i < axes.length; i++) {
                final Axis axis = axes[i];
                if (axis.getNumDimensions() == 0) {
                    int p = i;
                    for (int j=0; j<axes.length; j++) {
                        if (axis.direction.compareTo(axes[j].direction) > 0) {
                            p = j + 1;      // After the last element that should be ordered before `axis`.
                        }
                    }
                    if (p != i) {
                        if (p > i) p--;
                        System.arraycopy(axes, i+1, axes, i, axes.length - (i+1));      // Remove element at i.
                        System.arraycopy(axes, p, axes, p+1, axes.length - (p+1));      // Insert a space at p.
                        axes[p] = axis;
                        break;
                    }
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
     * Returns the coordinate reference system inferred from axes, or {@code null} if none.
     * This method creates the CRS the first time it is invoked and caches the result,
     * for allowing {@link Decoder#getReferenceSystemInfo()} to be cheaper.
     *
     * <p>This CRS is inferred only from analysis of grid axes. It does not take in account {@link GridMapping} information.
     * This CRS may be overwritten by another CRS parsed from Well Known Text or other attributes. This overwriting is done
     * by {@link Variable#getGridGeometry()}. But even if the CRS is going to be overwritten, we still need to create it in
     * this method because this CRS will be used for adjusting axis order or for completion if grid mapping does not include
     * information for all dimensions.</p>
     *
     * @param  decoder           the decoder for which CRS are constructed.
     * @param  warnings          previous warnings, for avoiding to log the same message twice. Can be null.
     * @param  linearizations    contains CRS to use instead of CRS inferred by this method, or null or empty if none.
     * @param  reorderGridToCRS  an affine transform doing a final step in a "grid to CRS" transform for ordering axes.
     *         Not used by this method, but may be modified for taking in account axis order changes caused by replacements
     *         defined in {@code linearizations}. Ignored (can be null) if {@code linearizations} is null.
     * @return the CRS for this grid geometry, or {@code null}.
     * @throws IOException if an I/O operation was necessary but failed.
     * @throws DataStoreException if the CRS cannot be constructed.
     */
    final CoordinateReferenceSystem getCoordinateReferenceSystem(final Decoder decoder, final List<Exception> warnings,
            final List<GridCacheValue> linearizations, final Matrix reorderGridToCRS)
            throws IOException, DataStoreException
    {
        final boolean useCache = (linearizations == null) || linearizations.isEmpty();
        if (useCache & isCRSDetermined) {
            return crs;
        } else try {
            if (useCache) isCRSDetermined = true;               // Set now for avoiding new attempts if creation fail.
            final CoordinateReferenceSystem result = CRSBuilder.assemble(decoder, this, linearizations, reorderGridToCRS);
            if (useCache) crs = result;
            return result;
        } catch (FactoryException | NullPointerException ex) {
            if (isNewWarning(ex, warnings)) {
                canNotCreate(decoder, "getCoordinateReferenceSystem", Resources.Keys.CanNotCreateCRS_3, ex);
            }
            return null;
        }
    }

    /**
     * Returns {@code true} if the given exception has not already been logged.
     * If {@code true}, then this method add the given exception to the warnings list.
     */
    private static boolean isNewWarning(final Exception ex, final List<Exception> warnings) {
        if (warnings != null) {
            for (final Exception previous : warnings) {
                if (Exceptions.messageEquals(ex, previous)) {
                    return false;
                }
            }
            warnings.add(ex);
        }
        return true;
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
            if (axis.getNumDimensions() == 1) {
                final DimensionNameType name;
                if (AxisDirections.isVertical(axis.direction)) {
                    name = DimensionNameType.VERTICAL;
                } else if (AxisDirections.isTemporal(axis.direction)) {
                    name = DimensionNameType.TIME;
                } else {
                    continue;
                }
                int dim = axis.gridDimensionIndices[0];
                dim = names.length - 1 - dim;               // Convert netCDF order to "natural" order.
                if (dim >= 0) names[dim] = name;
            }
        }
        return new GridExtent(names, null, high, false);
    }

    /**
     * Returns an object containing the grid size, the CRS and the conversion from grid indices to CRS coordinates.
     * {@code GridGeometry} is the public object exposed to users. It uses the dimensions given by axes, which are
     * usually the same dimensions as the ones of the variable using this grid geometry but not always.
     * Caller may need to call {@link GridExtent#resize(long...)} for adjusting.
     *
     * @param   decoder   the decoder for which grid geometries are constructed.
     * @return  the public grid geometry (may be {@code null}).
     * @throws  IOException if an I/O operation was necessary but failed.
     * @throws  DataStoreException if the CRS cannot be constructed.
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
            int lastSrcDim = getSourceDimensions();         // Will be decremented later, then kept final.
            int lastTgtDim = axes.length;                   // Should be `getTargetDimensions()` but some axes may have been excluded.
            final int[] deferred = new int[axes.length];    // Indices of axes that have been deferred.
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
             * which seems most closely oriented toward axis direction (i.e. the first element in axis.gridDimensionIndices).
             *
             * Example: if the `axes` array contains (longitude, latitude) in that order, and if the longitude axis said
             * that its preferred dimension is 1 (after conversion to "natural" order) while the latitude axis said that
             * its preferred dimension is 0, then we build the following matrix:
             *
             *    ┌         ┐
             *    │ 0  1  0 │   axes[0] (longitude), preferred grid dimension = 1
             *    │ 1  0  0 │   axes[1] (latitude),  preferred grid dimension = 0
             *    │ 0  0  1 │
             *    └         ┘
             *
             * The preferred grid dimensions are stored in the `gridDimensionIndices` array.
             * In above example this is {1, 0}.
             */
            final int[] gridDimensionIndices = new int[nonLinears.size()];
            Arrays.fill(gridDimensionIndices, -1);
            for (int i=0; i<gridDimensionIndices.length; i++) {
                final int tgtDim = deferred[i];
                final Axis axis = axes[tgtDim];
findFree:       for (int srcDim : axis.gridDimensionIndices) {                  // In preference order (will take only one).
                    srcDim = lastSrcDim - srcDim;                               // Convert netCDF order to "natural" order.
                    for (int j=affine.getNumRow(); --j>=0;) {
                        if (affine.getElement(j, srcDim) != 0) {
                            continue findFree;
                        }
                    }
                    gridDimensionIndices[i] = srcDim;
                    affine.setElement(tgtDim, srcDim, 1);
                    break;
                }
            }
            /*
             * Search for non-linear transforms not yet constructed. It may be because the transform requires a
             * two-dimensional localization grid. Those transforms require two variables, i.e. "two-dimensional"
             * axes come in pairs.
             */
            final List<GridCacheValue> linearizations = new ArrayList<>();
            for (int i=0; i<nonLinears.size(); i++) {         // Length of `nonLinears` may change in this loop.
                if (nonLinears.get(i) == null) {
                    for (int j=i; ++j < nonLinears.size();) {
                        if (nonLinears.get(j) == null) {
                            /*
                             * Found a pair of axes.  Prepare an array of length 2, to be reordered later in the
                             * axis order declared in `gridDimensionIndices`. This is not necessarily the same order
                             * than iteration order because it depends on values of `axis.gridDimensionIndices[0]`.
                             * Those values take in account what is the "main" dimension of each axis.
                             */
                            final Axis[] gridAxes = new Axis[] {
                                axes[deferred[i]],
                                axes[deferred[j]]
                            };
                            final int srcDim   = gridDimensionIndices[i];
                            final int otherDim = gridDimensionIndices[j];
                            switch (srcDim - otherDim) {
                                case -1: break;
                                case +1: ArraysExt.swap(gridAxes, 0, 1); break;
                                default: continue;            // Needs axes at consecutive source dimensions.
                            }
                            final GridCacheValue grid = gridAxes[0].createLocalizationGrid(gridAxes[1]);
                            if (grid != null) {
                                /*
                                 * Replace the first transform by the two-dimensional localization grid and
                                 * remove the other transform. Removals need to be done in arrays too.
                                 */
                                nonLinears.set(i, grid.gridToCRS);
                                nonLinears.remove(j);
                                final int n = nonLinears.size() - j;
                                System.arraycopy(deferred,             j+1, deferred,             j, n);
                                System.arraycopy(gridDimensionIndices, j+1, gridDimensionIndices, j, n);
                                if (otherDim < srcDim) {
                                    gridDimensionIndices[i] = otherDim;     // Index of the first dimension.
                                }
                                if (grid.linearizationTarget != null) {
                                    linearizations.add(grid);
                                }
                                break;                                      // Continue the `i` loop.
                            }
                        }
                    }
                }
            }
            /*
             * If at least one `gridDimensionIndices` is undefined, the variable is maybe not a grid.
             * It happens for example if the variable is a trajectory, in which case we have two
             * CRS dimensions (e.g. latitude and longitude) but only one variable dimension;
             * the first CRS dimension has been associated to that variable and the other CRS
             * dimension is orphan.
             */
            for (final int s : gridDimensionIndices) {
                if (s < 0) return null;
            }
            /*
             * Create the coordinate reference system now, because this method may modify the `affine` transform.
             * This modification happens only if `Convention.linearizers()` specified transforms to apply on the
             * localization grid for making it more linear. This is a profile-dependent feature.
             */
            final CoordinateReferenceSystem crs = getCoordinateReferenceSystem(decoder, null, linearizations, affine);
            /*
             * Final transform, as the concatenation of the non-linear transforms followed by the affine transform.
             * We concatenate the affine transform last because it may change axis order.
             */
            MathTransform gridToCRS = null;
            final int nonLinearCount = nonLinears.size();
            final MathTransformFactory factory = decoder.getMathTransformFactory();
            // Not a non-linear transform, but we abuse this list for convenience.
            nonLinears.add(factory.createAffineTransform(affine));
            for (int i=0; i <= nonLinearCount; i++) {
                MathTransform tr = nonLinears.get(i);
                if (tr != null) {
                    if (i < nonLinearCount) {
                        final int srcDim = gridDimensionIndices[i];
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
            for (final Axis axis : axes) {
                if (axis.isCellCorner()) {
                    anchor = PixelInCell.CELL_CORNER;
                    break;
                }
            }
            geometry = new GridGeometry(getExtent(axes), anchor, gridToCRS, crs);
        } catch (FactoryException | TransformException | RuntimeException ex) {
            canNotCreate(decoder, "getGridGeometry", Resources.Keys.CanNotCreateGridGeometry_3, ex);
        }
        return geometry;
    }

    /**
     * Returns whether we computed a "grid to CRS" transform relative to pixel center or pixel corner.
     * The default value is {@link PixelInCell#CELL_CENTER}, but may be modified after invocation of
     * {@link #getGridGeometry(Decoder)}.
     */
    final PixelInCell getAnchor() {
        return anchor;
    }

    /**
     * Logs a warning about a CRS or grid geometry that cannot be created.
     *
     * @param  caller  one of {@code "getCoordinateReferenceSystem"} or {@code "getGridGeometry"}.
     * @param  key     one of {@link Resources.Keys#CanNotCreateCRS_3} or {@link Resources.Keys#CanNotCreateGridGeometry_3}.
     */
    private void canNotCreate(final Decoder decoder, final String caller, final short key, final Exception ex) {
        CharSequence message = null;
        if (ex instanceof LocalizationGridException) {
            message = ((LocalizationGridException) ex).getPotentialCause();
        }
        if (message == null) {
            message = ex.getLocalizedMessage();
        }
        warning(decoder.listeners, Grid.class, caller, ex, null, key, decoder.getFilename(), getName(), message);
    }
}
