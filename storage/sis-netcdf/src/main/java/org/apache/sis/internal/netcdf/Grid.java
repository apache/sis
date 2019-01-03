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
import org.opengis.referencing.operation.TransformException;
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
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.util.NullArgumentException;
import org.apache.sis.math.Vector;


/**
 * Information about the grid geometry and the conversion from grid coordinates to geodetic coordinates.
 * More than one variable may share the same grid.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.0
 *
 * @see Decoder#getGridGeometries()
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
     * @see #getAxes()
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
     * Returns the number of dimensions of source coordinates in the <cite>"grid to CRS"</cite> conversion.
     * This is the number of dimensions of the <em>grid</em>.
     *
     * @return number of grid dimensions.
     */
    public abstract int getSourceDimensions();

    /**
     * Returns the number of dimensions of target coordinates in the <cite>"grid to CRS"</cite> conversion.
     * This is the number of dimensions of the <em>coordinate reference system</em>.
     * It should be equal to the size of the array returned by {@link #getAxes()},
     * but caller should be robust to inconsistencies.
     *
     * @return number of CRS dimensions.
     */
    public abstract int getTargetDimensions();

    /**
     * Returns the number of cells along each source dimension, in "natural" order.
     * This method may return {@code null} if the grid shape can not be determined.
     *
     * @return number of cells along each source dimension, in "natural" (opposite of netCDF) order, or {@code null}.
     *
     * @see Variable#getShape()
     */
    protected abstract long[] getShape();

    /**
     * Returns the axes of the coordinate reference system. The size of this array is expected equals to the
     * value returned by {@link #getTargetDimensions()}, but the caller should be robust to inconsistencies.
     * The axis order is as declared in the netCDF file (reverse of "natural" order).
     *
     * <p>This method returns a direct reference to the cached array; do not modify.</p>
     *
     * @return the CRS axes, in netCDF order (reverse of "natural" order).
     * @throws IOException if an I/O operation was necessary but failed.
     * @throws DataStoreException if a logical error occurred.
     * @throws ArithmeticException if the size of an axis exceeds {@link Integer#MAX_VALUE}, or other overflow occurs.
     */
    @SuppressWarnings("ReturnOfCollectionOrArrayField")
    public final Axis[] getAxes() throws IOException, DataStoreException {
        if (axes == null) {
            axes = createAxes();
        }
        return axes;
    }

    /**
     * Creates the axes to be returned by {@link #getAxes()}. This method is invoked only once when first needed.
     *
     * @return the CRS axes, in netCDF order (reverse of "natural" order).
     * @throws IOException if an I/O operation was necessary but failed.
     * @throws DataStoreException if a logical error occurred.
     * @throws ArithmeticException if the size of an axis exceeds {@link Integer#MAX_VALUE}, or other overflow occurs.
     */
    protected abstract Axis[] createAxes() throws IOException, DataStoreException;

    /**
     * Returns a coordinate for the given two-dimensional grid coordinate axis. This is (indirectly) a callback
     * method for {@link #getAxes()}. The (<var>i</var>, <var>j</var>) indices are grid indices <em>before</em>
     * they get reordered by the {@link Axis} constructor. In the netCDF UCAR API, this method maps directly to
     * {@link ucar.nc2.dataset.CoordinateAxis2D#getCoordValue(int, int)}.
     *
     * @param  axis  an implementation-dependent object representing the two-dimensional axis.
     * @param  j     the fastest varying (right-most) index.
     * @param  i     the slowest varying (left-most) index.
     * @return the coordinate at the given index, or {@link Double#NaN} if it can not be computed.
     * @throws IOException if an I/O operation was necessary but failed.
     * @throws DataStoreException if a logical error occurred.
     * @throws ArithmeticException if the axis size exceeds {@link Integer#MAX_VALUE}, or other overflow occurs.
     */
    protected abstract double coordinateForAxis(Variable axis, int j, int i) throws IOException, DataStoreException;

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
            final Axis[] axes = getAxes();
            for (int i=axes.length; --i >= 0;) {                // NetCDF order is reverse of "natural" order.
                CRSBuilder.dispatch(builders, axes[i]);
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
     * @param  axes  value of {@link #getAxes()}. Should be the same as {@link #axes}.
     */
    @SuppressWarnings("fallthrough")
    private GridExtent getExtent(final Axis[] axes) {
        final long[] high = getShape();
        if (high == null) {
            return null;
        }
        final DimensionNameType[] names = new DimensionNameType[high.length];
        switch (names.length) {
            default: names[1] = DimensionNameType.ROW;      // Fall through
            case 1:  names[0] = DimensionNameType.COLUMN;   // Fall through
            case 0:  break;
        }
        for (final Axis axis : axes) {
            if (axis.sourceDimensions.length == 1) {
                final DimensionNameType name;
                if (AxisDirections.isVertical(axis.direction)) {
                    name = DimensionNameType.VERTICAL;
                } else if (AxisDirections.isTemporal(axis.direction)) {
                    name = DimensionNameType.TIME;
                } else {
                    continue;
                }
                final int dim = axis.sourceDimensions[0];
                if (dim >= 0 && dim < names.length) {
                    names[names.length - 1 - dim] = name;
                }
            }
        }
        return new GridExtent(names, null, high, false);
    }

    /**
     * Returns an object containing the grid size, the CRS and the conversion from grid indices to CRS coordinates.
     * This is the public object exposed to users.
     *
     * @param   decoder   the decoder for which grid geometries are constructed.
     * @return  the public grid geometry (may be {@code null}).
     * @throws  IOException if an I/O operation was necessary but failed.
     * @throws  DataStoreException if the CRS can not be constructed.
     */
    public final GridGeometry getGridGeometry(final Decoder decoder) throws IOException, DataStoreException {
        if (!isGeometryDetermined) try {
            isGeometryDetermined = true;        // Set now for avoiding new attempts if creation fail.
            final Axis[] axes = getAxes();      // In netCDF order (reverse of "natural" order).
            /*
             * Creates the "grid to CRS" transform. The number of columns is the number of dimensions in the grid
             * (the source) +1, and the number of rows is the number of dimensions in the CRS (the target) +1.
             * The order of dimensions in the transform is the reverse of the netCDF axis order.
             */
            int srcEnd = getSourceDimensions();
            int tgtEnd = getTargetDimensions();
            final int[] deferred = new int[axes.length];                    // Indices of axes that have been deferred.
            final List<MathTransform> nonLinears = new ArrayList<>();
            final Matrix affine = Matrices.createZero(tgtEnd + 1, srcEnd + 1);
            affine.setElement(tgtEnd--, srcEnd--, 1);
            for (int i=axes.length; --i >= 0;) {
                if (!axes[i].trySetTransform(affine, srcEnd, tgtEnd - i, nonLinears)) {
                    deferred[nonLinears.size() - 1] = i;
                }
            }
            /*
             * If we have not been able to set some coefficients in the matrix (because some transforms are non-linear),
             * set a single scale factor to 1 in the matrix row. The coefficient that we set to 1 is the one for the source
             * dimension which is not already taken by another row.
             */
            final int[] sourceDimensions = new int[nonLinears.size()];
            Arrays.fill(sourceDimensions, -1);
            for (int i=0; i<sourceDimensions.length; i++) {
                final int d = deferred[i];
                final Axis axis = axes[d];
findFree:       for (int srcDim : axis.sourceDimensions) {
                    srcDim = srcEnd - srcDim;
                    for (int j=affine.getNumRow(); --j>=0;) {
                        if (affine.getElement(j, srcDim) != 0) {
                            continue findFree;
                        }
                    }
                    sourceDimensions[i] = srcDim;
                    final int tgtDim = tgtEnd - d;
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
                    final Axis axis = axes[deferred[i]];
                    if (axis.sourceDimensions.length == 2) {
                        final int d1 = axis.sourceDimensions[0];
                        final int d2 = axis.sourceDimensions[1];
                        for (int j=i; ++j < nonLinears.size();) {
                            if (nonLinears.get(j) == null) {
                                final Axis other = axes[deferred[j]];
                                if (other.sourceDimensions.length == 2) {
                                    final int o1 = other.sourceDimensions[0];
                                    final int o2 = other.sourceDimensions[1];
                                    if ((o1 == d1 && o2 == d2) || (o1 == d2 && o2 == d1)) {
                                        /*
                                         * Found two axes for the same set of dimensions, which implies that they have
                                         * the same shape (width and height).  In current implementation, we also need
                                         * those axes to be at consecutive source dimensions.
                                         */
                                        final int srcDim   = sourceDimensions[i];
                                        final int otherDim = sourceDimensions[j];
                                        if (Math.abs(srcDim - otherDim) == 1) {
                                            final int width  = axis.sourceSizes[0];
                                            final int height = axis.sourceSizes[1];
                                            final LocalizationGridBuilder grid = new LocalizationGridBuilder(width, height);
                                            final Vector v1 =  axis.coordinates.read();
                                            final Vector v2 = other.coordinates.read();
                                            final double[] target = new double[2];
                                            int index = 0;
                                            for (int y=0; y<height; y++) {
                                                for (int x=0; x<width; x++) {
                                                    target[0] = v1.doubleValue(index);
                                                    target[1] = v2.doubleValue(index);
                                                    grid.setControlPoint(x, y, target);
                                                    index++;
                                                }
                                            }
                                            /*
                                             * Replace the first transform by the two-dimensional localization grid and
                                             * remove the other transform. Removals need to be done in arrays too.
                                             */
                                            nonLinears.set(i, grid.create(factory));
                                            nonLinears.remove(j);
                                            final int n = nonLinears.size() - j;
                                            System.arraycopy(deferred,         j+1, deferred,         j, n);
                                            System.arraycopy(sourceDimensions, j+1, sourceDimensions, j, n);
                                            if (otherDim < srcDim) {
                                                sourceDimensions[i] = otherDim;
                                            }
                                        }
                                    }
                                }
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
                                        (srcEnd + 1) - (srcDim + tr.getSourceDimensions()));
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
        } catch (FactoryException ex) {
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
