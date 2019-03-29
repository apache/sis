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
 * We use spherical formulas instead than ellipsoidal formulas because the spherical ones are faster
 * and more stable (the inverse transforms are exact, up to rounding errors). Non-linear transforms
 * are tested in "trials and errors" and the one resulting in the best correlation coefficients is
 * selected.
 *
 * <p>Current implementation provides a hard-coded list of linearized, but future version may allow
 * customization depending on the netCDF file being decoded.</p>
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.0
 *
 * @see org.apache.sis.referencing.operation.builder.LocalizationGridBuilder#addLinearizers(Map, int...)
 *
 * @since 1.0
 * @module
 */
final class Linearizer {
    /**
     * Hard-coded set of transforms that may help in making localization grids more linear.
     * Operations defined in this map require (<var>longitude</var>, <var>latitude</var>) axis order.
     */
    private static final Map<String,MathTransform> PROJECTIONS = new HashMap<>(4);
    static {
        final MathTransformFactory factory = DefaultFactories.forClass(MathTransformFactory.class);
        if (factory != null) {    // Should never be null, but be tolerant to configuration oddity.
            final String[] projections = new String[] {
                "Mercator (Spherical)"
                // More projections may be added in the future.
            };
            /*
             * The exact value of sphere radius does not matter because a linear regression will
             * be applied anyway. However it matter to define a sphere instead than an ellipsoid
             * because the spherical equations are simpler (consequently faster and more stable).
             */
            for (final String operation : projections) try {
                final ParameterValueGroup pg = factory.getDefaultParameters(operation);
                pg.parameter(Constants.SEMI_MAJOR).setValue(ReferencingServices.AUTHALIC_RADIUS);
                pg.parameter(Constants.SEMI_MINOR).setValue(ReferencingServices.AUTHALIC_RADIUS);
                PROJECTIONS.put(operation, factory.createParameterizedTransform(pg));
            } catch (FactoryException e) {
                /*
                 * Should never happen. But if it happens anyway, do not cause the whole netCDF reader
                 * to fail for all files because of this error. Declare this error as originating from
                 * Variable.getGridGeometry() because it is the caller (indirectly) for this class.
                 */
                Logging.unexpectedException(Logging.getLogger(Modules.NETCDF), Variable.class, "getGridGeometry", e);
            }
        }
    }

    /**
     * Do not allow instantiation of this class.
     */
    private Linearizer() {
    }

    /**
     * Applies non-linear transform candidates to the given localization grid.
     *
     * @param  grid  the grid on which to add non-linear transform candidates.
     * @param  axes  coordinate system axes in CRS order.
     */
    static void applyTo(final LocalizationGridBuilder grid, final Axis[] axes) {
        int xdim = -1, ydim = -1;
        for (int i=axes.length; --i >= 0;) {
            switch (axes[i].abbreviation) {
                case 'λ': xdim = i; break;
                case 'φ': ydim = i; break;
            }
        }
        if (xdim >= 0 && ydim >= 0) {
            grid.addLinearizers(PROJECTIONS, xdim, ydim);
        }
    }
}
