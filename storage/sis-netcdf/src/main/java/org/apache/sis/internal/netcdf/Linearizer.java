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

import java.util.Set;
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import org.opengis.geometry.Envelope;
import org.opengis.referencing.crs.SingleCRS;
import org.opengis.referencing.crs.ProjectedCRS;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.Matrix;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;
import org.apache.sis.referencing.operation.matrix.Matrix3;
import org.apache.sis.referencing.operation.transform.MathTransforms;
import org.apache.sis.referencing.operation.builder.LocalizationGridBuilder;
import org.apache.sis.referencing.IdentifiedObjects;
import org.apache.sis.referencing.CommonCRS;
import org.apache.sis.internal.util.Strings;
import org.apache.sis.internal.referencing.AxisDirections;
import org.apache.sis.internal.referencing.ReferencingUtilities;
import org.apache.sis.storage.DataStoreReferencingException;
import org.apache.sis.util.ArraysExt;


/**
 * A two-dimensional non-linear transform to try in an attempt to make a localization grid more linear.
 * Non-linear transforms are tested in "trials and errors" and the one resulting in best correlation
 * coefficients is selected.
 *
 * <p>Before linearization, source coordinates may be in (latitude, longitude) or (longitude, latitude) order
 * depending on the order of dimensions in netCDF variable. But after linearization, axes will be in a fixed
 * order determined by {@link #targetCRS}. In other words, netCDF dimension order shall be ignored if a
 * linearization is applied.</p>
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.1
 *
 * @see org.apache.sis.referencing.operation.builder.LocalizationGridBuilder#addLinearizers(Map, boolean, int...)
 *
 * @since 1.0
 * @module
 */
public final class Linearizer {
    /**
     * The datum to use as one of the predefined constants. The ellipsoid size do not matter
     * because a linear regression will be applied anyway. However the eccentricity matter.
     *
     * <p>When a non-linear transform exists in spherical or ellipsoidal variants, it may be sufficient to use
     * the spherical formulas instead than ellipsoidal formulas because the spherical ones are faster and more
     * stable (because the inverse transforms are exact, up to rounding errors). The errors caused by the use
     * of spherical formulas are compensated by the localization grid used after the linearizer.
     * Spherical formulas can be requested by setting this field to {@link CommonCRS#SPHERE}.</p>
     *
     * @see Convention#defaultHorizontalCRS(boolean)
     */
    private final CommonCRS datum;

    /**
     * The type of projection to create.
     * Current implementation supports only Universal Transverse Mercator (UTM) projection,
     * but we nevertheless define this enumeration as a place-holder for more types in the future.
     */
    public enum Type {
        /**
         * Universal Transverse Mercator projection.
         */
        UTM
    }

    /**
     * The type of projection to create (Mercator, UTM, <i>etc</i>).
     */
    final Type type;

    /**
     * The target coordinate reference system after application of the non-linear transform.
     * May depend on the netCDF file being read (for example for choosing a UTM zone).
     */
    private CoordinateReferenceSystem targetCRS;

    /**
     * Whether axes need to be swapped in order to have the same direction before and after the transform.
     * For example if input coordinates have (east, north) directions, then output coordinates shall have
     * (east, north) directions as well. This flag specifies whether input coordinates must be swapped for
     * making above condition true.
     */
    private boolean axisSwap;

    /**
     * Creates a new linearizer working on the specified datum.
     *
     * @param  datum  the datum to use. Should be consistent with {@link Convention#defaultHorizontalCRS(boolean)}.
     * @param  type   the type of projection to create (Mercator, UTM, <i>etc</i>).
     */
    public Linearizer(final CommonCRS datum, final Type type) {
        this.datum = datum;
        this.type  = type;
    }

    /**
     * Returns the name used for identifying this linearizer in {@link LocalizationGridBuilder}.
     */
    final String name() {
        return type.name();
    }

    /**
     * Returns the target CRS computed by {@link #gridToTargetCRS gridToTargetCRS(…)}.
     */
    final CoordinateReferenceSystem getTargetCRS() {
        return targetCRS;
    }

    /**
     * Returns whether axes need to be swapped in order to have the same direction before and after the transform.
     * For example if input coordinates have (east, north) directions, then output coordinates shall have (east, north)
     * directions as well. This flag specifies whether input coordinates must be swapped for making above condition true.
     *
     * @see GridCacheValue#axisSwap
     */
    final boolean axisSwap() {
        return axisSwap;
    }

    /**
     * Returns a string representation for debugging purposes.
     */
    @Override
    public String toString() {
        return Strings.toString(getClass(), "type", type, "targetCRS", IdentifiedObjects.getName(targetCRS, null));
    }

