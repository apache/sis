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

import java.util.Set;
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import org.opengis.geometry.Envelope;
import org.opengis.util.InternationalString;
import org.opengis.referencing.crs.SingleCRS;
import org.opengis.referencing.crs.ProjectedCRS;
import org.opengis.referencing.cs.CoordinateSystem;
import org.opengis.referencing.operation.Matrix;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;
import org.apache.sis.referencing.IdentifiedObjects;
import org.apache.sis.referencing.CommonCRS;
import org.apache.sis.referencing.operation.matrix.Matrix3;
import org.apache.sis.referencing.operation.transform.MathTransforms;
import org.apache.sis.referencing.operation.builder.LocalizationGridBuilder;
import org.apache.sis.util.ArraysExt;
import org.apache.sis.util.internal.shared.Strings;
import org.apache.sis.referencing.internal.shared.AxisDirections;
import org.apache.sis.referencing.internal.shared.ReferencingUtilities;
import org.apache.sis.storage.DataStoreReferencingException;
import org.apache.sis.storage.netcdf.internal.Resources;


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
 * <p>A new instance of this class shall be created for each netCDF file is read.</p>
 *
 * @author  Martin Desruisseaux (Geomatys)
 *
 * @see org.apache.sis.referencing.operation.builder.LocalizationGridBuilder#addLinearizers(Map, boolean, int...)
 */
public final class Linearizer {
    /**
     * Number of dimensions of the grid.
     *
     * @see org.apache.sis.referencing.operation.builder.ResidualGrid#SOURCE_DIMENSION
     */
    private static final int SOURCE_DIMENSION = 2;

    /**
     * The datum to use as one of the predefined constants. The ellipsoid size do not matter
     * because a linear regression will be applied anyway. However, the eccentricity matter.
     *
     * <p>When a non-linear transform exists in spherical or ellipsoidal variants, it may be sufficient to use
     * the spherical formulas instead of ellipsoidal formulas because the spherical ones are faster and more
     * stable (because the inverse transforms are exact, up to rounding errors). The errors caused by the use
     * of spherical formulas are compensated by the localization grid used after the linearizer.
     * Spherical formulas can be requested by setting this field to {@link CommonCRS#SPHERE}.</p>
     *
     * @see Convention#defaultHorizontalCRS(boolean)
     */
    private final CommonCRS datum;

    /**
     * The type of projection to create.
     * Current implementation supports only Universal Transverse Mercator (UTM) and stereographic projections,
     * but we nevertheless define this enumeration as a place-holder for more types in the future.
     */
    public enum Type {
        /**
         * Universal Transverse Mercator (UTM) or Polar Stereographic projection, depending on whether the image
         * is close to a pole or not. The CRS is selected by a call to {@link CommonCRS#universal(double, double)}.
         * The point given is the {@code universal(…)} is determined as below:
         *
         * <ul>
         *   <li>If the image is far enough from equator (at a latitude of {@value #POLAR_THRESHOLD} or further),
         *       then the point will be in the center of the border closest to the pole.</li>
         *   <li>Otherwise the point is in the middle of the image.</li>
         * </ul>
         *
         * <h4>Rational</h4>
         * The intent is to increase the chances to get the Polar Stereographic projection for images close to pole.
         * This is necessary because longitude values may become far from central meridian at latitudes such as 88°,
         * causing the Transverse Mercator projection to produce NaN numbers.
         */
        UNIVERSAL;

        /**
         * Minimal latitude (in degrees) for forcing the {@link #UNIVERSAL} mode to consider a point
         * on the border closest to pole for deciding whether to use UTM or stereographic projection.
         * 60° is the limit of the domain of validity of Polar Stereographic methods.
         */
        static final int POLAR_THRESHOLD = 60;
    }

    /**
     * The type of projection to create (Mercator, UTM, <i>etc</i>).
     */
    final Type type;

    /**
     * The target coordinate reference system after application of the non-linear transform.
     * May depend on the netCDF file being read (for example for choosing a UTM zone).
     */
    private SingleCRS targetCRS;

    /**
     * Whether axes need to be swapped in order to have the same direction before and after the transform.
     * For example if input coordinates have (east, north) directions, then output coordinates shall have
     * (east, north) directions as well. This flag specifies whether input coordinates must be swapped for
     * making above condition true.
     */
    private boolean axisSwap;

