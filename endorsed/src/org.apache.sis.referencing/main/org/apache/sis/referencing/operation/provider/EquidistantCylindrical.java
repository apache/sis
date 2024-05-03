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

import jakarta.xml.bind.annotation.XmlTransient;
import org.opengis.parameter.ParameterDescriptorGroup;
import org.apache.sis.parameter.Parameters;
import org.apache.sis.parameter.ParameterBuilder;
import org.apache.sis.metadata.iso.citation.Citations;
import org.apache.sis.referencing.operation.projection.NormalizedProjection;


/**
 * The provider for <q>Equidistant Cylindrical</q> projection
 * (EPSG:1028, <span class="deprecated">EPSG:9842</span>).
 *
 * <h2>Note</h2>
 * EPSG:1028 is the current codes, while EPSG:9842 is a deprecated code.
 * The new and deprecated definitions differ only by their parameters. In the Apache SIS implementation,
 * both current and legacy definitions are known, but the legacy names are marked as deprecated.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
@XmlTransient
public final class EquidistantCylindrical extends MapProjection {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = -1180656445349342258L;

    /**
     * The group of all parameters expected by this coordinate operation.
     */
    private static final ParameterDescriptorGroup PARAMETERS;
    static {
        final ParameterBuilder builder = builder();
        PARAMETERS = addIdentifierAndLegacy(builder, "1028", "9842")    // 9842 uses deprecated parameter names.
                .addName(                   "Equidistant Cylindrical")
                .addName(Citations.ESRI,    "Equidistant_Cylindrical")
                .addName(Citations.GEOTIFF, "CT_Equirectangular")
                .addName(Citations.PROJ4,   "eqc")
                .addIdentifier(Citations.GEOTIFF, "17")
                .createGroupForMapProjection(
                        Equirectangular.STANDARD_PARALLEL,
                        Equirectangular.LATITUDE_OF_ORIGIN,     // Not formally an Equidistant Cylindrical parameter.
                        Equirectangular.LONGITUDE_OF_ORIGIN,
                        Equirectangular.FALSE_EASTING,
                        Equirectangular.FALSE_NORTHING);
    }

    /**
     * Constructs a new provider.
     */
    public EquidistantCylindrical() {
        super(PARAMETERS);
    }

    /**
     * Creates a map projection on an ellipsoid having a semi-major axis length of 1.
     *
     * @return the map projection created from the given parameter values.
     */
    @Override
    protected NormalizedProjection createProjection(final Parameters parameters) {
        return new org.apache.sis.referencing.operation.projection.EquidistantCylindrical(this, parameters);
    }
}
