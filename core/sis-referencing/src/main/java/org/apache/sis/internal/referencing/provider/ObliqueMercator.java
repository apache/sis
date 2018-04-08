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
import org.apache.sis.parameter.Parameters;
import org.apache.sis.parameter.ParameterBuilder;
import org.apache.sis.metadata.iso.citation.Citations;
import org.apache.sis.referencing.operation.projection.NormalizedProjection;
import org.apache.sis.measure.Units;



/**
 * The provider for <cite>"Hotine Oblique Mercator (variant A)"</cite> projection (EPSG:9812).
 * Also the parent class for all variants.
 *
 * @author  Rueben Schulz (UBC)
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.0
 * @since   1.0
 * @module
 */
@XmlTransient
public class ObliqueMercator extends AbstractMercator {
    /**
     * For compatibility with different versions during deserialization.
     */
    private static final long serialVersionUID = 9100327311220773612L;

    /**
     * The {@value} EPSG identifier for <cite>"Hotine Oblique Mercator (variant A)"</cite>.
     * Should be preferred to the name when available.
     */
    public static final String IDENTIFIER_A = "9812";

    /**
     * The operation parameter descriptor for the <cite>Latitude of projection centre</cite> (φc) parameter value.
     * Valid values range is [-90 … 90]° and default value is 0°.
     */
    public static final ParameterDescriptor<Double> LATITUDE_OF_CENTRE;

    /**
     * The operation parameter descriptor for the <cite>Longitude of projection centre</cite> (λc) parameter value.
     * Valid values range is [-180 … 180]° and default value is 0°.
     */
    public static final ParameterDescriptor<Double> LONGITUDE_OF_CENTRE;

    /**
     * The operation parameter descriptor for the <cite>Azimuth of initial line</cite> (α) parameter value.
     * Valid values ranges are [-360 … -270]°, [-90 … 90]° and [270 … 360]°. There is no default value.
     */
    public static final ParameterDescriptor<Double> AZIMUTH;

    /**
     * The operation parameter descriptor for the <cite>Angle from rectified to skew grid</cite> (γ) parameter value.
     * Valid values range is [-360 … 360]° and default value is the value given to the {@link #AZIMUTH} parameter.
     */
    public static final ParameterDescriptor<Double> RECTIFIED_GRID_ANGLE;

    /**
     * The operation parameter descriptor for the <cite>Scale factor on initial line</cite> (k) parameter value.
     * Valid values range is (0 … ∞) and default value is 1.
     */
    public static final ParameterDescriptor<Double> SCALE_FACTOR;

    /**
     * The group of all parameters expected by this coordinate operation.
     */
    private static final ParameterDescriptorGroup PARAMETERS;
    static {
        final ParameterBuilder builder = builder();

        LATITUDE_OF_CENTRE = createLatitude(builder.addNamesAndIdentifiers(AlbersEqualArea.LATITUDE_OF_FALSE_ORIGIN)
                .reidentify(Citations.EPSG, "8811")
                .rename    (Citations.EPSG, "Latitude of projection centre")
                .addName   (Citations.ESRI, "Latitude_Of_Center")
                .rename    (Citations.NETCDF), false);                  // Remove the netCDF name.

        LONGITUDE_OF_CENTRE = createLongitude(builder.addNamesAndIdentifiers(AlbersEqualArea.LONGITUDE_OF_FALSE_ORIGIN)
                .reidentify(Citations.EPSG, "8812")
                .rename    (Citations.EPSG, "Longitude of projection centre")
                .addName   (Citations.ESRI, "Longitude_Of_Center")
                .rename    (Citations.NETCDF));                         // Remove the netCDF name.

        AZIMUTH = builder
                .addIdentifier("8813")
                .addIdentifier(Citations.GEOTIFF, "3094")
                .addName("Azimuth of initial line")
                .addName(Citations.OGC,     "azimuth")
                .addName(Citations.ESRI,    "Azimuth")
                .addName(Citations.GEOTIFF, "AzimuthAngle")
                .addName(Citations.PROJ4,   "alpha")
                .createBounded(-360, 360, Double.NaN, Units.DEGREE);

        RECTIFIED_GRID_ANGLE = builder
                .addIdentifier("8814")
                .addName("Angle from Rectified to Skew Grid")
                .addName(Citations.OGC,     "rectified_grid_angle")
                .addName(Citations.ESRI,    "XY_Plane_Rotation")
                .addName(Citations.GEOTIFF, "RectifiedGridAngle")
                .createBounded(-360, 360, Double.NaN, Units.DEGREE);

        SCALE_FACTOR = createScale(builder
                .addNamesAndIdentifiers(Mercator1SP.SCALE_FACTOR)
                .reidentify(Citations.EPSG,    "8815")
                .reidentify(Citations.GEOTIFF, "3093")
                .rename    (Citations.EPSG,    "Scale factor on initial line")
                .rename    (Citations.GEOTIFF, "ScaleAtCenter")
                .rename    (Citations.NETCDF));                                 // Remove the netCDF name.

        PARAMETERS = builder
                .addIdentifier(IDENTIFIER_A)
                .addName("Hotine Oblique Mercator (variant A)")                 // Starting from EPSG version 7.6
                .addName("Hotine Oblique Mercator")                             // Prior to EPSG version 7.6
                .addName      (Citations.OGC,     "Hotine_Oblique_Mercator")
                .addName      (Citations.ESRI,    "Hotine_Oblique_Mercator_Azimuth_Natural_Origin")
                .addName      (Citations.ESRI,    "Rectified_Skew_Orthomorphic_Natural_Origin")
                .addName      (Citations.S57,     "Hotine Oblique Mercator")
                .addName      (Citations.S57,     "HOM")
                .addIdentifier(Citations.S57,     "5")
                .addName      (Citations.GEOTIFF, "CT_ObliqueMercator_Hotine")
                .createGroupForMapProjection(
                        LATITUDE_OF_CENTRE,
                        LONGITUDE_OF_CENTRE,
                        AZIMUTH,
                        RECTIFIED_GRID_ANGLE,
                        SCALE_FACTOR,
                        FALSE_EASTING,
                        FALSE_NORTHING);
    }

    /**
     * Constructs a new provider.
     */
    public ObliqueMercator() {
        super(PARAMETERS);
    }

    /**
     * For subclass constructors only.
     */
    ObliqueMercator(final ParameterDescriptorGroup parameters) {
        super(parameters);
    }

    /**
     * {@inheritDoc}
     *
     * @return the map projection created from the given parameter values.
     */
    @Override
    protected final NormalizedProjection createProjection(final Parameters parameters) {
        return new org.apache.sis.referencing.operation.projection.ObliqueMercator(this, parameters);
   }
}
