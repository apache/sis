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
package org.apache.sis.referencing.operation.provider;

import java.util.Arrays;
import jakarta.xml.bind.annotation.XmlTransient;
import org.opengis.parameter.ParameterDescriptorGroup;


/**
 * The provider for <cite>"Geocentric translations (geog3D domain)"</cite> (EPSG:1035).
 * This is a special case of {@link PositionVector7Param3D} where only the translation
 * terms can be set to a non-null value.
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 */
@XmlTransient
public final class GeocentricTranslation3D extends GeocentricAffineBetweenGeographic {
    /**
     * Serial number for inter-operability with different versions.
     */
    private static final long serialVersionUID = 2208429505407588276L;

    /**
     * The group of all parameters expected by this coordinate operation.
     */
    static final ParameterDescriptorGroup PARAMETERS;
    static {
        PARAMETERS = builder()
                .addIdentifier("1035")
                .addName("Geocentric translations (geog3D domain)")
                .createGroupWithSameParameters(GeocentricTranslation2D.PARAMETERS);
    }

    /**
     * The providers for all combinations between 2D and 3D cases.
     */
    static final GeocentricAffineBetweenGeographic[] REDIMENSIONED = new GeocentricAffineBetweenGeographic[4];
    static {
        Arrays.setAll(REDIMENSIONED, (i) -> (i == INDEX_OF_2D)
                ? new GeocentricTranslation2D(i)
                : new GeocentricTranslation3D(i));
    }

    /**
     * Returns the provider for the specified combination of source and target dimensions.
     */
    @Override
    final GeodeticOperation redimensioned(int indexOfDim) {
        return REDIMENSIONED[indexOfDim];
    }

    /**
     * Creates a copy of this provider.
     *
     * @deprecated This is a temporary constructor before replacement by a {@code provider()} method with JDK9.
     */
    @Deprecated
    public GeocentricTranslation3D() {
        super(REDIMENSIONED[INDEX_OF_3D]);
    }

    /**
     * Constructs a provider for the given dimensions.
     *
     * @param indexOfDim  number of dimensions as the index in {@code redimensioned} array.
     */
    private GeocentricTranslation3D(int indexOfDim) {
        super(Type.TRANSLATION, PARAMETERS, indexOfDim);
    }
}
