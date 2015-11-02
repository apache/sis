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
import org.apache.sis.metadata.iso.citation.Citations;
import org.apache.sis.parameter.ParameterBuilder;


/**
 * The provider for <cite>"Polar Stereographic (Variant A)"</cite> projection (EPSG:9810).
 *
 * @author  Rueben Schulz (UBC)
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.6
 * @version 0.7
 * @module
 *
 * @see <a href="http://www.remotesensing.org/geotiff/proj_list/polar_stereographic.html">Polar Stereographic on RemoteSensing.org</a>
 */
@XmlTransient
public final class PolarStereographicA extends AbstractStereographic {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = 538262714055500925L;

    /**
     * The EPSG name for this projection.
     */
    public static final String NAME = "Polar Stereographic (variant A)";

    /**
     * The EPSG identifier, to be preferred to the name when available.
     */
    public static final String IDENTIFIER = "9810";

    /**
     * The operation parameter descriptor for the <cite>Latitude of natural origin</cite> (φ₀) parameter value.
     * Valid values can be -90° or 90° only. There is no default value.
     */
    public static final ParameterDescriptor<Double> LATITUDE_OF_ORIGIN = LambertConformal1SP.LATITUDE_OF_ORIGIN;

    /**
     * The operation parameter descriptor for the <cite>Longitude of natural origin</cite> (λ₀) parameter value.
     * Valid values range is [-180 … 180]° and default value is 0°.
     */
    public static final ParameterDescriptor<Double> LONGITUDE_OF_ORIGIN;

    /**
     * The group of all parameters expected by this coordinate operation.
     */
    private static final ParameterDescriptorGroup PARAMETERS;
    static {
        final ParameterBuilder builder = builder();
        LONGITUDE_OF_ORIGIN = createLongitude(builder
                .addNamesAndIdentifiers(ObliqueStereographic.LONGITUDE_OF_ORIGIN)
                .rename(Citations.GEOTIFF, "StraightVertPoleLong"));

        PARAMETERS = builder
                .addIdentifier(             IDENTIFIER)
                .addName(                   NAME)
                .addName(Citations.OGC,     "Polar_Stereographic")
                .addName(Citations.GEOTIFF, "CT_PolarStereographic")
                .addName(Citations.PROJ4,   "stere")
                .addIdentifier(Citations.GEOTIFF, "15")
                .createGroupForMapProjection(
                        LATITUDE_OF_ORIGIN,     // Can be only ±90°
                        LONGITUDE_OF_ORIGIN,
                        SCALE_FACTOR,
                        FALSE_EASTING,
                        FALSE_NORTHING);
    }

    /**
     * Constructs a new provider.
     */
    public PolarStereographicA() {
        super(PARAMETERS);
    }
}
