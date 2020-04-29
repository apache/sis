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
import org.opengis.referencing.operation.CylindricalProjection;
import org.apache.sis.parameter.Parameters;
import org.apache.sis.referencing.operation.projection.NormalizedProjection;
import org.apache.sis.referencing.operation.projection.CylindricalEqualArea;


/**
 * The provider for <cite>"Lambert Cylindrical Equal Area (Spherical)"</cite> projection (EPSG:9834).
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 0.8
 * @since   0.8
 * @module
 */
@XmlTransient
public final class LambertCylindricalEqualAreaSpherical extends MapProjection {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = 1456941129750586197L;

    /**
     * The EPSG identifier, to be preferred to the name when available.
     */
    public static final String IDENTIFIER = "9834";

    /**
     * The group of all parameters expected by this coordinate operation.
     */
    private static final ParameterDescriptorGroup PARAMETERS;
    static {
        PARAMETERS = builder()
                .addIdentifier(IDENTIFIER)
                .addName("Lambert Cylindrical Equal Area (Spherical)")
                .createGroupForMapProjection(
                        LambertCylindricalEqualArea.STANDARD_PARALLEL,
                        LambertCylindricalEqualArea.LONGITUDE_OF_ORIGIN,
                        LambertCylindricalEqualArea.SCALE_FACTOR,   // Not formally a Cylindrical Equal Area parameter.
                        LambertCylindricalEqualArea.FALSE_EASTING,
                        LambertCylindricalEqualArea.FALSE_NORTHING);
    }

    /**
     * Constructs a new provider.
     */
    public LambertCylindricalEqualAreaSpherical() {
        super(PARAMETERS);
    }

    /**
     * Returns the operation type for this map projection.
     *
     * @return {@code CylindricalProjection.class}
     */
    @Override
    public final Class<CylindricalProjection> getOperationType() {
        return CylindricalProjection.class;
    }

    /**
     * {@inheritDoc}
     *
     * @return the map projection created from the given parameter values.
     */
    @Override
    protected NormalizedProjection createProjection(final Parameters parameters) {
        return new CylindricalEqualArea(this, parameters);
    }
}
