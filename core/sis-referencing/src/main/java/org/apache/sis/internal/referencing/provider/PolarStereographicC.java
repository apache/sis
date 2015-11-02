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


/**
 * The provider for <cite>"Polar Stereographic (Variant C)"</cite> projection (EPSG:9830).
 * This is very similar to variant B except in the way to use the false northing.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.6
 * @version 0.6
 * @module
 */
@XmlTransient
public final class PolarStereographicC extends AbstractStereographic {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = 9094868630348858992L;

    /**
     * The EPSG identifier, to be preferred to the name when available.
     */
    public static final String IDENTIFIER = "9830";

    /**
     * The operation parameter descriptor for the <cite>Easting at false origin</cite> (Ef) parameter value.
     * Valid values range is unrestricted and default value is 0 metre.
     */
    public static final ParameterDescriptor<Double> EASTING_AT_FALSE_ORIGIN = RegionalMercator.EASTING_AT_FALSE_ORIGIN;

    /**
     * The operation parameter descriptor for the <cite>Northing at false origin</cite> (Nf) parameter value.
     * Valid values range is unrestricted and default value is 0 metre.
     */
    public static final ParameterDescriptor<Double> NORTHING_AT_FALSE_ORIGIN = RegionalMercator.NORTHING_AT_FALSE_ORIGIN;

    /**
     * The group of all parameters expected by this coordinate operation.
     */
    private static final ParameterDescriptorGroup PARAMETERS;
    static {
        PARAMETERS = builder()
                .addIdentifier(IDENTIFIER)
                .addName("Polar Stereographic (variant C)")
                .createGroupForMapProjection(
                        PolarStereographicB.STANDARD_PARALLEL,
                        PolarStereographicB.LONGITUDE_OF_ORIGIN,
                        PolarStereographicB.SCALE_FACTOR,       // Not formally a parameter of this projection.
                        EASTING_AT_FALSE_ORIGIN,
                        NORTHING_AT_FALSE_ORIGIN);
    }

    /**
     * Constructs a new provider.
     */
    public PolarStereographicC() {
        super(PARAMETERS);
    }
}
