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

import javax.measure.unit.NonSI;
import javax.xml.bind.annotation.XmlTransient;
import org.opengis.parameter.ParameterDescriptor;
import org.opengis.parameter.ParameterDescriptorGroup;
import org.apache.sis.metadata.iso.citation.Citations;
import org.apache.sis.parameter.ParameterBuilder;
import org.apache.sis.parameter.DefaultParameterDescriptor;
import org.apache.sis.measure.Latitude;
import org.apache.sis.measure.MeasurementRange;


/**
 * The provider for <cite>"Stereographic North South"</cite> projection (ESRI).
 *
 * @author  Rueben Schulz (UBC)
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.6
 * @version 0.7
 * @module
 */
@XmlTransient
public final class PolarStereographicSouth extends AbstractStereographic {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = -6173635411676914083L;

    /**
     * Copies all names and identifiers, but using the ESRI authority as the primary name.
     * This is a convenience method for defining the parameters of an ESRI-specific projection
     * using the EPSG parameters as template.
     */
    private static ParameterBuilder addNamesAndIdentifiers(final ParameterDescriptor<Double> source, final ParameterBuilder builder) {
        return except(source, Citations.ESRI, null, builder.addName(sameNameAs(Citations.ESRI, source)).addName(source.getName()));
    }

    /**
     * Returns the same parameter than the given one, except that the primary name is the ESRI name
     * instead than the EPSG one.
     */
    @SuppressWarnings("unchecked")
    private static ParameterDescriptor<Double> forESRI(final ParameterDescriptor<Double> source, final ParameterBuilder builder) {
        return addNamesAndIdentifiers(source, builder).createBounded((MeasurementRange<Double>)
                ((DefaultParameterDescriptor<Double>) source).getValueDomain(), source.getDefaultValue());
    }

    /**
     * The group of all parameters expected by this coordinate operation.
     */
    static final ParameterDescriptorGroup PARAMETERS;
    static {
        final ParameterBuilder builder = builder();
        final ParameterDescriptor<?>[] parameters = {
            addNamesAndIdentifiers(PolarStereographicB.STANDARD_PARALLEL, builder)
                   .createBounded(Latitude.MIN_VALUE, 0, Latitude.MIN_VALUE, NonSI.DEGREE_ANGLE),

            forESRI(PolarStereographicB.LONGITUDE_OF_ORIGIN, builder),
            forESRI(PolarStereographicB.SCALE_FACTOR, builder),
            forESRI(PolarStereographicB.FALSE_EASTING, builder),
            forESRI(PolarStereographicB.FALSE_NORTHING, builder)
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
