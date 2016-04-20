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

import javax.measure.unit.SI;
import javax.measure.unit.NonSI;
import javax.measure.unit.Unit;
import org.opengis.parameter.ParameterDescriptor;
import org.opengis.parameter.ParameterDescriptorGroup;
import org.apache.sis.parameter.ParameterBuilder;


/**
 * The provider for <cite>"P6 (I = J-90°) seismic bin grid transformation"</cite> transformation (EPSG:1049).
 *
 * This conversion is not yet implemented in Apache SIS, but we need to at least accept the parameters
 * for a Well Known Text (WKT) parsing test in the {@link org.apache.sis.io.wkt.WKTParserTest} class.
 *
 * <p>This class may be promoted to a real operation if we implement the formulas in a future Apache SIS version.</p>
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.6
 * @version 0.6
 * @module
 */
@SuppressWarnings("serial")
public final strictfp class SeismicBinGridMock extends ProviderMock {
    /**
     * The group of all parameters expected by this coordinate operation.
     */
    private static final ParameterDescriptorGroup PARAMETERS;
    static {
        final ParameterBuilder builder = builder();
        final ParameterDescriptor<?>[] parameters = {
            builder.addIdentifier("8733").addName("Bin grid origin I").create(Double.NaN, Unit.ONE),
            builder.addIdentifier("8734").addName("Bin grid origin J").create(Double.NaN, Unit.ONE),
            builder.addIdentifier("8735").addName("Bin grid origin Easting").create(Double.NaN, SI.METRE),
            builder.addIdentifier("8736").addName("Bin grid origin Northing").create(Double.NaN, SI.METRE),
            builder.addIdentifier("8737").addName("Scale factor of bin grid").create(Double.NaN, Unit.ONE),
            builder.addIdentifier("8738").addName("Bin width on I-axis").create(Double.NaN, SI.METRE),
            builder.addIdentifier("8739").addName("Bin width on J-axis").create(Double.NaN, SI.METRE),
            builder.addIdentifier("8740").addName("Map grid bearing of bin grid J-axis").create(Double.NaN, NonSI.DEGREE_ANGLE),
            builder.addIdentifier("8741").addName("Bin node increment on I-axis").create(Double.NaN, Unit.ONE),
            builder.addIdentifier("8742").addName("Bin node increment on J-axis").create(Double.NaN, Unit.ONE)
        };
        PARAMETERS = builder
                .addIdentifier("1049")
                .addName("P6 (I = J-90°) seismic bin grid transformation")
                .createGroup(parameters);
    }

    /**
     * Creates a new <cite>"Pole rotation"</cite> operation method.
     */
    public SeismicBinGridMock() {
        super(2, 2, PARAMETERS);
    }
}
