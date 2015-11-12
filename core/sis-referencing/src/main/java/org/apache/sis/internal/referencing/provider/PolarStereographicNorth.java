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

import java.util.List;
import javax.measure.unit.NonSI;
import javax.xml.bind.annotation.XmlTransient;
import org.opengis.parameter.ParameterDescriptor;
import org.opengis.parameter.ParameterDescriptorGroup;
import org.opengis.parameter.GeneralParameterDescriptor;
import org.apache.sis.metadata.iso.citation.Citations;
import org.apache.sis.parameter.ParameterBuilder;
import org.apache.sis.measure.Latitude;


/**
 * The provider for <cite>"Stereographic North Pole"</cite> projection (ESRI).
 *
 * @author  Rueben Schulz (UBC)
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.6
 * @version 0.6
 * @module
 */
@XmlTransient
public final class PolarStereographicNorth extends AbstractStereographic {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = -5112694856914399464L;

    /**
     * The group of all parameters expected by this coordinate operation.
     */
    private static final ParameterDescriptorGroup PARAMETERS;
    static {
        List<GeneralParameterDescriptor> sp = PolarStereographicSouth.PARAMETERS.descriptors();
        sp = sp.subList(2, sp.size());  // Skip the "semi-major" and "semi-minor" parameters.
        @SuppressWarnings("SuspiciousToArrayCall") // We know PolarStereographicSouth content.
        ParameterDescriptor<?>[] parameters = sp.toArray(new ParameterDescriptor<?>[sp.size()]);

        // Replace the "Standard Parallel" parameter from [-90 … 0]° domain to [0 … 90]° domain.
        final ParameterBuilder builder = builder();
        parameters[0] = builder.addNamesAndIdentifiers(parameters[0]).createBounded(
                       0, Latitude.MAX_VALUE, Latitude.MAX_VALUE, NonSI.DEGREE_ANGLE);

        PARAMETERS = builder
                .addName(Citations.ESRI, "Stereographic_North_Pole")
                .createGroupForMapProjection(parameters);
    }

    /**
     * Constructs a new provider.
     */
    public PolarStereographicNorth() {
        super(PARAMETERS);
    }
}
