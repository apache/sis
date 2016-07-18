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
import org.opengis.referencing.operation.CylindricalProjection;
import org.apache.sis.metadata.iso.citation.Citations;
import org.apache.sis.parameter.Parameters;
import org.apache.sis.referencing.operation.projection.NormalizedProjection;
import org.apache.sis.referencing.operation.projection.CylindricalEqualArea;


/**
 * The provider for <cite>"Lambert Cylindrical Equal Area"</cite> projection (EPSG:9835).
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.8
 * @version 0.8
 * @module
 *
 * @see <a href="http://www.remotesensing.org/geotiff/proj_list/cylindrical_equal_area.html">Cylindrical Equal Area on RemoteSensing.org</a>
 */
@XmlTransient
public final class LambertCylindricalEqualArea extends MapProjection {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = -672278344635217838L;

    /**
     * The operation parameter descriptor for the <cite>Latitude of 1st standard parallel</cite> (φ₁) parameter value.
     * Valid values range is (-90 … 90)° and default value is 0°.
     */
    public static final ParameterDescriptor<Double> STANDARD_PARALLEL = Equirectangular.STANDARD_PARALLEL;

    /**
     * The operation parameter descriptor for the <cite>Longitude of natural origin</cite> (λ₀) parameter value.
     * Valid values range is [-180 … 180]° and default value is 0°.
     */
    public static final ParameterDescriptor<Double> LONGITUDE_OF_ORIGIN = Mercator1SP.LONGITUDE_OF_ORIGIN;

    /**
     * The operation parameter descriptor for the <cite>False easting</cite> (FE) parameter value.
     * Valid values range is unrestricted and default value is 0 metre.
     */
    public static final ParameterDescriptor<Double> FALSE_EASTING = Equirectangular.FALSE_EASTING;

    /**
     * The operation parameter descriptor for the <cite>False northing</cite> (FN) parameter value.
     * Valid values range is unrestricted and default value is 0 metre.
     */
    public static final ParameterDescriptor<Double> FALSE_NORTHING = Equirectangular.FALSE_NORTHING;

    /**
     * The group of all parameters expected by this coordinate operation.
     */
    static final ParameterDescriptorGroup PARAMETERS;
    static {
        PARAMETERS = builder()
                .addIdentifier(             "9835")
                .addName(                   "Lambert Cylindrical Equal Area")
                .addName(Citations.OGC,     "Cylindrical_Equal_Area")
                .addName(Citations.ESRI,    "Cylindrical_Equal_Area")
                .addName(Citations.GEOTIFF, "CT_CylindricalEqualArea")
                .addName(Citations.PROJ4,   "cea")
                .addIdentifier(Citations.GEOTIFF, "28")
                .createGroupForMapProjection(
                        STANDARD_PARALLEL,
                        LONGITUDE_OF_ORIGIN,
                        Mercator2SP.SCALE_FACTOR,           // Not formally a Cylindrical Equal Area parameter.
                        FALSE_EASTING,
                        FALSE_NORTHING);
    }

    /**
     * Constructs a new provider.
     */
    public LambertCylindricalEqualArea() {
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
     * @return The map projection created from the given parameter values.
     */
    @Override
    protected NormalizedProjection createProjection(final Parameters parameters) {
        return new CylindricalEqualArea(this, parameters);
    }
}
