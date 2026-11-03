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
package org.apache.sis.geometry.wrapper;

import java.util.logging.Filter;
import org.opengis.util.FactoryException;
import org.opengis.referencing.crs.SingleCRS;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.cs.AxisDirection;
import org.apache.sis.referencing.CRS;
import org.apache.sis.referencing.CommonCRS;
import org.apache.sis.feature.internal.Resources;
import org.apache.sis.referencing.internal.shared.AxisDirections;
import org.apache.sis.referencing.internal.shared.EllipsoidalHeightCombiner;
import org.apache.sis.referencing.internal.shared.ReferencingFactoryContainer;
import org.apache.sis.util.collection.BackingStoreException;


/**
 * Identification of which dimensions are contained in a sequence of coordinate tuples.
 * Can also {@linkplain #adjust(CoordinateReferenceSystem, SingleCRS) adjust a <abbr>CRS</abbr>}
 * to the layout described by an enumeration value.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
public enum Dimensions {
    /**
     * Two-dimensional tuples with <var>x</var>, <var>y</var> coordinates.
     */
    XY(false, false, (byte) Geometries.BIDIMENSIONAL),

    /**
     * Three-dimensional tuples with <var>x</var>, <var>y</var>, <var>z</var> coordinates.
     */
    XYZ(true, false, (byte) Geometries.TRIDIMENSIONAL),

    /**
     * Three-dimensional tuples with <var>x</var>, <var>y</var>, <var>m</var> coordinates.
     */
    XYM(false, true, (byte) Geometries.TRIDIMENSIONAL),

    /**
     * Four-dimensional tuples with <var>x</var>, <var>y</var>, <var>z</var>, <var>m</var> coordinates.
     */
    XYZM(true, true, (byte) 4);

    /**
     * Whether this set of dimensions include the <var>z</var> coordinate.
     */
    public final boolean hasZ;

    /**
     * Whether this set of dimensions includes the <var>m</var> coordinate.
     */
    public final boolean hasM;

    /**
     * Number of dimensions: 2, 3 or 4.
     */
    public final byte count;

    /**
     * Creates a new enumeration value for the given number of dimensions.
     */
    private Dimensions(final boolean hasZ, final boolean hasM, final byte count) {
        this.hasZ  = hasZ;
        this.hasM  = hasM;
        this.count = count;
    }

    /**
     * Returns the enumeration value for the given support of <var>z</var> and <var>m</var> coordinates.
     *
     * @param  hasZ  whether the <var>z</var> coordinate is included.
     * @param  hasM  whether the <var>m</var> coordinate is included.
     * @return enumeration value for the given included coordinates.
     */
    public static Dimensions forZorM(final boolean hasZ, final boolean hasM) {
        return hasZ ? (hasM ? XYZM : XYZ) : (hasM ? XYM : XY);
    }

    /**
     * Returns an enumeration value for the given number of dimensions.
     *
     * @param  count    the number of dimensions.
     * @param  preferM  the coordinate to use if the number of dimensions is 3:
     *         {@code false} for <var>z</var> or {@code true} for <var>m</var>.
     * @return enumeration for the given number of dimensions.
     * @throws IllegalArgumentException if the given number if less than 2 or more than 3.
     */
    public static Dimensions forCount(final int count, final boolean preferM) {
        switch (count) {
            case Geometries.BIDIMENSIONAL:  return XY;
            case Geometries.TRIDIMENSIONAL: return preferM ? XYM : XYZ;
            case 4:  return XYZM;
            default: throw new IllegalArgumentException(Resources.format(Resources.Keys.UnsupportedGeometryObject_1, count));
        }
    }

    /**
     * Returns an enumeration value for the given coordinate reference system.
     * This method does not try to provide an accurate description of the given <abbr>CRS</abbr>.
     * It only returns an estimated best match.
     *
     * @param  crs  the coordinate reference system for which to get an enumeration value, or {@code null}.
     * @return enumeration value for this given <abbr>CRS</abbr>. Never null but may be inaccurate.
     */
    public static Dimensions forCRS(final CoordinateReferenceSystem crs) {
        boolean hasZ = AxisDirections.indexOfColinear(crs.getCoordinateSystem(), AxisDirection.UP) >= 0;
        int count = CRS.getDimensionOrZero(crs);
        if (hasZ) count--;  // Number of dimensions other than vertical.
        return forZorM(hasZ, count > Geometries.BIDIMENSIONAL);
    }

    /**
     * Derives a <abbr>CRS</abbr> with the dimensions specified by this enumeration value.
     * If the given {@code base} <abbr>CRS</abbr> contains the (<var>X</var>, <var>Y</var>),
     * <var>Z</var> and/or <var>M</var> dimensions identified by this enumeration value,
     * these dimensions will be selected and put in the order implied by this enumeration.
     * If some dimensions need to be added, the following heuristics are applied:
     *
     * <ul>
     *   <li>If the given {@code base} has no {@linkplain CRS#getHorizontalComponent horizontal component},
     *     the {@linkplain CommonCRS#defaultGeographic() default geographic <abbr>CRS</abbr>} is used.</li>
     *   <li>If the given {@code base} has no {@linkplain CRS#getVerticalComponent vertical component} and
     *     a <var>Z</var> component is requested, the default is ellipsoidal height in metres.</li>
     *   <li>If the given {@code base} has no {@linkplain CRS#getTemporalComponent temporal component} and
     *     a <var>M</var> component is requested, the default is the given {@code defaultM} argument.</li>
     * </ul>
     *
     * @param  base           <abbr>CRS</abbr> containing the dimensions to use in the <abbr>CRS</abbr> to return.
     * @param  factories      factories to use for creating new <abbr>CRS</abbr>, or {@code null} for the default.
     * @param  defaultM       default <abbr>CRS</abbr> for the <abbr>M</abbr> dimension, or {@code null} if none.
     * @param  warningFilter  whether to log warnings, or {@code null} for the default behavior.
     * @return a <abbr>CRS</abbr> with the dimensions identified by this enumeration value.
     * @throws FactoryException if an error occurred while querying the authority factory or creating a <abbr>CRS</abbr>.
     */
    @SuppressWarnings("element-type-mismatch")
    public CoordinateReferenceSystem adjust(final CoordinateReferenceSystem base,
                                            final ReferencingFactoryContainer factories,
                                            final SingleCRS defaultM,
                                            final Filter warningFilter)
            throws FactoryException
    {
        CoordinateReferenceSystem crs;
        try {
            boolean isAuthoritative = false;
            crs = hasZ ? CRS.getGeospatialComponent(base)
                       : CRS.getHorizontalComponent(base);
            if (crs == null) {
                crs = CommonCRS.defaultGeographic();
                isAuthoritative = true;
            }
            if (hasZ && CRS.getDimensionOrZero(crs) <= Geometries.BIDIMENSIONAL) {
                SingleCRS toAdd = CRS.getVerticalComponent(base, true);
                if (toAdd == null) {
                    toAdd = CommonCRS.Vertical.ELLIPSOIDAL.crs();
                }
                crs = new EllipsoidalHeightCombiner(factories).createCompoundCRS(crs, toAdd);
                isAuthoritative = false;
            }
            // If the CRS has been created by this method, replace by an authoritative definition.
            if (!(isAuthoritative || crs == base || CRS.getSingleComponents(base).contains(crs))) {
                crs = CRS.fromAuthority(crs, (factories == null) ? null : factories.getCRSAuthorityFactory(), warningFilter);
            }
            if (hasM) {
                SingleCRS toAdd = CRS.getTemporalComponent(base);
                if (toAdd != null || (toAdd = defaultM) != null) {
                    crs = new EllipsoidalHeightCombiner(factories).createCompoundCRS(crs, toAdd);
                }
            }
        } catch (BackingStoreException e) {
            throw e.unwrapOrRethrow(FactoryException.class);
        }
        return crs;
    }
}