    /**
     * The image span in degrees of longitude, or 0 if not computed.
     * This is used for giving a hint about why a projection may have failed.
     *
     * @see #getPotentialCause()
     */
    private float longitudeSpan;

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
    final SingleCRS getTargetCRS() {
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
     * If this linearizer can give a probable reason why it failed to compute the localization grid, returns that reason.
     * Otherwise returns {@code null}.
     *
     * @return potential error cause, or {@code null} if unknown.
     */
    final InternationalString getPotentialCause() {
        if (longitudeSpan >= 180 - 6) {         // 180° of longitude minus a UTM zone width.
            final InternationalString name = IdentifiedObjects.getDisplayName(targetCRS);
            return Resources.formatInternational(Resources.Keys.GridLongitudeSpanTooWide_2,
                                                 longitudeSpan, (name != null) ? name : type);
        }
        return null;
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
     * @throws TransformException if grid coordinates cannot be obtained. Actually this exception
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
             * Create a Universal Transverse Mercator (UTM) projection for the zone containing a point in the grid.
             * First, we compute an estimation of the bounding box in geographic coordinates (using grid corners).
             * Then if the box is far enough from equator, we use the point on the side closest to the pole.
             */
            case UNIVERSAL: {
                final Envelope bounds = grid.getSourceEnvelope(false);
                double x, y, xmin, xmax, ymin, ymax;
                {   // For keeping `median` variable local.
                    final double[] median = grid.getControlPoint(
                            (int) Math.round(bounds.getMedian(0)),
                            (int) Math.round(bounds.getMedian(1)));
                    x = median[xdim]; xmin = xmax = x;
                    y = median[ydim]; ymin = ymax = y;
                }
                final int[] gc = new int[SOURCE_DIMENSION];
                for (int i=0; i<4; i++) {
                    for (int d=0; d<SOURCE_DIMENSION; d++) {
                        gc[d] = (int) Math.round(((i & (1 << d)) == 0) ? bounds.getMinimum(d) : bounds.getMaximum(d));
                    }
                    final double[] cp = grid.getControlPoint(gc[0], gc[1]);
                    double c = cp[xdim];
                    if (c < xmin) xmin = c;
                    if (c > xmax) xmax = c;
                    c = cp[ydim];
                    if (c < ymin) ymin = c;
                    if (c > ymax) ymax = c;
                }
                longitudeSpan = (float) (xmax - xmin);      // For providing a hint in case of failure.
                /*
                 * If the image is far from equator, replace the middle point by a point close to pole.
                 * The intent is to avoid using UTM projection for latitudes such as 89°N, because a single
                 * NaN in transformed coordinates is enough for blocking creation of the localization grid.
                 */
                     if (ymin >= +Type.POLAR_THRESHOLD) y = ymax;
                else if (ymax <= -Type.POLAR_THRESHOLD) y = ymin;
                final ProjectedCRS crs = datum.universal(y, x);
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
     * @throws TransformException if grid coordinates cannot be obtained. Actually this exception should never
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
             * Consequently, arguments below shall specify only the dimensions to select without reordering axes.
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
     * @param  replacements      the {@link #targetCRS} of linearizations. Usually a list of size 1.
     * @param  reorderGridToCRS  an affine transform doing a final step in a "grid to CRS" transform for ordering axes.
     *         Not used by this method, but modified for taking in account axis order changes caused by replacements.
     */
    static void replaceInCompoundCRS(final SingleCRS[] components, final List<GridCacheValue> replacements,
                                     final Matrix reorderGridToCRS) throws DataStoreReferencingException
    {
        Matrix original = null;
search: for (final GridCacheValue replacement : replacements) {
            final SingleCRS targetCRS = replacement.linearizationTarget;
            final CoordinateSystem targetCS = targetCRS.getCoordinateSystem();
            int firstDimension = 0;
            for (int i=0; i < components.length; i++) {
                final SingleCRS sourceCRS = components[i];
                /*
                 * In the most typical cases, the source CRS is geographic and the target CRS is projected.
                 * We can generally associate a source axis to a target axis by looking at their directions.
                 * For example, the "Longitude" source axis is approximately colinear with the "Easting" target axis.
                 * However, there is a use case where target axis directions cannot be matched directly to source:
                 * if the projection is a polar projection with target axis directions such as "South along 90°E",
                 * then `AxisDirections.indexOfColinear(CoordinateSystem, CoordinateSystem)} will not find a match.
                 * We need the more flexible `indicesOfLenientMapping(…)` method.
                 */
                final int[] r = AxisDirections.indicesOfLenientMapping(sourceCRS.getCoordinateSystem(), targetCS);
                if (r != null) {
                    components[i] = targetCRS;
                    if (replacement.axisSwap) {
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
                    continue search;
                }
                firstDimension += sourceCRS.getCoordinateSystem().getDimension();
            }
            /*
             * If a replacement cannot be applied, fail CRS construction.
             * May be relaxed in a future version if we have a use case.
             */
            throw new DataStoreReferencingException(Resources.format(
                    Resources.Keys.CanNotInjectComponent_1, IdentifiedObjects.getName(targetCRS, null)));
        }
    }
}
