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
import org.apache.sis.parameter.ParameterBuilder;
import org.apache.sis.metadata.iso.citation.Citations;


/**
 * The provider for <cite>"Lambert Conic Conformal (1SP)"</cite> projection (EPSG:9801).
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @author  Rueben Schulz (UBC)
 * @since   0.6
 * @version 0.6
 * @module
 *
 * @see <a href="http://www.remotesensing.org/geotiff/proj_list/lambert_conic_conformal_1sp.html">Lambert Conic Conformal 1SP on RemoteSensing.org</a>
 */
public final class LambertConformal1SP extends AbstractLambert {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = -4243116402872545772L;

    /**
     * The operation parameter descriptor for the <cite>Latitude of natural origin</cite> (φ₀) parameter value.
     * Valid values range is [-90 … 90]° and default value is 0°.
     */
    public static final ParameterDescriptor<Double> LATITUDE_OF_ORIGIN = Mercator1SP.LATITUDE_OF_ORIGIN;

    /**
     * The operation parameter descriptor for the <cite>Longitude of natural origin</cite> (λ₀) parameter value.
     * Valid values range is [-180 … 180]° and default value is 0°.
     */
    public static final ParameterDescriptor<Double> CENTRAL_MERIDIAN = Mercator1SP.CENTRAL_MERIDIAN;

    /**
     * The operation parameter descriptor for the <cite>Scale factor at natural origin</cite> (k₀) parameter value.
     * Valid values range is (0 … ∞) and default value is 1.
     */
    public static final ParameterDescriptor<Double> SCALE_FACTOR = Mercator1SP.SCALE_FACTOR;

    /**
     * The group of all parameters expected by this coordinate operation.
     */
    static final ParameterDescriptorGroup PARAMETERS;
    static {
        final ParameterBuilder builder = builder();
        PARAMETERS = builder
            .addIdentifier(              "9801")
            .addName(                    "Lambert Conic Conformal (1SP)")
            .addName(Citations.OGC,      "Lambert_Conformal_Conic_1SP")
            .addName(Citations.GEOTIFF,  "CT_LambertConfConic_1SP")
            .addName(Citations.PROJ4,    "lcc")
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
    public LambertConformal1SP() {
        super(PARAMETERS);
    }
}
