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

import javax.xml.bind.annotation.XmlTransient;
import org.opengis.parameter.ParameterDescriptor;
import org.opengis.parameter.ParameterDescriptorGroup;
import org.apache.sis.internal.util.Constants;
import org.apache.sis.parameter.ParameterBuilder;
import org.apache.sis.metadata.iso.citation.Citations;


/**
 * The provider for <cite>"Mercator (variant A)"</cite> projection (EPSG:9804).
 * EPSG defines two projections with the same parameters, 1026 being the spherical case and 9804 the ellipsoidal case.
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @author  Rueben Schulz (UBC)
 * @since   0.6
 * @version 0.6
 * @module
 *
 * @see <a href="http://www.remotesensing.org/geotiff/proj_list/mercator_1sp.html">Mercator 1SP on RemoteSensing.org</a>
 */
@XmlTransient
public final class Mercator1SP extends AbstractMercator {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = -5886510621481710072L;

    /**
     * The operation parameter descriptor for the <cite>Latitude of natural origin</cite> (φ₀) parameter value.
     * In theory, this parameter should not be used and its value should be 0 in all cases.
     * This parameter is included in the EPSG dataset for completeness in CRS labelling only.
     */
    public static final ParameterDescriptor<Double> LATITUDE_OF_ORIGIN;

    /**
     * The operation parameter descriptor for the <cite>Longitude of natural origin</cite> (λ₀) parameter value.
     * Valid values range is [-180 … 180]° and default value is 0°.
     */
    public static final ParameterDescriptor<Double> LONGITUDE_OF_ORIGIN;

    /**
     * The operation parameter descriptor for the <cite>Scale factor at natural origin</cite> (k₀) parameter value.
     * Valid values range is (0 … ∞) and default value is 1.
     */
    public static final ParameterDescriptor<Double> SCALE_FACTOR;

    /**
     * The group of all parameters expected by this coordinate operation.
     */
    static final ParameterDescriptorGroup PARAMETERS;
    static {
        final ParameterBuilder builder = builder();
        LATITUDE_OF_ORIGIN = createZeroConstant(builder.addNamesAndIdentifiers(Equirectangular.LATITUDE_OF_ORIGIN)
                .rename(Citations.GEOTIFF, "NatOriginLat")
                .setRemarks(Equirectangular.LATITUDE_OF_ORIGIN.getRemarks()));

        LONGITUDE_OF_ORIGIN = createLongitude(builder.addNamesAndIdentifiers(Equirectangular.LONGITUDE_OF_ORIGIN)
                .rename(Citations.GEOTIFF, "NatOriginLong"));

        SCALE_FACTOR = createScale(builder
                .addIdentifier("8805")
                .addName("Scale factor at natural origin")
                .addName(Citations.OGC,     Constants.SCALE_FACTOR)
                .addName(Citations.ESRI,    "Scale_Factor")
                .addName(Citations.NETCDF,  "scale_factor_at_projection_origin")
                .addName(Citations.GEOTIFF, "ScaleAtNatOrigin")
                .addName(Citations.PROJ4,   "k"));

        PARAMETERS = builder
                .addIdentifier(              "9804")                        // The ellipsoidal case
                .addName(                    "Mercator (variant A)")        // Starting from EPSG version 7.6
                .addName(                    "Mercator (1SP)")              // Prior to EPSG version 7.6
                .addName(Citations.OGC,      "Mercator_1SP")
                .addName(Citations.GEOTIFF,  "CT_Mercator")
                .addName(Citations.PROJ4,    "merc")
                .addIdentifier(Citations.GEOTIFF,   "7")
                .addIdentifier(Citations.MAP_INFO, "10")    // MapInfo names this projection "Mercator".
                .createGroupForMapProjection(
                        LATITUDE_OF_ORIGIN,
                        LONGITUDE_OF_ORIGIN,
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
}
