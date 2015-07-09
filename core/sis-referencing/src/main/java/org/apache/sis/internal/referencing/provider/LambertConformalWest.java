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
 * The provider for <cite>Lambert Conic Conformal (West Orientated)"</cite> projection (EPSG:9826).
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.6
 * @version 0.6
 * @module
 */
public final class LambertConformalWest extends AbstractLambert {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = -6226753337274190088L;

    /**
     * The EPSG identifier, to be preferred to the name when available.
     */
    public static final String IDENTIFIER = "9826";

    /**
     * The group of all parameters expected by this coordinate operation.
     */
    static final ParameterDescriptorGroup PARAMETERS;
    static {
        final ParameterBuilder builder = builder();

        PARAMETERS = builder
            .addIdentifier(IDENTIFIER)
            .addName("Lambert Conic Conformal (West Orientated)")
            .createGroupForMapProjection(
                    LambertConformal1SP.LATITUDE_OF_ORIGIN,
                    LambertConformal1SP.CENTRAL_MERIDIAN,
                    LambertConformal1SP.SCALE_FACTOR,
                    LambertConformal1SP.FALSE_EASTING,
                    LambertConformal1SP.FALSE_NORTHING);
    }

    /**
     * Constructs a new provider.
     */
    public LambertConformalWest() {
        super(PARAMETERS);
    }
}
