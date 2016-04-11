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
import org.opengis.parameter.ParameterDescriptorGroup;


/**
 * The provider for <cite>"Geocentric translations (geog2D domain)"</cite> (EPSG:9603).
 * This is a special case of {@link PositionVector7Param2D} where only the translation
 * terms can be set to a non-null value.
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @since   0.7
 * @version 0.7
 * @module
 */
@XmlTransient
public final class GeocentricTranslation2D extends GeocentricAffineBetweenGeographic {
    /**
     * Serial number for inter-operability with different versions.
     */
    private static final long serialVersionUID = -7160250630666911608L;

    /**
     * The group of all parameters expected by this coordinate operation.
     */
    static final ParameterDescriptorGroup PARAMETERS;
    static {
        PARAMETERS = builder()
                .addIdentifier("9603")
                .addName("Geocentric translations (geog2D domain)")
                .createGroup(SRC_SEMI_MAJOR,
                             SRC_SEMI_MINOR,
                             TGT_SEMI_MAJOR,
                             TGT_SEMI_MINOR,
                             TX, TY, TZ);
    }

    /**
     * Constructs the provider.
     */
    public GeocentricTranslation2D() {
        this(null);
    }

    /**
     * Constructs a provider that can be resized.
     */
    GeocentricTranslation2D(GeodeticOperation[] redimensioned) {
        super(2, 2, PARAMETERS, redimensioned);
    }

    /**
     * Returns the three-dimensional variant of this operation method.
     */
    @Override
    Class<GeocentricTranslation3D> variant3D() {
        return GeocentricTranslation3D.class;
    }

    /**
     * Returns the type of this operation.
     */
    @Override
    int getType() {
        return TRANSLATION;
    }
}