    /**
     * Creates a transform for the given localization grid. The returned transform expects source coordinates in
     * (latitude, longitude) or (longitude, latitude) order, depending on {@code xdim} and {@code ydim} values.
     * Target coordinates will be in the order defined by {@link #targetCRS}, which is assigned by this method.
     *
     * @param  grid  the grid on which to add non-linear transform.
     * @param  xdim  index of longitude dimension in the grid control points.
     * @param  ydim  index of latitude dimension in the grid control points.
     * @return a two-dimensional transform expecting source coordinates in (longitude, latitude) order.
     * @throws TransformException if grid coordinates can not be obtained. Actually this exception
     *         should never happen because the {@code MathTransform} used is a linear transform.
     */
    private MathTransform gridToTargetCRS(final LocalizationGridBuilder grid, final int xdim, final int ydim)
            throws TransformException
    {
        MathTransform transform;
        switch (type) {
            default: {
                throw new AssertionError(type);
            }
            /*
             * Create a Universal Transverse Mercator (UTM) projection for the zone containing a point in
             * the middle of the grid. We apply `Math.signum(…)` on the latitude for avoiding stereographic
             * projections near poles and for avoiding Norway and Svalbard special cases.
             */
            case UTM: {
                final Envelope bounds = grid.getSourceEnvelope(false);
                final double[] median = grid.getControlPoint(
                        (int) Math.round(bounds.getMedian(0)),
                        (int) Math.round(bounds.getMedian(1)));
                final ProjectedCRS crs = datum.universal(Math.signum(median[ydim]), median[xdim]);
                assert ReferencingUtilities.startsWithNorthEast(crs.getBaseCRS().getCoordinateSystem());
                transform = crs.getConversionFromBase().getMathTransform();
                targetCRS = crs;
                break;
            }
        }
        /*
         * Above transform expects (latitude, longitude) inputs (verified by assertion).
         * If grid coordinates are in (longitude, latitude) order, we must swap inputs.
         */
        axisSwap = ydim < xdim;
        if (!axisSwap) {
            final Matrix3 m = new Matrix3();
            m.m00 = m.m11 = 0;
            m.m01 = m.m10 = 1;
            transform = MathTransforms.concatenate(MathTransforms.linear(m), transform);
        }
        return transform;
    }

    /**
     * Applies non-linear transform candidates to the given localization grid.
     * This method tries to locate longitude and latitude axes. If those axes are found,
     * they will be used as input coordinates for the {@link MathTransform} instances
     * (typically map projections) created by {@link #gridToTargetCRS gridToTargetCRS(…)}.
     * Those transforms are then {@linkplain LocalizationGridBuilder#addLinearizers given
     * to the localization grid} for consideration in attempts to make the grid more linear.
     *
     * @param  sourceAxes   coordinate system axes in CRS order.
     * @param  linearizers  the linearizers to apply.
     * @param  grid         the grid on which to add non-linear transform candidates.
     * @throws TransformException if grid coordinates can not be obtained. Actually this exception should never
     *         happen because the {@code MathTransform} used is a linear transform. We propagate this exception
     *         because it is more convenient to have it handled by the caller together with other exceptions.
     */
    static void setCandidatesOnGrid(final Axis[] sourceAxes, final Set<Linearizer> linearizers, final LocalizationGridBuilder grid)
            throws TransformException
    {
        int xdim = -1, ydim = -1;
        for (int i=sourceAxes.length; --i >= 0;) {
            switch (sourceAxes[i].abbreviation) {
                case 'λ': xdim = i; break;
                case 'φ': ydim = i; break;
            }
        }
        if ((xdim | ydim) >= 0) {
            final Map<String,MathTransform> projections = new HashMap<>();
            for (final Linearizer linearizer : linearizers) {
                final MathTransform transform = linearizer.gridToTargetCRS(grid, xdim, ydim);
                projections.put(linearizer.name(), transform);
            }
            /*
             * Axis order before linearization was taken in account by above `gridToTargetCRS(…, xdim, ydim)`.
             * Consequently arguments below shall specify only the dimensions to select without reordering axes.
             * Note that after linearization, axes will be in a fixed order determined by the CRS.
             */
            grid.addLinearizers(projections, false, Math.min(xdim, ydim), Math.max(xdim, ydim));
        }
    }

    /**
     * Given CRS components inferred by {@link CRSBuilder}, replaces CRS components in the dimensions
     * where linearization has been applied. The CRS components to replace are inferred from axis directions.
     *
     * <p>This static method is defined here for keeping in a single class all codes related to linearization.</p>
     *
     * @param  components        the components of the compound CRS that {@link CRSBuilder} inferred.
     * @param  replacements      the {@link #targetCRS} of linearizations.
     * @param  reorderGridToCRS  an affine transform doing a final step in a "grid to CRS" transform for ordering axes.
     *         Not used by this method, but modified for taking in account axis order changes caused by replacements.
     */
    static void replaceInCompoundCRS(final SingleCRS[] components, final List<GridCacheValue> replacements,
                                     final Matrix reorderGridToCRS) throws DataStoreReferencingException
    {
        Matrix original = null;
search: for (final GridCacheValue cache : replacements) {
            final CoordinateReferenceSystem targetCRS = cache.linearizationTarget;
            int firstDimension = 0;
            for (int i=0; i < components.length; i++) {
                final SingleCRS sourceCRS = components[i];
                final int[] r = AxisDirections.indicesOfColinear(sourceCRS.getCoordinateSystem(), targetCRS.getCoordinateSystem());
                if (r != null) {
                    if (cache.axisSwap) {
                        ArraysExt.swap(r, 0, 1);
                    }
                    for (int j=0; j<r.length; j++) {
                        if (r[j] != j) {
                            final int oldRow = r[j] + firstDimension;
                            final int newRow =   j  + firstDimension;
                            if (original == null) {
                                original = reorderGridToCRS.clone();
                            }
                            for (int k = original.getNumCol(); --k >= 0;) {
                                reorderGridToCRS.setElement(newRow, k, original.getElement(oldRow, k));
                            }
                        }
                    }
                    components[i] = (ProjectedCRS) targetCRS;
                    continue search;
                }
                firstDimension += sourceCRS.getCoordinateSystem().getDimension();
            }
            // If a replacement can not be applied, fail CRS construction.
            // May be relaxed in a future version if we have a use case.
            throw new DataStoreReferencingException(Resources.format(
                    Resources.Keys.CanNotInjectComponent_1, IdentifiedObjects.getName(targetCRS, null)));
        }
    }
}
