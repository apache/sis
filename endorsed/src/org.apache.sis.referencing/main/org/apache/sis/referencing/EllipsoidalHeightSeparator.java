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
package org.apache.sis.referencing;

import org.opengis.util.FactoryException;
import org.opengis.referencing.cs.VerticalCS;
import org.opengis.referencing.cs.CartesianCS;
import org.opengis.referencing.cs.EllipsoidalCS;
import org.opengis.referencing.cs.CoordinateSystem;
import org.opengis.referencing.cs.CoordinateSystemAxis;
import org.opengis.referencing.crs.CRSFactory;
import org.opengis.referencing.crs.SingleCRS;
import org.opengis.referencing.crs.VerticalCRS;
import org.opengis.referencing.crs.ProjectedCRS;
import org.opengis.referencing.crs.GeodeticCRS;
import org.opengis.referencing.datum.GeodeticDatum;
import org.opengis.referencing.operation.Conversion;
import org.apache.sis.referencing.internal.shared.AxisDirections;
import org.apache.sis.referencing.internal.shared.ReferencingUtilities;
import org.apache.sis.referencing.cs.CoordinateSystems;
import org.apache.sis.referencing.cs.AxisFilter;
import org.apache.sis.referencing.operation.DefaultConversion;
import org.apache.sis.util.Utilities;
import org.apache.sis.util.resources.Errors;
import org.apache.sis.referencing.factory.GeodeticObjectFactory;
import static org.apache.sis.referencing.internal.shared.ReferencingUtilities.getPropertiesForModifiedCRS;

// Specific to the main and geoapi-3.1 branches:
import org.opengis.referencing.crs.GeographicCRS;

// Specific to the main branch:
import org.apache.sis.referencing.datum.DatumOrEnsemble;
import org.apache.sis.referencing.datum.DefaultDatumEnsemble;
import static org.apache.sis.pending.geoapi.referencing.MissingMethods.getDatumEnsemble;


/**
 * Helper class for separating the ellipsoidal height from the horizontal part of a CRS.
 * This is the converse of {@link org.apache.sis.referencing.internal.shared.EllipsoidalHeightCombiner}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 *
 * @see org.apache.sis.referencing.internal.shared.EllipsoidalHeightCombiner
 */
final class EllipsoidalHeightSeparator implements AxisFilter {
    /**
     * The value of {@link SingleCRS#getDatum()}.
     */
    private final GeodeticDatum datum;

    /**
     * The datum ensemble of the <abbr>CRS</abbr> to separate, or {@code null} if none.
     */
    private final DefaultDatumEnsemble<GeodeticDatum> ensemble;

    /**
     * Workaround for GeoAPI 3.0 (to be removed with GeoAPI 3.1).
     */
    private final GeodeticDatum pseudo;

    /**
     * Whether to extract the vertical component ({@code true}) or the horizontal component ({@code false}).
     */
    private final boolean vertical;

    /**
     * Creates a new separator for a CRS having the given base.
     *
     * @param  baseCRS   the CRS to separate, or the base CRS of the projected CRS to separate.
     * @param  vertical  whether to extract the vertical component ({@code true}) or the horizontal component ({@code false}).
     */
    EllipsoidalHeightSeparator(final GeodeticCRS baseCRS, final boolean vertical) {
        this.datum    = baseCRS.getDatum();
        this.ensemble = getDatumEnsemble(baseCRS);
        this.pseudo   = DatumOrEnsemble.asDatum(baseCRS);
        this.vertical = vertical;
    }

    /**
     * Returns {@code true} if the given axis shall be included in the new coordinate system.
     */
    @Override
    public boolean accept(final CoordinateSystemAxis axis) {
        return AxisDirections.isVertical(axis.getDirection()) == vertical;
    }

    /**
     * The factory to use for creating new coordinate reference system.
     */
    private static CRSFactory factory() {
        return GeodeticObjectFactory.provider();
    }

    /**
     * Extracts the horizontal or vertical component of the coordinate reference system.
     *
     * @param  crs  the coordinate reference system from which to extract the horizontal or vertical component.
     * @return the requested component.
     * @throws IllegalArgumentException if the specified coordinate system cannot be filtered.
     *         It may be because the coordinate system would contain an illegal number of axes,
     *         or because an axis would have an unexpected direction or unexpected unit of measurement.
     * @throws ClassCastException if a coordinate system is not of the expected type.
     */
    SingleCRS separate(final SingleCRS crs) throws FactoryException {
        final CoordinateSystem cs = CoordinateSystems.replaceAxes(crs.getCoordinateSystem(), this);
        if (vertical) {
            VerticalCRS component = CommonCRS.Vertical.ELLIPSOIDAL.crs();
            if (!Utilities.equalsIgnoreMetadata(component.getCoordinateSystem(), cs)) {
                component = factory().createVerticalCRS(getPropertiesForModifiedCRS(component),
                                                        DatumOrEnsemble.asDatum(component),
                                                        (VerticalCS) cs);
            }
            return component;
        }
        /*
         * Horizontal CRS requested. If geographic, try to use one of the predefined instances if suitable.
         * If no predefined instance match, create a new CRS.
         */
        if (crs instanceof GeodeticCRS) {
            if (!(cs instanceof EllipsoidalCS)) {
                throw new IllegalArgumentException(Errors.format(
                        Errors.Keys.UnsupportedCoordinateSystem_1, IdentifiedObjects.getName(cs, null)));
            }
            final CommonCRS ref = CommonCRS.WGS84;
            if (Utilities.equalsIgnoreMetadata(ref.geographic().getCoordinateSystem(), cs)) {
                final CommonCRS c = CommonCRS.forDatum(datum, ensemble);
                if (c != null) return c.geographic();
            } else if (Utilities.equalsIgnoreMetadata(ref.normalizedGeographic().getCoordinateSystem(), cs)) {
                final CommonCRS c = CommonCRS.forDatum(datum, ensemble);
                if (c != null) return c.normalizedGeographic();
            }
            return factory().createGeographicCRS(getPropertiesForModifiedCRS(crs), pseudo, (EllipsoidalCS) cs);
        }
        /*
         * In the projected CRS case, in addition of reducing the number of dimensions in the CartesianCS,
         * we also need to reduce the number of dimensions in the base CRS and in the conversion.
         */
        if (crs instanceof ProjectedCRS) {
            GeographicCRS baseCRS = ((ProjectedCRS) crs).getBaseCRS();
            if (ReferencingUtilities.getDimension(baseCRS) != 2) {
                baseCRS = (GeographicCRS) separate(baseCRS);
            }
            Conversion projection = ((ProjectedCRS) crs).getConversionFromBase();
            /*
             * The conversion object of the given CRS has a base (source) CRS and a target CRS that are not
             * the ones of the new `ProjectedCRS` to create. In addition it has a `MathTransform` expecting
             * three-dimensional coordinates, while we need 2 dimensions. We cannot use that transform even
             * after reducing its number of dimensions (with `TransformSeparator`) because the `ProjectedCRS`
             * constructor expects a normalized transform, while the `projection.getMathTransform()` may not
             * be normalized. So we are better to let constructor recreate the transform from the parameters.
             */
            projection = new DefaultConversion(getPropertiesForModifiedCRS(projection),
                                projection.getMethod(), null, projection.getParameterValues());
            return factory().createProjectedCRS(getPropertiesForModifiedCRS(crs), baseCRS, projection, (CartesianCS) cs);
        }
        throw new IllegalArgumentException(Errors.format(Errors.Keys.UnsupportedType_1, crs.getClass()));
    }
}
