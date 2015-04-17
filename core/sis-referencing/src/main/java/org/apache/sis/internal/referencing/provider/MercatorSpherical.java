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
        PARAMETERS = builder
            .addIdentifier(IDENTIFIER)
            .addDeprecatedIdentifier("9841", IDENTIFIER)
            .addName("Mercator (Spherical)")                                          // Starting from EPSG version 7.6
            .addDeprecatedName("Mercator (1SP) (Spherical)", "Mercator (Spherical)")  // Prior to EPSG version 7.6
            .createGroupForMapProjection(
                    Mercator1SP.LATITUDE_OF_ORIGIN,
                    Mercator1SP.CENTRAL_MERIDIAN,
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
