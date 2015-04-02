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

import org.opengis.parameter.ParameterDescriptor;
import org.opengis.parameter.ParameterDescriptorGroup;
import org.opengis.referencing.operation.CylindricalProjection;
import org.apache.sis.parameter.Parameters;
import org.apache.sis.parameter.ParameterBuilder;
import org.apache.sis.metadata.iso.citation.Citations;
import org.apache.sis.referencing.operation.projection.Mercator;
import org.apache.sis.referencing.operation.projection.NormalizedProjection;


/**
 * The provider for <cite>"Mercator (variant A)"</cite> projection
 * (EPSG:9804, EPSG:1026, <span class="deprecated">EPSG:9841</span>).
 * EPSG defines two codes for this projection, 1026 being the spherical case and 9804 the ellipsoidal case.
 *
 * <p>This provider reuses many of the parameters defined in {@link Mercator2SP}.</p>
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @author  Rueben Schulz (UBC)
 * @since   0.6
 * @version 0.6
 * @module
 *
 * @see <a href="http://www.remotesensing.org/geotiff/proj_list/mercator_1sp.html">Mercator 1SP on RemoteSensing.org</a>
 */
public final class Mercator1SP extends MapProjection {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = -5886510621481710072L;

    /**
     * The name of this projection method.
     */
    static final String NAME = "Mercator (variant A)";

    /**
     * The operation parameter descriptor for the <cite>Latitude of natural origin</cite> (φ₀) parameter value.
     * Valid values range is (-90 … 90)° and default value is 0°.
     *
     * <p>In theory, this parameter should not be used and its value should be 0 in all cases.
     * This parameter is included in the EPSG dataset for completeness in CRS labelling only.</p>
     */
    static final ParameterDescriptor<Double> LATITUDE_OF_ORIGIN;

    /**
     * The operation parameter descriptor for the <cite>Longitude of natural origin</cite> (λ₀) parameter value.
     * Valid values range is [-180 … 180]° and default value is 0°.
     */
    static final ParameterDescriptor<Double> CENTRAL_MERIDIAN;

    /**
     * The operation parameter descriptor for the <cite>Scale factor at natural origin</cite> (k₀) parameter value.
     * Valid values range is (0 … ∞) and default value is 1.
     */
    static final ParameterDescriptor<Double> SCALE_FACTOR;

    /**
     * The operation parameter descriptor for the <cite>False easting</cite> (FE) parameter value.
     * Valid values range is unrestricted and default value is 0 metre.
     */
    static final ParameterDescriptor<Double> FALSE_EASTING;

    /**
     * The operation parameter descriptor for the <cite>False northing</cite> (FN) parameter value.
     * Valid values range is unrestricted and default value is 0 metre.
     */
    static final ParameterDescriptor<Double> FALSE_NORTHING;

    /**
     * The group of all parameters expected by this coordinate operation.
     */
    public static final ParameterDescriptorGroup PARAMETERS;
    static {
        final ParameterBuilder builder = builder();

        LATITUDE_OF_ORIGIN = createConstant(builder.addNamesAndIdentifiers(Mercator2SP.LATITUDE_OF_ORIGIN)
                .replaceNames(Citations.ESRI,    (String[]) null)
                .replaceNames(Citations.NETCDF,  (String[]) null)
                .setRemarks(Mercator2SP.LATITUDE_OF_ORIGIN.getRemarks()), 0.0);

        CENTRAL_MERIDIAN = createLongitude(builder.addNamesAndIdentifiers(Mercator2SP.CENTRAL_MERIDIAN)
                .replaceNames(Citations.ESRI,    (String[]) null)
                .replaceNames(Citations.NETCDF,  (String[]) null));

        SCALE_FACTOR = createScale(builder.addNamesAndIdentifiers(Mercator2SP.SCALE_FACTOR)
                .replaceNames(Citations.ESRI,    (String[]) null)
                .replaceNames(Citations.NETCDF,  (String[]) null));

        FALSE_EASTING = createShift(builder.addNamesAndIdentifiers(Mercator2SP.FALSE_EASTING)
                .replaceNames(Citations.ESRI,    (String[]) null)
                .replaceNames(Citations.NETCDF,  (String[]) null));

        FALSE_NORTHING = createShift(builder.addNamesAndIdentifiers(Mercator2SP.FALSE_NORTHING)
                .replaceNames(Citations.ESRI,    (String[]) null)
                .replaceNames(Citations.NETCDF,  (String[]) null));

        PARAMETERS = builder
            .addIdentifier(             "9804")                                                   // The ellipsoidal case
            .addIdentifier(             "1026")                                                   // The spherical case
            .addDeprecatedIdentifier(   "9841", "1026")                                           // The spherical (1SP) case
            .addName(NAME            /* "Mercator (variant A)" */)                                // Starting from EPSG version 7.6
            .addName(                   "Mercator (Spherical)")                                   // Starting from EPSG version 7.6
            .addName(                   "Mercator (1SP)")                                         // Prior to EPSG version 7.6
            .addDeprecatedName(         "Mercator (1SP) (Spherical)", "Mercator (Spherical)")     // Prior to EPSG version 7.6
            .addName(Citations.OGC,     "Mercator_1SP")
            .addName(sameNameAs(Citations.GEOTIFF, Mercator2SP.PARAMETERS))
            .addName(sameNameAs(Citations.PROJ4,   Mercator2SP.PARAMETERS))
            .addIdentifier(Citations.GEOTIFF,   "7")
            .addIdentifier(Citations.MAP_INFO, "10")    // MapInfo names this projection "Mercator".
            .createGroupForMapProjection(
                    LATITUDE_OF_ORIGIN,
                    CENTRAL_MERIDIAN,
                    SCALE_FACTOR,
                    FALSE_EASTING,
                    FALSE_NORTHING);
    }

    /**
     * Constructs a new provider.
     */
    public Mercator1SP() {
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
    protected NormalizedProjection createProjection(final Parameters parameters) {
        return new Mercator(this, parameters);
    }
}
