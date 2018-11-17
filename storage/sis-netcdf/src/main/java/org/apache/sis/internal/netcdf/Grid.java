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
import java.util.ArrayList;
import java.util.Collections;
import java.io.IOException;
import org.opengis.util.FactoryException;
import org.opengis.referencing.datum.PixelInCell;
import org.opengis.referencing.operation.Matrix;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;
import org.opengis.referencing.cs.CoordinateSystem;
import org.opengis.referencing.crs.SingleCRS;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.metadata.spatial.DimensionNameType;
import org.apache.sis.internal.metadata.AxisDirections;
import org.apache.sis.referencing.operation.matrix.Matrices;
import org.apache.sis.referencing.operation.transform.MathTransforms;
import org.apache.sis.coverage.grid.GridExtent;
import org.apache.sis.coverage.grid.GridGeometry;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.util.NullArgumentException;


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
     * The axes, created when first needed.
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
     * The geometry of this grid (is extent, its CRS and its conversion to the CRS).
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
     * Returns an object containing the grid size, the CRS and the conversion from grid indices to CRS coordinates.
     * This is the public object exposed to users.
     *
     * @param   decoder   the decoder for which grid geometries are constructed.
     * @return  the public grid geometry (may be {@code null}).
     * @throws  IOException if an I/O operation was necessary but failed.
     * @throws  DataStoreException if the CRS can not be constructed.
     */
    @SuppressWarnings("fallthrough")
    public final GridGeometry getGridGeometry(final Decoder decoder) throws IOException, DataStoreException {
        if (!isGeometryDetermined) try {
            isGeometryDetermined = true;        // Set now for avoiding new attempts if creation fail.
            final Axis[] axes = getAxes();      // In netCDF order (reverse of "natural" order).
            /*
             * Build the grid extent if the shape is available. The shape may not be available
             * if a dimension has unlimited length. The dimension names are informative only.
             */
            final GridExtent extent;
            final long[] high = getShape();
            if (high != null) {
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
                extent = new GridExtent(names, new long[high.length], high, false);
            } else {
                extent = null;
            }
            /*
             * Creates the "grid to CRS" transform. The number of columns is the number of dimensions in the grid
             * (the source) +1, and the number of rows is the number of dimensions in the CRS (the target) +1.
             * The order of dimensions in the transform is the reverse of the netCDF axis order.
             */
            int srcEnd = getSourceDimensions();
            int tgtEnd = getTargetDimensions();
            final Matrix affine = Matrices.createZero(tgtEnd + 1, srcEnd + 1);
            affine.setElement(tgtEnd--, srcEnd--, 1);
            for (int i=axes.length; --i >= 0;) {
                final Axis axis = axes[i];
                final int tgtDim = tgtEnd - i;
                if (axis.sourceDimensions.length == 1) {
                    final int srcDim = srcEnd - axis.sourceDimensions[0];
                    if (axis.coordinates.trySetTransform(affine, srcDim, tgtDim)) {
                        continue;
                    }
                }
                for (int srcDim : axis.sourceDimensions) {
                    affine.setElement(tgtDim, srcEnd - srcDim, 1);
                    // TODO: prepare non-linear transform here for later concatenation.
                }
            }
            MathTransform gridToCRS = MathTransforms.linear(affine);
            geometry = new GridGeometry(extent, PixelInCell.CELL_CENTER, gridToCRS, getCoordinateReferenceSystem(decoder));
        } catch (TransformException ex) {
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
