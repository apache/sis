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

import org.opengis.util.GenericName;
import org.opengis.parameter.ParameterValueGroup;
import org.opengis.parameter.ParameterDescriptor;
import org.opengis.parameter.ParameterDescriptorGroup;
import org.opengis.referencing.operation.MathTransform2D;
import org.opengis.referencing.operation.CylindricalProjection;
import org.apache.sis.parameter.ParameterBuilder;
import org.apache.sis.metadata.iso.citation.Citations;
import org.apache.sis.internal.referencing.DeprecatedCode;
import org.apache.sis.internal.referencing.DeprecatedName;


/**
 * The provider for "<cite>Mercator (variant A)</cite>" projection
 * (EPSG:9804, EPSG:1026, <span class="deprecated">EPSG:9841</span>).
 * EPSG defines two codes for this projection, 1026 being the spherical case and 9804 the ellipsoidal case.
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
     * The operation parameter descriptor for the {@linkplain
     * org.apache.sis.referencing.operation.projection.UnitaryProjection.Parameters#centralMeridian
     * central meridian} parameter value.
     *
     * This parameter is mandatory.
     * Valid values range is [-180 … 180]° and default value is 0°.
     */
    public static final ParameterDescriptor<Double> CENTRAL_MERIDIAN;

    /**
     * The operation parameter descriptor for the {@linkplain
     * org.apache.sis.referencing.operation.projection.UnitaryProjection.Parameters#latitudeOfOrigin
     * latitude of origin} parameter value.
     *
     * This parameter is mandatory.
     * Valid values range is [-90 … 90]° and default value is 0°.
     */
    public static final ParameterDescriptor<Double> LATITUDE_OF_ORIGIN;

    /**
     * The operation parameter descriptor for the {@linkplain
     * org.apache.sis.referencing.operation.projection.UnitaryProjection.Parameters#scaleFactor
     * scale factor} parameter value.
     *
     * This parameter is mandatory.
     * Valid values range is (0 … ∞) and default value is 1.
     */
    public static final ParameterDescriptor<Double> SCALE_FACTOR;

    /**
     * The operation parameter descriptor for the {@linkplain
     * org.apache.sis.referencing.operation.projection.UnitaryProjection.Parameters#falseEasting
     * false easting} parameter value.
     *
     * This parameter is mandatory.
     * Valid values range is unrestricted and default value is 0 metre.
     */
    public static final ParameterDescriptor<Double> FALSE_EASTING;

    /**
     * The operation parameter descriptor for the {@linkplain
     * org.apache.sis.referencing.operation.projection.UnitaryProjection.Parameters#falseNorthing
     * false northing} parameter value.
     *
     * This parameter is mandatory.
     * Valid values range is unrestricted and default value is 0 metre.
     */
    public static final ParameterDescriptor<Double> FALSE_NORTHING;

    /**
     * The group of all parameters expected by this coordinate operation.
     */
    private static final ParameterDescriptorGroup PARAMETERS;
    static {
        final ParameterBuilder builder = builder();
        LATITUDE_OF_ORIGIN = createLatitude(builder
                .addIdentifier("8801")
                .addName("Latitude of natural origin")
                .addName(Citations.OGC,     "latitude_of_origin")
                .addName(Citations.GEOTIFF, "NatOriginLat")
                .addName(Citations.PROJ4,   "lat_0"), false);

        CENTRAL_MERIDIAN = createLongitude(builder
                .addIdentifier("8802")
                .addName("Longitude of natural origin")
                .addName(Citations.OGC,     "central_meridian")
                .addName(Citations.GEOTIFF, "NatOriginLong")
                .addName(Citations.PROJ4,   "lon_0"));

        SCALE_FACTOR = createScale(builder
                .addIdentifier("8805")
                .addName("Scale factor at natural origin")
                .addName(Citations.OGC,     "scale_factor")
                .addName(Citations.GEOTIFF, "ScaleAtNatOrigin")
                .addName(Citations.PROJ4,   "k"));

        FALSE_EASTING = createShift(builder
                .addIdentifier("8806")
                .addName("False easting")
                .addName(Citations.OGC,     "false_easting")
                .addName(Citations.GEOTIFF, "FalseEasting")
                .addName(Citations.PROJ4,   "x_0"));

        FALSE_NORTHING = createShift(builder
                .addIdentifier("8807")
                .addName("False northing")
                .addName(Citations.OGC,     "false_northing")
                .addName(Citations.GEOTIFF, "FalseNorthing")
                .addName(Citations.PROJ4,   "y_0"));

        PARAMETERS = builder
            .addIdentifier("9804")              // The ellipsoidal case
            .addIdentifier("1026")              // The spherical case
            .addName("Mercator (variant A)")    // Starting from EPSG version 7.6
            .addName("Mercator (Spherical)")
            .addIdentifier(        new DeprecatedCode((short) 9841, (short) 1026))      // The spherical (1SP) case
            .addName((GenericName) new DeprecatedName("Mercator (1SP)"))                // Prior to EPSG version 7.6
            .addName((GenericName) new DeprecatedName("Mercator (1SP) (Spherical)"))
            .addName(Citations.OGC,     "Mercator_1SP")
            .addName(Citations.GEOTIFF, "CT_Mercator")
            .addName(Citations.PROJ4,   "merc")
            .addIdentifier(Citations.GEOTIFF,  "7")
//          .addIdentifier(Citations.MAP_INFO, "10")
//          .addIdentifier(Citations.MAP_INFO, "26")
            .createGroupForMapProjection(LATITUDE_OF_ORIGIN, CENTRAL_MERIDIAN,
                    SCALE_FACTOR, FALSE_EASTING, FALSE_NORTHING);
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
    public MathTransform2D createMathTransform(ParameterValueGroup values) {
        return null; // TODO Mercator.create(getParameters(), values);
    }
}
