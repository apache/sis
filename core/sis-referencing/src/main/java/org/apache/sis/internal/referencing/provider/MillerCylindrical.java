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
package org.apache.sis.internal.referencing.provider;

import org.opengis.util.InternationalString;
import org.opengis.parameter.ParameterDescriptor;
import org.opengis.parameter.ParameterValueGroup;
import org.opengis.parameter.ParameterDescriptorGroup;
import org.opengis.referencing.operation.MathTransform2D;
import org.opengis.referencing.operation.CylindricalProjection;
import org.apache.sis.internal.util.Constants;
import org.apache.sis.metadata.iso.citation.Citations;
import org.apache.sis.parameter.ParameterBuilder;


/**
 * The provider for "<cite>Miller Cylindrical</cite>" projection.
 * This is a {@link Mercator1SP} projection with the following modifications:
 *
 * <ol>
 *   <li>The latitude of parallels are scaled by a factor of 0.8 before the projection (actually 2×0.4 where
 *       the factor 2 is required for canceling the scaling performed by the classical Mercator formula).</li>
 *   <li>The northing is multiplied by 1.25 after the projection.</li>
 * </ol>
 *
 * Note that the Miller projection is typically used with spherical formulas. However the Apache SIS implementation
 * supports also the ellipsoidal formulas. If spherical formulas are desired, then the parameters shall contains
 * semi-major and semi-minor axis lengths of equal length.
 *
 * <div class="section">Additional identifiers:</div>
 * This projection has the following identifiers from the French mapping agency (IGNF),
 * which have not yet been declared in this class:
 *
 * <ul>
 *   <li>Name {@code "Miller_Cylindrical_Sphere"}</li>
 *   <li>Identifier {@code "PRC9901"}</li>
 * </ul>
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.6
 * @version 0.6
 *
 * @see <a href="http://www.remotesensing.org/geotiff/proj_list/miller_cylindrical.html">Miller Cylindrical on RemoteSensing.org</a>
 */
public class MillerCylindrical extends MapProjection {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = -7682370461334391883L;

    /**
     * The name of this operation method.
     */
    public static final String NAME = "Miller_Cylindrical";

    /**
     * The group of all parameters expected by this coordinate operation.
     */
    public static final ParameterDescriptorGroup PARAMETERS;
    static {
        final ParameterBuilder builder = builder().setCodeSpace(Citations.OGC, Constants.OGC);

        final ParameterDescriptor<?> latitudeOfCenter = createLatitude(
                builder.addName("latitude_of_center")
                       .addName(Citations.GEOTIFF, "ProjCenterLat")
                       .addName(sameNameAs(Citations.PROJ4, Mercator1SP.LATITUDE_OF_ORIGIN)), false);

        final ParameterDescriptor<?> longitudeOfCenter = createLongitude(
                builder.addName("longitude_of_center")
                       .addName(Citations.GEOTIFF, "ProjCenterLong")
                       .addName(sameNameAs(Citations.PROJ4, Mercator1SP.CENTRAL_MERIDIAN)));

        final ParameterDescriptor<?> falseEasting  = createShift(exceptEPSG(Mercator1SP.FALSE_EASTING,  builder));
        final ParameterDescriptor<?> falseNorthing = createShift(exceptEPSG(Mercator1SP.FALSE_NORTHING, builder));
        /*
         * The scale factor is not formally a parameter of the "Miller Cylindrical" projection.
         * But we declare it as an optional parameters because it is sometime used.
         */
        final InternationalString remarks = notFormalParameter(Mercator1SP.NAME, "Miller Cylindrical");
        final ParameterDescriptor<?> scaleFactor = createScale(exceptEPSG(Mercator1SP.SCALE_FACTOR, builder)
                .setRemarks(remarks).setRequired(false));

        PARAMETERS = builder
            .addName      (NAME)
            .addName      (Citations.GEOTIFF,  "CT_MillerCylindrical")
            .addIdentifier(Citations.GEOTIFF,  "20")
            .addName      (Citations.PROJ4,    "mill")
            .addIdentifier(Citations.MAP_INFO, "11")
            .createGroupForMapProjection(
                    latitudeOfCenter,
                    longitudeOfCenter,
                    scaleFactor, // Not an official parameter, provided for compatibility with those who still use it.
                    falseEasting,
                    falseNorthing);
    }

    /**
     * Constructs a new provider.
     */
    public MillerCylindrical() {
        super(PARAMETERS);
    }

    /**
     * Returns the operation type for this map projection.
     *
     * @return {@code CylindricalProjection.class}
     */
    @Override
    public Class<CylindricalProjection> getOperationType() {
        return CylindricalProjection.class;
    }

    /**
     * {@inheritDoc}
     *
     * @return The map projection created from the given parameter values.
     */
    @Override
    public MathTransform2D createMathTransform(ParameterValueGroup values) {
        return null; // TODO Mercator.create(this, values);
    }
}
