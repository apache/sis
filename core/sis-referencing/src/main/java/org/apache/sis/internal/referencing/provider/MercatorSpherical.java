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


/**
 * The provider for <cite>"Mercator (Spherical)"</cite> projection (EPSG:1026, <span class="deprecated">EPSG:9841</span>).
 * This provider reuses all parameters defined in {@link Mercator2SP}, except that the standard parallel is made optional
 * since it is not formally a parameter of this projection.
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @author  Rueben Schulz (UBC)
 * @since   0.6
 * @version 0.6
 * @module
 */
@XmlTransient
public final class MercatorSpherical extends AbstractMercator {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = 4761755206841656129L;

    /**
     * The EPSG identifier, to be preferred to the name when available.
     */
    public static final String IDENTIFIER = "1026";

    /**
     * The group of all parameters expected by this coordinate operation.
     */
    static final ParameterDescriptorGroup PARAMETERS;
    static {
        final ParameterBuilder builder = builder();
        /*
         * The scale factor is used by Mercator1SP and is not formally a parameter of "Mercator (Spherical)" projection.
         * Nevertheless we declare it as an optional parameter because it was used in EPSG:7.9:3785 (the legacy "Popular
         * Visualisation CRS / Mercator", now deprecated). But at the difference of what we did in Mercator2SP, we keep
         * the EPSG name here since there is no "standard parallel" parameter.  So the "Scale factor at natural origin"
         * parameter name still okay provided that the "Latitude of natural origin" parameter value stay zero (which is
         * normally enforced by the Mercator1SP.LATITUDE_OF_ORIGIN minimum and maximum values).
         */
        final ParameterDescriptor<Double> scaleFactor = createScale(builder
                .addNamesAndIdentifiers(Mercator1SP.SCALE_FACTOR)
                .setRemarks(Mercator2SP.SCALE_FACTOR.getRemarks())
                .setRequired(false));

        PARAMETERS = addNameAndLegacy(addIdentifierAndLegacy(builder, IDENTIFIER, "9841"),
                "Mercator (Spherical)", "Mercator (1SP) (Spherical)")   // "Mercator (Spherical)" starting from EPSG version 7.6
                .createGroupForMapProjection(
                        Mercator1SP.LATITUDE_OF_ORIGIN,
                        Mercator1SP.LONGITUDE_OF_ORIGIN,
                        scaleFactor,
                        FALSE_EASTING,
                        FALSE_NORTHING);
    }

    /**
     * Constructs a new provider.
     */
    public MercatorSpherical() {
        super(PARAMETERS);
    }
}
