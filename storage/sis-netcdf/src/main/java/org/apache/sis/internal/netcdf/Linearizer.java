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
import org.opengis.util.FactoryException;
import org.opengis.parameter.ParameterValueGroup;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.MathTransformFactory;
import org.apache.sis.referencing.operation.builder.LocalizationGridBuilder;
import org.apache.sis.internal.metadata.ReferencingServices;
import org.apache.sis.internal.system.DefaultFactories;
import org.apache.sis.internal.system.Modules;
import org.apache.sis.internal.util.Constants;
import org.apache.sis.util.logging.Logging;


/**
 * Two-dimensional non-linear transforms to try in attempts to make a localization grid more linear.
 * Non-linear transforms are tested in "trials and errors" and the one resulting in best correlation
 * coefficients is selected. This enumeration identifies which linearizers to try for a given file.
 *
 * <p>When a non-linear transform exists in spherical or ellipsoidal variants, we use the spherical
 * formulas instead than ellipsoidal formulas because the spherical ones are faster and more stable
 * (because the inverse transforms are exact, up to rounding errors).  The errors caused by the use
 * of spherical formulas are compensated by the localization grid used after the linearizer.</p>
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.0
 *
 * @see org.apache.sis.referencing.operation.builder.LocalizationGridBuilder#addLinearizers(Map, int...)
 *
 * @since 1.0
 * @module
 */
public enum Linearizer {
    /**
     * Mercator (Spherical) projection. Inputs are latitude and longitude in any order (axis order will
     * be detected by inspection of {@link Axis} elements). Outputs are projected coordinates.
     */
    MERCATOR("Mercator (Spherical)");

    /**
     * The map projection method to use for constructing {@link #transform}, or {@code null} if the operation
     * is not a map projection or has already be constructed.  If non-null, the identified operation requires
     * (<var>longitude</var>, <var>latitude</var>) axis order.
     */
    private String projection;

    /**
     * The transform to apply, or {@code null} if none or not yet created. This is created by {@link #transform()}
     * when first needed. The value after initialization may still be {@code null} if initialization failed.
     */
    private MathTransform transform;

    /**
     * Creates a new linearizer for the given projection method.
     */
    private Linearizer(final String projection) {
        this.projection = projection;
    }

    /**
     * Returns the hard-coded transform represented by this enumeration that may help to make a localization grid
     * more linear.
     */
    private synchronized MathTransform transform() {
        final String p = projection;
        if (p != null) {
            projection = null;                              // Set to null now in case of failure.
            final MathTransformFactory factory = DefaultFactories.forClass(MathTransformFactory.class);
            if (factory != null) try {    // Should never be null, but be tolerant to configuration oddity.
                /*
                 * The exact value of sphere radius does not matter because a linear regression will
                 * be applied anyway. However it matter to define a sphere instead than an ellipsoid
                 * because the spherical equations are simpler (consequently faster and more stable).
                 */
                final ParameterValueGroup pg = factory.getDefaultParameters(p);
                pg.parameter(Constants.SEMI_MAJOR).setValue(ReferencingServices.AUTHALIC_RADIUS);
                pg.parameter(Constants.SEMI_MINOR).setValue(ReferencingServices.AUTHALIC_RADIUS);
                transform = factory.createParameterizedTransform(pg);
            } catch (FactoryException e) {
                /*
                 * Should never happen. But if it happens anyway, do not cause the whole netCDF reader
                 * to fail for all files because of this error. Declare this error as originating from
                 * Variable.getGridGeometry() because it is the caller (indirectly) for this class.
                 */
                Logging.unexpectedException(Logging.getLogger(Modules.NETCDF), Variable.class, "getGridGeometry", e);
            }
        }
        return transform;
    }

    /**
     * Applies non-linear transform candidates to the given localization grid.
     *
     * @param  linearizers  the linearizers to apply.
     * @param  grid         the grid on which to add non-linear transform candidates.
     * @param  axes         coordinate system axes in CRS order.
     */
    static void applyTo(final Set<Linearizer> linearizers, final LocalizationGridBuilder grid, final Axis[] axes) {
        int xdim = -1, ydim = -1;
        for (int i=axes.length; --i >= 0;) {
            switch (axes[i].abbreviation) {
                case 'λ': xdim = i; break;
                case 'φ': ydim = i; break;
            }
        }
        if (xdim >= 0 && ydim >= 0) {
            final Map<String,MathTransform> projections = new HashMap<>();
            for (final Linearizer linearizer : linearizers) {
                if (linearizer.transform != null) {
                    projections.put(linearizer.name(), linearizer.transform());
                }
            }
            grid.addLinearizers(projections, xdim, ydim);
        }
    }
}
