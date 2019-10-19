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
import org.apache.sis.metadata.iso.citation.Citations;
import org.apache.sis.parameter.ParameterBuilder;
import org.apache.sis.measure.Latitude;
import org.apache.sis.measure.Units;


/**
 * The provider for <cite>"Stereographic North South"</cite> projection (ESRI).
 *
 * @author  Rueben Schulz (UBC)
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.0
 * @since   0.6
 * @module
 */
@XmlTransient
public final class PolarStereographicSouth extends AbstractStereographic {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = -6173635411676914083L;

    /**
     * The group of all parameters expected by this coordinate operation.
     */
    static final ParameterDescriptorGroup PARAMETERS;
    static {
        final ParameterBuilder builder = builder();
        final ParameterDescriptor<Double> standardParallel =
                ESRI.copyNames(builder, PolarStereographicB.STANDARD_PARALLEL)
                .createBounded(Latitude.MIN_VALUE, 0, Latitude.MIN_VALUE, Units.DEGREE);

        final ParameterDescriptor<?>[] parameters = {
            standardParallel,
            ESRI.CENTRAL_MERIDIAN,
            PolarStereographicB.SCALE_FACTOR,                       // Not formally a parameter of this projection.
            ESRI.FALSE_EASTING,
            ESRI.FALSE_NORTHING
        };

        PARAMETERS = builder
                .addName(Citations.ESRI, "Stereographic_South_Pole")
                .createGroupForMapProjection(parameters);
    }

    /**
     * Constructs a new provider.
     */
    public PolarStereographicSouth() {
        super(PARAMETERS);
    }
}
