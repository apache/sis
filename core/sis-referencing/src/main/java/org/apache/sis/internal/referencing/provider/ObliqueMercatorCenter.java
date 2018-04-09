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
import org.apache.sis.parameter.ParameterBuilder;
import org.apache.sis.metadata.iso.citation.Citations;

import static org.apache.sis.referencing.IdentifiedObjects.getIdentifier;


/**
 * The provider for <cite>"Hotine Oblique Mercator (variant B)"</cite> projection (EPSG:9815).
 *
 * @author  Rueben Schulz (UBC)
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.0
 * @since   1.0
 * @module
 */
@XmlTransient
public final class ObliqueMercatorCenter extends ObliqueMercator {
    /**
     * For compatibility with different versions during deserialization.
     */
    private static final long serialVersionUID = 1404490143062736127L;

    /**
     * The {@value} EPSG identifier for <cite>"Hotine Oblique Mercator (variant B)"</cite>.
     * Should be preferred to the name when available.
     */
    public static final String IDENTIFIER = "9815";

    /**
     * The operation parameter descriptor for the <cite>Easting at projection centre</cite> (Ec) parameter value.
     * Valid values range is unrestricted and default value is 0 metre.
     */
    public static final ParameterDescriptor<Double> EASTING_AT_CENTRE;

    /**
     * The operation parameter descriptor for the <cite>Northing at projection centre</cite> (Nc) parameter value.
     * Valid values range is unrestricted and default value is 0 metre.
     */
    public static final ParameterDescriptor<Double> NORTHING_AT_CENTRE;

    /**
     * The group of all parameters expected by this coordinate operation.
     */
    private static final ParameterDescriptorGroup PARAMETERS;
    static {
        final ParameterBuilder builder = builder();

        EASTING_AT_CENTRE = createShift(builder
                .addNamesAndIdentifiers(FALSE_EASTING)
                .reidentify(Citations.EPSG,    "8816")
                .reidentify(Citations.GEOTIFF, "3090")
                .rename(Citations.EPSG, "Easting at projection centre")
                .rename(Citations.GEOTIFF, "CenterEasting")
                .rename(Citations.NETCDF));                                 // Delete the netCDF name.

        NORTHING_AT_CENTRE = createShift(builder
                .addNamesAndIdentifiers(FALSE_NORTHING)
                .reidentify(Citations.EPSG,    "8817")
                .reidentify(Citations.GEOTIFF, "3091")
                .rename(Citations.EPSG, "Northing at projection centre")
                .rename(Citations.GEOTIFF, "CenterNorthing")
                .rename(Citations.NETCDF));                                 // Delete the netCDF name.

        PARAMETERS = builder
                .addIdentifier(IDENTIFIER)
                .addName("Hotine Oblique Mercator (variant B)")             // Starting from EPSG version 7.6
                .addName("Rectified Skew Orthomorphic (RSO)")               // Alias
                .addName("Oblique Mercator")                                // Prior to EPSG version 7.6
                .addName      (Citations.OGC,     "Oblique_Mercator")
                .addName      (Citations.ESRI,    "Hotine_Oblique_Mercator_Azimuth_Center")
                .addName      (Citations.ESRI,    "Rectified_Skew_Orthomorphic_Center")
                .addName      (Citations.S57,     "Oblique Mercator")
                .addName      (Citations.S57,     "OME")
                .addIdentifier(Citations.S57,     "9")
                .addName      (Citations.GEOTIFF, "CT_ObliqueMercator")
                .addIdentifier(getIdentifier(PARAMETERS_A, Citations.GEOTIFF))      // Same GeoTIFF identifier.
                .addName      (Citations.PROJ4,   "omerc")
                .createGroupForMapProjection(
                        LATITUDE_OF_CENTRE,
                        LONGITUDE_OF_CENTRE,
                        AZIMUTH,
                        RECTIFIED_GRID_ANGLE,
                        SCALE_FACTOR,
                        EASTING_AT_CENTRE,
                        NORTHING_AT_CENTRE);
    }

    /**
     * Constructs a new provider.
     */
    public ObliqueMercatorCenter() {
        super(PARAMETERS);
    }
}
