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
import org.apache.sis.parameter.ParameterBuilder;
import org.opengis.parameter.ParameterDescriptor;
import org.opengis.parameter.ParameterDescriptorGroup;


/**
 * The provider for "<cite>Lambert Azimuthal Equal Area (Spherical)</cite>" projection (EPSG:1027).
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.2
 * @since   1.2
 * @module
 */
@XmlTransient
public final class LambertAzimuthalEqualAreaSpherical extends LambertAzimuthalEqualArea {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = -8356440804799174833L;

    /**
     * The EPSG identifier, to be preferred to the name when available.
     */
    public static final String IDENTIFIER = "1027";

    /**
     * The group of all parameters expected by this coordinate operation.
     */
    static final ParameterDescriptorGroup PARAMETERS;
    static {
        final ParameterBuilder builder = builder();

        final ParameterDescriptor<Double> latitudeOfOrigin = createLatitude(builder
                .addNamesAndIdentifiers(LATITUDE_OF_ORIGIN).setDeprecated(true)
                .addName("Spherical latitude of origin"), true);

        final ParameterDescriptor<Double> longitudeOfOrigin = createLongitude(builder
                .addNamesAndIdentifiers(LONGITUDE_OF_ORIGIN).setDeprecated(true)
                .addName("Spherical longitude of origin"));

        PARAMETERS = builder
                .addIdentifier(IDENTIFIER)
                .addName("Lambert Azimuthal Equal Area (Spherical)")
                .setDeprecated(true).addIdentifier("9821").setDeprecated(false)
                .createGroupForMapProjection(
                        latitudeOfOrigin,
                        longitudeOfOrigin,
                        FALSE_EASTING,
                        FALSE_NORTHING);
    }

    /**
     * Constructs a new provider.
     */
    public LambertAzimuthalEqualAreaSpherical() {
        super(PARAMETERS);
    }
}
