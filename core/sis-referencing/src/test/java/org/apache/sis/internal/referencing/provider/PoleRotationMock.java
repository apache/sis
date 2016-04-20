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
import org.opengis.parameter.ParameterDescriptor;
import org.opengis.parameter.ParameterDescriptorGroup;
import org.apache.sis.parameter.ParameterBuilder;


/**
 * The provider for <cite>"Pole rotation"</cite> conversion.
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
public final strictfp class PoleRotationMock extends ProviderMock {
    /**
     * The group of all parameters expected by this coordinate operation.
     */
    private static final ParameterDescriptorGroup PARAMETERS;
    static {
        final ParameterBuilder builder = new ParameterBuilder().setRequired(true);
        final ParameterDescriptor<?>[] parameters = {
            createLatitude (builder.addName("Latitude of rotated pole"), true),
            createLongitude(builder.addName("Longitude of rotated pole")),
                            builder.addName("Axis rotation").create(Double.NaN, NonSI.DEGREE_ANGLE)
        };
        PARAMETERS = builder.addName("Pole rotation").createGroup(parameters);
    }

    /**
     * Creates a new <cite>"Pole rotation"</cite> operation method.
     */
    public PoleRotationMock() {
        super(2, 2, PARAMETERS);
    }
}
